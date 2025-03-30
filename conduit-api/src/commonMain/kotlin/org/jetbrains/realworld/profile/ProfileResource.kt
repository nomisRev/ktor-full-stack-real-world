package org.jetbrains.realworld.profile

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import org.jetbrains.realworld.Root

@Resource("/profiles/{username}")
data class ProfileResource(val username: String, val root: Root = Root) {
    @Resource("follow")
    data class Follow(val parent: ProfileResource)
}

@Serializable
data class Profile(
    val username: String,
    val bio: String? = null,
    val image: String? = null,
    val following: Boolean = false
)

@Serializable
data class ProfileResponse(val profile: Profile)