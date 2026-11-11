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
 * Coze v3/chat SSE 客户端。
 * 文档：https://www.coze.cn/docs/developer_guides/chat_v3
 * 流式读取 event: conversation.message.completed（type=answer）拼接回答。
 */
@Component
public class CozeClient {

    private static final Logger log = LoggerFactory.getLogger(CozeClient.class);
    private static final ObjectMapper M = new ObjectMapper();

    @Value("${coze.api-base:https://api.coze.cn}")
    private String apiBase;
    @Value("${coze.bot-id}")
    private String botId;
    @Value("${coze.pat:}")
    private String pat;

    /** 发起对话并返回完整回答文本；失败抛异常由调用方兜底 */
    public String chat(String userId, String question) throws Exception {
        if (pat == null || pat.isEmpty()) {
            throw new IllegalStateException("Coze PAT 未配置（WECOM_COZE_PAT）");
        }
        com.fasterxml.jackson.databind.node.ObjectNode msg = M.createObjectNode()
                .put("role", "user")
                .put("content", question)
                .put("content_type", "text");
        String payload = M.writeValueAsString(M.createObjectNode()
                .put("bot_id", botId)
                .put("user_id", userId)
                .put("stream", true)
                .put("auto_save_history", true)
                .set("additional_messages", M.createArrayNode().add(msg)));

        HttpURLConnection c = (HttpURLConnection) new URL(apiBase + "/v3/chat").openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Authorization", "Bearer " + pat);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("Accept", "text/event-stream");
        c.setConnectTimeout(10_000);
        c.setReadTimeout(90_000);
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int code = c.getResponseCode();
        if (code >= 400) {
            throw new IllegalStateException("Coze HTTP " + code + ": " + readAll(c, code));
        }

        StringBuilder answer = new StringBuilder();
        String currentEvent = "";
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "\"[DONE]\"".equals(data) || "[DONE]".equals(data)) {
                    continue;
                }
                JsonNode n;
                try {
                    n = M.readTree(data);
                } catch (Exception ignore) {
                    continue;
                }
                if ("conversation.message.completed".equals(currentEvent)) {
                    if ("answer".equals(n.path("type").asText())) {
                        answer.append(n.path("content").asText());
                    }
                } else if ("conversation.chat.failed".equals(currentEvent)) {
                    throw new IllegalStateException("Coze chat failed code=" + n.path("code").asInt()
                            + " msg=" + n.path("msg").asText());
                } else if ("conversation.chat.completed".equals(currentEvent)) {
                    log.info("Coze 会话完成 conversation_id={}", n.path("id").asText());
                }
            }
        }
        String result = answer.toString().trim();
        if (result.isEmpty()) {
            throw new IllegalStateException("Coze 返回空回答");
        }
        return result;
    }

    private String readAll(HttpURLConnection c, int code) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                c.getErrorStream() != null ? c.getErrorStream() : c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
