package com.ai_planner.backend.service;

import com.ai_planner.backend.dto.CreateTaskRequest;
import com.ai_planner.backend.dto.TaskResponse;
import com.ai_planner.backend.dto.UpdateTaskRequest;
import com.ai_planner.backend.model.Task;
import com.ai_planner.backend.model.User;
import com.ai_planner.backend.repository.TaskRepository;
import com.ai_planner.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private GroqService groqService;

    @InjectMocks private TaskService taskService;

    // Reusable test fixtures — set up once, used across multiple tests
    private User testUser;
    private Task testTask;

    // @BeforeEach runs before every single @Test method in this class
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("nat")
                .email("nat@test.com")
                .build();

        testTask = Task.builder()
                .id(10L)
                .user(testUser)
                .whatToDo("Study Spring Boot")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(Task.Priority.HIGH)
                .category(Task.Category.SCHOOL)
                .status(Task.Status.PENDING)
                .build();
    }

    // --- getTasksForUser() ---

    @Test
    void getTasksForUser_shouldReturnMappedResponses() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.findByUser(testUser)).thenReturn(List.of(testTask));

        List<TaskResponse> result = taskService.getTasksForUser(1L);

        assertEquals(1, result.size());
        assertEquals("Study Spring Boot", result.get(0).whatToDo());
        assertEquals("HIGH", result.get(0).priority());
    }

    @Test
    void getTasksForUser_withUnknownUser_shouldThrow404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> taskService.getTasksForUser(99L)
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    // --- createTask() ---

    @Test
    void createTask_shouldBuildCorrectEntityAndSave() {
        CreateTaskRequest req = new CreateTaskRequest(
                "Write unit tests",
                LocalDate.now().plusDays(3),
                Task.Priority.MEDIUM,
                Task.Category.SCHOOL
        );

        // Build the entity that the repository will return after save
        Task saved = Task.builder()
                .id(11L)
                .user(testUser)
                .whatToDo(req.whatToDo())
                .dueDate(req.dueDate())
                .priority(req.priority())
                .category(req.category())
                .status(Task.Status.PENDING)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse result = taskService.createTask(1L, req);

        assertEquals(11L, result.id());
        assertEquals("Write unit tests", result.whatToDo());
        assertEquals("MEDIUM", result.priority());

        // Verify the entity was actually persisted
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_withNullPriority_shouldDefaultToMedium() {
        // null priority in the request — service should default it
        CreateTaskRequest req = new CreateTaskRequest(
                "Some task", LocalDate.now(), null, Task.Category.WORK
        );

        Task saved = Task.builder()
                .id(12L).user(testUser)
                .whatToDo("Some task")
                .priority(Task.Priority.MEDIUM) // defaulted
                .category(Task.Category.WORK)
                .status(Task.Status.PENDING)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse result = taskService.createTask(1L, req);

        assertEquals("MEDIUM", result.priority());
    }

    // --- updateTask() ---

    @Test
    void updateTask_shouldUpdateOnlyProvidedFields() {
        // Only updating status — other fields should be untouched
        UpdateTaskRequest req = new UpdateTaskRequest(
                null, null, null, null, Task.Status.COMPLETED
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(
                invocation -> invocation.getArgument(0) // return whatever was passed to save()
        );

        TaskResponse result = taskService.updateTask(1L, 10L, req);

        assertEquals("DONE", result.status());
        assertEquals("Study Spring Boot", result.whatToDo()); // unchanged
    }

    @Test
    void updateTask_byWrongUser_shouldThrow403() {
        UpdateTaskRequest req = new UpdateTaskRequest(
                null, null, null, null, Task.Status.COMPLETED
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));

        // userId 99L doesn't own this task (testTask.user.id = 1L)
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> taskService.updateTask(99L, 10L, req)
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    // --- deleteTask() ---

    @Test
    void deleteTask_shouldDeleteSubtasksFirst() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));

        taskService.deleteTask(1L, 10L);

        // Verify ORDER: subtasks must be deleted before the parent
        // InOrder ensures Mockito checks calls happened in this exact sequence
        var inOrder = inOrder(taskRepository);
        inOrder.verify(taskRepository).deleteByParent(testTask);
        inOrder.verify(taskRepository).delete(testTask);
    }

    @Test
    void deleteTask_withNonExistentTask_shouldThrow404() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> taskService.deleteTask(1L, 999L)
        );
        assertEquals(404, ex.getStatusCode().value());
    }

    // --- decompose() ---

    @Test
    void decompose_shouldPersistEachSubtaskWithParentSet() {
        List<CreateTaskRequest> fakeSubtasks = List.of(
                new CreateTaskRequest("Setup project",
                        LocalDate.now().plusDays(1), Task.Priority.HIGH, Task.Category.SCHOOL),
                new CreateTaskRequest("Write entities",
                        LocalDate.now().plusDays(2), Task.Priority.MEDIUM, Task.Category.SCHOOL)
        );

        when(taskRepository.findById(10L)).thenReturn(Optional.of(testTask));
        when(groqService.decompose(testTask)).thenReturn(fakeSubtasks);

        // For each save() call, return a Task with an incremented ID
        final long[] idCounter = {100L};
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(idCounter[0]++); // simulate DB assigning an ID
            return t;
        });

        List<TaskResponse> result = taskService.decompose(1L, 10L);

        assertEquals(2, result.size());

        // Verify save() was called twice — once per subtask
        verify(taskRepository, times(2)).save(any(Task.class));

        // Verify GroqService was called with the correct task
        verify(groqService, times(1)).decompose(testTask);
    }
}