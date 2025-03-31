package org.jetbrains.realworld.config

import io.ktor.server.application.Application
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

fun Application.dependencies(): Dependencies {
    val jwt = JwtConfig.load(environment)
    val database = setupDatabase(DatabaseConfig.load(environment))
    val userService = UserService(jwt, database, Argon2Hasher())
    val profileRepository = ProfileRepository(database)
    val articleRepository = ArticleRepository(database, profileRepository)
    val commentRepository = CommentRepository(database, profileRepository)
    return Dependencies(jwt, userService, profileRepository, commentRepository, articleRepository)
}
