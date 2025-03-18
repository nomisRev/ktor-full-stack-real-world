package org.jetbrains.realworld.article

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for handling tag-related operations.
 */
class TagService(private val database: Database) {

    // Cache for tags to improve performance
    private data class CachedTags(val tags: List<String>, val timestamp: Instant)

    // Cache expiration time in seconds
    private val cacheExpirationSeconds = 300L // 5 minutes

    // Thread-safe cache
    private val tagsCache = ConcurrentHashMap<String, CachedTags>()

    /**
     * Get all unique tags in the system.
     * This method uses caching to improve performance.
     *
     * @return A TagsResponse containing a list of all unique tags.
     */
    fun getAllTags(): TagsResponse {
        // Check if we have a valid cache entry
        val cacheKey = "all_tags"
        val cachedValue = tagsCache[cacheKey]

        if (cachedValue != null) {
            val age = Duration.between(cachedValue.timestamp, Instant.now()).seconds
            if (age < cacheExpirationSeconds) {
                // Cache is still valid, return cached value
                return TagsResponse(cachedValue.tags)
            }
        }

        // Cache is invalid or doesn't exist, fetch from database
        val tags = transaction(database) {
            Tags.selectAll()
                .map { it[Tags.name] }
                .sorted()
        }

        // Update cache
        tagsCache[cacheKey] = CachedTags(tags, Instant.now())

        return TagsResponse(tags)
    }
}
