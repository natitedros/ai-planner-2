package com.ai_planner.backend.dto;

import com.ai_planner.backend.model.Task;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank String whatToDo,
        LocalDate dueDate,
        Task.Priority priority,
        Task.Category category
) {}