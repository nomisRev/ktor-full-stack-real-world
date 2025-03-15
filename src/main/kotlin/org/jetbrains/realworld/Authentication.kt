package org.jetbrains.realworld

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond
import org.jetbrains.realworld.error.ErrorResponse
import java.util.*

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expirationMillis: Long,
) {
    companion object {
        fun load(environment: ApplicationEnvironment) = JwtConfig(
            secret = environment.config.property("jwt.secret").getString(),
            issuer = environment.config.property("jwt.issuer").getString(),
            audience = environment.config.property("jwt.audience").getString(),
            realm = environment.config.property("jwt.realm").getString(),
            expirationMillis = environment.config.property("jwt.expiration").getString().toLong(),
        )
    }
}

fun generateToken(userId: Int, config: JwtConfig): String = JWT.create()
    .withSubject("Authentication")
    .withIssuer(config.issuer)
    .withAudience(config.audience)
    .withClaim("userId", userId)
    .withExpiresAt(Date(System.currentTimeMillis() + config.expirationMillis))
    .sign(Algorithm.HMAC256(config.secret))

fun Application.configureAuthentication(config: JwtConfig): Unit =
    authentication {
        jwt {
            realm = config.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.secret))
                    .withIssuer(config.issuer)
                    .withAudience(config.audience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != 0) JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse.fromMessage("Token is not valid or has expired")
                )
            }
        }
    }