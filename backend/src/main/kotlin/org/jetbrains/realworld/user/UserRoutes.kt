package org.jetbrains.realworld.user

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.realworld.UserJWT
import org.jetbrains.realworld.error.GenericErrorModel
import org.postgresql.util.PSQLState

fun Route.userRoutes(userService: UserService) {
    post<UsersResource> {
        val request = call.receive<NewUserRequest>()
        val user = userService.registerUser(request.user)
        if (user != null) call.respond(HttpStatusCode.Created, UserResponse(user))
        else call.respond(HttpStatusCode.UnprocessableEntity, GenericErrorModel("email is already registered"))
    }

    post<UsersResource.Login> {
        val request = call.receive<UserLoginRequest>()
        when (val result = userService.loginUser(request.user)) {
            is UserService.LoginResult.Success -> call.respond(HttpStatusCode.OK, UserResponse(result.user))
            is UserService.LoginResult.UserNotFound -> call.respond(
                HttpStatusCode.UnprocessableEntity,
                GenericErrorModel("email or password is invalid")
            )

            is UserService.LoginResult.InvalidCredentials -> call.respond(
                HttpStatusCode.UnprocessableEntity,
                GenericErrorModel("email or password is invalid")
            )
        }
    }

    authenticate {
        get<UserResource> { _ ->
            val principal = call.principal<UserJWT>()!!
            call.respond(HttpStatusCode.OK, UserResponse(principal.user))
        }

        put<UserResource> { _ ->
            val principal = call.principal<UserJWT>()!!
            val request = call.receive<UserUpdateRequest>()
            // TODO: incorrect update which results in conflict. email or username.
            val user = userService.updateUserOrNull(principal.userId, request.user)
            if (user != null) call.respond(HttpStatusCode.OK, UserResponse(user))
            else call.respond(HttpStatusCode.NotFound, mapOf("errors" to mapOf("user" to listOf("not found"))))
        }
    }
}
