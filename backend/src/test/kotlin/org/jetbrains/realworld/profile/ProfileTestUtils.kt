package org.jetbrains.realworld.profile

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.jetbrains.realworld.tokenAuth
import org.jetbrains.realworld.user.User


suspend fun HttpClient.followUser(username: String, token: String): Profile {
    val request = post("/api/profiles/$username/follow") {
        contentType(ContentType.Application.Json)
        tokenAuth(token)
    }
    return request.body<ProfileResponse>().profile
}
