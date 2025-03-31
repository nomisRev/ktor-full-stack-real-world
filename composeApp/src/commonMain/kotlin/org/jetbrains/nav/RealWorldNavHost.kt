package org.jetbrains.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.jetbrains.auth.AuthViewModel
import org.jetbrains.auth.LoginContent
import org.jetbrains.auth.RegisterContent
import org.jetbrains.detail.ArticleDetailContent
import org.jetbrains.detail.ArticleDetailViewModel
import org.jetbrains.home.ArticleListContent
import org.jetbrains.home.ArticleListViewModel
import kotlin.reflect.typeOf

@Composable
fun RealWorldNavHost(
    listViewModel: ArticleListViewModel,
    detailViewModel: ArticleDetailViewModel,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    NavHost(navController, if (isAuthenticated) ArticleListContent else LoginContent) {
        composable<LoginContent> {
            val uiState by authViewModel.loginViewModel.uiState.collectAsState()
            LoginContent(
                uiState = uiState,
                onLogin = authViewModel.loginViewModel::login,
                onNavigateToRegister = {
                    navController.navigate(RegisterContent)
                }
            )
        }

        composable<RegisterContent> {
            val uiState by authViewModel.registerViewModel.uiState.collectAsState()
            RegisterContent(
                uiState = uiState,
                onRegister = authViewModel.registerViewModel::register,
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable<ArticleListContent> {
            val uiState by listViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                listViewModel.loadArticles()
            }

            ArticleListContent(
                uiState = uiState,
                onRefresh = listViewModel::loadArticles,
                onArticleClick = { article ->
                    navController.navigate(ArticleDetail(article.slug))
                }
            )
        }

        composable<ArticleDetail>(typeMap = mapOf(typeOf<Slug>() to SlugNavType)) { backStackEntry: NavBackStackEntry ->
            val slug = backStackEntry.toRoute<ArticleDetail>().slug
            val uiState by detailViewModel.uiState.collectAsState()

            LaunchedEffect(slug) {
                detailViewModel.loadArticle(slug)
            }

            ArticleDetailContent(uiState = uiState, onBackClick = navController::popBackStack)
        }
    }
}
