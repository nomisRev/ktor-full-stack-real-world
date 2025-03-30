package org.jetbrains.realworld.user

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import org.jetbrains.realworld.Root

@Resource("/users")
class UsersResource(val root: Root = Root) {
    @Resource("login")
    class Login(val parent: UsersResource)
}

@Resource("/user")
class UserResource(val root: Root = Root)

@Serializable
data class User(
    val email: String,
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    val token: String? = null
)

@Serializable
data class NewUserRequest(val user: NewUser)

@Serializable
data class NewUser(val username: String, val email: String, val password: String)

@Serializable
data class UserLoginRequest(val user: UserLogin)

@Serializable
data class UserLogin(val email: String, val password: String)

@Serializable
data class UserUpdateRequest(val user: UserUpdate)

@Serializable
data class UserUpdate(
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val bio: String? = null,
    val image: String? = null
)

@Serializable
data class UserResponse(val user: User)
