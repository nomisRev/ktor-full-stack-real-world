package org.jetbrains.realworld

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.jetbrains.realworld.user.Argon2Hasher
import org.jetbrains.realworld.user.UserService
import org.jetbrains.realworld.user.userRoutes
import org.jetbrains.realworld.profile.ProfileRepository
import org.jetbrains.realworld.profile.profileRoutes
import org.jetbrains.realworld.article.ArticleRepository
import org.jetbrains.realworld.article.articleRoutes
import org.jetbrains.realworld.comment.CommentRepository
import org.jetbrains.realworld.comment.commentRoutes

fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain.main(args)


fun Application.module() {
    val database = setupDatabase(DatabaseConfig.load(environment))
    install(ContentNegotiation) { json() }
    install(DefaultHeaders) { header(HttpHeaders.Server, "RealWorld Conduit API") }
    install(Resources)
    installMetrics()

    val jwtConfig = JwtConfig.load(environment)
    val userService = UserService(jwtConfig, database, Argon2Hasher())
    val profileRepository = ProfileRepository(database)
    val articleRepository = ArticleRepository(database, profileRepository)
    val commentRepository = CommentRepository(database, profileRepository)

    configureAuthentication(jwtConfig, userService)
    configureValidation()

    routing {
        userRoutes(userService)
        profileRoutes(profileRepository)
        articleRoutes(articleRepository)
        commentRoutes(commentRepository)
    }
}

fun Application.installMetrics() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = prometheus
    }
    monitor.subscribe(ApplicationStopped) { prometheus.close() }
}
