package org.jetbrains.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import org.jetbrains.feed.FeedContent
import org.jetbrains.feed.FeedViewModel
import org.jetbrains.home.ArticleListContent
import org.jetbrains.home.ArticleListViewModel
import org.jetbrains.profile.ProfileContent
import org.jetbrains.profile.ProfileViewModel
import kotlin.reflect.typeOf

@Composable
fun RealWorldNavHost(
    listViewModel: ArticleListViewModel,
    detailViewModel: ArticleDetailViewModel,
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel,
    profileViewModel: ProfileViewModel
) {
    val navController = rememberNavController()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            if (isAuthenticated) {
                BottomNavigation {
                    BottomNavigationItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Articles") },
                        label = { Text("Articles") },
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            navController.navigate(ArticleListContent) {
                                popUpTo(ArticleListContent) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                    BottomNavigationItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Feed") },
                        label = { Text("Feed") },
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            navController.navigate(FeedContent) {
                                popUpTo(ArticleListContent) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                    BottomNavigationItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            navController.navigate(ProfileContent) {
                                popUpTo(ArticleListContent) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController, 
            ArticleListContent,
            Modifier.padding(innerPadding)
        ) {
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
                    },
                    isAuthenticated = isAuthenticated,
                    onLoginClick = {
                        navController.navigate(LoginContent)
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

            composable<FeedContent> {
                val uiState by feedViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    feedViewModel.loadFeed()
                }

                FeedContent(
                    uiState = uiState,
                    onRefresh = feedViewModel::loadFeed,
                    onArticleClick = { article ->
                        navController.navigate(ArticleDetail(article.slug))
                    }
                )
            }

            composable<ProfileContent> {
                val uiState by profileViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    profileViewModel.loadProfile()
                }

                ProfileContent(
                    uiState = uiState,
                    onRefresh = profileViewModel::loadProfile,
                    onLogout = {
                        authViewModel.logout()
                        selectedTab = 0
                        navController.navigate(ArticleListContent) {
                            popUpTo(ArticleListContent) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}
