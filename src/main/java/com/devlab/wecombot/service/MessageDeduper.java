package com.devlab.wecombot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MsgId 去重器：企微在 5 秒内未收到响应会重推消息，网络抖动也可能重复推送。
 * 惰性过期清理，5 分钟前的记录直接淘汰。
 */
@Component
public class MessageDeduper {

    private static final Logger log = LoggerFactory.getLogger(MessageDeduper.class);
    private static final long EXPIRE_MS = 5 * 60 * 1000L;

    private final Map<String, Long> seen = new ConcurrentHashMap<>();

    /** @return true=首次收到（应处理）；false=重复推送（丢弃） */
    public boolean firstSeen(String msgId) {
        if (msgId == null || msgId.isEmpty()) {
            return true;
        }
        long now = System.currentTimeMillis();
        cleanup(now);
        Long prev = seen.putIfAbsent(msgId, now);
        if (prev != null) {
            log.warn("重复消息 MsgId={}，丢弃（首次收到于 {}ms 前）", msgId, now - prev);
            return false;
        }
        return true;
    }

    private void cleanup(long now) {
        if (seen.size() < 1000) {
            return;
        }
        Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > EXPIRE_MS) {
                it.remove();
            }
        }
    }
}
