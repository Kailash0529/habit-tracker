package com.habittracker.habit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    Optional<HabitLog> findByHabitIdAndLogDate(Long habitId, LocalDate logDate);

    List<HabitLog> findAllByHabitIdOrderByLogDateAsc(Long habitId);

    List<HabitLog> findAllByHabitIdAndLogDateBetweenOrderByLogDateAsc(Long habitId, LocalDate from, LocalDate to);

    List<HabitLog> findAllByHabitIdInAndLogDate(Collection<Long> habitIds, LocalDate logDate);

    List<HabitLog> findAllByHabitIdInAndLogDateBetween(Collection<Long> habitIds, LocalDate from, LocalDate to);
}
