package org.jetbrains.realworld.comment

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.realworld.OffsetDateTimeAsString
import org.jetbrains.realworld.OffsetDateTimeAsStringSerializer
import org.jetbrains.realworld.article.Articles
import org.jetbrains.realworld.profile.Profile
import org.jetbrains.realworld.user.Users
import java.time.OffsetDateTime
import java.time.ZoneOffset

object Comments : LongIdTable("comments", "comment_id") {
    val articleId = reference("article_id", Articles)
    val authorId = reference("author_id", Users)
    val body = text("body")
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now(ZoneOffset.UTC))
    val updatedAt = timestampWithTimeZone("updated_at").default(OffsetDateTime.now(ZoneOffset.UTC))
}

@Serializable
data class Comment(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTimeAsString,
    val updatedAt: OffsetDateTimeAsString,
    val author: Profile
)

@Serializable
data class NewCommentRequest(val comment: NewComment)

@Serializable
data class NewComment(val body: String)

@Serializable
data class SingleCommentResponse(val comment: Comment)

@Serializable
data class MultipleCommentsResponse(val comments: List<Comment>)
