# Backend Structure and File Purpose Report

## 1. Directory Structure Overview

- **backend/**
  - **src/main/java/com/ai_planner/backend/**
    - **config/**: Configuration classes for Spring Boot (e.g., beans, security).
    - **controller/**: REST API controllers that handle HTTP requests.
    - **dto/**: Data Transfer Objects for request/response payloads.
    - **model/**: Entity classes representing database tables.
    - **repository/**: Interfaces for database access (Spring Data JPA).
    - **service/**: Business logic and service classes.
  - **src/main/resources/**
    - **application.properties**: Main configuration file for the Spring Boot app.
    - **static/**, **templates/**: For static files and server-side templates (not used in your code).
  - **src/test/java/com/ai_planner/backend/**
    - Test classes for integration and unit testing.
  - **pom.xml**: Maven build file, manages dependencies and project configuration.

---

## 2. Annotation Keyword List

| Annotation                  | Description                                                                 |
|-----------------------------|-----------------------------------------------------------------------------|
| @SpringBootApplication      | Marks the main class of a Spring Boot app; enables auto-configuration.      |
| @Configuration              | Declares a class as a source of bean definitions.                           |
| @Bean                       | Marks a method to produce a bean managed by Spring.                         |
| @EnableWebSecurity          | Enables Spring Security’s web security support.                             |
| @RestController             | Marks a class as a REST controller (returns JSON).                          |
| @RequestMapping             | Maps HTTP requests to handler methods/classes.                              |
| @PostMapping, @GetMapping   | Maps HTTP POST/GET requests to methods.                                     |
| @PutMapping, @DeleteMapping | Maps HTTP PUT/DELETE requests to methods.                                   |
| @ResponseStatus             | Sets the HTTP status for a response.                                        |
| @RequestBody                | Binds the HTTP request body to a method parameter.                          |
| @PathVariable               | Binds a URI template variable to a method parameter.                        |
| @Valid                      | Triggers validation on a method parameter.                                  |
| @NotBlank, @Email           | Validation constraints for fields.                                          |
| @Entity                     | Marks a class as a JPA entity (database table).                             |
| @Table                      | Specifies the table name for an entity.                                     |
| @Id                         | Marks the primary key field of an entity.                                   |
| @GeneratedValue             | Specifies how the primary key is generated.                                 |
| @ManyToOne, @JoinColumn     | Defines entity relationships and foreign keys.                              |
| @Enumerated                 | Specifies how enums are stored in the database.                             |
| @Column                     | Customizes a column in the database.                                        |
| @PrePersist                 | Marks a method to run before saving an entity.                              |
| @Repository                 | Marks a class as a Spring Data repository.                                  |
| @Service                    | Marks a class as a service (business logic).                                |
| @RequiredArgsConstructor    | Lombok: generates a constructor for all final fields.                       |
| @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor | Lombok: generates getters, setters, constructors, etc. |
| @Autowired                  | Injects dependencies automatically.                                         |
| @Value                      | Injects values from properties files.                                       |
| @Transactional              | Marks a method/class as transactional.                                      |
| @SpringBootTest             | Used in tests to load the full application context.                         |
| @AutoConfigureMockMvc       | Used in tests to auto-configure MockMvc for controller testing.             |
| @Test, @BeforeEach, @AfterEach | JUnit: marks test methods and setup/teardown methods.                  |
| @ExtendWith, @Mock, @InjectMocks, @Spy | Mockito: for mocking in unit tests.                             |

---

## 3. File-by-File Explanations

### Main Application

#### BackendApplication.java
- **Purpose**: Entry point for the Spring Boot application.
- **Main Method**: `public static void main(String[] args)` starts the app.
- **Annotations**: `@SpringBootApplication` (enables auto-configuration, component scanning).

---

### Configuration

#### config/AppConfig.java
- **Purpose**: Declares beans for dependency injection.
- **Main Method**: `public RestTemplate restTemplate()` provides a RestTemplate bean for HTTP calls.
- **Annotations**: `@Configuration`, `@Bean`.

#### config/SecurityConfig.java
- **Purpose**: Configures security (authentication, authorization).
- **Main Methods**:
  - `public SecurityFilterChain filterChain(HttpSecurity http)`: Configures HTTP security.
  - `public PasswordEncoder passwordEncoder()`: Provides a BCrypt password encoder bean.
- **Annotations**: `@Configuration`, `@EnableWebSecurity`, `@Bean`.

---

### Controllers

#### controller/AuthController.java
- **Purpose**: Handles authentication (register, login).
- **Main Methods**:
  - `register`: Registers a new user.
  - `login`: Authenticates a user.
- **Annotations**: `@RestController`, `@RequestMapping`, `@PostMapping`, `@ResponseStatus`, `@Valid`, `@RequestBody`, `@RequiredArgsConstructor`.

#### controller/TaskController.java
- **Purpose**: Handles task CRUD and decomposition.
- **Main Methods**:
  - `getItems`: Gets all tasks for a user.
  - `addItem`: Adds a new task.
  - `updateItem`: Updates a task.
  - `deleteItem`: Deletes a task.
  - `decompose`: Breaks a task into subtasks using AI.
- **Annotations**: `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@ResponseStatus`, `@PathVariable`, `@RequestBody`, `@Valid`, `@RequiredArgsConstructor`.

---

### DTOs (Data Transfer Objects)

#### dto/CreateTaskRequest.java, dto/UpdateTaskRequest.java, dto/LoginRequest.java, dto/RegisterRequest.java
- **Purpose**: Represent request payloads for creating/updating tasks and user authentication.
- **Fields**: Use validation annotations like `@NotBlank`, `@Email`.
- **Annotations**: `@NotBlank`, `@Email`.

#### dto/TaskResponse.java
- **Purpose**: Represents the response payload for a task.
- **Main Method**: `from(Task t)` static factory method to convert a Task entity to a response.
- **Annotations**: None (uses record syntax).

---

### Models

#### model/Task.java
- **Purpose**: Represents a task entity in the database.
- **Fields**: id, user, whatToDo, dueDate, priority, category, status, parent, createdAt.
- **Main Methods**: Getters/setters, builder, constructors (all generated by Lombok), `prePersist()` (sets createdAt and default status).
- **Annotations**: `@Entity`, `@Table`, `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@JoinColumn`, `@Enumerated`, `@Column`, `@PrePersist`.

#### model/User.java
- **Purpose**: Represents a user entity in the database.
- **Fields**: id, username, email, passwordHash, createdAt.
- **Main Methods**: Getters/setters, builder, constructors (Lombok), `prePersist()` (sets createdAt).
- **Annotations**: `@Entity`, `@Table`, `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Id`, `@GeneratedValue`, `@Column`, `@PrePersist`.

---

### Repositories

#### repository/TaskRepository.java, repository/UserRepository.java
- **Purpose**: Provide CRUD and custom queries for Task and User entities.
- **Main Methods**: Inherit CRUD methods from `JpaRepository`; custom methods like `findByUser`, `findByUsername`, etc., are auto-implemented by Spring Data JPA.
- **Annotations**: None (interface extends `JpaRepository`).

---

### Services

#### service/AuthService.java
- **Purpose**: Handles user registration and login logic.
- **Main Methods**: `register`, `login`.
- **Annotations**: `@Service`, `@RequiredArgsConstructor`.

#### service/TaskService.java
- **Purpose**: Handles business logic for tasks (CRUD, decomposition).
- **Main Methods**: `getTasksForUser`, `createTask`, `updateTask`, `deleteTask`, `decompose`, plus private helpers.
- **Annotations**: `@Service`, `@RequiredArgsConstructor`, `@Transactional`.

#### service/GroqService.java
- **Purpose**: Integrates with the Groq AI API to decompose tasks.
- **Main Methods**: `decompose`, `parseGroqResponse`.
- **Annotations**: `@Service`, `@RequiredArgsConstructor`, `@Value`.

---

### Configuration Files

#### application.properties
- **Purpose**: Configures database, JPA, and external API settings.
- **Examples**: Database URL, username, password, JPA settings, Groq API key and URL.

---

### Build File

#### pom.xml
- **Purpose**: Maven build file; manages dependencies (Spring Boot, JPA, Security, etc.), Java version, and project metadata.
- **Key Sections**: `<dependencies>`, `<properties>`, `<build>`.

---

### Test Files

#### BackendApplicationTests.java
- **Purpose**: Basic test to check if the Spring context loads.
- **Annotations**: `@SpringBootTest`, `@Test`.

#### controller/AuthControllerIntegrationTest.java, controller/TaskControllerIntegrationTest.java
- **Purpose**: Integration tests for controllers using MockMvc.
- **Annotations**: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Test`, `@Autowired`, `@AfterEach`.

#### service/AuthServiceTest.java, service/GroqServiceTest.java, service/TaskServiceTest.java
- **Purpose**: Unit tests for service classes using Mockito.
- **Annotations**: `@ExtendWith`, `@Mock`, `@InjectMocks`, `@Spy`, `@Test`, `@BeforeEach`.

---

This report should give a beginner a clear understanding of the structure, purpose, and key concepts in your Spring Boot backend. If you need further breakdowns or code examples for any section, let me know!
