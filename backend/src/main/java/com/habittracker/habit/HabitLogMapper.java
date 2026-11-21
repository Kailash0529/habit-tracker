package com.habittracker.habit;

import com.habittracker.habit.dto.HabitLogDto;
import org.springframework.stereotype.Component;

@Component
public class HabitLogMapper {

    public HabitLogDto toDto(HabitLog habitLog) {
        if (habitLog == null) {
            return null;
        }
        return HabitLogDto.builder()
                .id(habitLog.getId())
                .habitId(habitLog.getHabitId())
                .logDate(habitLog.getLogDate())
                .completed(habitLog.isCompleted())
                .note(habitLog.getNote())
                .build();
    }
}
