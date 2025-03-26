package org.jetbrains.realworld.article

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.realworld.UserJWT
import org.jetbrains.realworld.error.ErrorResponse

@Resource("/articles")
class ArticlesResource {
    @Resource("feed")
    class Feed(val parent: ArticlesResource, val limit: Int? = null, val offset: Int? = null)

    @Resource("{slug}")
    class BySlug(val parent: ArticlesResource, val slug: String) {
        @Resource("favorite")
        class Favorite(val parent: BySlug)
    }
}

fun Route.articleRoutes(articleService: ArticleService) {
    authenticate(optional = true) {
        get<ArticlesResource> { resource ->
            val userId = call.principal<UserJWT>()?.userId
            val tag = call.request.queryParameters["tag"]
            val author = call.request.queryParameters["author"]
            val favorited = call.request.queryParameters["favorited"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val result = articleService.getArticles(
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
            val article = articleService.getArticleBySlug(resource.slug, principal?.userId)

            if (article != null) {
                call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("article", "not found"))
            }
        }
    }

    authenticate {
        get<ArticlesResource.Feed> { resource ->
            val principal = call.principal<UserJWT>()!!
            val limit = resource.limit ?: 20
            val offset = resource.offset ?: 0

            val result = articleService.getFeed(
                currentUserId = principal.userId,
                limit = limit,
                offset = offset
            )

            call.respond(HttpStatusCode.OK, result)
        }

        post<ArticlesResource> {
            val principal = call.principal<UserJWT>()!!
            val request = call.receive<NewArticleRequest>()

            val article = articleService.createArticle(principal.userId, request.article)

            if (article != null) call.respond(HttpStatusCode.Created, SingleArticleResponse(article))
            else call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse("article", "could not be created"))
        }

        put<ArticlesResource.BySlug> { resource ->
            val principal = call.principal<UserJWT>()!!
            val request = call.receive<UpdateArticleRequest>()

            val article = articleService.updateArticle(resource.slug, principal.userId, request.article)

            if (article != null) {
                call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("article", "not found"))
            }
        }

        delete<ArticlesResource.BySlug> { resource ->
            val principal = call.principal<UserJWT>()!!

            val success = articleService.deleteArticle(resource.slug, principal.userId)

            if (success) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("article", "not found"))
            }
        }

        post<ArticlesResource.BySlug.Favorite> { resource ->
            val principal = call.principal<UserJWT>()!!

            val article = articleService.favoriteArticle(resource.parent.slug, principal.userId)

            if (article != null) {
                call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("article", "not found"))
            }
        }

        delete<ArticlesResource.BySlug.Favorite> { resource ->
            val principal = call.principal<UserJWT>()!!

            val article = articleService.unfavoriteArticle(resource.parent.slug, principal.userId)

            if (article != null) {
                call.respond(HttpStatusCode.OK, SingleArticleResponse(article))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("article", "not found"))
            }
        }
    }
}
