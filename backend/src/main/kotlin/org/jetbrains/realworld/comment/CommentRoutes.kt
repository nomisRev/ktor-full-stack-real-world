package org.jetbrains.realworld.comment

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.realworld.UserJWT
import org.jetbrains.realworld.error.ErrorResponse

fun Route.commentRoutes(commentRepository: CommentRepository) {
    authenticate(optional = true) {
        get<CommentsResource> { resource ->
            val principal = call.principal<UserJWT>()
            val comments = commentRepository.getComments(resource.slug, principal?.userId)

            if (comments != null) {
                call.respond(HttpStatusCode.OK, MultipleCommentsResponse(comments))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("article", "not found"))
            }
        }
    }

    authenticate {
        post<CommentsResource> { resource ->
            val principal = call.principal<UserJWT>()!!
            val request = call.receive<NewCommentRequest>()

            val comment = commentRepository.createComment(resource.slug, principal.userId, request.comment)

            if (comment != null) {
                call.respond(HttpStatusCode.OK, SingleCommentResponse(comment))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("article", "not found"))
            }
        }

        delete<CommentsResource.ById> { byId ->
            val principal = call.principal<UserJWT>()!!
            val success = commentRepository.deleteComment(byId.parent.slug, byId.id, principal.userId)

            if (success) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("comment", "not found or not owned by user"))
            }
        }
    }
}