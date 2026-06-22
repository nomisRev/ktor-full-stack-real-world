package org.jetbrains.realworld.profile

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.realworld.DatabaseSpec
import org.jetbrains.realworld.user.Argon2Hasher
import org.jetbrains.realworld.user.Users
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileServiceTest : DatabaseSpec() {
    private val service by lazy { ProfileRepository(database) }
    private val hasher by lazy { Argon2Hasher() }

    private suspend fun createTestUser(
        username: String = Uuid.random().toString(),
        email: String = "${Uuid.random()}@example.com",
        bio: String? = null,
        image: String? = null
    ): Long {
        val password = "password"
        val (salt, hash) = hasher.encrypt(password)

        return transaction(database) {
            Users.insert {
                it[Users.username] = username
                it[Users.email] = email
                it[Users.password] = hash
                it[Users.salt] = salt
                it[Users.bio] = bio
                it[Users.image] = image
            } get Users.id
        }.value
    }

    @Test
    fun testGetProfileNonExistent() = runBlocking {
        val profile = service.getProfileOrNull("nonexistent")
        assertNull(profile, "Profile should be null for non-existent username")
    }

    @Test
    fun testGetProfile() = runBlocking {
        val random = Uuid.random()
        val username = "user-$random"
        val bio = "Test bio"
        val image = "https://example.com/image.jpg"

        val _ = createTestUser(username = username, bio = bio, image = image)

        val profile = service.getProfileOrNull(username)
        assertNotNull(profile, "Profile should not be null")
        assertEquals(username, profile.username)
        assertEquals(bio, profile.bio)
        assertEquals(image, profile.image)
        assertFalse(profile.following, "User should not be following themselves")
    }

    @Test
    fun testFollowUser() = runBlocking {
        val followerUsername = "follower"
        val followedUsername = "followed"

        val followerId = createTestUser(username = followerUsername)
        val _ = createTestUser(username = followedUsername)

        val profile = service.followUser(followedUsername, followerId)
        assertNotNull(profile, "Profile should not be null")
        assertEquals(followedUsername, profile.username)
        assertTrue(profile.following, "User should be following the profile")

        // Verify that following the same user again doesn't cause errors
        val profileAgain = service.followUser(followedUsername, followerId)
        assertNotNull(profileAgain, "Profile should not be null when following again")
        assertTrue(profileAgain.following, "User should still be following the profile")
    }

    @Test
    fun testFollowSelf() = runBlocking {
        val username = "selffollow"
        val userId = createTestUser(username = username)

        val profile = service.followUser(username, userId)
        assertNotNull(profile, "Profile should not be null")
        assertEquals(username, profile.username)
        assertFalse(profile.following, "User should not be able to follow themselves")
    }

    @Test
    fun testUnfollowUser() = runBlocking {
        val followerUsername = "unfollower"
        val followedUsername = "unfollowed"

        val followerId = createTestUser(username = followerUsername)
        val _ = createTestUser(username = followedUsername)

        // First follow the user
        val followedProfile = service.followUser(followedUsername, followerId)
        assertNotNull(followedProfile, "Profile should not be null")
        assertTrue(followedProfile.following, "User should be following the profile")

        // Then unfollow
        val unfollowedProfile = service.unfollowUser(followedUsername, followerId)
        assertNotNull(unfollowedProfile, "Profile should not be null")
        assertEquals(followedUsername, unfollowedProfile.username)
        assertFalse(unfollowedProfile.following, "User should not be following the profile after unfollowing")

        // Verify that unfollowing a user that is not followed doesn't cause errors
        val unfollowedAgain = service.unfollowUser(followedUsername, followerId)
        assertNotNull(unfollowedAgain, "Profile should not be null when unfollowing again")
        assertFalse(unfollowedAgain.following, "User should still not be following the profile")
    }

    @Test
    fun testUnfollowNonExistentUser() = runBlocking {
        val followerId = createTestUser()

        val profile = service.unfollowUser("nonexistent", followerId)
        assertNull(profile, "Profile should be null for non-existent username")
    }
}
