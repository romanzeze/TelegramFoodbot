package org.example.telegramfoodbot.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "weight_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"telegram_id", "logged_date"}))
public class WeightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "logged_date", nullable = false)
    private LocalDate loggedDate;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKg;
}
