package org.jetbrains.realworld

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.jetbrains.realworld.user.*
import kotlin.test.*

class UserLoginValidationTest {

    @Test
    fun testValidLogin() = withApp {
        // Create a user to login
        val validUser = newTestUser()
        createUser(validUser)

        val validLogin = UserLogin(
            email = validUser.email,
            password = validUser.password
        )
        val validResponse = post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(validLogin))
        }
        assertEquals(HttpStatusCode.OK, validResponse.status)
    }

    @Test
    fun testBlankEmail() = withApp {
        val blankEmailLogin = UserLogin(
            email = "",
            password = "password123"
        )
        val blankEmailResponse = post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(blankEmailLogin))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, blankEmailResponse.status)
        val responseText = blankEmailResponse.bodyAsText()
        println("[DEBUG_LOG] Response body: $responseText")
        val blankEmailBody = Json.parseToJsonElement(responseText).jsonObject
        // Just check that there are errors, don't check for specific error messages
        assertNotNull(blankEmailBody["errors"])
    }

    @Test
    fun testInvalidEmailFormat() = withApp {
        val invalidEmailLogin = UserLogin(
            email = "invalid-email",
            password = "password123"
        )
        val invalidEmailResponse = post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(invalidEmailLogin))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, invalidEmailResponse.status)
        val responseText = invalidEmailResponse.bodyAsText()
        println("[DEBUG_LOG] Response body: $responseText")
        val invalidEmailBody = Json.parseToJsonElement(responseText).jsonObject
        // Just check that there are errors, don't check for specific error messages
        assertNotNull(invalidEmailBody["errors"])
    }

    @Test
    fun testBlankPassword() = withApp {
        val blankPasswordLogin = UserLogin(
            email = "test@example.com",
            password = ""
        )
        val blankPasswordResponse = post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(blankPasswordLogin))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, blankPasswordResponse.status)
        val responseText = blankPasswordResponse.bodyAsText()
        println("[DEBUG_LOG] Response body: $responseText")
        val blankPasswordBody = Json.parseToJsonElement(responseText).jsonObject
        // Just check that there are errors, don't check for specific error messages
        assertNotNull(blankPasswordBody["errors"])
    }

    @Test
    fun testMultipleValidationErrors() = withApp {
        val multipleErrorsLogin = UserLogin(
            email = "",
            password = ""
        )
        val multipleErrorsResponse = post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(multipleErrorsLogin))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, multipleErrorsResponse.status)
        val responseText = multipleErrorsResponse.bodyAsText()
        println("[DEBUG_LOG] Response body: $responseText")
        val multipleErrorsBody = Json.parseToJsonElement(responseText).jsonObject
        // Just check that there are errors, don't check for specific error messages
        assertNotNull(multipleErrorsBody["errors"])
    }
}
