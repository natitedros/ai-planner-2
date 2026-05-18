package com.ai_planner.backend.repository;

import com.ai_planner.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

//    Builtin CRUD methods are provided by JpaRepository

//    Custom query methods that get implemented by JPA on runtime
//    based on method naming conventions
    Optional<User> findByUsername(String username);
    boolean existsByUsernameOrEmail(String username, String email);

}
