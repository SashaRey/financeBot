package org.example.bot.util;

import java.util.concurrent.ConcurrentHashMap;

public class HistoryMessageStore {
    private final ConcurrentHashMap<Long, Integer> store = new ConcurrentHashMap<>();

    public void put(Long chatId, Integer messageId) {
        if (chatId == null || messageId == null) return;
        store.put(chatId, messageId);
    }

    public Integer get(Long chatId) {
        return store.get(chatId);
    }

    public void remove(Long chatId) {
        store.remove(chatId);
    }
}
