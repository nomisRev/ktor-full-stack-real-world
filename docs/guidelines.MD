# Kotlin Project Guidelines

## General Principles

- Write referentially transparent code
- Keep code concise and focused
- Follow idiomatic Kotlin practices
- Prioritize readability and maintainability
- Never leave unused imports
- Use List instead of MutableList, etc
- Use `val` instead of `var` by default
- Only use `var` when state mutation is necessary
- Use data classes for representing state
- Keep data classes focused and immutable
- Avoid throwing exceptions for control flow
- Prefer KotlinX libraries where available
- Maintain versions in the Version Catalog (libs.versions.toml)
- Use Kotlin Gradle Script (build.gradle.kts)
- Do not downgrade dependencies manually

## Architecture & Design

- Maintain a clear separation of concerns
- Use dependency injection where appropriate
- Follow structured concurrency (e.g., supervisorScope for fault isolation)
- Validate all external inputs (e.g., user data, API requests) to prevent injection attacks
- Avoid hardcoding secrets (use environment variables)
- Leverage higher-order functions (e.g., map, filter) and avoid imperative loops where possible

## Database & Resources

- Do not use Exposed DAO, only use the Exposed (Core) DSL
- Do not use `object XXXFactory`, use proper class and passing of arguments
- Make sure to close all Closeable and AutoCloseable resources
- Make sure to close Exposed Database using TransactionManager.closeAndUnregister(database)
- Use environment.monitor.subscribe(ApplicationStopped) { } to close resources and dependencies

## Testing

- Write unit tests for business logic
- Use Ktor's testing utilities for integration tests
- Follow TDD practices where possible
- Try to write as many failing tests as you can before fixing them
- Avoid writing many assertions in a test and try to keep the test small
- Use runBlocking only in tests, not production code
- Avoid using Thread.sleep(); use delay() instead
- Do not rely on static test data, but rely on unique test data so tests cannot conflict

## Code Style & Documentation

- Follow the official Kotlin Style Guide for consistency
- Use copy() carefully to avoid unintended side effects in immutable structures
- Don't write meaningless comments that express in natural text what is already clear from the function names or code
- Avoid overly verbose documentation comments that simply repeat property names and types

## Concurrency & Performance

- Follow structured concurrency principles
- Use delay() instead of Thread.sleep()
- Use copy() carefully with immutable data structures