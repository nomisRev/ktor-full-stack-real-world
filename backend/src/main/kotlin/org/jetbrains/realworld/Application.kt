package org.jetbrains.realworld

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.config.getAs
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.jetbrains.realworld.user.userRoutes
import org.jetbrains.realworld.profile.profileRoutes
import org.jetbrains.realworld.article.articleRoutes
import org.jetbrains.realworld.comment.commentRoutes
import org.jetbrains.realworld.config.Config
import org.jetbrains.realworld.config.dependencies
import org.jetbrains.realworld.config.configureAuthentication
import org.jetbrains.realworld.config.configureValidation
import kotlin.time.Duration

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

suspend fun Application.module() {
    val config = environment.config.getAs<Config>()
    val deps = dependencies(config)

    install(CallLogging)
    install(ContentNegotiation) { json() }
    install(DefaultHeaders) { header(HttpHeaders.Server, "RealWorld Conduit API") }
    install(Resources)
    installMetrics()
    configureAuthentication(deps.jwt, deps.users)
    configureValidation()

    routing {
        userRoutes(deps.users)
        profileRoutes(deps.profiles)
        articleRoutes(deps.articles)
        commentRoutes(deps.comments)
    }
}

private fun Application.installMetrics() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = prometheus
    }
    monitor.subscribe(ApplicationStopped) { prometheus.close() }
}
