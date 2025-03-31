package org.jetbrains.nav

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@SerialName("ArticleList")
data object ArticleListContent

@Serializable
@JvmInline
value class Slug(val slug: String)

@Serializable
@SerialName("ArticleDetail")
data class ArticleDetail(val slug: String)

internal val SlugNavType = object : NavType<Slug>(isNullableAllowed = false) {
    override fun get(bundle: SavedState, key: String): Slug = bundle.read { Slug(getString(key)) }
    override fun put(bundle: SavedState, key: String, value: Slug) = bundle.write { putString(key, value.slug) }
    override fun parseValue(value: String): Slug = Slug(value)
    override fun serializeAsValue(value: Slug): String = value.slug
}

@Serializable
@SerialName("Register")
data object RegisterContent

@Serializable
@SerialName("Login")
data object LoginContent

@Serializable
@SerialName("Feed")
data object FeedContent

@Serializable
@SerialName("Profile")
data object ProfileContent
