package org.example.telegramfoodbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bot")
public record BotConfig(String token, String username) {}
