package org.jetbrains.realworld.profile

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.realworld.user.Users
import java.time.OffsetDateTime
import java.time.ZoneOffset

object Follows : Table("follows") {
    val followerId = reference("follower_id", Users)
    val followedId = reference("followed_id", Users)
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now(ZoneOffset.UTC))
    
    override val primaryKey = PrimaryKey(followerId, followedId)
}

@Serializable
data class Profile(
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    val following: Boolean = false
)

@Serializable
data class ProfileResponse(val profile: Profile)