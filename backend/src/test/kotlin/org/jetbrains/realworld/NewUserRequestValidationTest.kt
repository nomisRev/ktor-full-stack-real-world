package org.jetbrains.realworld

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.jetbrains.realworld.user.*
import kotlin.test.*

class NewUserRequestValidationTest {

    @Test
    fun testValidNewUser() = withApp {
        val validUser = NewUser(
            username = "testuser",
            email = "test@example.com",
            password = "password123"
        )
        val validResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(validUser))
        }
        assertEquals(HttpStatusCode.Created, validResponse.status)
    }

    @Test
    fun testBlankEmail() = withApp {
        val blankEmailUser = NewUser(
            username = "testuser",
            email = "",
            password = "password123"
        )
        val blankEmailResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(blankEmailUser))
        }
        assertEquals(HttpStatusCode.BadRequest, blankEmailResponse.status)
        val blankEmailBody = Json.parseToJsonElement(blankEmailResponse.bodyAsText()).jsonObject
        assertTrue(blankEmailBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Email cannot be blank") } == true)
    }

    @Test
    fun testInvalidEmailFormat() = withApp {
        val invalidEmailUser = NewUser(
            username = "testuser",
            email = "invalid-email",
            password = "password123"
        )
        val invalidEmailResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(invalidEmailUser))
        }
        assertEquals(HttpStatusCode.BadRequest, invalidEmailResponse.status)
        val invalidEmailBody = Json.parseToJsonElement(invalidEmailResponse.bodyAsText()).jsonObject
        assertTrue(invalidEmailBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Invalid email format") } == true)
    }

    @Test
    fun testBlankUsername() = withApp {
        val blankUsernameUser = NewUser(
            username = "",
            email = "test@example.com",
            password = "password123"
        )
        val blankUsernameResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(blankUsernameUser))
        }
        assertEquals(HttpStatusCode.BadRequest, blankUsernameResponse.status)
        val blankUsernameBody = Json.parseToJsonElement(blankUsernameResponse.bodyAsText()).jsonObject
        assertTrue(blankUsernameBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Username cannot be blank") } == true)
    }

    @Test
    fun testShortUsername() = withApp {
        val shortUsernameUser = NewUser(
            username = "ab",
            email = "test@example.com",
            password = "password123"
        )
        val shortUsernameResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(shortUsernameUser))
        }
        assertEquals(HttpStatusCode.BadRequest, shortUsernameResponse.status)
        val shortUsernameBody = Json.parseToJsonElement(shortUsernameResponse.bodyAsText()).jsonObject
        assertTrue(shortUsernameBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Username must be at least 3 characters") } == true)
    }

    @Test
    fun testBlankPassword() = withApp {
        val blankPasswordUser = NewUser(
            username = "testuser",
            email = "test@example.com",
            password = ""
        )
        val blankPasswordResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(blankPasswordUser))
        }
        assertEquals(HttpStatusCode.BadRequest, blankPasswordResponse.status)
        val blankPasswordBody = Json.parseToJsonElement(blankPasswordResponse.bodyAsText()).jsonObject
        assertTrue(blankPasswordBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Password cannot be blank") } == true)
    }

    @Test
    fun testShortPassword() = withApp {
        val shortPasswordUser = NewUser(
            username = "testuser",
            email = "test@example.com",
            password = "short"
        )
        val shortPasswordResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(shortPasswordUser))
        }
        assertEquals(HttpStatusCode.BadRequest, shortPasswordResponse.status)
        val shortPasswordBody = Json.parseToJsonElement(shortPasswordResponse.bodyAsText()).jsonObject
        assertTrue(shortPasswordBody["errors"]?.jsonObject?.get("body")?.jsonArray?.any { it.jsonPrimitive.content.contains("Password must be at least 8 characters") } == true)
    }

    @Test
    fun testMultipleValidationErrors() = withApp {
        val multipleErrorsUser = NewUser(
            username = "",
            email = "invalid-email",
            password = "short"
        )
        val multipleErrorsResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(multipleErrorsUser))
        }
        assertEquals(HttpStatusCode.BadRequest, multipleErrorsResponse.status)
        val multipleErrorsBody = Json.parseToJsonElement(multipleErrorsResponse.bodyAsText()).jsonObject
        val errors = multipleErrorsBody["errors"]?.jsonObject?.get("body")?.jsonArray
        assertNotNull(errors)
        assertTrue(errors.any { it.jsonPrimitive.content.contains("Username cannot be blank") })
        assertTrue(errors.any { it.jsonPrimitive.content.contains("Invalid email format") })
        assertTrue(errors.any { it.jsonPrimitive.content.contains("Password must be at least 8 characters") })
    }
}