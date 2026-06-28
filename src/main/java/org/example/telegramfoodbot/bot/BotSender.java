package org.example.telegramfoodbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * Breaks the FoodBot ↔ Command circular dependency.
 * ObjectProvider resolves FoodBot on first use (after context is fully initialized),
 * avoiding CGLIB proxying of final methods in DefaultAbsSender.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotSender {

    private static final ReplyKeyboardMarkup MAIN_MENU;

    static {
        KeyboardRow row1 = new KeyboardRow(List.of(
                new KeyboardButton("📊 Сьогодні"),
                new KeyboardButton("📅 Тиждень")));
        KeyboardRow row2 = new KeyboardRow(List.of(
                new KeyboardButton("⚖️ Вага"),
                new KeyboardButton("⚙️ Профіль")));
        MAIN_MENU = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .resizeKeyboard(true)
                .build();
    }

    private final ObjectProvider<FoodBot> botProvider;

    public void send(long chatId, String text) {
        send(SendMessage.builder().chatId(chatId).text(text).build());
    }

    public void send(SendMessage msg) {
        try {
            botProvider.getObject().execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}", msg.getChatId(), e);
        }
    }

    public void sendMenu(long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(MAIN_MENU)
                .build();
        try {
            botProvider.getObject().execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send menu message to {}", chatId, e);
        }
    }

    public void sendWithKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            botProvider.getObject().execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send keyboard message to {}", chatId, e);
        }
    }

    public void answerCallback(String callbackQueryId) {
        try {
            botProvider.getObject().execute(
                    AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback {}", callbackQueryId, e);
        }
    }
}
