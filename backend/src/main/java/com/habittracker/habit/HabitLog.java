package com.habittracker.habit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "habit_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_habit_logs_habit_date", columnNames = {"habit_id", "log_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "habit_id", nullable = false)
    private Long habitId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(columnDefinition = "TEXT")
    private String note;
}
