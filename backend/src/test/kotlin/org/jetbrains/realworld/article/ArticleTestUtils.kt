package org.jetbrains.realworld.article

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.jetbrains.realworld.user.User

suspend fun HttpClient.createArticle(
    user: User,
    title: String,
    description: String = "Test Description",
    body: String = "Test Body",
    tagList: List<String> = emptyList()
): Article {
    val newArticle = NewArticle(title, description, body, tagList)
    val response = post("/articles") {
        contentType(ContentType.Application.Json)
        bearerAuth(user.token!!)
        setBody(NewArticleRequest(newArticle))
    }
    return response.body<SingleArticleResponse>().article
}

fun Article.withoutBody(): ArticleWithoutBody =
    ArticleWithoutBody(
        slug = slug,
        title = title,
        description = description,
        tagList = tagList,
        createdAt = createdAt,
        updatedAt = updatedAt,
        favorited = favorited,
        favoritesCount = favoritesCount,
        author = author
    )