package org.jetbrains.realworld.article

import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.realworld.profile.Follows
import org.jetbrains.realworld.profile.Profile
import org.jetbrains.realworld.user.Users
import kotlin.time.Clock

object Articles : LongIdTable("articles", "article_id") {
    val slug = varchar("slug", 255).uniqueIndex()
    val title = varchar("title", 255)
    val description = text("description")
    val body = text("body")
    val authorId = reference("author_id", Users).uniqueIndex("idx_articles_author_id")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
        .uniqueIndex("idx_articles_created_at")
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

object Tags : LongIdTable("tags", "tag_id") {
    val name = varchar("name", 255).uniqueIndex()
}

object ArticleTags : Table("article_tags") {
    val articleId = reference("article_id", Articles)
    val tagId = reference("tag_id", Tags).uniqueIndex("idx_article_tags_tag_id")

    override val primaryKey = PrimaryKey(articleId, tagId)
}

object Favorites : Table("favorites") {
    val userId = reference("user_id", Users)
    val articleId = reference("article_id", Articles).uniqueIndex("idx_favorites_article_id")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(userId, articleId)
}

private object TotalCount : ExpressionWithColumnType<Long>() {
    override val columnType = LongColumnType()

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("COUNT(*) OVER ()")
    }
}

private object FavoriteCount : ExpressionWithColumnType<Long>() {
    override val columnType = LongColumnType()

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("COUNT(*)")
    }
}

class ArticleRepository(private val database: Database) {

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

        val rows = Articles
            .innerJoin(Users) { Articles.authorId eq Users.id }
            .select(
                Articles.id,
                Articles.slug,
                Articles.title,
                Articles.description,
                Articles.createdAt,
                Articles.updatedAt,
                Users.id,
                Users.username,
                Users.bio,
                Users.image,
                TotalCount
            )
            .where { whereTag and whereAuthor and whereFavorited }
            .limit(limit)
            .offset(offset.toLong())
            .orderBy(Articles.createdAt, SortOrder.DESC)
            .toList()

        val totalCount = rows.firstOrNull()?.get(TotalCount) ?: 0L
        val articleIds = rows.map { it[Articles.id].value }
        val authorIds = rows.map { it[Users.id].value }.toSet()
        val tagsByArticleId = fetchTags(articleIds)
        val favoritesCountByArticleId = fetchFavoritesCounts(articleIds)
        val favoritedArticleIds =
            if (currentUserId != null) fetchFavoritedArticleIds(articleIds, currentUserId) else emptySet()
        val followingAuthorIds =
            if (currentUserId != null) fetchFollowingAuthorIds(authorIds, currentUserId) else emptySet()

        val articles = rows.map { row ->
            val articleId = row[Articles.id].value
            val authorId = row[Users.id].value

            val articleTags = tagsByArticleId[articleId].orEmpty()
            val orderedTags = if (tag != null) articleTags.sortedWith(compareBy { it != tag }) else articleTags

            ArticleWithoutBody(
                slug = row[Articles.slug],
                title = row[Articles.title],
                description = row[Articles.description],
                tagList = orderedTags,
                createdAt = row[Articles.createdAt],
                updatedAt = row[Articles.updatedAt],
                favorited = articleId in favoritedArticleIds,
                favoritesCount = favoritesCountByArticleId[articleId]?.toInt() ?: 0,
                author = Profile(
                    username = row[Users.username],
                    bio = row[Users.bio],
                    image = row[Users.image],
                    following = authorId in followingAuthorIds
                )
            )
        }

