package com.ai_planner.backend.controller;

import com.ai_planner.backend.dto.CreateTaskRequest;
import com.ai_planner.backend.dto.TaskResponse;
import com.ai_planner.backend.dto.UpdateTaskRequest;
import com.ai_planner.backend.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // User ID comes from a custom header (same as your Flask app)
    private Long requireUserId(HttpServletRequest request) {
        String raw = request.getHeader("X-User-ID");
        if (raw == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return Long.parseLong(raw);
    }

    @GetMapping
    public List<TaskResponse> getItems(HttpServletRequest req) {
        return taskService.getTasksForUser(requireUserId(req));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse addItem(@Valid @RequestBody CreateTaskRequest body,
                                HttpServletRequest req) {
        return taskService.createTask(requireUserId(req), body);
    }

    @PutMapping("/{id}")
    public TaskResponse updateItem(@PathVariable Long id,
                                   @RequestBody UpdateTaskRequest body,
                                   HttpServletRequest req) {
        return taskService.updateTask(requireUserId(req), id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteItem(@PathVariable Long id,
                                          HttpServletRequest req) {
        taskService.deleteTask(requireUserId(req), id);
        return Map.of("message", "Deleted");
    }

    @PostMapping("/{id}/decompose")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, List<TaskResponse>> decompose(@PathVariable Long id,
                                                     HttpServletRequest req) {
        return Map.of("subtasks", taskService.decompose(requireUserId(req), id));
    }
}
