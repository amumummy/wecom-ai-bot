package com.devlab.wecombot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企微应用配置。真实值通过环境变量注入（见 deploy/env.example.sh），不在代码中硬编码。
 */
@Component
@ConfigurationProperties(prefix = "wecom")
public class WecomProperties {

    /** 企业ID（我的企业 → 企业信息） */
    private String corpid;

    /** 应用详情页的 AgentId */
    private String agentId;

    /** 回调配置 Token */
    private String token;

    /** 回调配置 EncodingAESKey（43位） */
    private String aesKey;

    /** 应用详情页的 Secret（阶段B 调 Coze / 企微 API 用） */
    private String secret;

    public String getCorpid() {
        return corpid;
    }

    public void setCorpid(String corpid) {
        this.corpid = corpid;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
