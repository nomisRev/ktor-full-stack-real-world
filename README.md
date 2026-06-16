# RealWorld Conduit - Kotlin Multiplatform Project

This project is a Kotlin Multiplatform implementation of the [RealWorld](https://github.com/gothinkster/realworld) application, providing a medium.com clone with both backend and frontend components. It demonstrates how to build a full-stack application using Kotlin across multiple platforms.

## Project Structure

The project is organized into several modules:

### Backend
A Ktor-based server that implements the RealWorld API specification, providing endpoints for user authentication, profiles, articles, comments, and more.

### Conduit API
A Kotlin Multiplatform library that defines the API contract (resources, data models) shared between the backend and client applications. This enables type-safe API communication across all platforms.

### App
A Kotlin Multiplatform UI application split into platform app modules and a shared Compose Multiplatform module:
- `app/shared` contains shared UI and client logic
- `app/androidApp` contains the Android app host
- `app/desktopApp` contains the Desktop (JVM) app host
- `app/webApp` contains the Web app host
- `app/iosApp` contains the native iOS app that integrates with the shared Kotlin framework

## Tech Stack

### Backend
- [Kotlin](https://kotlinlang.org/) - Programming language
- [Ktor](https://ktor.io/) - Web framework
- [Exposed](https://github.com/JetBrains/Exposed) - SQL framework
- [PostgreSQL](https://www.postgresql.org/) - Database
- [JWT](https://jwt.io/) - Authentication
- [Argon2](https://github.com/P-H-C/phc-winner-argon2) - Password hashing
- [Flyway](https://flywaydb.org/) - Database migrations
- [Micrometer](https://micrometer.io/) - Metrics collection
- [Prometheus](https://prometheus.io/) - Monitoring

### Frontend
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) - Cross-platform Kotlin
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - UI framework
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Asynchronous programming
- [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization) - JSON serialization
- [KotlinX DateTime](https://github.com/Kotlin/kotlinx-datetime) - Date and time handling

## Development Guidelines

For detailed development guidelines, please refer to the [Guidelines](docs/guidelines.MD) document. These guidelines cover:

- Kotlin coding practices
- Architecture and design principles
- Database and resource management
- Testing strategies
- Code style and documentation
- Concurrency and performance considerations

## Useful Links

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Ktor Documentation](https://ktor.io/docs/home.html)
- [RealWorld API Spec](https://github.com/gothinkster/realworld/tree/master/api)

## API Features

The backend implements the following features from the RealWorld API specification:

### User Authentication
- Registration: `POST /users` - Register a new user
- Login: `POST /users/login` - Login a user
- Current User: `GET /user` - Get the current user
- Update User: `PUT /user` - Update the current user

### Profiles
- Get Profile: `GET /profiles/{username}` - Get a user's profile
- Follow User: `POST /profiles/{username}/follow` - Follow a user
- Unfollow User: `DELETE /profiles/{username}/follow` - Unfollow a user

### Articles
- Create Article: `POST /articles` - Create a new article
- List Articles: `GET /articles` - List articles with filtering
- Feed Articles: `GET /articles/feed` - Get articles from followed users
- Get Article: `GET /articles/{slug}` - Get an article
- Update Article: `PUT /articles/{slug}` - Update an article
- Delete Article: `DELETE /articles/{slug}` - Delete an article

### Comments
- Add Comment: `POST /articles/{slug}/comments` - Add a comment to an article
- Get Comments: `GET /articles/{slug}/comments` - Get comments for an article
- Delete Comment: `DELETE /articles/{slug}/comments/{id}` - Delete a comment

### Favorites
- Favorite Article: `POST /articles/{slug}/favorite` - Favorite an article
- Unfavorite Article: `DELETE /articles/{slug}/favorite` - Unfavorite an article

### Tags
- Get Tags: `GET /tags` - Get popular tags

## Building & Running

### Prerequisites

- JDK 11 or higher
- Android SDK (for Android development)
- Xcode (for iOS development)
- Docker and Docker Compose (for running PostgreSQL)

### Database Setup

The backend uses PostgreSQL as the database. You can start it using Docker Compose:

```bash
docker-compose up -d
```

This will start a PostgreSQL instance with the configuration specified in the `docker-compose.yaml` file.

### Running the Backend

To build and run the backend, use one of the following Gradle tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew :backend:test`     | Run the backend tests                                                |
| `./gradlew :backend:build`    | Build the backend                                                    |
| `./gradlew :backend:run`      | Run the backend server                                               |
| `./gradlew :backend:buildFatJar` | Build an executable JAR with all dependencies included            |

### Running the Compose apps

To run the Compose apps on different platforms:

| Task                                   | Description                                                  |
| ---------------------------------------|-------------------------------------------------------------- |
| `./gradlew :app:androidApp:assembleDebug` | Build the Android app                                    |
| `./gradlew :app:desktopApp:run`       | Run on desktop                                               |
| `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` | Run in browser (WebAssembly)                 |

### Running the iOS App

Open the `app/iosApp/iosApp.xcodeproj` file in Xcode and run the application from there.

### Configuration

The backend can be configured using environment variables or by modifying the `application.yaml` file in the `backend/src/main/resources` directory.

Key configuration properties:

- `database.host`: PostgreSQL host (default: localhost)
- `database.port`: PostgreSQL port (default: 5432)
- `database.name`: Database name (default: realworld)
- `database.username`: Database username (default: postgres)
- `database.password`: Database password (default: postgres)
- `jwt.secret`: Secret key for JWT token generation
- `jwt.issuer`: Issuer for JWT tokens
- `jwt.audience`: Audience for JWT tokens
- `jwt.realm`: Realm for JWT authentication

## API Documentation

Once the backend server is running, you can access the API at `http://localhost:8080`. The API follows the [RealWorld API specification](https://github.com/gothinkster/realworld/tree/master/api).
