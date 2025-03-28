package org.jetbrains.realworld.profile

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.realworld.UserJWT
import org.jetbrains.realworld.error.GenericErrorModel

fun Route.profileRoutes(profileRepository: ProfileRepository) {
    authenticate(optional = true) {
        get<ProfileResource> { resource ->
            val currentUser = call.principal<UserJWT>()
            val profile = profileRepository.getProfileOrNull(resource.username, currentUser?.userId)

            if (profile != null) call.respond(HttpStatusCode.OK, ProfileResponse(profile))
            else call.respond(HttpStatusCode.NotFound, GenericErrorModel("profile not found"))
        }
    }

    authenticate {
        post<ProfileResource.Follow> { resource ->
            val currentUser = call.principal<UserJWT>()
            if (currentUser == null) {
                call.respond(HttpStatusCode.Unauthorized, GenericErrorModel("authorization is required"))
                return@post
            }

            val profile = profileRepository.followUser(resource.parent.username, currentUser.userId)

            if (profile != null) call.respond(HttpStatusCode.OK, ProfileResponse(profile))
            else call.respond(
                HttpStatusCode.NotFound,
                GenericErrorModel("profile not found")
            )
        }

        delete<ProfileResource.Follow> { resource ->
            val currentUser = call.principal<UserJWT>()!!
            val profile = profileRepository.unfollowUser(resource.parent.username, currentUser.userId)

            if (profile != null) call.respond(HttpStatusCode.OK, ProfileResponse(profile))
            else call.respond(
                HttpStatusCode.NotFound,
                GenericErrorModel("profile not found")
            )
        }
    }
}
