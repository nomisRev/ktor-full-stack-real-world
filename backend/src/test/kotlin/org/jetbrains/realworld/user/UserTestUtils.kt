package org.jetbrains.realworld.user

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newTestUser(): NewUser {
    val random = Uuid.random()
    return NewUser(username = "$random User", email = "$random@example.com", password = "password")
}

suspend fun HttpClient.createUser(newUser: NewUser): User =
    post("/users") {
        contentType(ContentType.Application.Json)
        setBody(NewUserRequest(newUser))
    }.body<UserResponse>().user

suspend fun HttpClient.login(user: User, password: String): String =
    post("/users/login") {
        contentType(ContentType.Application.Json)
        setBody(UserLogin(user.email, password))
    }.body<UserResponse>().user.token!!
