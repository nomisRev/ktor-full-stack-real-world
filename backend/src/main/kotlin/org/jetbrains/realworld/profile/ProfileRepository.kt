package org.jetbrains.realworld.profile

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.realworld.user.Users
import java.time.OffsetDateTime
import java.time.ZoneOffset

object Follows : Table("follows") {
    val followerId = reference("follower_id", Users)
    val followedId = reference("followed_id", Users)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(followerId, followedId)
}

class ProfileRepository(private val database: Database) {

    fun getProfileOrNull(username: String, currentUserId: Long? = null): Profile? = transaction(database) {
        val user = Users.select(Users.id, Users.username, Users.bio, Users.image)
            .where { Users.username eq username }
            .singleOrNull() ?: return@transaction null

        val userId = user[Users.id].value

        val following = if (currentUserId != null) {
            Follows.select(Follows.followerId, Follows.followedId)
                .where { (Follows.followerId eq currentUserId) and (Follows.followedId eq userId) }
                .count() > 0
        } else false

        Profile(
            username = user[Users.username],
            bio = user[Users.bio],
            image = user[Users.image],
            following = following
        )
    }

    fun followUser(username: String, currentUserId: Long): Profile? = transaction(database) {
        val user = Users.select(Users.id, Users.username, Users.bio, Users.image)
            .where { Users.username eq username }
            .singleOrNull() ?: return@transaction null

        val userId = user[Users.id].value

        val following = if (userId != currentUserId) {
            Follows.insertIgnore {
                it[followerId] = currentUserId
                it[followedId] = userId
            }
            true
        } else false

        Profile(
            username = user[Users.username],
            bio = user[Users.bio],
            image = user[Users.image],
            following = following
        )
    }

    fun unfollowUser(username: String, currentUserId: Long): Profile? = transaction(database) {
        val user = Users.select(Users.id, Users.username, Users.bio, Users.image)
            .where { Users.username eq username }
            .singleOrNull() ?: return@transaction null
        val userId = user[Users.id].value
        Follows.deleteWhere { (Follows.followerId eq currentUserId).and(Follows.followedId eq userId) } == 1
        Profile(
            username = user[Users.username],
            bio = user[Users.bio],
            image = user[Users.image],
            following = false
        )
    }
}
