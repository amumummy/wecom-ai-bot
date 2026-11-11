package com.devlab.wecombot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 企业微信服务端 API 客户端：gettoken（带缓存）+ message/send 主动回复。
 * 文档：https://developer.work.weixin.qq.com/document/path/91039
 */
@Component
public class WecomClient {

    private static final Logger log = LoggerFactory.getLogger(WecomClient.class);
    private static final ObjectMapper M = new ObjectMapper();

    @Value("${wecom.corpid}")
    private String corpid;
    @Value("${wecom.secret}")
    private String secret;
    @Value("${wecom.agent-id}")
    private int agentId;

    private volatile String accessToken;
    private volatile long tokenExpireAt = 0;

    /** 获取 access_token，提前 60 秒刷新 */
    public synchronized String getToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireAt - 60_000L) {
            return accessToken;
        }
        String urlStr = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s",
                urlEncode(corpid), urlEncode(secret));
        String body = httpGet(urlStr);
        JsonNode n = M.readTree(body);
        int errcode = n.path("errcode").asInt(0);
        if (errcode != 0) {
            throw new IllegalStateException("gettoken 失败 errcode=" + errcode + " errmsg=" + n.path("errmsg").asText());
        }
        accessToken = n.path("access_token").asText();
        long expiresIn = n.path("expires_in").asLong(7200);
        tokenExpireAt = System.currentTimeMillis() + expiresIn * 1000L;
        log.info("gettoken 成功，{}s 后过期", expiresIn);
        return accessToken;
    }

    /** 发送文本消息给指定成员 */
    public void sendText(String touser, String content) {
        try {
            String payload = M.writeValueAsString(M.createObjectNode()
                    .put("touser", touser)
                    .put("msgtype", "text")
                    .put("agentid", agentId)
                    .set("text", M.createObjectNode().put("content", content)));
            String urlStr = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token="
                    + urlEncode(getToken());
            String resp = httpPostJson(urlStr, payload);
            JsonNode n = M.readTree(resp);
            int errcode = n.path("errcode").asInt(-1);
            if (errcode != 0) {
                log.error("message/send 失败 errcode={} errmsg={} touser={}", errcode, n.path("errmsg").asText(), touser);
            } else {
                log.info("回复成功 touser={}", touser);
            }
        } catch (Exception e) {
            log.error("sendText 异常 touser=" + touser, e);
        }
    }

    private static String urlEncode(String s) throws Exception {
        return java.net.URLEncoder.encode(s, "UTF-8");
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(10_000);
        return readAll(c);
    }

    private String httpPostJson(String urlStr, String json) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setConnectTimeout(10_000);
        c.setReadTimeout(15_000);
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return readAll(c);
    }

    private String readAll(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                code >= 400 ? c.getErrorStream() : c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
