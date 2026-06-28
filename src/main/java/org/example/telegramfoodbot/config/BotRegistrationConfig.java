package org.example.telegramfoodbot.config;

import lombok.extern.slf4j.Slf4j;
import org.example.telegramfoodbot.bot.FoodBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
public class BotRegistrationConfig {

    /*
     * telegrambots-spring-boot-starter 6.9.7.1 registers its auto-configuration
     * only via META-INF/spring.factories, which Spring Boot 3.x no longer reads
     * for EnableAutoConfiguration. TelegramBotStarterConfiguration therefore never
     * loads, TelegramBotsApi is never created, and the bot is never registered.
     *
     * We own the full setup here: create TelegramBotsApi and register the bot.
     */

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public BotSession botSession(TelegramBotsApi telegramBotsApi, FoodBot foodBot)
            throws TelegramApiException {
        log.info("Registering bot: {}", foodBot.getBotUsername());
        BotSession session = telegramBotsApi.registerBot(foodBot);
        log.info("Bot registered — polling started");
        return session;
    }
}
