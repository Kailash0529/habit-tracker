package com.habittracker.habit;

import com.habittracker.common.dto.ApiResponse;
import com.habittracker.habit.dto.CreateHabitRequest;
import com.habittracker.habit.dto.HabitDto;
import com.habittracker.habit.dto.UpdateHabitRequest;
import com.habittracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
@Tag(name = "Habits", description = "CRUD operations for managing habits")
public class HabitController {

    private final HabitService habitService;

    @GetMapping
    @Operation(summary = "List habits", description = "Retrieves all habits for the authenticated user, optionally filtered by archived status")
    public ResponseEntity<ApiResponse<List<HabitDto>>> getHabits(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean archived) {
        List<HabitDto> habits = habitService.getHabits(principal.getId(), archived);
        return ResponseEntity.ok(ApiResponse.ok(habits));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get habit by ID", description = "Retrieves a specific habit belonging to the authenticated user")
    public ResponseEntity<ApiResponse<HabitDto>> getHabitById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        HabitDto habit = habitService.getHabitById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(habit));
    }

    @PostMapping
    @Operation(summary = "Create habit", description = "Creates a new habit for the authenticated user")
    public ResponseEntity<ApiResponse<HabitDto>> createHabit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateHabitRequest request) {
        HabitDto habit = habitService.createHabit(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Habit created successfully", habit));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update habit", description = "Updates an existing habit belonging to the authenticated user")
    public ResponseEntity<ApiResponse<HabitDto>> updateHabit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateHabitRequest request) {
        HabitDto updatedHabit = habitService.updateHabit(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Habit updated successfully", updatedHabit));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete habit", description = "Deletes a habit belonging to the authenticated user")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        habitService.deleteHabit(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Habit deleted successfully", null));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive or unarchive habit", description = "Toggles archive state for a habit")
    public ResponseEntity<ApiResponse<HabitDto>> archiveHabit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean archive) {
        HabitDto habit = habitService.archiveHabit(id, principal.getId(), archive);
        String message = archive ? "Habit archived successfully" : "Habit unarchived successfully";
        return ResponseEntity.ok(ApiResponse.ok(message, habit));
    }
}
