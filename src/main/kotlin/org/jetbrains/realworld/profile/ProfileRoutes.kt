package org.jetbrains.realworld.profile

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.realworld.UserJWT
import org.jetbrains.realworld.error.ErrorResponse

@Resource("/profiles/{username}")
data class ProfileResource(val username: String) {
    @Resource("follow")
    data class Follow(val parent: ProfileResource)
}

fun Route.profileRoutes(profileService: ProfileService) {
    authenticate(optional = true) {
        get<ProfileResource> { resource ->
            val currentUser = call.principal<UserJWT>()
            val profile = profileService.getProfileOrNull(resource.username, currentUser?.userId)

            if (profile != null) call.respond(HttpStatusCode.OK, ProfileResponse(profile))
            else call.respond(HttpStatusCode.NotFound, ErrorResponse("profile", "not found"))
        }
    }

    authenticate {
        post<ProfileResource.Follow> { resource ->
            val currentUser = call.principal<UserJWT>()
            if (currentUser == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("authorization", "is required"))
                return@post
            }

            val profile = profileService.followUser(resource.parent.username, currentUser.userId)

            if (profile != null) call.respond(HttpStatusCode.OK, ProfileResponse(profile))
            else call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("profile", "not found")
            )
        }

        delete<ProfileResource.Follow> { resource ->
            val currentUser = call.principal<UserJWT>()!!
            val profile = profileService.unfollowUser(resource.parent.username, currentUser.userId)

            if (profile != null) call.respond(HttpStatusCode.OK, ProfileResponse(profile))
            else call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("profile", "not found")
            )
        }
    }
}
