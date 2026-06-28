package org.example.telegramfoodbot.service;

import lombok.RequiredArgsConstructor;
import org.example.telegramfoodbot.model.ActivityLevel;
import org.example.telegramfoodbot.model.UserProfile;
import org.example.telegramfoodbot.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository userProfileRepository;

    public Optional<UserProfile> findByTelegramId(long telegramId) {
        return userProfileRepository.findByTelegramId(telegramId);
    }

    @Transactional
    public UserProfile saveWithCalculatedTarget(UserProfile profile) {
        profile.setDailyTargetKcal(computeDailyTarget(profile));
        return userProfileRepository.save(profile);
    }

    public int computeDailyTarget(UserProfile profile) {
        double weight = profile.getWeightKg().doubleValue();
        double bmr = "MALE".equals(profile.getGender())
                ? 10.0 * weight + 6.25 * profile.getHeightCm() - 5.0 * profile.getAge() + 5
                : 10.0 * weight + 6.25 * profile.getHeightCm() - 5.0 * profile.getAge() - 161;

        double tdee = bmr * ActivityLevel.valueOf(profile.getActivityLevel()).getMultiplier();
        return (int) Math.max(1500, Math.round(tdee - 500));
    }
}
