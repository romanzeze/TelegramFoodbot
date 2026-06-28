package org.example.telegramfoodbot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramfoodbot.bot.BotSender;
import org.example.telegramfoodbot.dto.FoodAnalysisResult;
import org.example.telegramfoodbot.fsm.UserState;
import org.example.telegramfoodbot.model.FoodLog;
import org.example.telegramfoodbot.model.UserProfile;
import org.example.telegramfoodbot.service.CalorieService;
import org.example.telegramfoodbot.service.OpenAiService;
import org.example.telegramfoodbot.service.ProfileService;
import org.example.telegramfoodbot.service.TelegramFileService;
import org.example.telegramfoodbot.service.TempAnalysisStore;
import org.example.telegramfoodbot.service.UserStateStore;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoHandler {

    private final BotSender sender;
    private final TelegramFileService telegramFileService;
    private final OpenAiService openAiService;
    private final ProfileService profileService;
    private final CalorieService calorieService;
    private final TempAnalysisStore tempAnalysisStore;
    private final UserStateStore userStateStore;

    // ── Step 1: user sends a photo ──────────────────────────────────────────

    public void handle(Message msg) {
        long chatId = msg.getChatId();
        long userId = msg.getFrom().getId();

        if (profileService.findByTelegramId(userId).isEmpty()) {
            sender.send(chatId, "Спочатку налаштуй профіль: /setup");
            return;
        }

        sender.send(chatId, "Аналізую фото...");

        try {
            List<PhotoSize> photos = msg.getPhoto();
            String fileId = photos.stream()
                    .max(Comparator.comparingInt(PhotoSize::getFileSize))
                    .map(PhotoSize::getFileId)
                    .orElseThrow(() -> new IllegalStateException("No photo sizes"));

            byte[] imageBytes = telegramFileService.downloadPhotoBytes(fileId);
            FoodAnalysisResult result = openAiService.analyze(imageBytes);

            if (result.isError()) {
                sender.send(chatId, "На фото не знайдено їжу. Спробуй ще раз.");
                return;
            }

            tempAnalysisStore.put(userId, new TempAnalysisStore.Pending(result, fileId));
            sender.sendWithKeyboard(chatId, buildResultText(result), buildKeyboard(userId, result.getCalories()));

        } catch (Exception e) {
            log.error("Error processing photo for user {}", userId, e);
            sender.send(chatId, "Помилка при аналізі фото. Спробуй ще раз.");
        }
    }

    // ── Step 2: inline keyboard callbacks ──────────────────────────────────

    public void handleCallback(CallbackQuery cq) {
        String data = cq.getData();
        long userId = cq.getFrom().getId();
        long chatId = cq.getMessage().getChatId();

        sender.answerCallback(cq.getId());

        if (data.startsWith("save_")) {
            handleSave(userId, chatId);
        } else if (data.startsWith("refine_")) {
            handleRefineRequest(userId, chatId);
        }
    }

    private void handleSave(long userId, long chatId) {
        TempAnalysisStore.Pending pending = tempAnalysisStore.get(userId);
        if (pending == null) {
            sender.send(chatId, "Результат аналізу не знайдено. Надішли фото ще раз.");
            return;
        }

        FoodAnalysisResult result = pending.result();

        FoodLog foodLog = new FoodLog();
        foodLog.setTelegramId(userId);
        foodLog.setDish(result.getDish());
        foodLog.setGrams(result.getAssumedGrams());
        foodLog.setCalories(result.getCalories());
        foodLog.setProteinG(BigDecimal.valueOf(result.getProteinG()));
        foodLog.setFatG(BigDecimal.valueOf(result.getFatG()));
        foodLog.setCarbsG(BigDecimal.valueOf(result.getCarbsG()));
        foodLog.setConfidence(result.getConfidence());
        foodLog.setPortionNote(result.getPortionNote());
        foodLog.setPhotoFileId(pending.photoFileId());
        calorieService.saveLog(foodLog);

        tempAnalysisStore.remove(userId);

        int eaten = calorieService.sumCalories(calorieService.getTodayLogs(userId));
        Optional<UserProfile> profileOpt = profileService.findByTelegramId(userId);
        String balance = profileOpt.map(p -> {
            int target = p.getDailyTargetKcal();
            int remaining = target - eaten;
            return remaining >= 0
                    ? "З'їв: " + eaten + " / " + target + " ккал (залишилось " + remaining + ")"
                    : "З'їв: " + eaten + " / " + target + " ккал (перевищено на " + (-remaining) + ")";
        }).orElse("З'їв сьогодні: " + eaten + " ккал");

        sender.send(chatId, "✅ Збережено! " + result.getDish() + " — " + result.getCalories() + " ккал\n" + balance);
    }

    private void handleRefineRequest(long userId, long chatId) {
        if (tempAnalysisStore.get(userId) == null) {
            sender.send(chatId, "Результат аналізу не знайдено. Надішли фото ще раз.");
            return;
        }
        userStateStore.set(userId, UserState.AWAITING_REFINEMENT);
        sender.send(chatId, "Напиши що додати або виправити (наприклад: 'там ще був соус 50г і хліб')");
    }

    // ── Step 3: user sends refinement comment ──────────────────────────────

    public void handleRefinement(Message msg) {
        long userId = msg.getFrom().getId();
        long chatId = msg.getChatId();

        userStateStore.set(userId, UserState.IDLE);

        TempAnalysisStore.Pending pending = tempAnalysisStore.get(userId);
        if (pending == null) {
            sender.send(chatId, "Результат аналізу не знайдено. Надішли фото ще раз.");
            return;
        }

        sender.send(chatId, "Уточнюю...");

        try {
            FoodAnalysisResult refined = openAiService.refine(pending.result(), msg.getText().trim());
            if (refined.isError()) {
                sender.send(chatId, "Не вдалось уточнити. Спробуй ще раз або збережи як є.");
                return;
            }

            tempAnalysisStore.put(userId, new TempAnalysisStore.Pending(refined, pending.photoFileId()));
            sender.sendWithKeyboard(chatId, buildResultText(refined), buildKeyboard(userId, refined.getCalories()));

        } catch (Exception e) {
            log.error("Error refining analysis for user {}", userId, e);
            sender.send(chatId, "Помилка при уточненні. Спробуй ще раз.");
        }
    }

    public boolean isAwaitingRefinement(long userId) {
        return userStateStore.get(userId) == UserState.AWAITING_REFINEMENT;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String buildResultText(FoodAnalysisResult r) {
        String confidenceUa = switch (r.getConfidence() != null ? r.getConfidence() : "low") {
            case "high" -> "висока";
            case "medium" -> "середня";
            default -> "низька";
        };
        return String.format(
                "%s (~%d г)\n\nКалорії: %d ккал\nБ: %.1f г  Ж: %.1f г  В: %.1f г\nПорція: %s\nВпевненість: %s",
                r.getDish(), r.getAssumedGrams(),
                r.getCalories(),
                r.getProteinG(), r.getFatG(), r.getCarbsG(),
                r.getPortionNote() != null ? r.getPortionNote() : "—",
                confidenceUa
        );
    }

    private InlineKeyboardMarkup buildKeyboard(long userId, int calories) {
        InlineKeyboardButton refineBtn = InlineKeyboardButton.builder()
                .text("✏️ Уточнити")
                .callbackData("refine_" + userId)
                .build();
        InlineKeyboardButton saveBtn = InlineKeyboardButton.builder()
                .text("✅ Додати " + calories + " ккал")
                .callbackData("save_" + userId)
                .build();
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(refineBtn, saveBtn)))
                .build();
    }
}
