package com.ai_planner.backend.service;

import com.ai_planner.backend.dto.LoginRequest;
import com.ai_planner.backend.dto.RegisterRequest;
import com.ai_planner.backend.model.User;
import com.ai_planner.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service                   // ← registers this class as a Spring-managed bean
@RequiredArgsConstructor   // ← Lombok generates constructor for all final fields
public class AuthService {

    // Spring injects these automatically via the generated constructor.
    // Both are beans: UserRepository is a Spring Data proxy, PasswordEncoder
    // is the BCryptPasswordEncoder declared as @Bean in SecurityConfig.
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

//    Register implementation
    public Map<String, Object> register(RegisterRequest req) {
        // Check uniqueness before doing any work
        if (userRepository.existsByUsernameOrEmail(req.username(), req.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username or email already exists"
            );
        }

        // Build the entity — never set passwordHash to the raw string
        User user = User.builder()
                .username(req.username())
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password())) // BCrypt hash
                .build();

        // save() both inserts and returns the persisted entity with its generated ID
        user = userRepository.save(user);

        return Map.of(
                "user_id", user.getId(),
                "username", user.getUsername()
        );
    }

//    Login implementation
    public Map<String, Object> login(LoginRequest req) {
        // findByUsername returns Optional<User> — orElseThrow handles the missing case
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password"
                ));

        // passwordEncoder.matches(rawPassword, storedHash) — never compare strings directly
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid username or password"
            );
        }

        // At this point the user is verified. Later you'd issue a JWT here.
        return Map.of(
                "user_id", user.getId(),
                "username", user.getUsername()
        );
    }
}
