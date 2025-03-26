package org.jetbrains.realworld.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.time
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.realworld.JwtConfig
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date

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
    suspend fun registerUser(registration: NewUser): User {
        val res = hasher.encrypt(registration.password)
        val userId = transaction(database) {
            Users.insertAndGetId {
                it[email] = registration.email
                it[username] = registration.username
                it[password] = res.hash
                it[salt] = res.salt
            }.value
        }
        val newToken = createAndUpdateToken(userId)
        return User(
            email = registration.email,
            username = registration.username,
            token = newToken
        )
    }

    private class LoginResult(
        val userId: Long,
        val email: String,
        val username: String,
        val bio: String?,
        val image: String?,
        val salt: ByteArray,
        val password: ByteArray,
    )

    suspend fun loginUserOrNull(login: UserLogin): User? {
        val result = transaction(database) {
            val row =
                Users.select(Users.id, Users.email, Users.username, Users.bio, Users.image, Users.salt, Users.password)
                    .where { Users.email eq login.email }
                    .single()
            LoginResult(
                userId = row[Users.id].value,
                email = row[Users.email],
                username = row[Users.username],
                bio = row.getOrNull(Users.bio),
                image = row.getOrNull(Users.image),
                salt = row[Users.salt],
                password = row[Users.password]
            )
        }
        val success = hasher.verify(login.password, result.salt, result.password)
        return if (success) {
            val newToken = createAndUpdateToken(result.userId)
            User(
                email = result.email,
                username = result.username,
                bio = result.bio,
                image = result.image,
                token = newToken
            )
        } else null
    }

    fun getUserOrNull(userId: Long): User? = transaction(database) {
        Users.select(Users.username, Users.email, Users.bio, Users.image, Users.token)
            .where { Users.id eq userId }
            .singleOrNull()
            ?.toUser()
    }

    suspend fun updateUserOrNull(userId: Long, update: UserUpdate): User? {
        val result = update.password?.let { hasher.encrypt(it) }
        val newToken = if (result != null) createToken(userId) else null
        val hasUpdate =
            update.email != null || update.username != null || update.bio != null || update.image != null || update.password != null
        return transaction(database) {
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
                if (hasUpdate) row[updatedAt] = Clock.System.now()
            }.singleOrNull()?.toUser()
        }
    }

    private fun createToken(userId: Long): String =
        JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withClaim("user_id", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtConfig.expirationMillis))
            .sign(Algorithm.HMAC256(jwtConfig.secret))

    private fun createAndUpdateToken(userId: Long): String {
        val newToken = createToken(userId)
        transaction(database) {
            Users.update({ Users.id eq userId }) {
                it[token] = newToken
            }
        }
        return newToken
    }

    private fun ResultRow.toUser() = User(
        email = this[Users.email],
        username = this[Users.username],
        bio = this[Users.bio],
        image = this[Users.image],
        token = getOrNull(Users.token)
    )
}
