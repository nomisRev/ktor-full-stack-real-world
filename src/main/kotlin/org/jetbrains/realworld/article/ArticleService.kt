package org.jetbrains.realworld.article

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.realworld.profile.Follows
import org.jetbrains.realworld.profile.Profile
import org.jetbrains.realworld.profile.ProfileService
import org.jetbrains.realworld.user.Users
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ArticleService(
    private val database: Database,
    private val profileService: ProfileService
) {

    fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        currentUserId: Long?
    ): MultipleArticlesResponse = transaction(database) {
        val whereTag = if (tag != null) Articles.id inSubQuery (
                ArticleTags.innerJoin(Tags) { ArticleTags.tagId eq Tags.id }
                    .select(ArticleTags.articleId)
                    .where { Tags.name eq tag }
                ) else Op.TRUE

        val whereAuthor = if (author != null) Users.username eq author else Op.TRUE

        val whereFavorited = if (favorited != null) Articles.id inSubQuery (
                Favorites
                    .innerJoin(Users) { Favorites.userId eq Users.id }
                    .select(Favorites.articleId)
                    .where { Users.username eq favorited }
                ) else Op.TRUE

        val query = Articles
            .innerJoin(Users) { Articles.authorId eq Users.id }
            .selectAll()
            .where { whereTag and whereAuthor and whereFavorited }

        val totalCount = query.count()

        val articles = query
            .limit(limit)
            .offset(offset.toLong())
            .orderBy(Articles.createdAt, SortOrder.DESC)
            .map { row ->
                val articleId = row[Articles.id].value

                val tags = ArticleTags
                    .innerJoin(Tags) { ArticleTags.tagId eq Tags.id }
                    .select(Tags.name)
                    .where { ArticleTags.articleId eq articleId }
                    .map { it[Tags.name] }

                val favoritesCount = Favorites
                    .select(Favorites.userId)
                    .where { Favorites.articleId eq articleId }
                    .count()

                val favorited = if (currentUserId != null) {
                    Favorites
                        .select(Favorites.userId)
                        .where { (Favorites.articleId eq articleId) and (Favorites.userId eq currentUserId) }
                        .count() > 0
                } else false

                val authorProfile = profileService.getProfileOrNull(row[Users.username], currentUserId)!!

                ArticleWithoutBody(
                    slug = row[Articles.slug],
                    title = row[Articles.title],
                    description = row[Articles.description],
                    tagList = tags,
                    createdAt = row[Articles.createdAt],
                    updatedAt = row[Articles.updatedAt],
                    favorited = favorited,
                    favoritesCount = favoritesCount.toInt(),
                    author = authorProfile
                )
            }

        MultipleArticlesResponse(articles, totalCount.toInt())
    }

    fun getFeed(
        currentUserId: Long,
        limit: Int = 20,
        offset: Int = 0
    ): MultipleArticlesResponse = transaction(database) {
        val following = Follows.select(Follows.followedId).where { Follows.followerId eq currentUserId }

        val query = Articles
            .innerJoin(Users) { Articles.authorId eq Users.id }
            .selectAll()
            .where { Articles.authorId inSubQuery following }

        val totalCount = query.count()

        val articles = query.limit(limit)
            .offset(offset.toLong())
            .orderBy(Articles.createdAt, SortOrder.DESC)
            .map { row ->
                val articleId = row[Articles.id].value

                val tags = ArticleTags
                    .innerJoin(Tags) { ArticleTags.tagId eq Tags.id }
                    .select(Tags.name)
                    .where { ArticleTags.articleId eq articleId }
                    .map { it[Tags.name] }

                val favoritesCount = Favorites
                    .select(Favorites.userId)
                    .where { Favorites.articleId eq articleId }
                    .count()

                val favorited = Favorites
                    .select(Favorites.userId)
                    .where { (Favorites.articleId eq articleId) and (Favorites.userId eq currentUserId) }
                    .count() > 0

                val authorProfile = profileService.getProfileOrNull(row[Users.username], currentUserId)!!

                val createdAt = row[Articles.createdAt]
                val updatedAt = row[Articles.updatedAt]
                println("GetFeed.CreatedAt: $createdAt")
                println("GetFeed.UpdatedAt: $updatedAt")

                ArticleWithoutBody(
                    slug = row[Articles.slug],
                    title = row[Articles.title],
                    description = row[Articles.description],
                    tagList = tags,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    favorited = favorited,
                    favoritesCount = favoritesCount.toInt(),
                    author = authorProfile
                )
            }

        MultipleArticlesResponse(articles, totalCount.toInt())
    }

    fun updateArticle(slug: String, authorId: Long, update: UpdateArticle): Article? = transaction(database) {
        val article = Articles
            .select(Articles.id, Articles.authorId)
            .where { Articles.slug eq slug }
            .singleOrNull() ?: return@transaction null

        if (article[Articles.authorId].value != authorId) {
            return@transaction null
        }

        val articleId = article[Articles.id].value

        val newSlug = if (update.title != null) generateSlug(update.title) else slug

        Articles.update({ Articles.id eq articleId }) {
            if (update.title != null) {
                it[title] = update.title
                it[this.slug] = newSlug
            }
            if (update.description != null) it[description] = update.description
            if (update.body != null) it[body] = update.body
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }

        getArticleBySlug(newSlug, authorId)
    }

    fun createArticle(authorId: Long, newArticle: NewArticle): Article? = transaction(database) {
        val slug = generateSlug(newArticle.title)

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val insert = Articles.insertReturning(listOf(Articles.id, Articles.createdAt, Articles.updatedAt)) {
            it[this.slug] = slug
            it[title] = newArticle.title
            it[description] = newArticle.description
            it[body] = newArticle.body
            it[this.authorId] = authorId
            it[createdAt] = now
            it[updatedAt] = now
        }.single()

        val tagIds = newArticle.tagList.map { tagName ->
            val existingTagId = Tags.select(Tags.id)
                .where { Tags.name eq tagName }
                .singleOrNull()?.get(Tags.id)?.value

            existingTagId ?: Tags.insertAndGetId {
                it[name] = tagName
            }.value
        }

        ArticleTags.batchInsert(tagIds, shouldReturnGeneratedValues = false) { tagId ->
            this[ArticleTags.articleId] = insert[Articles.id]
            this[ArticleTags.tagId] = tagId
        }

        val author = Users.select(Users.username, Users.bio, Users.image)
            .where { Users.id eq authorId }
            .single()

        val authorProfile = Profile(
            username = author[Users.username],
            bio = author[Users.bio],
            image = author[Users.image],
            following = false // The author can't follow themselves
        )

        Article(
            slug = slug,
            title = newArticle.title,
            description = newArticle.description,
            body = newArticle.body,
            tagList = newArticle.tagList,
            createdAt = now,
            updatedAt = now,
            favorited = false,
            favoritesCount = 0,
            author = authorProfile
        )
    }

    fun getArticleBySlug(slug: String, currentUserId: Long? = null): Article? = transaction(database) {
        val row = Articles
            .innerJoin(Users) { Articles.authorId eq Users.id }
            .select(
                Articles.id, Articles.slug, Articles.title, Articles.description, Articles.body,
                Articles.createdAt, Articles.updatedAt, Articles.authorId,
                Users.username, Users.bio, Users.image
            )
            .where { Articles.slug eq slug }
            .singleOrNull() ?: return@transaction null

        val articleId = row[Articles.id].value

        val tags = ArticleTags
            .innerJoin(Tags) { ArticleTags.tagId eq Tags.id }
            .select(Tags.name)
            .where { ArticleTags.articleId eq articleId }
            .map { it[Tags.name] }

        val favoritesCount = Favorites
            .select(Favorites.userId)
            .where { Favorites.articleId eq articleId }
            .count()

        val favorited = if (currentUserId != null) {
            Favorites
                .select(Favorites.userId)
                .where { (Favorites.articleId eq articleId) and (Favorites.userId eq currentUserId) }
                .count() > 0
        } else false

        val authorProfile = profileService.getProfileOrNull(row[Users.username], currentUserId)!!

        Article(
            slug = row[Articles.slug],
            title = row[Articles.title],
            description = row[Articles.description],
            body = row[Articles.body],
            tagList = tags,
            createdAt = row[Articles.createdAt],
            updatedAt = row[Articles.updatedAt],
            favorited = favorited,
            favoritesCount = favoritesCount.toInt(),
            author = authorProfile
        )
    }

    fun deleteArticle(slug: String, authorId: Long): Boolean = transaction(database) {
        val article = Articles
            .select(Articles.id, Articles.authorId)
            .where { Articles.slug eq slug }
            .singleOrNull() ?: return@transaction false

        if (article[Articles.authorId].value != authorId) {
            return@transaction false
        }

        val articleId = article[Articles.id].value

        ArticleTags.deleteWhere { ArticleTags.articleId eq articleId }
        Favorites.deleteWhere { Favorites.articleId eq articleId }
        Articles.deleteWhere { Articles.id eq articleId } > 0
    }

    fun favoriteArticle(slug: String, userId: Long): Article? = transaction(database) {
        val article = Articles
            .select(Articles.id)
            .where { Articles.slug eq slug }
            .singleOrNull() ?: return@transaction null

        val articleId = article[Articles.id].value

        val alreadyFavorited = Favorites
            .select(Favorites.userId)
            .where { (Favorites.articleId eq articleId) and (Favorites.userId eq userId) }
            .count() > 0

        if (!alreadyFavorited) {
            Favorites.insert {
                it[this.articleId] = articleId
                it[this.userId] = userId
            }
        }

        getArticleBySlug(slug, userId)
    }

    fun unfavoriteArticle(slug: String, userId: Long): Article? = transaction(database) {
        val article = Articles
            .select(Articles.id)
            .where { Articles.slug eq slug }
            .singleOrNull() ?: return@transaction null

        val articleId = article[Articles.id].value

        Favorites.deleteWhere { (Favorites.articleId eq articleId) and (Favorites.userId eq userId) }

        getArticleBySlug(slug, userId)
    }

    private fun generateSlug(title: String): String =
        title.lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')
            .plus("-${System.currentTimeMillis()}")
}
