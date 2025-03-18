package org.jetbrains.realworld.article

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.realworld.OffsetDateTimeAsString
import org.jetbrains.realworld.profile.Profile
import org.jetbrains.realworld.user.Users
import java.time.OffsetDateTime
import java.time.ZoneOffset

object Articles : LongIdTable("articles", "article_id") {
    val slug = varchar("slug", 255).uniqueIndex()
    val title = varchar("title", 255)
    val description = text("description")
    val body = text("body")
    val authorId = reference("author_id", Users)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestampWithTimeZone)
}

object Tags : LongIdTable("tags", "tag_id") {
    val name = varchar("name", 255).uniqueIndex()
}

object ArticleTags : Table("article_tags") {
    val articleId = reference("article_id", Articles)
    val tagId = reference("tag_id", Tags)

    override val primaryKey = PrimaryKey(articleId, tagId)
}

object Favorites : Table("favorites") {
    val userId = reference("user_id", Users)
    val articleId = reference("article_id", Articles)
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now(ZoneOffset.UTC))

    override val primaryKey = PrimaryKey(userId, articleId)
}

@Serializable
data class Article(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String> = emptyList(),
    val createdAt: OffsetDateTimeAsString,
    val updatedAt: OffsetDateTimeAsString,
    val favorited: Boolean = false,
    val favoritesCount: Int = 0,
    val author: Profile
)

@Serializable
data class NewArticleRequest(val article: NewArticle)

@Serializable
data class NewArticle(
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String> = emptyList()
)

@Serializable
data class UpdateArticleRequest(val article: UpdateArticle)

@Serializable
data class UpdateArticle(
    val title: String? = null,
    val description: String? = null,
    val body: String? = null
)

@Serializable
data class SingleArticleResponse(val article: Article)

@Serializable
data class MultipleArticlesResponse(val articles: List<ArticleWithoutBody>, val articlesCount: Int)

@Serializable
data class ArticleWithoutBody(
    val slug: String,
    val title: String,
    val description: String,
    val tagList: List<String>,
    val createdAt: OffsetDateTimeAsString,
    val updatedAt: OffsetDateTimeAsString,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: Profile,
)

@Serializable
data class TagsResponse(val tags: List<String>)
