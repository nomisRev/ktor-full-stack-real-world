package org.jetbrains.realworld.profile

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.jetbrains.realworld.user.User

suspend fun HttpClient.getProfile(username: String, token: String? = null): Profile {
    val request = get("/profiles/$username") {
        contentType(ContentType.Application.Json)
        if (token != null) {
            bearerAuth(token)
        }
    }
    return request.body<ProfileResponse>().profile
}

suspend fun HttpClient.followUser(username: String, token: String): Profile {
    val request = post("/profiles/$username/follow") {
        contentType(ContentType.Application.Json)
        bearerAuth(token)
    }
    return request.body<ProfileResponse>().profile
}

suspend fun HttpClient.unfollowUser(username: String, token: String): Profile {
    val request = delete("/profiles/$username/follow") {
        contentType(ContentType.Application.Json)
        bearerAuth(token)
    }
    return request.body<ProfileResponse>().profile
}