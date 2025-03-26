package org.jetbrains.realworld.article

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.realworld.DatabaseSpec
import org.jetbrains.realworld.profile.Follows
import org.jetbrains.realworld.profile.ProfileService
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
class ArticleServiceTest : DatabaseSpec() {
    private val profileService by lazy { ProfileService(database) }
    private val service by lazy { ArticleService(database, profileService) }
    private val hasher by lazy { Argon2Hasher() }

    private suspend fun createTestUser(
        username: String = Uuid.random().toString(),
        email: String = "${Uuid.random()}@example.com",
        bio: String? = null,
        image: String? = null
    ): Long {
        val (salt, hash) = hasher.encrypt("password")

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
    ): Article {
        val newArticle = NewArticle(title, description, body, tagList)
        return service.createArticle(authorId, newArticle)!!
    }

    @Test
    fun testCreateArticle() = runBlocking {
        val authorId = createTestUser()
        val title = "Test Article"
        val description = "Test Description"
        val body = "Test Body"
        val tagList = listOf("test", "article")

        val article = service.createArticle(
            authorId,
            NewArticle(title, description, body, tagList)
        )

        assertNotNull(article, "Article should not be null")
        assertEquals(title, article.title)
        assertEquals(description, article.description)
        assertEquals(body, article.body)
        assertEquals(tagList, article.tagList)
        assertFalse(article.favorited)
        assertEquals(0, article.favoritesCount)
    }

    @Test
    fun testGetArticleBySlug() = runBlocking {
        val authorId = createTestUser()
        val article = createTestArticle(authorId)

        val retrievedArticle = service.getArticleBySlug(article.slug)
        assertNotNull(retrievedArticle, "Retrieved article should not be null")
        assertEquals(article.title, retrievedArticle.title)
        assertEquals(article.description, retrievedArticle.description)
        assertEquals(article.body, retrievedArticle.body)
        assertEquals(article.tagList, retrievedArticle.tagList)
    }

    @Test
    fun testGetNonExistentArticle() = runBlocking {
        val article = service.getArticleBySlug("non-existent-slug")
        assertNull(article, "Article should be null for non-existent slug")
    }

    @Test
    fun testUpdateArticle() = runBlocking {
        val authorId = createTestUser()
        val article = createTestArticle(authorId)

        val updatedTitle = "Updated Title"
        val updatedDescription = "Updated Description"
        val updatedBody = "Updated Body"

        val updatedArticle = service.updateArticle(
            article.slug,
            authorId,
            UpdateArticle(updatedTitle, updatedDescription, updatedBody)
        )

        assertNotNull(updatedArticle, "Updated article should not be null")
        assertEquals(updatedTitle, updatedArticle.title)
        assertEquals(updatedDescription, updatedArticle.description)
        assertEquals(updatedBody, updatedArticle.body)
    }

    @Test
    fun testUpdateArticleUnauthorized() = runBlocking {
        val authorId = createTestUser()
        val article = createTestArticle(authorId)
        val unauthorizedUserId = createTestUser()

        val updatedArticle = service.updateArticle(
            article.slug,
            unauthorizedUserId,
            UpdateArticle(title = "Unauthorized Update")
        )

        assertNull(updatedArticle, "Update should fail for unauthorized user")
    }

    @Test
    fun testDeleteArticle() = runBlocking {
        val authorId = createTestUser()
        val article = createTestArticle(authorId)

        val deleted = service.deleteArticle(article.slug, authorId)
        assertTrue(deleted, "Article should be deleted")

        val retrievedArticle = service.getArticleBySlug(article.slug)
        assertNull(retrievedArticle, "Article should not exist after deletion")
    }

    @Test
    fun testDeleteArticleUnauthorized(): Unit = runBlocking {
        val authorId = createTestUser()
        val article = createTestArticle(authorId)
        val unauthorizedUserId = createTestUser()

        val deleted = service.deleteArticle(article.slug, unauthorizedUserId)
        assertFalse(deleted, "Deletion should fail for unauthorized user")

        val retrievedArticle = service.getArticleBySlug(article.slug)
        assertNotNull(retrievedArticle, "Article should still exist")
    }

