# Backend Service Cursor Rules

This repository contains Cursor rule definitions that we follow when developing backend services (with a focus on production-grade Spring Boot code).

The canonical sources are the files under `.cursor/rules/*.mdc`. The README below summarizes those rules in a GitHub-friendly format.

## Code Quality Standards

- Generated code must follow these rules.
- Naming:
  - Use descriptive names (e.g., `userRepository`, `paymentProcessor`, `retryPolicy`).
- Method Size:
  - Prefer methods under 30 lines.
  - Break large methods into smaller functions.
- Logging:
  - Add meaningful logs for external calls, errors, retries, and critical flows.
- Exception Handling:
  - Use structured exception handling.
  - Avoid generic exceptions.
  - Prefer domain-specific exceptions.
- Testing:
  - Generated code should be testable, loosely coupled, and dependency injectable.

## Engineering Principles

- SOLID:
  - Single Responsibility: each class should have one responsibility.
  - Open/Closed: code should be open for extension but closed for modification.
  - Liskov Substitution: subtypes must be replaceable for their base types.
  - Interface Segregation: prefer small interfaces over large ones.
  - Dependency Inversion: depend on abstractions rather than concrete classes.
- OOP:
  - Prefer: encapsulation, composition over inheritance, immutability when possible, clear abstractions.
  - Avoid: god objects, tight coupling, static state when unnecessary.
- Code Design:
  - Ensure high cohesion, loose coupling, and clear boundaries between modules.

## Automatic Design Pattern Selection

- Before implementing complex logic, evaluate if a design pattern improves maintainability.
- Creational Patterns:
  - Factory Pattern:
    - Use when object creation logic becomes complex.
    - Use when multiple implementations exist.
    - Use when object creation should be abstracted.
  - Builder Pattern:
    - Use when objects have many optional parameters.
    - Use when object construction must be readable.
    - Use when immutable objects are desired.
- Structural Patterns:
  - Adapter Pattern: use when integrating incompatible interfaces.
  - Facade Pattern: use when simplifying access to complex subsystems.
  - Decorator Pattern: use when extending functionality dynamically without modifying base classes.
- Behavioral Patterns:
  - Strategy Pattern: use when multiple algorithms exist for the same task.
  - Observer Pattern: use when components need event notifications.
  - Command Pattern: use when requests must be encapsulated as objects.
- Pattern Guidelines:
  - Only apply patterns when they improve extensibility, readability, or testability.
  - Avoid unnecessary abstraction; prefer simple solutions.

## Architecture Guidelines

- Before writing code:
  - Identify the responsibilities of the component.
  - Determine if a design pattern is appropriate.
  - Ensure SOLID compliance.
  - Ensure the code remains testable.
- Layered Architecture:
  - Use clear separation of concerns.
  - Example layers:
    - Controller / API Layer
    - Service / Business Layer
    - Repository / Data Layer
    - Infrastructure Layer
- Responsibilities:
  - Controller:
    - Handles request and response
    - No business logic
  - Service:
    - Contains domain logic
    - Coordinates repositories
  - Repository:
    - Data access only
- Dependency Flow:
  - Controller -> Service -> Repository
  - Never reverse dependencies.
- Scalability:
  - Prefer stateless services, idempotent operations, and async processing when suitable.
- Error Handling:
  - Never swallow exceptions.
  - Convert internal exceptions to domain exceptions.

## Concurrency Rules

- Ensure thread safety when dealing with shared resources.
- Prefer:
  - `ExecutorService`
  - `CompletableFuture`
  - `ConcurrentHashMap`
- Avoid:
  - `synchronized` blocks unless necessary
  - shared mutable state
- Ensure:
  - idempotent operations
  - safe retries

## Spring Boot Backend Standards

Use production-grade practices when generating Spring Boot code.

### Project Structure

Prefer this structure:

- `controller/`
- `service/`
- `repository/`
- `dto/`
- `entity/`
- `config/`
- `exception/`
- `util/`

### Controllers

- Responsibilities:
  - request validation
  - request mapping
  - response transformation
- Controllers must not contain business logic.

### Services

- Use interface + implementation pattern.
- Example:
  - `UserService`
  - `UserServiceImpl`
- Responsibilities:
  - business logic
  - coordination between repositories
  - transaction management

### Dependency Injection

- Always use constructor injection.
- Avoid field injection.

### DTO Usage

- Never expose entity classes directly in APIs.
- Always use DTOs.

### Exception Handling

- Use centralized exception handling (e.g., `@ControllerAdvice`).
- Create custom exceptions for domain errors.

### Logging

- Use structured logging.
- Log important events:
  - external API calls
  - retries
  - failures
  - transaction boundaries

### Database Access

- Use repositories only for persistence logic.
- Never place business logic inside repositories.

### Transactions

- Use `@Transactional` at the service layer.

### Validation

Use:

- `@Valid`
- `@NotNull`
- `@NotBlank`

### Resilience

For external services prefer:

- retry
- timeout
- circuit breaker

### Security

Always validate:

- authentication
- authorization
- input data

### Performance

Prefer:

- caching
- pagination
- async processing for heavy tasks

