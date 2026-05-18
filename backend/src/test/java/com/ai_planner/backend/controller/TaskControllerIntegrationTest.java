package com.ai_planner.backend.controller;


import com.ai_planner.backend.dto.CreateTaskRequest;
import com.ai_planner.backend.model.Task;
import com.ai_planner.backend.model.User;
import com.ai_planner.backend.repository.TaskRepository;
import com.ai_planner.backend.repository.UserRepository;
import com.ai_planner.backend.service.GroqService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TaskRepository taskRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // @MockBean replaces the real GroqService bean in the Spring context.
    // We don't want real HTTP calls to Groq during integration tests.
    // This is the key difference from unit test @Mock — this one lives IN the context.
    @MockitoBean
    private GroqService groqService;

    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("nat")
                .email("nat@test.com")
                .passwordHash(passwordEncoder.encode("pass"))
                .build());

        testTask = taskRepository.save(Task.builder()
                .user(testUser)
                .whatToDo("Study Spring Boot")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(Task.Priority.HIGH)
                .category(Task.Category.SCHOOL)
                .build());
    }

    @AfterEach
    void tearDown() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getItems_shouldReturnUsersTasks() throws Exception {
        mockMvc.perform(get("/api/items")
                        .header("X-User-ID", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].whatToDo").value("Study Spring Boot"));
    }

    @Test
    void getItems_withoutHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_shouldReturn201AndPersistTask() throws Exception {
        Map<String, Object> body = Map.of(
                "whatToDo", "Write integration tests",
                "dueDate", LocalDate.now().plusDays(5).toString(),
                "priority", "MEDIUM",
                "category", "SCHOOL"
        );

        mockMvc.perform(post("/api/items")
                        .header("X-User-ID", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.whatToDo").value("Write integration tests"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void updateItem_shouldUpdateStatusTo_DONE() throws Exception {
        Map<String, String> body = Map.of("status", "DONE");

        mockMvc.perform(put("/api/items/" + testTask.getId())
                        .header("X-User-ID", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void deleteItem_shouldRemoveTaskFromDatabase() throws Exception {
        mockMvc.perform(delete("/api/items/" + testTask.getId())
                        .header("X-User-ID", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        // Verify it's actually gone from the DB — not just the response
        assertFalse(taskRepository.findById(testTask.getId()).isPresent());
    }

    @Test
    void decompose_shouldReturnSubtasksFromGroqService() throws Exception {
        // Set up what the mocked GroqService returns
        List<CreateTaskRequest> fakeSubtasks = List.of(
                new CreateTaskRequest("Setup project",
                        LocalDate.now().plusDays(1), Task.Priority.HIGH, Task.Category.SCHOOL),
                new CreateTaskRequest("Define entities",
                        LocalDate.now().plusDays(2), Task.Priority.MEDIUM, Task.Category.SCHOOL)
        );
        when(groqService.decompose(any(Task.class))).thenReturn(fakeSubtasks);

        mockMvc.perform(post("/api/items/" + testTask.getId() + "/decompose")
                        .header("X-User-ID", testUser.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtasks.length()").value(2))
                .andExpect(jsonPath("$.subtasks[0].whatToDo").value("Setup project"))
                .andExpect(jsonPath("$.subtasks[1].priority").value("MEDIUM"));
    }
}