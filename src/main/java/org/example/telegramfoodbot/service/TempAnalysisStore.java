package org.example.telegramfoodbot.service;

import org.example.telegramfoodbot.dto.FoodAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TempAnalysisStore {

    public record Pending(FoodAnalysisResult result, String photoFileId) {}

    private final ConcurrentHashMap<Long, Pending> store = new ConcurrentHashMap<>();

    public void put(long userId, Pending pending) {
        store.put(userId, pending);
    }

    public Pending get(long userId) {
        return store.get(userId);
    }

    public void remove(long userId) {
        store.remove(userId);
    }
}
