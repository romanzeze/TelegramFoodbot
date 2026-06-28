package org.example.telegramfoodbot.repository;

import org.example.telegramfoodbot.model.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

    Optional<WeightLog> findByTelegramIdAndLoggedDate(Long telegramId, LocalDate date);

    List<WeightLog> findByTelegramIdAndLoggedDateGreaterThanEqualOrderByLoggedDateAsc(
            Long telegramId, LocalDate from);
}
