package org.jetbrains.realworld.comment

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.realworld.article.Articles
import org.jetbrains.realworld.profile.ProfileRepository
import org.jetbrains.realworld.user.Users
import java.time.format.DateTimeFormatter

private object Comments : LongIdTable("comments", "comment_id") {
    val articleId = reference("article_id", Articles)
    val authorId = reference("author_id", Users)
    val body = text("body")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

class CommentRepository(
    private val database: Database,
    private val profileRepository: ProfileRepository
) {
    private val dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun getComments(slug: String, currentUserId: Long? = null): List<Comment>? = transaction(database) {
        val articleId = Articles
            .select(Articles.id)
            .where { Articles.slug eq slug }
            .singleOrNull()?.get(Articles.id)?.value

        if (articleId == null) null
        else Comments
            .innerJoin(Users) { Comments.authorId eq Users.id }
            .select(
                Comments.id, Comments.body, Comments.createdAt, Comments.updatedAt,
                Comments.authorId, Users.username
            )
            .where { Comments.articleId eq articleId }
            .orderBy(Comments.createdAt, SortOrder.DESC)
            .map { row -> row.toComment(currentUserId) }
    }

    fun createComment(slug: String, authorId: Long, newComment: NewComment): Comment? = transaction(database) {
        val articleId = Articles
            .select(Articles.id)
            .where { Articles.slug eq slug }
            .singleOrNull()?.get(Articles.id)?.value ?: return@transaction null

        val now = Clock.System.now()
        val commentId = Comments.insertAndGetId {
            it[this.articleId] = articleId
            it[this.authorId] = authorId
            it[body] = newComment.body
            it[createdAt] = now
            it[updatedAt] = now
        }.value

        val author = Users.select(Users.username)
            .where { Users.id eq authorId }
            .single()

        val authorProfile = profileRepository.getProfileOrNull(author[Users.username], authorId)!!

        Comment(
            id = commentId,
            body = newComment.body,
            createdAt = now,
            updatedAt = now,
            author = authorProfile
        )
    }

    fun deleteComment(slug: String, commentId: Long, currentUserId: Long): Boolean = transaction(database) {
        Comments.deleteWhere { (Comments.id eq commentId) and (Comments.authorId eq currentUserId) } > 0
    }

    private fun ResultRow.toComment(currentUserId: Long?): Comment {
        val authorProfile = profileRepository.getProfileOrNull(this[Users.username], currentUserId)!!
        return Comment(
            id = this[Comments.id].value,
            body = this[Comments.body],
            createdAt = this[Comments.createdAt],
            updatedAt = this[Comments.updatedAt],
            author = authorProfile
        )
    }
}