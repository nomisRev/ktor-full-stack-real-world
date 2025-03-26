package org.jetbrains.realworld.comment

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.realworld.DatabaseSpec
import org.jetbrains.realworld.article.ArticleRepository
import org.jetbrains.realworld.article.NewArticle
import org.jetbrains.realworld.profile.ProfileRepository
import org.jetbrains.realworld.user.Argon2Hasher
import org.jetbrains.realworld.user.Users
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class CommentServiceTest : DatabaseSpec() {
    private val profileRepository by lazy { ProfileRepository(database) }
    private val articleRepository by lazy { ArticleRepository(database, profileRepository) }
    private val commentRepository by lazy { CommentRepository(database, profileRepository) }
    private val hasher by lazy { Argon2Hasher() }

    private suspend fun createTestUser(
        username: String = Uuid.random().toString(),
        email: String = "${Uuid.random()}@example.com",
        bio: String? = null,
        image: String? = null
    ): Long {
        val password = "password"
        val (salt, hash) = hasher.encrypt(password)

        return transaction(database) {
            Users.insert {
                it[Users.username] = username
                it[Users.email] = email
                it[Users.password] = hash
                it[Users.salt] = salt
                it[Users.bio] = bio
                it[Users.image] = image
            } get Users.id
        }.value
    }

    private fun createTestArticle(
        authorId: Long,
        title: String = "Test Article ${Uuid.random()}",
        description: String = "Test Description",
        body: String = "Test Body",
        tagList: List<String> = listOf("test", "article")
    ): String {
        val newArticle = NewArticle(title, description, body, tagList)
        return articleRepository.createArticle(authorId, newArticle)!!.slug
    }

    private fun createTestComment(
        slug: String,
        authorId: Long,
        body: String = "Test comment ${Uuid.random()}"
    ): Comment {
        val newComment = NewComment(body)
        return commentRepository.createComment(slug, authorId, newComment)!!
    }

    @Test
    fun testCreateComment() = runBlocking {
        val username = "testuser-${Uuid.random()}"
        val authorId = createTestUser(username = username)
        val slug = createTestArticle(authorId)
        val commentBody = "This is a test comment"

        val comment = commentRepository.createComment(slug, authorId, NewComment(commentBody))

        assertNotNull(comment, "Comment should not be null")
        assertEquals(commentBody, comment.body)
        assertEquals(username, comment.author.username)
    }

    @Test
    fun testGetComments() = runBlocking {
        val authorId = createTestUser()
        val slug = createTestArticle(authorId)

        // Create multiple comments
        val comment1 = createTestComment(slug, authorId, "Comment 1")
        val comment2 = createTestComment(slug, authorId, "Comment 2")

        val comments = commentRepository.getComments(slug, authorId)

        assertNotNull(comments, "Comments should not be null")
        assertTrue(comments.size >= 2, "Should retrieve at least 2 comments")
        assertTrue(comments.any { it.body == "Comment 1" }, "Should contain Comment 1")
        assertTrue(comments.any { it.body == "Comment 2" }, "Should contain Comment 2")
    }

    @Test
    fun testGetCommentsForNonExistentArticle() = runBlocking {
        val authorId = createTestUser()
        val comments = commentRepository.getComments("non-existent-slug", authorId)

        assertNull(comments, "Comments should be null for non-existent article")
    }

    @Test
    fun testDeleteComment() = runBlocking {
        val authorId = createTestUser()
        val slug = createTestArticle(authorId)
        val comment = createTestComment(slug, authorId)

        val success = commentRepository.deleteComment(slug, comment.id, authorId)

        assertTrue(success, "Comment should be deleted")

        val comments = commentRepository.getComments(slug, authorId)
        assertNotNull(comments, "Comments should not be null")
        assertFalse(comments.any { it.id == comment.id }, "Deleted comment should not be in the list")
    }

    @Test
    fun testDeleteCommentUnauthorized() = runBlocking {
        val authorId = createTestUser()
        val unauthorizedUserId = createTestUser()
        val slug = createTestArticle(authorId)
        val comment = createTestComment(slug, authorId)

        val success = commentRepository.deleteComment(slug, comment.id, unauthorizedUserId)

        assertFalse(success, "Unauthorized user should not be able to delete comment")

        val comments = commentRepository.getComments(slug, authorId)
        assertNotNull(comments, "Comments should not be null")
        assertTrue(comments.any { it.id == comment.id }, "Comment should still exist")
    }
}
