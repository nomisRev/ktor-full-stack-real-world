package org.jetbrains.realworld.article

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.realworld.DatabaseSpec
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TagServiceTest : DatabaseSpec() {

    private val service by lazy { TagService(database) }

    @Test
    fun testGetAllTags() = runBlocking {
        // Create unique tags for testing
        val tag1 = "tag-${Uuid.random()}"
        val tag2 = "tag-${Uuid.random()}"
        val tag3 = "tag-${Uuid.random()}"

        // Insert tags directly into the database
        transaction(database) {
            Tags.insert {
                it[name] = tag1
            }
            Tags.insert {
                it[name] = tag2
            }
            Tags.insert {
                it[name] = tag3
            }
        }

        // Get all tags using the service
        val tagsResponse = service.getAllTags()

        // Verify that the response contains the tags we created
        assertTrue(tagsResponse.tags.contains(tag1), "Response should contain tag1")
        assertTrue(tagsResponse.tags.contains(tag2), "Response should contain tag2")
        assertTrue(tagsResponse.tags.contains(tag3), "Response should contain tag3")
    }
}
