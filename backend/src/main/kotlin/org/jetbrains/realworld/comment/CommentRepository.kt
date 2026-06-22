package org.jetbrains.realworld.comment

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.realworld.article.Articles
import org.jetbrains.realworld.comment.Comments.body
import org.jetbrains.realworld.comment.Comments.createdAt
import org.jetbrains.realworld.comment.Comments.updatedAt
import org.jetbrains.realworld.profile.Follows
import org.jetbrains.realworld.profile.Profile
import org.jetbrains.realworld.user.Users

object Comments : LongIdTable("comments", "comment_id") {
    val articleId = reference("article_id", Articles).uniqueIndex("idx_comments_article_id")
    val authorId = reference("author_id", Users).uniqueIndex("idx_comments_author_id")
    val body = text("body")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

class CommentRepository(private val database: Database) {
    fun getComments(slug: String, currentUserId: Long? = null): List<Comment>? = transaction(database) {
        val articleId = Articles
            .select(Articles.id)
            .where { Articles.slug eq slug }
            .singleOrNull()?.get(Articles.id)?.value
        if (articleId == null) null
        else {
            val rows = Comments
                .innerJoin(Users) { Comments.authorId eq Users.id }
                .select(
                    Comments.id, Comments.body, Comments.createdAt, Comments.updatedAt,
                    Comments.authorId, Users.id, Users.username, Users.bio, Users.image
                )
                .where { Comments.articleId eq articleId }
                .orderBy(Comments.createdAt, SortOrder.DESC)
                .toList()

            val authorIds = rows.map { it[Users.id].value }.toSet()
            val followingAuthorIds = if (currentUserId != null && authorIds.isNotEmpty()) {
                Follows.select(Follows.followedId)
                    .where { (Follows.followerId eq currentUserId) and (Follows.followedId inList authorIds) }
                    .map { it[Follows.followedId].value }
                    .toSet()
            } else emptySet()

            rows.map<ResultRow, Comment> { row -> row.toComment(currentUserId, followingAuthorIds) }
        }
    }

    fun createComment(slug: String, authorId: Long, newComment: NewComment): Comment? = transaction(database) {
        val articleId = Articles
            .select(Articles.id)
            .where { Articles.slug eq slug }
            .singleOrNull()?.get(Articles.id)?.value ?: return@transaction null

        val row: ResultRow =
            Comments.insertReturning(returning = [Comments.id, createdAt, updatedAt]) {
                it[this.articleId] = articleId
                it[this.authorId] = authorId
                it[body] = newComment.body
            }.single()

        val author = Users.select(Users.username, Users.bio, Users.image)
            .where { Users.id eq authorId }
            .single()

        val authorProfile = Profile(
            username = author[Users.username],
            bio = author[Users.bio],
            image = author[Users.image],
            following = false // The author can't follow themselves
        )

        Comment(
            id = row[Comments.id].value,
            body = newComment.body,
            createdAt = row[createdAt],
            updatedAt = row[updatedAt],
            author = authorProfile
        )
    }

    fun deleteComment(commentId: Long, currentUserId: Long): Boolean = transaction(database) {
        Comments.deleteWhere { (Comments.id eq commentId) and (Comments.authorId eq currentUserId) } > 0
    }

    private fun ResultRow.toComment(currentUserId: Long?, followingAuthorIds: Set<Long>): Comment =
        Comment(
            id = this[Comments.id].value,
            body = this[body],
            createdAt = this[createdAt],
            updatedAt = this[updatedAt],
            author = Profile(
                username = this[Users.username],
                bio = this[Users.bio],
                image = this[Users.image],
                following = currentUserId != null && this[Users.id].value in followingAuthorIds
            )
        )

}
