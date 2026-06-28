package org.example.telegramfoodbot.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramfoodbot.bot.BotSender;
import org.example.telegramfoodbot.fsm.SetupSession;
import org.example.telegramfoodbot.fsm.SetupStep;
import org.example.telegramfoodbot.model.UserProfile;
import org.example.telegramfoodbot.service.ProfileService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetupCommand {

    private final BotSender sender;
    private final ProfileService profileService;
    private final ConcurrentHashMap<Long, SetupSession> sessions = new ConcurrentHashMap<>();

    public boolean isInSetup(long userId) {
        return sessions.containsKey(userId);
    }

    public void start(Message msg) {
        long userId = msg.getFrom().getId();
        long chatId = msg.getChatId();
        sessions.put(userId, new SetupSession());
        askGender(chatId);
    }

    public void handleInput(Message msg) {
        long chatId = msg.getChatId();
        long userId = msg.getFrom().getId();
        SetupSession session = sessions.get(userId);
        if (session == null) return;

        String text = msg.getText().trim();

        switch (session.getStep()) {
            case AGE -> {
                try {
                    int age = Integer.parseInt(text);
                    if (age < 10 || age > 120) { sender.send(chatId, "Введи реальний вік (10–120):"); return; }
                    session.setAge(age);
                    session.setStep(SetupStep.HEIGHT);
                    sender.send(chatId, "Введи зріст у см (наприклад: 175):");
                } catch (NumberFormatException e) {
                    sender.send(chatId, "Потрібне ціле число. Наприклад: 28");
                }
            }
            case HEIGHT -> {
                try {
                    int height = Integer.parseInt(text);
                    if (height < 100 || height > 250) { sender.send(chatId, "Введи реальний зріст (100–250 см):"); return; }
                    session.setHeightCm(height);
                    session.setStep(SetupStep.WEIGHT);
                    sender.send(chatId, "Введи вагу у кг (наприклад: 75.5):");
                } catch (NumberFormatException e) {
                    sender.send(chatId, "Потрібне ціле число. Наприклад: 175");
                }
            }
            case WEIGHT -> {
                try {
                    double weight = Double.parseDouble(text.replace(',', '.'));
                    if (weight < 30 || weight > 300) { sender.send(chatId, "Введи реальну вагу (30–300 кг):"); return; }
                    session.setWeightKg(weight);
                    session.setStep(SetupStep.ACTIVITY);
                    askActivity(chatId);
                } catch (NumberFormatException e) {
                    sender.send(chatId, "Потрібне число. Наприклад: 75.5");
                }
            }
            default -> sender.send(chatId, "Скористайся кнопками нижче.");
        }
    }

    public void handleCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        long userId = callback.getFrom().getId();
        String data = callback.getData();
        SetupSession session = sessions.get(userId);
        if (session == null) return;

        switch (session.getStep()) {
            case GENDER -> {
                if (!data.startsWith("gender:")) return;
                session.setGender(data.substring(7));
                session.setStep(SetupStep.AGE);
                sender.send(chatId, "Введи свій вік:");
            }
            case ACTIVITY -> {
                if (!data.startsWith("activity:")) return;
                session.setActivityLevel(data.substring(9));
                finishSetup(chatId, userId, session);
            }
        }
    }

    private void askGender(long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        btn("Чоловік", "gender:MALE"),
                        btn("Жінка", "gender:FEMALE")
                ))).build();
        sender.send(SendMessage.builder()
                .chatId(chatId)
                .text("Вкажи стать:")
                .replyMarkup(markup)
                .build());
    }

    private void askActivity(long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(btn("Сидячий", "activity:SEDENTARY"), btn("Легка активність", "activity:LIGHT")),
                        List.of(btn("Середня", "activity:MODERATE"), btn("Висока", "activity:HIGH"))
                )).build();
        sender.send(SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Вкажи рівень активності:
                        • Сидячий — офіс, мінімум руху
                        • Легка — 1–3 тренування/тиждень
                        • Середня — 3–5 тренувань/тиждень
                        • Висока — щоденні інтенсивні тренування""")
                .replyMarkup(markup)
                .build());
    }

    private void finishSetup(long chatId, long userId, SetupSession session) {
        sessions.remove(userId);

        UserProfile profile = profileService.findByTelegramId(userId).orElse(new UserProfile());
        profile.setTelegramId(userId);
        profile.setGender(session.getGender());
        profile.setAge(session.getAge());
        profile.setHeightCm(session.getHeightCm());
        profile.setWeightKg(BigDecimal.valueOf(session.getWeightKg()));
        profile.setActivityLevel(session.getActivityLevel());
        profile = profileService.saveWithCalculatedTarget(profile);

        sender.sendMenu(chatId, String.format(
                "Профіль збережено! ✅\n\nЦіль: %d ккал/день\n\nКидай фото їжі або обирай дію:",
                profile.getDailyTargetKcal()
        ));
    }

    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }
}
