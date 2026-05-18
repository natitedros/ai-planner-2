package com.ai_planner.backend.controller;

import com.ai_planner.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest boots the FULL application context with H2 (from test application.properties)
// @AutoConfigureMockMvc wires in MockMvc automatically
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;

    // Clean up after each test so tests don't interfere with each other
    // (Instead of @Transactional, which can hide real rollback bugs in controllers)
    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void register_withValidData_shouldReturn201WithUserInfo() throws Exception {
        Map<String, String> body = Map.of(
                "username", "nat",
                "email", "nat@test.com",
                "password", "password123"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nat"))
                .andExpect(jsonPath("$.user_id").isNumber()); // DB assigned a real ID
    }

    @Test
    void register_withDuplicateUsername_shouldReturn409() throws Exception {
        Map<String, String> body = Map.of(
                "username", "nat",
                "email", "nat@test.com",
                "password", "password123"
        );

        // First registration — should succeed
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Second registration with same username — should fail
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_withValidCredentials_shouldReturn200() throws Exception {
        // Register first so the user exists in H2
        Map<String, String> registerBody = Map.of(
                "username", "nat",
                "email", "nat@test.com",
                "password", "password123"
        );
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        // Now login
        Map<String, String> loginBody = Map.of(
                "username", "nat",
                "password", "password123"
        );
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("nat"))
                .andExpect(jsonPath("$.user_id").isNumber());
    }

    @Test
    void login_withWrongPassword_shouldReturn401() throws Exception {
        Map<String, String> registerBody = Map.of(
                "username", "nat", "email", "nat@test.com", "password", "correctpass"
        );
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        Map<String, String> loginBody = Map.of(
                "username", "nat",
                "password", "wrongpass"
        );
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized());
    }
}
