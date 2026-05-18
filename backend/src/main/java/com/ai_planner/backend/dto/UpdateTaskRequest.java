package com.ai_planner.backend.dto;

import com.ai_planner.backend.model.Task;
import com.ai_planner.backend.model.User;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateTaskRequest(
        String whatToDo,
        LocalDate dueDate,
        Task.Priority priority,
        Task.Category category,
        Task.Status status
) {}