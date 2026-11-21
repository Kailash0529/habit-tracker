package com.habittracker.habit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "habits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type", nullable = false, length = 50)
    private FrequencyType frequencyType;

    @Column(name = "days_of_week", length = 100)
    private String daysOfWeek; // e.g. "MON,WED,FRI"

    @Column(name = "target_time", length = 20)
    private String targetTime; // e.g. "07:30"

    @Column(length = 30)
    @Builder.Default
    private String color = "#10B981";

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
