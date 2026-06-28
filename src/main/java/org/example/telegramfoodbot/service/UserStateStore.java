package org.example.telegramfoodbot.service;

import org.example.telegramfoodbot.fsm.UserState;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserStateStore {

    private final ConcurrentHashMap<Long, UserState> store = new ConcurrentHashMap<>();

    public UserState get(long userId) {
        return store.getOrDefault(userId, UserState.IDLE);
    }

    public void set(long userId, UserState state) {
        if (state == UserState.IDLE) {
            store.remove(userId);
        } else {
            store.put(userId, state);
        }
    }
}
