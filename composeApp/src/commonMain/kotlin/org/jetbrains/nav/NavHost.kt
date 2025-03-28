package org.jetbrains.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.detail.ArticleDetailContent
import org.jetbrains.detail.ArticleDetailViewModel
import org.jetbrains.home.ArticleListContent
import org.jetbrains.home.ArticleListViewModel
import kotlin.reflect.typeOf

@Composable
fun RealWorldNavHost(
    listViewModel: ArticleListViewModel,
    detailViewModel: ArticleDetailViewModel
) {
    val navController = rememberNavController()

    NavHost(navController, ArticleListContent) {
        composable<ArticleListContent> {
            val uiState by listViewModel.uiState.collectAsState()
            ArticleListContent(
                uiState = uiState,
                onRefresh = listViewModel::loadArticles,
                onArticleClick = { article ->
                    navController.navigate(ArticleDetail(article.slug))
                }
            )
        }

        composable<ArticleDetail>(typeMap = mapOf(typeOf<Slug>() to SlugNavType)) { backStackEntry: NavBackStackEntry ->
            val slug = TODO("")
            val uiState by detailViewModel.uiState.collectAsState()

            LaunchedEffect(slug) {
                detailViewModel.loadArticle(slug)
            }

            ArticleDetailContent(uiState = uiState, onBackClick = navController::popBackStack)
        }
    }
}