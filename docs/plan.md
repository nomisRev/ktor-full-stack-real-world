# RealWorld Conduit API Implementation Plan

This document outlines the plan for implementing the RealWorld Conduit API using Kotlin, Ktor, Exposed, and PostgreSQL. The plan is divided into sprints, each containing specific issues to be implemented.

## Sprint 1: Project Setup and Core Infrastructure

### Project Structure and Configuration

- [x] **Issue 1.1: Project Structure Setup**
  - Set up the basic project structure following Ktor conventions
  - Configure Gradle build files
  - Set up the Version Catalog (libs.versions.toml)
  - Configure application.conf for Ktor

- [x] **Issue 1.2: Database Configuration**
  - Set up PostgreSQL connection
  - Configure Exposed ORM
  - Implement database migration strategy (Flyway or similar)
  - Create database connection pooling

- [x] **Issue 1.3: Authentication Infrastructure**
  - Implement JWT token generation and validation
  - Set up authentication middleware in Ktor
  - Configure security settings

- [x] **Issue 1.4: Error Handling and Validation**
  - Implement global error handling
  - Set up request validation using Ktor's validation feature
  - Create standardized error responses

- [x] **Issue 1.5: Logging and Metrics**
  - Configure logging
  - Set up Micrometer metrics
  - Implement request logging middleware

## Sprint 2: User Management and Authentication

- [x] **Issue 2.1: User Data Model**
  - Create User table in the database
  - Implement User data classes
  - Set up password hashing with Argon2

- [x] **Issue 2.2: User Registration**
  - Implement `/users` POST endpoint (OpenAPI: `/users` POST, `NewUserRequest`, `UserResponse`)
  - Add validation for user registration
  - Handle duplicate email/username errors

- [x] **Issue 2.3: User Login**
  - Implement `/users/login` POST endpoint (OpenAPI: `/users/login` POST, `LoginUserRequest`, `UserResponse`)
  - Add validation for login credentials
  - Generate and return JWT token

- [x] **Issue 2.4: Current User Retrieval**
  - Implement `/user` GET endpoint (OpenAPI: `/user` GET, `UserResponse`)
  - Add authentication middleware
  - Return current user information

- [x] **Issue 2.5: User Update**
  - Implement `/user` PUT endpoint (OpenAPI: `/user` PUT, `UpdateUserRequest`, `UserResponse`)
  - Add validation for user updates
  - Handle email/username uniqueness

## Sprint 3: Profile Management

- [x] **Issue 3.1: Profile Data Model**
  - Create necessary database tables/relations
  - Implement Profile data classes (OpenAPI: `Profile` schema)

- [x] **Issue 3.2: Profile Retrieval**
  - Implement `/profiles/{username}` GET endpoint (OpenAPI: `/profiles/{username}` GET, `ProfileResponse`)
  - Handle non-existent profiles

- [x] **Issue 3.3: Follow User**
  - Implement `/profiles/{username}/follow` POST endpoint (OpenAPI: `/profiles/{username}/follow` POST, `ProfileResponse`)
  - Create follow relationship in database

- [x] **Issue 3.4: Unfollow User**
  - Implement `/profiles/{username}/follow` DELETE endpoint (OpenAPI: `/profiles/{username}/follow` DELETE, `ProfileResponse`)
  - Remove follow relationship from database

## Sprint 4: Article Management

- [x] **Issue 4.1: Article Data Model**
  - Create Article table in the database
  - Implement Article data classes (OpenAPI: `Article` schema)
  - Set up relationships with User and Tag models

- [x] **Issue 4.2: Create Article**
  - Implement `/articles` POST endpoint (OpenAPI: `/articles` POST, `NewArticleRequest`, `SingleArticleResponse`)
  - Add validation for article creation
  - Handle tag creation/association
  - Generate article slug

- [x] **Issue 4.3: Get Articles**
  - Implement `/articles` GET endpoint (OpenAPI: `/articles` GET, `MultipleArticlesResponse`)
  - Add filtering by tag, author, favorited
  - Implement pagination (offset, limit)

- [x] **Issue 4.4: Feed Articles**
  - Implement `/articles/feed` GET endpoint (OpenAPI: `/articles/feed` GET, `MultipleArticlesResponse`)
  - Filter articles by followed users
  - Implement pagination

- [x] **Issue 4.5: Get Article**
  - Implement `/articles/{slug}` GET endpoint (OpenAPI: `/articles/{slug}` GET, `SingleArticleResponse`)
  - Handle non-existent articles

- [x] **Issue 4.6: Update Article**
  - Implement `/articles/{slug}` PUT endpoint (OpenAPI: `/articles/{slug}` PUT, `UpdateArticleRequest`, `SingleArticleResponse`)
  - Add validation for article updates
  - Handle slug updates if title changes