        MultipleArticlesResponse(articles, totalCount.toInt())
    }

    fun getFeed(
        currentUserId: Long,
        limit: Int,
        offset: Int
    ): MultipleArticlesResponse = transaction(database) {
        addLogger(StdOutSqlLogger)
        val following = Follows.select(Follows.followedId)
            .where { Follows.followerId eq currentUserId }

        val rows = Articles
            .innerJoin(Users) { Articles.authorId eq Users.id }
            .select(
                Articles.id,
                Articles.slug,
                Articles.title,
                Articles.description,
                Articles.createdAt,
                Articles.updatedAt,
                Users.id,
                Users.username,
                Users.bio,
                Users.image,
                TotalCount
            )
            .where { Articles.authorId inSubQuery following }
            .limit(limit)
            .offset(offset.toLong())
            .orderBy(Articles.createdAt, SortOrder.DESC)
            .toList()

        val totalCount = rows.firstOrNull()?.get(TotalCount) ?: 0L
        val articleIds = rows.map { it[Articles.id].value }
        val authorIds = rows.map { it[Users.id].value }.toSet()
        val tagsByArticleId = fetchTags(articleIds)
        val favoritesCountByArticleId = fetchFavoritesCounts(articleIds)
        val favoritedArticleIds = fetchFavoritedArticleIds(articleIds, currentUserId)
        val followingAuthorIds = fetchFollowingAuthorIds(authorIds, currentUserId)

        val articles = rows.map { row ->
            val articleId = row[Articles.id].value
            val authorId = row[Users.id].value

            ArticleWithoutBody(
                slug = row[Articles.slug],
                title = row[Articles.title],
                description = row[Articles.description],
                tagList = tagsByArticleId[articleId].orEmpty(),
                createdAt = row[Articles.createdAt],
                updatedAt = row[Articles.updatedAt],
                favorited = articleId in favoritedArticleIds,
                favoritesCount = favoritesCountByArticleId[articleId]?.toInt() ?: 0,
                author = Profile(
                    username = row[Users.username],
                    bio = row[Users.bio],
                    image = row[Users.image],
                    following = authorId in followingAuthorIds
                )
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

        val newSlug = update.title?.let { generateSlug(it) } ?: slug

        Articles.update({ Articles.id eq articleId }) { row ->
            update.title?.let {
                row[title] = it
                row[this.slug] = newSlug
            }
            update.description?.let { row[description] = it }
            update.body?.let { row[body] = it }
            row[updatedAt] = Clock.System.now()
        }

        getArticleBySlug(newSlug, authorId)
    }

    fun createArticle(authorId: Long, newArticle: NewArticle): Article = transaction(database) {
        val slug = generateSlug(newArticle.title)

        val insert = Articles.insertReturning(listOf(Articles.id, Articles.createdAt, Articles.updatedAt)) {
            it[this.slug] = slug
            it[title] = newArticle.title
            it[description] = newArticle.description
            it[body] = newArticle.body
            it[this.authorId] = authorId
        }.single()

        val tagNames = newArticle.tagList.distinct()
        if (tagNames.isNotEmpty()) {
            Tags.batchInsert(tagNames, ignore = true, shouldReturnGeneratedValues = false) { tagName ->
                this[Tags.name] = tagName
            }
        }

        val tagIdsByName = if (tagNames.isEmpty()) {
            emptyMap()
        } else {
            Tags.select(Tags.id, Tags.name)
                .where { Tags.name inList tagNames }
                .associate { it[Tags.name] to it[Tags.id].value }
        }
        val tagIds = tagNames.map { tagName -> tagIdsByName.getValue(tagName) }

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
            tagList = tagNames,
            createdAt = insert[Articles.createdAt],
            updatedAt = insert[Articles.updatedAt],
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
                Users.id, Users.username, Users.bio, Users.image
            )
            .where { Articles.slug eq slug }
            .singleOrNull() ?: return@transaction null

        val articleId = row[Articles.id].value
        val authorId = row[Users.id].value
        val tags = fetchTags(listOf(articleId))[articleId].orEmpty()
        val favoritesCount = fetchFavoritesCounts(listOf(articleId))[articleId] ?: 0L
        val favorited = if (currentUserId != null) articleId in fetchFavoritedArticleIds(
            listOf(articleId),
            currentUserId
        ) else false

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
            author = Profile(
                username = row[Users.username],
                bio = row[Users.bio],
                image = row[Users.image],
                following = currentUserId != null && authorId in fetchFollowingAuthorIds(setOf(authorId), currentUserId)
            )
        )
    }

    fun deleteArticle(slug: String, authorId: Long): Boolean? = transaction(database) {
        val article = Articles
            .select(Articles.id, Articles.authorId)
            .where { Articles.slug eq slug }
            .singleOrNull() ?: return@transaction false

        if (article[Articles.authorId].value != authorId) return@transaction null

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
        val alreadyFavorited = isFavorited(articleId, userId)

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

    fun allTags(): List<String> = transaction(database) {
        Tags.select(Tags.name).map { it[Tags.name] }
    }


    private fun isFavorited(articleId: Long, currentUserId: Long): Boolean = Favorites
        .select(Favorites.userId)
        .where { (Favorites.articleId eq articleId) and (Favorites.userId eq currentUserId) }
        .limit(1)
        .singleOrNull() != null

    private fun fetchFavoritedArticleIds(articleIds: Collection<Long>, currentUserId: Long): Set<Long> {
        if (articleIds.isEmpty()) return emptySet()

        return Favorites
            .select(Favorites.articleId)
            .where { (Favorites.userId eq currentUserId) and (Favorites.articleId inList articleIds) }
            .map { it[Favorites.articleId].value }
            .toSet()
    }

    private fun fetchFavoritesCounts(articleIds: Collection<Long>): Map<Long, Long> {
        if (articleIds.isEmpty()) return emptyMap()

        return Favorites
            .select(Favorites.articleId, FavoriteCount)
            .where { Favorites.articleId inList articleIds }
            .groupBy(Favorites.articleId)
            .associate { row -> row[Favorites.articleId].value to row[FavoriteCount] }
    }

    private fun fetchFollowingAuthorIds(authorIds: Collection<Long>, currentUserId: Long): Set<Long> {
        if (authorIds.isEmpty()) return emptySet()

        return Follows
            .select(Follows.followedId)
            .where { (Follows.followerId eq currentUserId) and (Follows.followedId inList authorIds) }
            .map { it[Follows.followedId].value }
            .toSet()
    }

    private fun fetchTags(articleIds: Collection<Long>): Map<Long, List<String>> {
        if (articleIds.isEmpty()) return emptyMap()

        val rows = ArticleTags
            .innerJoin(Tags) { ArticleTags.tagId eq Tags.id }
            .select(ArticleTags.articleId, Tags.name)
            .where { ArticleTags.articleId inList articleIds }
            .toList()

        return rows
            .groupBy { it[ArticleTags.articleId].value }
            .mapValues { [_, rowsForArticle] -> rowsForArticle.map { row -> row[Tags.name] } }
    }

    private fun generateSlug(title: String): String =
        title.lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')
            .plus("-${System.currentTimeMillis()}")
}
