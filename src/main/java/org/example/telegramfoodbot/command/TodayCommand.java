package org.example.telegramfoodbot.command;

import lombok.RequiredArgsConstructor;
import org.example.telegramfoodbot.bot.BotSender;
import org.example.telegramfoodbot.model.FoodLog;
import org.example.telegramfoodbot.model.UserProfile;
import org.example.telegramfoodbot.service.CalorieService;
import org.example.telegramfoodbot.service.ProfileService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TodayCommand {

    private final BotSender sender;
    private final CalorieService calorieService;
    private final ProfileService profileService;

    public void handle(Message msg) {
        long chatId = msg.getChatId();
        long userId = msg.getFrom().getId();

        Optional<UserProfile> profileOpt = profileService.findByTelegramId(userId);
        if (profileOpt.isEmpty()) {
            sender.send(chatId, "Спочатку налаштуй профіль: /setup");
            return;
        }

        List<FoodLog> logs = calorieService.getTodayLogs(userId);
        int target = profileOpt.get().getDailyTargetKcal();
        int eaten = calorieService.sumCalories(logs);
        int remaining = target - eaten;

        StringBuilder sb = new StringBuilder("Сьогодні:\n\n");

        if (logs.isEmpty()) {
            sb.append("Записів ще немає.\n\n");
        } else {
            for (FoodLog log : logs) {
                sb.append(String.format("#%d %s (%d г) — %d ккал\n",
                        log.getId(), log.getDish(), log.getGrams(), log.getCalories()));
            }
            sb.append("\n");
        }

        sb.append(String.format("З'їдено: %d / %d ккал\n", eaten, target));
        sb.append(progressBar(eaten, target)).append("\n");

        if (remaining >= 0) {
            sb.append(String.format("Залишилось: %d ккал", remaining));
        } else {
            sb.append(String.format("Перевищено на: %d ккал", -remaining));
        }

        sender.send(chatId, sb.toString());
    }

    private String progressBar(int eaten, int target) {
        int percent = target > 0 ? Math.min(100, eaten * 100 / target) : 0;
        int filled = percent / 5;
        return "[" + "█".repeat(filled) + "░".repeat(20 - filled) + "] " + percent + "%";
    }
}
