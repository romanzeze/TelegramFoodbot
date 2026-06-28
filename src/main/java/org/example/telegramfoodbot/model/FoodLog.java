package org.example.telegramfoodbot.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "food_log")
public class FoodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @CreationTimestamp
    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    @Column(nullable = false)
    private String dish;

    @Column(nullable = false)
    private Integer grams;

    @Column(nullable = false)
    private Integer calories;

    @Column(name = "protein_g", nullable = false)
    private BigDecimal proteinG;

    @Column(name = "fat_g", nullable = false)
    private BigDecimal fatG;

    @Column(name = "carbs_g", nullable = false)
    private BigDecimal carbsG;

    @Column(length = 20)
    private String confidence;

    @Column(name = "portion_note")
    private String portionNote;

    @Column(name = "photo_file_id")
    private String photoFileId;
}
