package com.ai_planner.backend.repository;

import com.ai_planner.backend.model.Task;
import com.ai_planner.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

//  Builtin CRUD methods are provided by JpaRepository

//    Custom query methods that get implemented by JPA on runtime
//    based on method naming conventions
    List<Task> findByUser(User user);
    void deleteByParent(Task parent);
    List<Task> findByUserAndParentIsNull(User user);
}