    @Test
    fun `get articles with tag`() = runBlocking {
        val authorId = createTestUser()

        val tag1 = Uuid.random().toString()
        val tag2 = Uuid.random().toString()
        val tag3 = Uuid.random().toString()
        val allTag = Uuid.random().toString()
        createTestArticle(authorId, tagList = listOf(tag1, allTag))
        createTestArticle(authorId, tagList = listOf(tag2, allTag))
        createTestArticle(authorId, tagList = listOf(tag3, allTag))

        val limit = 2
        val (allArticles, totalCount) = service.getArticles(
            tag = allTag,
            author = null,
            favorited = null,
            limit = limit,
            offset = 0,
            currentUserId = null
        )
        assertEquals(limit, allArticles.size, "Should retrieve at least 3 articles")
        assertEquals(3, totalCount, "Total count should be 3")
    }

    @Test
    fun `get articles for author`() = runBlocking {
        val username = "${Uuid.random()}-author"
        val authorId = createTestUser(username = username)
        repeat(3) { createTestArticle(authorId) }
        val limit = 2

        val (allArticles, totalCount) = service.getArticles(
            tag = null,
            author = username,
            favorited = null,
            limit = limit,
            offset = 0,
            currentUserId = null
        )

        assertEquals(limit, allArticles.size, "Should retrieve at least 3 articles")
        assertEquals(3, totalCount, "Total count should be 3")
    }

    @Test
    fun testGetFeed() = runBlocking {
        val followerId = createTestUser()
        val followedId = createTestUser()
        val article1 = createTestArticle(followedId)
        val article2 = createTestArticle(followedId)

        val otherId = createTestUser()
        createTestArticle(otherId)

        transaction(database) {
            Follows.insert {
                it[Follows.followerId] = followerId
                it[Follows.followedId] = followedId
            }
        }
        val feedArticle1 = article1.copy(author = article1.author.copy(following = true)).withoutBody()
        val feedArticle2 = article2.copy(author = article2.author.copy(following = true)).withoutBody()

        val actualArticles = setOf(feedArticle1, feedArticle2)
        val (feedArticles, _) = service.getFeed(followerId)
        assertEquals(actualArticles, feedArticles.toSet())

        val (limitedFeed, _) = service.getFeed(followerId, limit = 1)
        assert(actualArticles.containsAll(limitedFeed))
        assert(limitedFeed.size == 1)
    }

    @Test
    fun testFavoriteArticle() = runBlocking {
        val authorId = createTestUser()
        val userId = createTestUser(username = "favoriter-${Uuid.random()}")
        val article = createTestArticle(authorId)

        val favoritedArticle = service.favoriteArticle(article.slug, userId)

        assertNotNull(favoritedArticle, "Favorited article should not be null")
        assertTrue(favoritedArticle.favorited, "Article should be marked as favorited")
        assertEquals(1, favoritedArticle.favoritesCount, "Favorites count should be 1")

        val favoritedAgain = service.favoriteArticle(article.slug, userId)

        assertNotNull(favoritedAgain, "Favorited article should not be null")
        assertTrue(favoritedAgain.favorited, "Article should still be marked as favorited")
        assertEquals(1, favoritedAgain.favoritesCount, "Favorites count should still be 1")
    }

    @Test
    fun testUnfavoriteArticle() = runBlocking {
        val authorId = createTestUser()
        val userId = createTestUser(username = "favoriter-${Uuid.random()}")
        val article = createTestArticle(authorId)

        val favoritedArticle = service.favoriteArticle(article.slug, userId)
        assertNotNull(favoritedArticle, "Favorited article should not be null")
        assertTrue(favoritedArticle!!.favorited, "Article should be marked as favorited")

        val unfavoritedArticle = service.unfavoriteArticle(article.slug, userId)

        assertNotNull(unfavoritedArticle, "Unfavorited article should not be null")
        assertFalse(unfavoritedArticle.favorited, "Article should not be marked as favorited")
        assertEquals(0, unfavoritedArticle.favoritesCount, "Favorites count should be 0")

        val unfavoritedAgain = service.unfavoriteArticle(article.slug, userId)

        assertNotNull(unfavoritedAgain, "Unfavorited article should not be null")
        assertFalse(unfavoritedAgain.favorited, "Article should still not be marked as favorited")
        assertEquals(0, unfavoritedAgain.favoritesCount, "Favorites count should still be 0")
    }
}
