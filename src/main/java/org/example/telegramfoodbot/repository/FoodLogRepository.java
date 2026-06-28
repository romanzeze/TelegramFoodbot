package org.example.telegramfoodbot.repository;

import org.example.telegramfoodbot.model.FoodLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    List<FoodLog> findByTelegramIdAndLoggedAtBetweenOrderByLoggedAtAsc(
            Long telegramId, LocalDateTime from, LocalDateTime to);
}
