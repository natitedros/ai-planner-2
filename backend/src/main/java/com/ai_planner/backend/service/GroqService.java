package com.ai_planner.backend.service;

import com.ai_planner.backend.dto.CreateTaskRequest;
import com.ai_planner.backend.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqService {

    // @Value reads from application.properties at startup and injects the string.
    // This is a different form of DI — not a bean, but a config value.
    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    // RestTemplate is a @Bean declared in AppConfig — Spring injects it here
    private final RestTemplate restTemplate;

    // ObjectMapper (Jackson) is auto-configured by Spring Boot — also injectable
    private final ObjectMapper objectMapper;

    public List<CreateTaskRequest> decompose(Task task) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); // sets "Authorization: Bearer <key>"

        String today = LocalDate.now().toString();

        String systemPrompt = """
                You are a task planning assistant. Break the given task into clear, actionable subtasks.
                Return ONLY a JSON object — no prose, no markdown.
                The JSON must exactly match this schema:
                {"subtasks": [{"what_to_do": "string", "due_date": "YYYY-MM-DD",
                "priority": "HIGH|MEDIUM|LOW", "category": "SCHOOL|WORK|PERSONAL|HEALTH|SOCIAL"}]}
                Today is %s. Return at most 7 subtasks.
                Assign realistic due dates relative to today and the task's due date.
                Pick the most fitting priority and category for each subtask.
                """.formatted(today);

        // Build the request body as a plain Map — Jackson serializes this to JSON
        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content",
                                "Break down this task: " + task.getWhatToDo()
                                        + " with due date " + task.getDueDate())
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        // exchange() makes the HTTP call and deserializes the response body into Map
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(apiUrl, HttpMethod.POST, httpEntity, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Groq API call failed: " + e.getMessage()
            );
        }

        return parseGroqResponse(response.getBody(), task);
    }

    @SuppressWarnings("unchecked")
    private List<CreateTaskRequest> parseGroqResponse(Map<?, ?> responseBody, Task parentTask) {
        try {
            // Groq wraps the model output in: choices[0].message.content (a JSON string)
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            String contentJson = (String) message.get("content");

            // Parse the inner JSON string into a Map
            Map<String, Object> parsed = objectMapper.readValue(contentJson, Map.class);
            List<Map<String, Object>> subtasksRaw =
                    (List<Map<String, Object>>) parsed.get("subtasks");

            // Map each raw subtask map → CreateTaskRequest
            return subtasksRaw.stream()
                    .limit(7)
                    .map(s -> {
                        String priorityStr = (String) s.getOrDefault("priority", "MEDIUM");
                        String categoryStr = (String) s.getOrDefault("category", "PERSONAL");
                        String dueDateStr  = (String) s.getOrDefault(
                                "due_date", LocalDate.now().toString()
                        );

                        Task.Priority priority;
                        Task.Category category;
                        try {
                            priority = Task.Priority.valueOf(priorityStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            priority = Task.Priority.MEDIUM;
                        }
                        try {
                            category = Task.Category.valueOf(categoryStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            category = Task.Category.PERSONAL;
                        }

                        return new CreateTaskRequest(
                                (String) s.getOrDefault("what_to_do", ""),
                                LocalDate.parse(dueDateStr),
                                priority,
                                category
                        );
                    })
                    .toList();

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to parse Groq response: " + e.getMessage()
            );
        }
    }
}
