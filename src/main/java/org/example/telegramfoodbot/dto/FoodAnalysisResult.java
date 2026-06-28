package org.example.telegramfoodbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodAnalysisResult {
    private String dish;
    private Integer assumedGrams;
    private Integer calories;
    private Double proteinG;
    private Double fatG;
    private Double carbsG;
    private String confidence;
    private String portionNote;
    private String error;

    public boolean isError() {
        return error != null && !error.isEmpty();
    }
}
