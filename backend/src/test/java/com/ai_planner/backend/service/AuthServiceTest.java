package com.ai_planner.backend.service;

import com.ai_planner.backend.dto.LoginRequest;
import com.ai_planner.backend.dto.RegisterRequest;
import com.ai_planner.backend.model.User;
import com.ai_planner.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// This annotation tells JUnit to use Mockito — no Spring context is started at all
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock creates a fake UserRepository — no real DB calls happen
    @Mock
    private UserRepository userRepository;

    // @Mock creates a fake PasswordEncoder — no real BCrypt hashing happens
    @Mock
    private PasswordEncoder passwordEncoder;

    // @InjectMocks creates a REAL AuthService and injects the mocks above into it
    // This is equivalent to: new AuthService(userRepository, passwordEncoder)
    @InjectMocks
    private AuthService authService;

    // --- register() tests ---

    @Test
    void register_withValidData_shouldReturnUserIdAndUsername() {
        // ARRANGE — set up what the mocks should return when called
        when(userRepository.existsByUsernameOrEmail("nat", "nat@test.com"))
                .thenReturn(false); // user doesn't exist yet

        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$hashed"); // fake hash

        // Build the User that save() will return (with a generated ID)
        User savedUser = User.builder()
                .id(1L)
                .username("nat")
                .email("nat@test.com")
                .passwordHash("$2a$hashed")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // ACT — call the real method
        var result = authService.register(
                new RegisterRequest("nat", "nat@test.com", "password123")
        );

        // ASSERT — verify what came back
        assertEquals(1L, result.get("user_id"));
        assertEquals("nat", result.get("username"));

        // Verify that save() was called exactly once with any User object
        verify(userRepository, times(1)).save(any(User.class));

        // Verify the password was encoded — never stored raw
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void register_withDuplicateUser_shouldThrow409() {
        // ARRANGE — simulate that the user already exists
        when(userRepository.existsByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(true);

        // ACT + ASSERT — expect an exception to be thrown
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(
                        new RegisterRequest("nat", "nat@test.com", "password123")
                )
        );

        assertEquals(409, ex.getStatusCode().value());

        // Verify save() was NEVER called — we shouldn't write to the DB
        verify(userRepository, never()).save(any());
    }

    // --- login() tests ---

    @Test
    void login_withValidCredentials_shouldReturnUserIdAndUsername() {
        User existingUser = User.builder()
                .id(1L)
                .username("nat")
                .passwordHash("$2a$hashed")
                .build();

        when(userRepository.findByUsername("nat")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);

        var result = authService.login(new LoginRequest("nat", "password123"));

        assertEquals(1L, result.get("user_id"));
        assertEquals("nat", result.get("username"));
    }

    @Test
    void login_withWrongPassword_shouldThrow401() {
        User existingUser = User.builder()
                .id(1L)
                .username("nat")
                .passwordHash("$2a$hashed")
                .build();

        when(userRepository.findByUsername("nat")).thenReturn(Optional.of(existingUser));
        // passwords.matches returns false — wrong password
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new LoginRequest("nat", "wrongpassword"))
        );

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void login_withNonExistentUser_shouldThrow401() {
        // findByUsername returns empty Optional — user doesn't exist
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new LoginRequest("ghost", "password123"))
        );

        assertEquals(401, ex.getStatusCode().value());
    }
}
