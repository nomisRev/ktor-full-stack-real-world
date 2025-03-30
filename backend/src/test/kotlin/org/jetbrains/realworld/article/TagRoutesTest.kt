package org.jetbrains.realworld.article

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.realworld.user.createUser
import org.jetbrains.realworld.user.newTestUser
import org.jetbrains.realworld.withApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TagRoutesTest {

    @Test
    fun testGetTags() = withApp {
        val user = createUser(newTestUser())

        // Create articles with unique tags to ensure they exist in the database
        val tag1 = "tag-${Uuid.random()}"
        val tag2 = "tag-${Uuid.random()}"
        val tag3 = "tag-${Uuid.random()}"

        createArticle(user, "Article with tag1", tagList = listOf(tag1))
        createArticle(user, "Article with tag2", tagList = listOf(tag2))
        createArticle(user, "Article with tag3", tagList = listOf(tag3))

        // Test the /tags endpoint
        val response = get("/api/tags") {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val tagsResponse = response.body<TagsResponse>()

        // Verify that the response contains the tags we created
        assertTrue(tagsResponse.tags.contains(tag1), "Response should contain tag1")
        assertTrue(tagsResponse.tags.contains(tag2), "Response should contain tag2")
        assertTrue(tagsResponse.tags.contains(tag3), "Response should contain tag3")
    }
}
