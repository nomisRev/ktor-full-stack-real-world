package org.jetbrains.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.realworld.article.Article
import org.jetbrains.realworld.article.ArticlesResource
import org.jetbrains.realworld.article.SingleArticleResponse

class ArticleDetailViewModel(private val client: HttpClient) : ViewModel() {
    private val _uiState = MutableStateFlow<ArticleDetailUiState>(ArticleDetailUiState.Loading)
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    fun loadArticle(slug: String) {
        viewModelScope.launch {
            _uiState.value = ArticleDetailUiState.Loading
            try {
                val response = client
                    .get(ArticlesResource.BySlug(ArticlesResource(), slug))
                    .body<SingleArticleResponse>()
                _uiState.value = ArticleDetailUiState.Success(response.article)
            } catch (e: Exception) {
                _uiState.value = ArticleDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class ArticleDetailUiState {
    data object Loading : ArticleDetailUiState()
    data class Success(val article: Article) : ArticleDetailUiState()
    data class Error(val message: String) : ArticleDetailUiState()
}
