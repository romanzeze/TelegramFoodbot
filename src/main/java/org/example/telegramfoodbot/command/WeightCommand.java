package org.example.telegramfoodbot.command;

import lombok.RequiredArgsConstructor;
import org.example.telegramfoodbot.bot.BotSender;
import org.example.telegramfoodbot.model.WeightLog;
import org.example.telegramfoodbot.service.CalorieService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class WeightCommand {

    private final BotSender sender;
    private final CalorieService calorieService;

    public void handle(Message msg) {
        long chatId = msg.getChatId();
        long userId = msg.getFrom().getId();
        String[] parts = msg.getText().trim().split("\\s+");

        if (parts.length < 2) {
            sender.send(chatId, "Вкажи вагу. Наприклад: /weight 82.5");
            return;
        }

        try {
            double weight = Double.parseDouble(parts[1].replace(',', '.'));
            if (weight < 30 || weight > 300) {
                sender.send(chatId, "Введи реальну вагу (30–300 кг).");
                return;
            }
            WeightLog log = calorieService.saveWeight(userId, weight);
            sender.send(chatId, String.format("Вагу записано: %.1f кг (%s)", log.getWeightKg(), log.getLoggedDate()));
        } catch (NumberFormatException e) {
            sender.send(chatId, "Невірний формат. Наприклад: /weight 82.5");
        }
    }
}
