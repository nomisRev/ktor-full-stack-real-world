package org.jetbrains.realworld.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import org.jetbrains.realworld.config.JwtConfig
import org.jetbrains.realworld.user.UserService.LoginResult.InvalidCredentials
import org.jetbrains.realworld.user.UserService.LoginResult.Success
import org.jetbrains.realworld.user.UserService.LoginResult.UserNotFound
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import java.util.Date
import kotlin.time.Clock

object Users : LongIdTable("users", "user_id") {
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 255).uniqueIndex()
    val password = binary("password")
    val salt = binary("salt")
    val bio = text("bio").nullable()
    val image = varchar("image", 255).nullable()
    val token = text("token").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

class UserService(
    private val jwtConfig: JwtConfig,
    private val database: Database,
    private val hasher: Argon2Hasher
) {
    sealed class RegistrationResult {
        data class Success(val user: User) : RegistrationResult()
        data class Conflict(val message: String) : RegistrationResult()
    }

    sealed class UpdateResult {
        data class Success(val user: User) : UpdateResult()
        object NotFound : UpdateResult()
        data class Conflict(val message: String) : UpdateResult()
    }

    suspend fun registerUser(registration: NewUser): RegistrationResult {
        val res = hasher.encrypt(registration.password)
        return transaction(database) {
            try {
                val userId = Users.insertAndGetId {
                    it[email] = registration.email
                    it[username] = registration.username
                    it[password] = res.hash
                    it[salt] = res.salt
                }.value

                RegistrationResult.Success(
                    User(
                        email = registration.email,
                        username = registration.username,
                        token = createAndUpdateToken(userId)
                    )
                )
            } catch (e: ExposedSQLException) {
                when {
                    e.sqlState != PSQLState.UNIQUE_VIOLATION.state -> throw e
                    else -> RegistrationResult.Conflict(uniqueViolationMessage(e) ?: "username or email is already registered")
                }
            }
        }
    }

    private class UserCredentials(
        val userId: Long,
        val email: String,
        val username: String,
        val bio: String?,
        val image: String?,
        val salt: ByteArray,
        val password: ByteArray,
    )

    sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        object UserNotFound : LoginResult()
        object InvalidCredentials : LoginResult()
    }

    /**
     * Trying to login can result in [LoginResult.Success], [LoginResult.UserNotFound] or [LoginResult.InvalidCredentials].
     */
    suspend fun loginUser(login: UserLogin): LoginResult {
        val credentials = transaction(database) {
            Users.select(Users.id, Users.email, Users.username, Users.bio, Users.image, Users.salt, Users.password)
                .where { Users.email eq login.email }
                .singleOrNull()?.let { row ->
                    UserCredentials(
                        userId = row[Users.id].value,
                        email = row[Users.email],
                        username = row[Users.username],
                        bio = row.getOrNull(Users.bio),
                        image = row.getOrNull(Users.image),
                        salt = row[Users.salt],
                        password = row[Users.password]
                    )
                }
        } ?: return UserNotFound

        val success = hasher.verify(login.password, credentials.salt, credentials.password)
        return if (success) {
            val newToken = transaction(database) { createAndUpdateToken(credentials.userId) }
            Success(
                User(
                    email = credentials.email,
                    username = credentials.username,
                    bio = credentials.bio,
                    image = credentials.image,
                    token = newToken
                )
            )
        } else InvalidCredentials
    }

    fun getUserOrNull(userId: Long): User? = transaction(database) {
        Users.select(Users.username, Users.email, Users.bio, Users.image, Users.token)
            .where { Users.id eq userId }
            .singleOrNull()
            ?.toUser()
    }

    suspend fun updateUser(userId: Long, update: UserUpdate): UpdateResult {
        val result = update.password?.let { hasher.encrypt(it) }
        val newToken = if (result != null) createToken(userId) else null
        val hasUpdate =
            update.email != null || update.username != null || update.bio != null || update.image != null || update.password != null

        return if (!hasUpdate) {
            getUserOrNull(userId)?.let { UpdateResult.Success(it) } ?: UpdateResult.NotFound
        } else transaction(database) {
            try {
                Users.updateReturning(
                    listOf(Users.id, Users.username, Users.email, Users.bio, Users.image, Users.token),
                    { Users.id eq userId }
                ) { row ->
                    update.email?.let { row[email] = it }
                    update.username?.let { row[username] = it }
                    update.bio?.let { row[bio] = it }
                    update.image?.let { row[image] = it }
                    if (result != null) {
                        row[salt] = result.salt
                        row[password] = result.hash
                        row[token] = newToken
                    }
                    row[updatedAt] = Clock.System.now()
                }.singleOrNull()?.toUser()?.let { UpdateResult.Success(it) } ?: UpdateResult.NotFound
            } catch (e: ExposedSQLException) {
                when {
                    e.sqlState != PSQLState.UNIQUE_VIOLATION.state -> throw e
                    else -> UpdateResult.Conflict(uniqueViolationMessage(e) ?: "username or email is already registered")
                }
            }
        }
    }

    private fun createToken(userId: Long): String =
        JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withClaim("user_id", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtConfig.expiration.inWholeMilliseconds))
            .sign(Algorithm.HMAC256(jwtConfig.secret))

    private fun createAndUpdateToken(userId: Long): String {
        val newToken = createToken(userId)
        Users.update({ Users.id eq userId }) {
            it[token] = newToken
        }
        return newToken
    }

    private fun uniqueViolationMessage(e: ExposedSQLException): String? {
        val constraint = (e.cause as? PSQLException)?.serverErrorMessage?.constraint
        return when (constraint) {
            "users_email_key" -> "email is already registered"
            "users_username_key" -> "username is already registered"
            else -> throw e
        }
    }

    private fun ResultRow.toUser() = User(
        email = this[Users.email],
        username = this[Users.username],
        bio = this[Users.bio],
        image = this[Users.image],
        token = getOrNull(Users.token)
    )
}
