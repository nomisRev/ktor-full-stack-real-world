# RealWorld Conduit API

This project is an implementation of the [RealWorld](https://github.com/gothinkster/realworld) backend API specification using Kotlin and Ktor. It provides a real-world example of a medium.com clone.

## Project Overview

The RealWorld Conduit API provides endpoints for:
- User authentication (registration, login)
- User profiles (get profile, follow/unfollow)
- More features coming soon (articles, comments, favorites)

## Tech Stack

- [Kotlin](https://kotlinlang.org/) - Programming language
- [Ktor](https://ktor.io/) - Web framework
- [Exposed](https://github.com/JetBrains/Exposed) - SQL framework
- [PostgreSQL](https://www.postgresql.org/) - Database
- [JWT](https://jwt.io/) - Authentication
- [Argon2](https://github.com/P-H-C/phc-winner-argon2) - Password hashing
- [Flyway](https://flywaydb.org/) - Database migrations

## Development Guidelines

For detailed development guidelines, please refer to the [Guidelines](docs/guidelines.MD) document.

## Useful Links

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- [RealWorld API Spec](https://github.com/gothinkster/realworld/tree/master/api)

## Implemented Features

The project currently implements the following features from the RealWorld API specification:

### User Authentication
- Registration: `POST /users` - Register a new user
- Login: `POST /users/login` - Login a user
- Current User: `GET /user` - Get the current user
- Update User: `PUT /user` - Update the current user

### Profiles
- Get Profile: `GET /profiles/{username}` - Get a user's profile
- Follow User: `POST /profiles/{username}/follow` - Follow a user
- Unfollow User: `DELETE /profiles/{username}/follow` - Unfollow a user

## Planned Features

The following features are planned for future implementation:

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
- Docker and Docker Compose (for running PostgreSQL)

### Database Setup

The project uses PostgreSQL as the database. You can start it using Docker Compose:

```bash
docker-compose up -d
```

This will start a PostgreSQL instance with the configuration specified in the `docker-compose.yaml` file.

### Running the Application

To build and run the application, use one of the following Gradle tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `./gradlew run`               | Run the server                                                       |
| `./gradlew buildFatJar`       | Build an executable JAR with all dependencies included               |
| `./gradlew runDocker`         | Run using Docker (requires Docker to be installed)                   |

### Configuration

The application can be configured using environment variables or by modifying the `application.yaml` file in the `src/main/resources` directory.

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

### API Documentation

Once the server is running, you can access the API at `http://localhost:8080`. The API follows the [RealWorld API specification](https://github.com/gothinkster/realworld/tree/master/api).
