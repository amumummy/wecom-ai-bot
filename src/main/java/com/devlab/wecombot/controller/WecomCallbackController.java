package com.devlab.wecombot.controller;

import com.devlab.wecombot.config.WecomProperties;
import com.devlab.wecombot.crypto.AesException;
import com.devlab.wecombot.crypto.WXBizMsgCrypt;
import com.devlab.wecombot.service.CozeClient;
import com.devlab.wecombot.service.MessageDeduper;
import com.devlab.wecombot.service.ReplyMsgParser;
import com.devlab.wecombot.service.WecomClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 企微回调入口。
 *
 * GET  /wecom/callback —— URL 验证（企微后台点保存时触发，必须原样返回解密后的 echostr）
 * POST /wecom/callback —— 消息推送：立即回空串（先应答，规避5秒超时），异步走 AI 链路：
 *        解析消息 → MsgId 去重 → 调 Coze v3/chat SSE → gettoken + message/send 主动回复
 */
@RestController
@RequestMapping("/wecom")
public class WecomCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WecomCallbackController.class);
    private static final String FALLBACK = "我这边思考超时了，请再发一次试试~";

    private final WXBizMsgCrypt crypt;
    private final MessageDeduper deduper;
    private final CozeClient coze;
    private final WecomClient wecom;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    public WecomCallbackController(WecomProperties props,
                                   MessageDeduper deduper,
                                   CozeClient coze,
                                   WecomClient wecom) throws AesException {
        // fail-fast：Token / AESKey 未配置时启动即报错，避免带病运行
        this.crypt = new WXBizMsgCrypt(props.getToken(), props.getAesKey(), props.getCorpid());
        this.deduper = deduper;
        this.coze = coze;
        this.wecom = wecom;
        log.info("WXBizMsgCrypt initialized, corpid={}", props.getCorpid());
    }

    @GetMapping("/callback")
    public String verify(@RequestParam("msg_signature") String msgSignature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam("echostr") String echostr) {
        try {
            String plain = crypt.VerifyURL(msgSignature, timestamp, nonce, echostr);
            log.info("[URL验证] 通过, echostr明文长度={}", plain.length());
            return plain;
        } catch (AesException e) {
            log.error("[URL验证] 失败 code={}, msg={}", e.getCode(), e.getMessage());
            return "fail";
        }
    }

    @PostMapping("/callback")
    public String receive(@RequestParam("msg_signature") String msgSignature,
                          @RequestParam("timestamp") String timestamp,
                          @RequestParam("nonce") String nonce,
                          @RequestBody String encryptedXml) {
        long start = System.currentTimeMillis();
        try {
            String plainXml = crypt.DecryptMsg(msgSignature, timestamp, nonce, encryptedXml);
            log.info("[消息推送] 解密耗时 {}ms", System.currentTimeMillis() - start);

            ReplyMsgParser.ReplyMsg msg = ReplyMsgParser.parse(plainXml);
            if (!"text".equals(msg.msgType)) {
                log.info("[消息推送] 非文本消息 type={} 忽略", msg.msgType);
                return "";
            }
            if (!deduper.firstSeen(msg.msgId)) {
                return "";
            }
            final String user = msg.fromUser;
            final String question = msg.content;
            log.info("[AI链路] user={} question={}", user, question);

            // 先应答（本方法返回空串），异步处理 AI 链路
            pool.submit(() -> handleAi(user, question));
            return "";
        } catch (AesException e) {
            log.error("[消息推送] 解密失败 code={}, msg={}", e.getCode(), e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("[消息推送] 处理异常", e);
            return "";
        }
    }

    private void handleAi(String user, String question) {
        String answer;
        try {
            answer = coze.chat(user, question);
        } catch (Exception e) {
            log.error("[AI链路] Coze 调用失败 question=" + question, e);
            answer = FALLBACK;
        }
        wecom.sendText(user, answer);
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdown();
    }
}
