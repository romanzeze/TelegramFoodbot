package org.example.telegramfoodbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramfoodbot.model.FoodLog;
import org.example.telegramfoodbot.model.WeightLog;
import org.example.telegramfoodbot.repository.FoodLogRepository;
import org.example.telegramfoodbot.repository.WeightLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalorieService {

    private final FoodLogRepository foodLogRepository;
    private final WeightLogRepository weightLogRepository;

    public List<FoodLog> getTodayLogs(long telegramId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        return foodLogRepository.findByTelegramIdAndLoggedAtBetweenOrderByLoggedAtAsc(telegramId, start, end);
    }

    public List<FoodLog> getLogsForPeriod(long telegramId, LocalDateTime from, LocalDateTime to) {
        return foodLogRepository.findByTelegramIdAndLoggedAtBetweenOrderByLoggedAtAsc(telegramId, from, to);
    }

    public int sumCalories(List<FoodLog> logs) {
        return logs.stream().mapToInt(FoodLog::getCalories).sum();
    }

    @Transactional
    public FoodLog saveLog(FoodLog log) {
        return foodLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Optional<FoodLog> findLogById(long logId) {
        return foodLogRepository.findById(logId);
    }

    @Transactional
    public FoodLog rescaleLog(FoodLog log, int newGrams) {
        BigDecimal ratio = BigDecimal.valueOf((double) newGrams / log.getGrams());
        log.setGrams(newGrams);
        log.setCalories((int) Math.round(log.getCalories() * ratio.doubleValue()));
        log.setProteinG(log.getProteinG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        log.setFatG(log.getFatG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        log.setCarbsG(log.getCarbsG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        return foodLogRepository.save(log);
    }

    @Transactional
    public WeightLog saveWeight(long telegramId, double weightKg) {
        WeightLog log = weightLogRepository
                .findByTelegramIdAndLoggedDate(telegramId, LocalDate.now())
                .orElse(new WeightLog());
        log.setTelegramId(telegramId);
        log.setLoggedDate(LocalDate.now());
        log.setWeightKg(BigDecimal.valueOf(weightKg));
        return weightLogRepository.save(log);
    }

    public List<WeightLog> getWeightLogs(long telegramId, LocalDate from) {
        return weightLogRepository
                .findByTelegramIdAndLoggedDateGreaterThanEqualOrderByLoggedDateAsc(telegramId, from);
    }

}
