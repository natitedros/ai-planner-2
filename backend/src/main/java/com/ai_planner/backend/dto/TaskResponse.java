package com.ai_planner.backend.dto;

import com.ai_planner.backend.model.Task;

import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String whatToDo,
        LocalDate dueDate,
        String priority,
        String category,
        String status,
        Long parentId
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(), t.getWhatToDo(), t.getDueDate(),
                t.getPriority().name(), t.getCategory().name(),
                t.getStatus().name(),
                t.getParent() != null ? t.getParent().getId() : null
        );
    }
}
