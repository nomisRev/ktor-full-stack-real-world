package org.jetbrains.realworld.comment

import io.ktor.resources.Resource
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.realworld.profile.Profile

@Resource("/articles/{slug}/comments")
class CommentsResource(val slug: String) {
    @Resource("{id}")
    class ById(val parent: CommentsResource, val id: Long)
}


@Serializable
data class Comment(
    val id: Long,
    val body: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val author: Profile
)

@Serializable
data class NewCommentRequest(val comment: NewComment)

@Serializable
data class NewComment(val body: String)

@Serializable
data class SingleCommentResponse(val comment: Comment)

@Serializable
data class MultipleCommentsResponse(val comments: List<Comment>)
