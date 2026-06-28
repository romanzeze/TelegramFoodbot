package org.example.telegramfoodbot.repository;

import org.example.telegramfoodbot.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByTelegramId(Long telegramId);
}
