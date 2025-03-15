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
import org.jetbrains.realworld.error.configureErrorHandling

fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain.main(args)


fun Application.module() {
    val database = setupDatabase(DatabaseConfig.load(environment))
    install(ContentNegotiation) { json() }
    install(DefaultHeaders) { header(HttpHeaders.Server, "RealWorld Conduit API") }
    install(Resources)
    installMetrics()

    configureErrorHandling()
    configureValidation()

    configureAuthentication(JwtConfig.load(environment))

    routing {

    }
}

fun Application.installMetrics() {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = prometheus
    }
    monitor.subscribe(ApplicationStopped) { prometheus.close() }
}
