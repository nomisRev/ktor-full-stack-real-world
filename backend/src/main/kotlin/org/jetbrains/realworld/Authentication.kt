package org.jetbrains.realworld

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond
import org.jetbrains.realworld.error.ErrorResponse
import org.jetbrains.realworld.user.User
import org.jetbrains.realworld.user.UserService
import org.slf4j.LoggerFactory
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

data class UserJWT(val userId: Long, val user: User)

fun Application.configureAuthentication(config: JwtConfig, users: UserService): Unit =
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
                val now = Date()
                val expired = credential.expiresAt?.before(now) == true
                val userId = credential.getClaim("user_id", Long::class)
                when {
                    userId == null -> respond(HttpStatusCode.Unauthorized, "Invalid token")
                    expired -> respond(HttpStatusCode.Unauthorized, "Token has expired")
                    else -> when (val user = users.getUserOrNull(userId)) {
                        null -> respond(HttpStatusCode.Unauthorized, "User doesn't exist")
                        else -> UserJWT(userId, user)
                    }
                }
            }
        }
    }
