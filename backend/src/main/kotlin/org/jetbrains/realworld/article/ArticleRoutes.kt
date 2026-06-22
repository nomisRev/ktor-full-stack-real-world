package org.jetbrains.realworld.article

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.realworld.config.UserJWT
import org.jetbrains.realworld.error.GenericErrorModel

fun Route.articleRoutes(articleRepository: ArticleRepository) {
    authenticate(optional = true) {
        get<ArticlesResource> { resource ->
            val userId = call.principal<UserJWT>()?.userId
            val tag = call.request.queryParameters["tag"]
            val author = call.request.queryParameters["author"]
            val favorited = call.request.queryParameters["favorited"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val result = articleRepository.getArticles(
                tag = tag,
                author = author,
                favorited = favorited,
                limit = limit,
                offset = offset,
                currentUserId = userId
            )

            call.respond(HttpStatusCode.OK, result)
        }

        get<ArticlesResource.BySlug> { resource ->
            val principal = call.principal<UserJWT>()

            val article = articleRepository.getArticleBySlug(resource.slug, principal?.userId)

            if (article != null) call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            else call.respond(HttpStatusCode.NotFound, GenericErrorModel("article not found"))
        }
    }

    authenticate {
        get<ArticlesResource.Feed> { resource ->
            val principal = call.principal<UserJWT>()!!

            val result = articleRepository.getFeed(
                currentUserId = principal.userId,
                limit = resource.limit ?: 20,
                offset = resource.offset ?: 0
            )

            call.respond(HttpStatusCode.OK, result)
        }

        post<ArticlesResource> {
            val principal = call.principal<UserJWT>()!!
            val request = call.receive<NewArticleRequest>()

            val article = articleRepository.createArticle(principal.userId, request.article)

            call.respond(HttpStatusCode.Created, SingleArticleResponse(article))
        }

        put<ArticlesResource.BySlug> { resource ->
            val principal = call.principal<UserJWT>()!!
            val request = call.receive<UpdateArticleRequest>()

            val article = articleRepository.updateArticle(resource.slug, principal.userId, request.article)

            if (article != null) call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            else call.respond(HttpStatusCode.NotFound, GenericErrorModel("article not found"))
        }

        delete<ArticlesResource.BySlug> { resource ->
            val principal = call.principal<UserJWT>()!!

            val success = articleRepository.deleteArticle(resource.slug, principal.userId)

            when (success) {
                null -> call.respond(
                    HttpStatusCode.Forbidden,
                    GenericErrorModel("only the author can delete the article")
                )

                true -> call.respond(HttpStatusCode.NoContent)
                false -> call.respond(HttpStatusCode.NotFound, GenericErrorModel("article not found"))
            }
        }

        post<ArticlesResource.BySlug.Favorite> { resource ->
            val principal = call.principal<UserJWT>()!!

            val article = articleRepository.favoriteArticle(resource.parent.slug, principal.userId)

            if (article != null) call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            else call.respond(HttpStatusCode.NotFound, GenericErrorModel("article not found"))
        }

        delete<ArticlesResource.BySlug.Favorite> { resource ->
            val principal = call.principal<UserJWT>()!!

            val article = articleRepository.unfavoriteArticle(resource.parent.slug, principal.userId)

            if (article != null) call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            else call.respond(HttpStatusCode.NotFound, GenericErrorModel("article not found"))
        }
    }

    get<TagsResource> {
        val tags = articleRepository.allTags()
        call.respond(HttpStatusCode.OK, TagsResponse(tags))
    }
}
