package org.example.telegramfoodbot.command;

import lombok.RequiredArgsConstructor;
import org.example.telegramfoodbot.bot.BotSender;
import org.example.telegramfoodbot.model.FoodLog;
import org.example.telegramfoodbot.service.CalorieService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EditCommand {

    private final BotSender sender;
    private final CalorieService calorieService;

    public void handle(Message msg) {
        long chatId = msg.getChatId();
        long userId = msg.getFrom().getId();
        String[] parts = msg.getText().trim().split("\\s+");

        if (parts.length < 3) {
            sender.send(chatId, "Формат: /edit {id} {нова_вага_г}\nНаприклад: /edit 42 200");
            return;
        }

        try {
            long logId = Long.parseLong(parts[1]);
            int newGrams = Integer.parseInt(parts[2]);
            if (newGrams <= 0) {
                sender.send(chatId, "Вага має бути більше 0.");
                return;
            }

            Optional<FoodLog> logOpt = calorieService.findLogById(logId);
            if (logOpt.isEmpty()) {
                sender.send(chatId, "Запис #" + logId + " не знайдено.");
                return;
            }

            FoodLog log = logOpt.get();
            if (!log.getTelegramId().equals(userId)) {
                sender.send(chatId, "Це не твій запис.");
                return;
            }

            FoodLog updated = calorieService.rescaleLog(log, newGrams);
            sender.send(chatId, String.format(
                    "Запис #%d оновлено:\n%s (%d г) — %d ккал\nБ: %.1f г  Ж: %.1f г  В: %.1f г",
                    updated.getId(), updated.getDish(), updated.getGrams(), updated.getCalories(),
                    updated.getProteinG(), updated.getFatG(), updated.getCarbsG()
            ));
        } catch (NumberFormatException e) {
            sender.send(chatId, "Невірний формат. Наприклад: /edit 42 200");
        }
    }
}
