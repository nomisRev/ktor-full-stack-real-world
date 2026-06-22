package org.jetbrains.realworld.article

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.realworld.tokenAuth
import org.jetbrains.realworld.user.User
import org.jetbrains.realworld.user.createUser
import org.jetbrains.realworld.user.newTestUser
import org.jetbrains.realworld.withApp
import kotlin.test.*

class ArticleRoutesTest {

    @Test
    fun testCreateArticle() = withApp {
        val user = createUser(newTestUser())
        val newArticle = NewArticle(
            title = "Test Article",
            description = "Test Description",
            body = "Test Body",
            tagList = listOf("test", "article")
        )

        val response = post("/api/articles") {
            contentType(ContentType.Application.Json)
            tokenAuth(user.token!!)
            setBody(NewArticleRequest(newArticle))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val article = response.body<SingleArticleResponse>().article
        assertEquals(newArticle.title, article.title)
        assertEquals(newArticle.description, article.description)
        assertEquals(newArticle.body, article.body)
        assertEquals(newArticle.tagList, article.tagList)
        assertEquals(user.username, article.author.username)
    }

    @Test
    fun testGetArticle() = withApp {
        val user = createUser(newTestUser())
        val article = createArticle(
            user,
            "Get Article Test",
            "Test Description",
            "Test Body",
            listOf("test", "get")
        )

        val response = get("/api/articles/${article.slug}") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val retrievedArticle = response.body<SingleArticleResponse>().article
        assertEquals(article.slug, retrievedArticle.slug)
        assertEquals(article.title, retrievedArticle.title)
        assertEquals(article.description, retrievedArticle.description)
        assertEquals(article.body, retrievedArticle.body)
        assertEquals(article.tagList, retrievedArticle.tagList)
    }

    @Test
    fun testGetNonExistentArticle() = withApp {
        val response = get("/api/articles/non-existent-slug") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testUpdateArticle() = withApp {
        val user = createUser(newTestUser())
        val article = createArticle(
            user,
            "Update Article Test",
            "Original Description",
            "Original Body",
            listOf("test", "update")
        )

        val updateArticle = UpdateArticle(
            title = "Updated Title",
            description = "Updated Description",
            body = "Updated Body"
        )
        val response = put("/api/articles/${article.slug}") {
            contentType(ContentType.Application.Json)
            tokenAuth(user.token!!)
            setBody(UpdateArticleRequest(updateArticle))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updatedArticle = response.body<SingleArticleResponse>().article
        assertEquals(updateArticle.title, updatedArticle.title)
        assertEquals(updateArticle.description, updatedArticle.description)
        assertEquals(updateArticle.body, updatedArticle.body)
    }

    @Test
    fun testUpdateArticleUnauthorized() = withApp {
        val author = createUser(newTestUser())
        val unauthorizedUser = createUser(newTestUser())
        val article = createArticle(
            author,
            "Unauthorized Update Test",
            "Original Description",
            "Original Body"
        )

        val updateArticle = UpdateArticle(title = "Unauthorized Update")
        val response = put("/api/articles/${article.slug}") {
            contentType(ContentType.Application.Json)
            tokenAuth(unauthorizedUser.token!!)
            setBody(UpdateArticleRequest(updateArticle))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testDeleteArticle() = withApp {
        val user = createUser(newTestUser())
        val article = createArticle(
            user,
            "Delete Article Test",
            "Test Description",
            "Test Body"
        )

        val deleteResponse = delete("/api/articles/${article.slug}") {
            contentType(ContentType.Application.Json)
            tokenAuth(user.token!!)
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        val getResponse = get("/api/articles/${article.slug}") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun testDeleteArticleUnauthorized() = withApp {
        val author = createUser(newTestUser())
        val unauthorizedUser = createUser(newTestUser())
        val article = createArticle(
            author,
            "Unauthorized Delete Test",
            "Test Description",
            "Test Body"
        )

        val deleteResponse = delete("/api/articles/${article.slug}") {
            contentType(ContentType.Application.Json)
            tokenAuth(unauthorizedUser.token!!)
        }

        assertEquals(HttpStatusCode.Forbidden, deleteResponse.status)

        val getResponse = get("/api/articles/${article.slug}") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
    }

    @Test
    fun testGetArticles() = withApp {
        val user = createUser(newTestUser())

        val _ = createArticle(user, "Article 1", tagList = listOf("tag1", "tag2"))
        val _ = createArticle(user, "Article 2", tagList = listOf("tag2", "tag3"))

        val response = get("/api/articles") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val articles = response.body<MultipleArticlesResponse>()
        assertTrue(articles.articles.isNotEmpty(), "Should return articles")
        assertTrue(articles.articlesCount > 0, "Articles count should be greater than 0")

        val tagResponse = get("/api/articles?tag=tag1") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, tagResponse.status)
        val tagArticles = tagResponse.body<MultipleArticlesResponse>()
        assertTrue(tagArticles.articles.isNotEmpty(), "Should return articles with tag1")

        val authorResponse = get("/api/articles?author=${user.username}") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, authorResponse.status)
        val authorArticles = authorResponse.body<MultipleArticlesResponse>()
        assertTrue(authorArticles.articles.isNotEmpty(), "Should return articles by author")

        val paginationResponse = get("/api/articles?limit=1") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, paginationResponse.status)
        val paginatedArticles = paginationResponse.body<MultipleArticlesResponse>()
        assertEquals(1, paginatedArticles.articles.size, "Should return exactly 1 article")
    }

    @Test
    fun testTagOrder() = withApp {
        val user = createUser(newTestUser())

        // Create an article with multiple tags, with "dragons" not as the first tag
        val _ = createArticle(user, "Dragon Training Article", tagList = listOf("training", "dragons"))

        // Retrieve the article by the "dragons" tag
        val response = get("/api/articles?tag=dragons") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val articles = response.body<MultipleArticlesResponse>()
        assertTrue(articles.articles.isNotEmpty(), "Should return articles with dragons tag")

        // Verify that the first tag is "dragons"
        val firstArticle = articles.articles.first()
        assertTrue(firstArticle.tagList.isNotEmpty(), "Article should have tags")
        assertEquals("dragons", firstArticle.tagList.first(), "First tag should be 'dragons'")

        // Verify that the second tag is "training"
        assertTrue(firstArticle.tagList.size >= 2, "Article should have at least 2 tags")
        assertEquals("training", firstArticle.tagList[1], "Second tag should be 'training'")
    }

    @Test
    fun testGetFeed() = withApp {
        val follower = createUser(newTestUser())
        val followed = createUser(newTestUser())

        val _ = post("api/profiles/${followed.username}/follow") {
            contentType(ContentType.Application.Json)
            tokenAuth(follower.token!!)
        }

        val _ = createArticle(followed, "Feed Article 1")
        val _ = createArticle(followed, "Feed Article 2")

        val otherUser = createUser(newTestUser())
        val _ = createArticle(otherUser, "Other Article")

        val response = get("/api/articles/feed") {
            contentType(ContentType.Application.Json)
            tokenAuth(follower.token!!)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val feed = response.body<MultipleArticlesResponse>()
        assertEquals(2, feed.articles.size, "Should return 2 articles from followed user")
        assertEquals(2, feed.articlesCount, "Articles count should be 2")

        val paginationResponse = get("/api/articles/feed?limit=1") {
            contentType(ContentType.Application.Json)
            tokenAuth(follower.token!!)
        }

        assertEquals(HttpStatusCode.OK, paginationResponse.status)
        val paginatedFeed = paginationResponse.body<MultipleArticlesResponse>()
        assertEquals(1, paginatedFeed.articles.size, "Should return exactly 1 article")
    }
}
