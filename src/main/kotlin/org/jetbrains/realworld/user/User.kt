package org.jetbrains.realworld.user

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.time.ZoneOffset

object Users : LongIdTable("users", "user_id") {
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 255).uniqueIndex()
    val password = binary("password")
    val salt = binary("salt")
    val bio = text("bio").nullable()
    val image = varchar("image", 255).nullable()
    val token = text("token").nullable()
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now(ZoneOffset.UTC))
    val updatedAt = timestampWithTimeZone("updated_at").default(OffsetDateTime.now(ZoneOffset.UTC))
}

@Serializable
data class User(
    val email: String,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    val token: String? = null
)

@Serializable
data class NewUserRequest(val user: NewUser)

@Serializable
data class NewUser(val username: String, val email: String, val password: String)

@Serializable
data class UserLoginRequest(val user: UserLogin)

@Serializable
data class UserLogin(val email: String, val password: String)

@Serializable
data class UserUpdateRequest(val user: UserUpdate)

@Serializable
data class UserUpdate(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null
)

@Serializable
data class UserResponse(val user: User)
