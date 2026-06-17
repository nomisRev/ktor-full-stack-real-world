package org.jetbrains.realworld

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json

fun withApp(test: suspend HttpClient.() -> Unit) =
    testApplication {
        environment {
            config = ApplicationConfig("application.yaml")
                .mergeWith(PostgresContainer.getMapAppConfig())
                .mergeWith(
                    MapApplicationConfig(

                        "jwt.secret" to "s",
                        "jwt.issuer" to "i",
                        "jwt.audience" to "a",
                        "jwt.realm" to "r",
                        "jwt.expirationMillis" to "3600"
                    )
                )
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(Json { prettyPrint = true })
            }
            install(Resources)
        }
        startApplication()
        test(client)
    }


fun HttpMessageBuilder.tokenAuth(token: String): Unit =
    header(HttpHeaders.Authorization, "Token $token")