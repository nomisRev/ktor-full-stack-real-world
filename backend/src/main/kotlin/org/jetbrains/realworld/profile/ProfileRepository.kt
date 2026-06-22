package org.jetbrains.realworld.profile

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.realworld.user.Users

object Follows : Table("follows") {
    val followerId = reference("follower_id", Users)
    val followedId = reference("followed_id", Users)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(followerId, followedId)
}

class ProfileRepository(private val database: Database) {
    fun getProfileOrNull(username: String, currentUserId: Long? = null): Profile? =
        withUser(username) { userId ->
            if (currentUserId != null) {
                Follows.select(Follows.followerId, Follows.followedId)
                    .where { (Follows.followerId eq currentUserId) and (Follows.followedId eq userId) }
                    .limit(1)
                    .singleOrNull() != null
            } else false
        }

    fun followUser(username: String, currentUserId: Long): Profile? =
        withUser(username) { userId ->
            if (userId != currentUserId) {
                Follows.insertIgnore {
                    it[followerId] = currentUserId
                    it[followedId] = userId
                }
                true
            } else false
        }

    fun unfollowUser(username: String, currentUserId: Long): Profile? =
        withUser(username) { userId ->
            Follows.deleteWhere { (Follows.followerId eq currentUserId).and(Follows.followedId eq userId) }
            false
        }

    private inline fun withUser(
        username: String,
        crossinline block: Transaction.(userId: Long) -> Boolean
    ): Profile? = transaction(database) {
        val user = Users.select(Users.id, Users.username, Users.bio, Users.image)
            .where { Users.username eq username }
            .singleOrNull() ?: return@transaction null
        val userId = user[Users.id].value
        val following = block(userId)
        Profile(
            username = user[Users.username],
            bio = user[Users.bio],
            image = user[Users.image],
            following = following
        )
    }
}
