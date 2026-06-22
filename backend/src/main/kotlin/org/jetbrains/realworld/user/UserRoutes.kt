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
import org.jetbrains.realworld.config.UserJWT
import org.jetbrains.realworld.error.GenericErrorModel

fun Route.userRoutes(userService: UserService) {
    post<UsersResource> {
        val request = call.receive<NewUserRequest>()

        when (val result = userService.registerUser(request.user)) {
            is UserService.RegistrationResult.Success -> call.respond(HttpStatusCode.Created, UserResponse(result.user))
            is UserService.RegistrationResult.Conflict -> call.respond(
                HttpStatusCode.UnprocessableEntity,
                GenericErrorModel(result.message)
            )
        }
    }

    post<UsersResource.Login> {
        val request = call.receive<UserLoginRequest>()

        val result = userService.loginUser(request.user)

        when (result) {
            is UserService.LoginResult.Success -> call.respond(HttpStatusCode.OK, UserResponse(result.user))
            is UserService.LoginResult.UserNotFound,
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

            when (val result = userService.updateUser(principal.userId, request.user)) {
                is UserService.UpdateResult.Success -> call.respond(HttpStatusCode.OK, UserResponse(result.user))
                UserService.UpdateResult.NotFound -> call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("errors" to mapOf("user" to listOf("not found")))
                )
                is UserService.UpdateResult.Conflict -> call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    GenericErrorModel(result.message)
                )
            }
        }
    }
}
