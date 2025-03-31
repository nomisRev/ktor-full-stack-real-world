package org.jetbrains.realworld

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.jetbrains.realworld.article.*
import org.jetbrains.realworld.user.*
import kotlin.test.*

class NewArticleRequestValidationTest {

    @Test
    fun testValidArticle() = withApp {
        // Create a user to create an article
        val user = createUser(newTestUser())

        val validArticle = NewArticle(
            title = "Test Article",
            description = "Test Description",
            body = "Test Body",
            tagList = listOf("test", "article")
        )
        val validResponse = post("/api/articles") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewArticleRequest(validArticle))
        }
        assertEquals(HttpStatusCode.Created, validResponse.status)
    }

    @Test
    fun testBlankTitle() = withApp {
        val user = createUser(newTestUser())
        
        val blankTitleArticle = NewArticle(
            title = "",
            description = "Test Description",
            body = "Test Body"
        )
        val blankTitleResponse = post("/api/articles") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewArticleRequest(blankTitleArticle))
        }
        assertEquals(HttpStatusCode.BadRequest, blankTitleResponse.status)
        val blankTitleBody = Json.parseToJsonElement(blankTitleResponse.bodyAsText()).jsonObject
        assertTrue(blankTitleBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Title cannot be blank") } == true)
    }

    @Test
    fun testBlankDescription() = withApp {
        val user = createUser(newTestUser())
        
        val blankDescriptionArticle = NewArticle(
            title = "Test Article",
            description = "",
            body = "Test Body"
        )
        val blankDescriptionResponse = post("/api/articles") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewArticleRequest(blankDescriptionArticle))
        }
        assertEquals(HttpStatusCode.BadRequest, blankDescriptionResponse.status)
        val blankDescriptionBody = Json.parseToJsonElement(blankDescriptionResponse.bodyAsText()).jsonObject
        assertTrue(blankDescriptionBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Description cannot be blank") } == true)
    }

    @Test
    fun testBlankBody() = withApp {
        val user = createUser(newTestUser())
        
        val blankBodyArticle = NewArticle(
            title = "Test Article",
            description = "Test Description",
            body = ""
        )
        val blankBodyResponse = post("/api/articles") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewArticleRequest(blankBodyArticle))
        }
        assertEquals(HttpStatusCode.BadRequest, blankBodyResponse.status)
        val blankBodyResponseBody = Json.parseToJsonElement(blankBodyResponse.bodyAsText()).jsonObject
        assertTrue(blankBodyResponseBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Body cannot be blank") } == true)
    }

    @Test
    fun testMultipleValidationErrors() = withApp {
        val user = createUser(newTestUser())
        
        val multipleErrorsArticle = NewArticle(
            title = "",
            description = "",
            body = ""
        )
        val multipleErrorsResponse = post("/api/articles") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(NewArticleRequest(multipleErrorsArticle))
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