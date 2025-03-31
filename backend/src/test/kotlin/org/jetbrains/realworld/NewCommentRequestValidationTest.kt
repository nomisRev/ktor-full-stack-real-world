package org.jetbrains.realworld

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.jetbrains.realworld.article.*
import org.jetbrains.realworld.comment.*
import org.jetbrains.realworld.user.*
import kotlin.test.*

class NewCommentRequestValidationTest {

    private suspend fun createArticle(client: io.ktor.client.HttpClient, user: User): String {
        val article = NewArticle(
            title = "Test Article for Comment",
            description = "Test Description",
            body = "Test Body",
            tagList = listOf("test", "article")
        )
        val response = client.post("/api/articles") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewArticleRequest(article))
        }
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return responseBody["article"]?.jsonObject?.get("slug")?.jsonPrimitive?.content ?: ""
    }

    @Test
    fun testValidComment() = withApp {
        // Create a user and an article to comment on
        val user = createUser(newTestUser())
        val slug = createArticle(this, user)

        val validComment = NewComment(
            body = "This is a valid comment"
        )
        val validResponse = post("/api/articles/$slug/comments") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewCommentRequest(validComment))
        }
        assertEquals(HttpStatusCode.OK, validResponse.status)
    }

    @Test
    fun testBlankBody() = withApp {
        val user = createUser(newTestUser())
        val slug = createArticle(this, user)
        
        val blankBodyComment = NewComment(
            body = ""
        )
        val blankBodyResponse = post("/api/articles/$slug/comments") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewCommentRequest(blankBodyComment))
        }
        assertEquals(HttpStatusCode.BadRequest, blankBodyResponse.status)
        val blankBodyResponseBody = Json.parseToJsonElement(blankBodyResponse.bodyAsText()).jsonObject
        assertTrue(blankBodyResponseBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Comment body cannot be blank") } == true)
    }
}