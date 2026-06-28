package org.example.telegramfoodbot.command;

import lombok.RequiredArgsConstructor;
import org.example.telegramfoodbot.bot.BotSender;
import org.example.telegramfoodbot.model.FoodLog;
import org.example.telegramfoodbot.model.UserProfile;
import org.example.telegramfoodbot.model.WeightLog;
import org.example.telegramfoodbot.service.CalorieService;
import org.example.telegramfoodbot.service.ProfileService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WeekCommand {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM");

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
        int target = profileOpt.get().getDailyTargetKcal();

        LocalDate weekAgo = LocalDate.now().minusDays(6);
        List<FoodLog> logs = calorieService.getLogsForPeriod(
                userId, weekAgo.atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX));
        List<WeightLog> weightLogs = calorieService.getWeightLogs(userId, weekAgo);

        Map<LocalDate, List<FoodLog>> byDay = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getLoggedAt().toLocalDate()));

        StringBuilder sb = new StringBuilder("Статистика за 7 днів:\n\n");

        int totalCalories = 0;
        int daysWithData = 0;
        int daysInTarget = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<FoodLog> dayLogs = byDay.getOrDefault(date, List.of());
            int dayCalories = dayLogs.stream().mapToInt(FoodLog::getCalories).sum();

            if (!dayLogs.isEmpty()) {
                totalCalories += dayCalories;
                daysWithData++;
                if (dayCalories <= target) daysInTarget++;
            }

            String status = dayLogs.isEmpty() ? "—" : (dayCalories <= target ? "+" : "!");
            sb.append(String.format("%s %s: %d ккал [%s]\n",
                    date.format(FMT), shortDayName(date.getDayOfWeek()), dayCalories, status));
        }

        sb.append("\n");
        int avgCalories = daysWithData > 0 ? totalCalories / daysWithData : 0;
        sb.append(String.format("Середнє: %d ккал/день\n", avgCalories));
        sb.append(String.format("Ціль (%d ккал): %d/%d днів\n", target, daysInTarget, daysWithData));
        sb.append("[+] = вклався в ціль, [!] = перевищено");

        if (!weightLogs.isEmpty()) {
            sb.append("\n\nТренд ваги:\n");
            for (WeightLog wl : weightLogs) {
                sb.append(String.format("%s: %.1f кг\n", wl.getLoggedDate().format(FMT), wl.getWeightKg()));
            }
            if (weightLogs.size() > 1) {
                double diff = weightLogs.get(weightLogs.size() - 1).getWeightKg()
                        .subtract(weightLogs.get(0).getWeightKg())
                        .doubleValue();
                sb.append(String.format("Зміна за тиждень: %+.1f кг", diff));
            }
        }

        sender.send(chatId, sb.toString());
    }

    private String shortDayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Пн";
            case TUESDAY -> "Вт";
            case WEDNESDAY -> "Ср";
            case THURSDAY -> "Чт";
            case FRIDAY -> "Пт";
            case SATURDAY -> "Сб";
            case SUNDAY -> "Нд";
        };
    }
}
