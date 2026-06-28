package org.example.telegramfoodbot.fsm;

import lombok.Data;

@Data
public class SetupSession {
    private SetupStep step = SetupStep.GENDER;
    private String gender;
    private Integer age;
    private Integer heightCm;
    private Double weightKg;
    private String activityLevel;
}
