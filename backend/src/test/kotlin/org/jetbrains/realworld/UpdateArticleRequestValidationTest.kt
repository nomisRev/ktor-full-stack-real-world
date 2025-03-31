package org.jetbrains.realworld

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.jetbrains.realworld.article.*
import org.jetbrains.realworld.user.*
import kotlin.test.*

class UpdateArticleRequestValidationTest {

    private suspend fun createArticle(client: io.ktor.client.HttpClient, user: User): String {
        val article = NewArticle(
            title = "Test Article for Update",
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
    fun testValidUpdate() = withApp {
        // Create a user and an article to update
        val user = createUser(newTestUser())
        val slug = createArticle(this, user)

        val validUpdate = UpdateArticle(
            title = "Updated Title",
            description = "Updated Description",
            body = "Updated Body"
        )
        val validResponse = put("/api/articles/$slug") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(UpdateArticleRequest(validUpdate))
        }
        assertEquals(HttpStatusCode.OK, validResponse.status)
    }

    @Test
    fun testBlankTitle() = withApp {
        val user = createUser(newTestUser())
        val slug = createArticle(this, user)
        
        val blankTitleUpdate = UpdateArticle(
            title = ""
        )
        val blankTitleResponse = put("/api/articles/$slug") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(UpdateArticleRequest(blankTitleUpdate))
        }
        assertEquals(HttpStatusCode.BadRequest, blankTitleResponse.status)
        val blankTitleBody = Json.parseToJsonElement(blankTitleResponse.bodyAsText()).jsonObject
        assertTrue(blankTitleBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Title cannot be blank") } == true)
    }

    @Test
    fun testBlankDescription() = withApp {
        val user = createUser(newTestUser())
        val slug = createArticle(this, user)
        
        val blankDescriptionUpdate = UpdateArticle(
            description = ""
        )
        val blankDescriptionResponse = put("/api/articles/$slug") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(UpdateArticleRequest(blankDescriptionUpdate))
        }
        assertEquals(HttpStatusCode.BadRequest, blankDescriptionResponse.status)
        val blankDescriptionBody = Json.parseToJsonElement(blankDescriptionResponse.bodyAsText()).jsonObject
        assertTrue(blankDescriptionBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Description cannot be blank") } == true)
    }

    @Test
    fun testBlankBody() = withApp {
        val user = createUser(newTestUser())
        val slug = createArticle(this, user)
        
        val blankBodyUpdate = UpdateArticle(
            body = ""
        )
        val blankBodyResponse = put("/api/articles/$slug") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(UpdateArticleRequest(blankBodyUpdate))
        }
        assertEquals(HttpStatusCode.BadRequest, blankBodyResponse.status)
        val blankBodyResponseBody = Json.parseToJsonElement(blankBodyResponse.bodyAsText()).jsonObject
        assertTrue(blankBodyResponseBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Body cannot be blank") } == true)
    }

    @Test
    fun testMultipleValidationErrors() = withApp {
        val user = createUser(newTestUser())
        val slug = createArticle(this, user)
        
        val multipleErrorsUpdate = UpdateArticle(
            title = "",
            description = "",
            body = ""
        )
        val multipleErrorsResponse = put("/api/articles/$slug") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(UpdateArticleRequest(multipleErrorsUpdate))
        }
        assertEquals(HttpStatusCode.BadRequest, multipleErrorsResponse.status)
        val multipleErrorsBody = Json.parseToJsonElement(multipleErrorsResponse.bodyAsText()).jsonObject
        val errors = multipleErrorsBody["errors"]?.jsonObject?.get("body")?.jsonArray
        assertNotNull(errors)
        assertTrue(errors.any { it.jsonPrimitive.content.contains("Title cannot be blank") })
        assertTrue(errors.any { it.jsonPrimitive.content.contains("Description cannot be blank") })
        assertTrue(errors.any { it.jsonPrimitive.content.contains("Body cannot be blank") })
    }
}