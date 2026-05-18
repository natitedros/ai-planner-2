package com.ai_planner.backend.service;

import com.ai_planner.backend.dto.CreateTaskRequest;
import com.ai_planner.backend.model.Task;
import com.ai_planner.backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroqServiceTest {

    @Mock
    private RestTemplate restTemplate;

    // @Spy wraps a REAL ObjectMapper — we want actual JSON parsing, not a mock
    // Use @Spy when you want the real implementation but still want to verify calls
    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private GroqService groqService;

    // Helper: build a fake Task to pass into decompose()
    private Task buildTask() {
        User user = User.builder().id(1L).username("nat").build();
        return Task.builder()
                .id(10L)
                .user(user)
                .whatToDo("Build a Spring Boot REST API")
                .dueDate(LocalDate.now().plusDays(14))
                .priority(Task.Priority.HIGH)
                .category(Task.Category.SCHOOL)
                .build();
    }

    // Helper: build the nested Map structure that Groq actually returns
    private Map<String, Object> buildGroqResponse(String contentJson) {
        return Map.of(
                "choices", List.of(
                        Map.of("message",
                                Map.of("content", contentJson)
                        )
                )
        );
    }

    @Test
    void decompose_withValidResponse_shouldReturnParsedSubtasks() {
        // ARRANGE
        String fakeGroqContent = """
                {
                  "subtasks": [
                    {
                      "what_to_do": "Set up Spring Initializr project",
                      "due_date": "%s",
                      "priority": "HIGH",
                      "category": "SCHOOL"
                    },
                    {
                      "what_to_do": "Define JPA entities",
                      "due_date": "%s",
                      "priority": "MEDIUM",
                      "category": "SCHOOL"
                    }
                  ]
                }
                """.formatted(
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(4)
        );

        Map<String, Object> fakeResponse = buildGroqResponse(fakeGroqContent);

        // Tell the mock RestTemplate to return our fake response
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(fakeResponse, HttpStatus.OK));

        // ACT
        List<CreateTaskRequest> result = groqService.decompose(buildTask());

        // ASSERT
        assertEquals(2, result.size());
        assertEquals("Set up Spring Initializr project", result.get(0).whatToDo());
        assertEquals(Task.Priority.HIGH, result.get(0).priority());
        assertEquals(Task.Category.SCHOOL, result.get(0).category());
        assertEquals("Define JPA entities", result.get(1).whatToDo());
        assertEquals(Task.Priority.MEDIUM, result.get(1).priority());
    }

    @Test
    void decompose_withMoreThan7Subtasks_shouldCapAt7() {
        // Build a response with 9 subtasks — service must limit to 7
        StringBuilder subtasks = new StringBuilder("[");
        for (int i = 1; i <= 9; i++) {
            subtasks.append("""
                    {"what_to_do": "Subtask %d", "due_date": "%s",
                     "priority": "LOW", "category": "PERSONAL"}
                    """.formatted(i, LocalDate.now().plusDays(i)));
            if (i < 9) subtasks.append(",");
        }
        subtasks.append("]");

        String content = "{\"subtasks\":" + subtasks + "}";
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(buildGroqResponse(content), HttpStatus.OK));

        List<CreateTaskRequest> result = groqService.decompose(buildTask());

        assertEquals(7, result.size()); // capped at 7
    }

    @Test
    void decompose_withInvalidPriority_shouldFallbackToMedium() {
        String content = """
                {"subtasks": [{"what_to_do": "Some task", "due_date": "%s",
                 "priority": "URGENT", "category": "WORK"}]}
                """.formatted(LocalDate.now().plusDays(1));

        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(buildGroqResponse(content), HttpStatus.OK));

        List<CreateTaskRequest> result = groqService.decompose(buildTask());

        // "URGENT" is not a valid Priority enum value — should fall back to MEDIUM
        assertEquals(Task.Priority.MEDIUM, result.get(0).priority());
    }

    @Test
    void decompose_whenRestTemplateFails_shouldThrow502() {
        // Simulate a network error or Groq being down
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> groqService.decompose(buildTask())
        );

        assertEquals(502, ex.getStatusCode().value());
    }
}
