package com.ai_planner.backend.service;

import com.ai_planner.backend.dto.CreateTaskRequest;
import com.ai_planner.backend.dto.TaskResponse;
import com.ai_planner.backend.dto.UpdateTaskRequest;
import com.ai_planner.backend.model.Task;
import com.ai_planner.backend.model.User;
import com.ai_planner.backend.repository.TaskRepository;
import com.ai_planner.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service   // ← registers this class as a Spring bean
@RequiredArgsConstructor  // Lombok: constructor injection for all final fields
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final GroqService groqService;      // injected automatically

//    READ
    public List<TaskResponse> getTasksForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return taskRepository.findByUser(user)
                .stream().map(TaskResponse::from).toList();
    }

//    CREATE
    public TaskResponse createTask(Long userId, CreateTaskRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Task task = Task.builder()
                .user(user)
                .whatToDo(req.whatToDo())
                .dueDate(req.dueDate())
                .priority(req.priority() != null ? req.priority() : Task.Priority.MEDIUM)
                .category(req.category() != null ? req.category() : Task.Category.PERSONAL)
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

//    UPDATE
    public TaskResponse updateTask(Long userId, Long taskId, UpdateTaskRequest req) {
        Task task = findTaskOrThrow(taskId);
        assertOwnership(task, userId);

        // Only update fields that were actually sent in the request (non-null)
        if (req.whatToDo() != null)  task.setWhatToDo(req.whatToDo());
        if (req.dueDate() != null)   task.setDueDate(req.dueDate());
        if (req.priority() != null)  task.setPriority(req.priority());
        if (req.category() != null)  task.setCategory(req.category());
        if (req.status() != null)    task.setStatus(req.status());

        return TaskResponse.from(taskRepository.save(task));
    }

//    DELETE
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!task.getUser().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        taskRepository.deleteByParent(task);  // delete subtasks first
        taskRepository.delete(task);
    }

//    DECOMPOSE
    public List<TaskResponse> decompose(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!task.getUser().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        List<CreateTaskRequest> subtaskRequests = groqService.decompose(task);
        return subtaskRequests.stream().map(req -> {
            Task subtask = Task.builder()
                    .user(task.getUser())
                    .whatToDo(req.whatToDo())
                    .dueDate(req.dueDate())
                    .priority(req.priority())
                    .category(req.category())
                    .parent(task)
                    .build();
            return TaskResponse.from(taskRepository.save(subtask));
        }).toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers — reduce repetition across methods above
    // -------------------------------------------------------------------------

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));
    }

    private Task findTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"
                ));
    }

    private void assertOwnership(Task task, Long userId) {
        if (!task.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied"
            );
        }
    }
}
