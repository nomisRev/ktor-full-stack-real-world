package org.jetbrains.realworld.user

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.jetbrains.realworld.withApp
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UserRoutesTest {

    @Test
    fun testUserRegistration() = withApp {
        val user = newTestUser()
        val response = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(user))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val registered = response.body<UserResponse>().user
        assertEquals(user.username, registered.username)
        assertEquals(user.email, registered.email)
        assertNotNull(registered.token)
    }

    @Test
    fun testUserLogin() = withApp {
        val newUser = newTestUser()
        val user = createUser(newUser)

        val response = post("/api/users/login") {
            contentType(ContentType.Application.Json)
            setBody(UserLoginRequest(UserLogin(user.email, newUser.password)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val login = response.body<UserResponse>().user
        assertNotNull(user)
        assertEquals(user.username, login.username)
        assertEquals(user.email, login.email)
    }

    @Test
    fun testGetCurrentUser() = withApp {
        val user = createUser(newTestUser())

        val response = get("/api/user") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val currentUser = response.body<UserResponse>().user
        assertEquals(user.username, currentUser.username)
        assertEquals(user.email, currentUser.email)
        assertEquals(user.token, currentUser.token)
        assertEquals(user.bio, currentUser.bio)
        assertEquals(user.image, currentUser.image)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testUpdateUser() = withApp {
        val user = createUser(newTestUser())
        val random = Uuid.random()
        val update = UserUpdate(
            username = "updateduser-$random",
            bio = "This is my updated bio",
            email = "updated$random@example.com"
        )
        val response = put("/api/user") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${user.token!!}")
            setBody(UserUpdateRequest(update))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<UserResponse>().user
        assertEquals(updated.username, update.username)
        assertEquals(updated.email, update.email)
        assertEquals(updated.bio, update.bio)
    }

    @Test
    fun testInvalidRegistration() = withApp {
        val invalidUser = NewUser(username = "", email = "invalid-email", password = "short")
        val registerResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(invalidUser))
        }
        assertEquals(HttpStatusCode.BadRequest, registerResponse.status)
    }

    @Test
    fun testDuplicateRegistration() = withApp {
        val newUser = newTestUser()
        val _ = createUser(newUser)

        val duplicateResponse = post("/api/users") {
            contentType(ContentType.Application.Json)
            setBody(NewUserRequest(newUser))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, duplicateResponse.status)
    }
}
