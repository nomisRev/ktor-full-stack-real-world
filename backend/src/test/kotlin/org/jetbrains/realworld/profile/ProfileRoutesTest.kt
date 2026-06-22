package org.jetbrains.realworld.profile

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.plugins.resources.delete
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.path
import org.jetbrains.realworld.tokenAuth
import org.jetbrains.realworld.user.createUser
import org.jetbrains.realworld.user.newTestUser
import org.jetbrains.realworld.withApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProfileRoutesTest {

    @Test
    fun testGetProfile() = withApp {
        val user = createUser(newTestUser())

        val response = get("/api/profiles/${user.username}") {
            contentType(ContentType.Application.Json)
            tokenAuth(user.token!!)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val profile = response.body<ProfileResponse>().profile
        assertEquals(user.username, profile.username)
        assertEquals(user.bio, profile.bio)
        assertEquals(user.image, profile.image)
        assertFalse(profile.following)
    }

    @Test
    fun testGetNonExistentProfile() = withApp {
        val response = get("/api/profiles/nonexistent") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testFollowUser() = withApp {
        val follower = createUser(newTestUser())
        val followed = createUser(newTestUser())

        val initialResponse = get("/api/profiles/${followed.username}") {
            contentType(ContentType.Application.Json)
            tokenAuth(follower.token!!)
        }

        assertEquals(HttpStatusCode.OK, initialResponse.status)
        val initialProfile = initialResponse.body<ProfileResponse>().profile
        assertEquals(followed.username, initialProfile.username)

        val followResponse = post("/api/profiles/${followed.username}/follow") {
            contentType(ContentType.Application.Json)
            tokenAuth(follower.token!!)
        }

        assertEquals(HttpStatusCode.OK, followResponse.status)
        val followProfile = followResponse.body<ProfileResponse>().profile
        assertEquals(followed.username, followProfile.username)

        assertEquals(HttpStatusCode.OK, followResponse.status)
    }

    @Test
    fun testGetProfileAuthenticated() = withApp {
        val user1 = createUser(newTestUser())
        val user2 = createUser(newTestUser())

        val followResponse = post("/api/profiles/${user2.username}/follow") {
            contentType(ContentType.Application.Json)
            tokenAuth(user1.token!!)
        }

        assertEquals(HttpStatusCode.OK, followResponse.status)
    }

    @Test
    fun testFollowNonExistentUser() = withApp {
        val user = createUser(newTestUser())

        val response = post("/api/profiles/nonexistent/follow") {
            contentType(ContentType.Application.Json)
            tokenAuth(user.token!!)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    suspend fun HttpClient.followUser(username: String, token: String): Profile {
        val request = post("/api/profiles/$username/follow") {
            contentType(ContentType.Application.Json)
            tokenAuth(token)
        }
        return request.body<ProfileResponse>().profile
    }

    @Test
    fun testUnfollowUser() = withApp {
        val follower = createUser(newTestUser())
        val followed = createUser(newTestUser())

        val _ = followUser(followed.username, follower.token!!)

        val response = delete("/api/profiles/${followed.username}/follow") {
            contentType(ContentType.Application.Json)
            tokenAuth(follower.token!!)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val profile = response.body<ProfileResponse>().profile
        assertEquals(followed.username, profile.username)
        assertFalse(profile.following)

        val getResponse = get("/api/profiles/${followed.username}") {
            contentType(ContentType.Application.Json)
            tokenAuth(follower.token!!)
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val getProfile = getResponse.body<ProfileResponse>().profile
        assertFalse(getProfile.following)
    }

    @Test
    fun testUnfollowNonExistentUser() = withApp {
        val user = createUser(newTestUser())

        val response = delete("/api/profiles/nonexistent/follow") {
            contentType(ContentType.Application.Json)
            tokenAuth(user.token!!)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testUnauthenticatedFollow() = withApp {
        val user = createUser(newTestUser())
        val response = post {
            url { path("api", "profiles", user.username, "follow") }
            contentType(ContentType.Application.Json)
            tokenAuth("invalid-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testUnauthenticatedUnfollow() = withApp {
        val user = createUser(newTestUser())
        val response = delete(ProfileResource.Follow(ProfileResource(user.username))) {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