- [x] **Issue 4.7: Delete Article**
  - Implement `/articles/{slug}` DELETE endpoint (OpenAPI: `/articles/{slug}` DELETE, `EmptyOkResponse`)
  - Add authorization check (only author can delete)
  - Handle cascading deletes (comments, favorites)

## Sprint 5: Comments and Favorites

- [x] **Issue 5.1: Comment Data Model**
  - Create Comment table in the database
  - Implement Comment data classes (OpenAPI: `Comment` schema)
  - Set up relationships with Article and User models

- [x] **Issue 5.2: Create Comment**
  - Implement `/articles/{slug}/comments` POST endpoint (OpenAPI: `/articles/{slug}/comments` POST, `NewCommentRequest`, `SingleCommentResponse`)
  - Add validation for comment creation

- [x] **Issue 5.3: Get Comments**
  - Implement `/articles/{slug}/comments` GET endpoint (OpenAPI: `/articles/{slug}/comments` GET, `MultipleCommentsResponse`)
  - Handle article not found

- [x] **Issue 5.4: Delete Comment**
  - Implement `/articles/{slug}/comments/{id}` DELETE endpoint (OpenAPI: `/articles/{slug}/comments/{id}` DELETE, `EmptyOkResponse`)
  - Add authorization check (only author can delete)

- [x] **Issue 5.5: Favorite Article**
  - Implement `/articles/{slug}/favorite` POST endpoint (OpenAPI: `/articles/{slug}/favorite` POST, `SingleArticleResponse`)
  - Update favorites count in Article

- [x] **Issue 5.6: Unfavorite Article**
  - Implement `/articles/{slug}/favorite` DELETE endpoint (OpenAPI: `/articles/{slug}/favorite` DELETE, `SingleArticleResponse`)
  - Update favorites count in Article

## Sprint 6: Tags and Final Features

- [x] **Issue 6.1: Tag Data Model**
  - Create Tag table in the database
  - Implement Tag data classes (OpenAPI: Tag string array in `Article` schema)
  - Set up many-to-many relationship with Articles

- [x] **Issue 6.2: Get Tags**
  - Implement `/tags` GET endpoint (OpenAPI: `/tags` GET, `TagsResponse`)
  - Return all unique tags in the system

- [x] **Issue 6.3: Integration Testing**
  - Set up integration tests using Ktor's testing utilities
  - Test all API endpoints
  - Verify against the Postman collection

- [x] **Issue 6.4: Performance Optimization**
  - Optimize database queries
  - Add caching where appropriate
  - Ensure efficient pagination

## Sprint 7: Documentation and Deployment

- [ ] **Issue 7.1: API Documentation**
  - Generate API documentation from OpenAPI spec
  - Add usage examples

- [ ] **Issue 7.2: Deployment Configuration**
  - Set up Docker configuration
  - Create docker-compose for local development
  - Configure environment variables

- [ ] **Issue 7.3: CI/CD Pipeline**
  - Set up GitHub Actions for CI/CD
  - Configure automated testing
  - Set up deployment workflow

- [ ] **Issue 7.4: Final Testing**
  - Run the provided API tests
  - Fix any remaining issues
  - Ensure all endpoints work as expected

## Technical Considerations

Throughout the implementation, we will adhere to the following principles:

1. **Immutability**: Use immutable data structures where possible
2. **Functional Programming**: Prefer pure functions and avoid side effects
3. **Clean Architecture**: Separate concerns into appropriate layers
4. **Testing**: Write unit and integration tests for all functionality
5. **Security**: Properly handle authentication, authorization, and input validation
6. **Performance**: Optimize database queries and use connection pooling
7. **Documentation**: Document code and APIs thoroughly

## Database Schema

The database schema will include the following main tables:

1. **Users**: Store user information (username, email, password hash, bio, image)
2. **Articles**: Store article information (slug, title, description, body, created/updated dates)
3. **Comments**: Store comments on articles (body, created/updated dates)
4. **Tags**: Store unique tags
5. **ArticleTags**: Many-to-many relationship between articles and tags
6. **Favorites**: Store which users have favorited which articles
7. **Follows**: Store which users follow which other users

## API Endpoints

The API will implement all endpoints as specified in the OpenAPI specification, including:

- Authentication endpoints (`/users/login`, `/users`)
- User endpoints (`/user`)
- Profile endpoints (`/profiles/{username}`, `/profiles/{username}/follow`)
- Article endpoints (`/articles`, `/articles/feed`, `/articles/{slug}`)
- Comment endpoints (`/articles/{slug}/comments`, `/articles/{slug}/comments/{id}`)
- Favorite endpoints (`/articles/{slug}/favorite`)
- Tag endpoints (`/tags`)
