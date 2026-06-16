package org.jetbrains.realworld.config

import io.ktor.server.application.Application
import kotlinx.serialization.Serializable
import org.jetbrains.realworld.article.ArticleRepository
import org.jetbrains.realworld.comment.CommentRepository
import org.jetbrains.realworld.profile.ProfileRepository
import org.jetbrains.realworld.user.Argon2Hasher
import org.jetbrains.realworld.user.UserService

class Dependencies(
    val jwt: JwtConfig,
    val users: UserService,
    val profiles: ProfileRepository,
    val comments: CommentRepository,
    val articles: ArticleRepository,
)

@Serializable
data class Config(
    val jwt: JwtConfig,
    val database: DatabaseConfig,
)

fun Application.dependencies(config: Config): Dependencies {
    val database = setupDatabase(config.database)
    val userService = UserService(config.jwt, database, Argon2Hasher())
    val profileRepository = ProfileRepository(database)
    val articleRepository = ArticleRepository(database, profileRepository)
    val commentRepository = CommentRepository(database, profileRepository)
    return Dependencies(config.jwt, userService, profileRepository, commentRepository, articleRepository)
}
