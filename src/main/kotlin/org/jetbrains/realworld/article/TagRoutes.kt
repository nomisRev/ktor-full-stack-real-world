package org.jetbrains.realworld.article

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

/**
 * Resource for the /tags endpoint.
 */
@Resource("/tags")
class TagsResource

/**
 * Extension function to add tag routes to a Route.
 *
 * @param tagService The TagService to use for handling tag operations.
 */
fun Route.tagRoutes(tagService: TagService) {
    get<TagsResource> {
        val tags = tagService.getAllTags()
        call.respond(HttpStatusCode.OK, tags)
    }
}