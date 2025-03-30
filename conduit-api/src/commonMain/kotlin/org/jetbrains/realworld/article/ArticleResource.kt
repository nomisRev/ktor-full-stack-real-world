package org.jetbrains.realworld.article

import io.ktor.resources.Resource
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.realworld.Root
import org.jetbrains.realworld.profile.Profile

@Resource("/articles")
class ArticlesResource(val root: Root = Root) {
    @Resource("feed")
    class Feed(val parent: ArticlesResource, val limit: Int? = null, val offset: Int? = null)

    @Resource("{slug}")
    class BySlug(val parent: ArticlesResource, val slug: String) {
        @Resource("favorite")
        class Favorite(val parent: BySlug)
    }
}


@Serializable
data class Article(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
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
    val createdAt: Instant,
    val updatedAt: Instant,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: Profile,
)

@Serializable
data class TagsResponse(val tags: List<String>)
