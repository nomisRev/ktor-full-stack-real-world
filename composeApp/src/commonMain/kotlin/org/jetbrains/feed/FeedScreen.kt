package org.jetbrains.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.home.ArticleItem
import org.jetbrains.realworld.article.ArticleWithoutBody

@Composable
fun FeedContent(
    uiState: FeedUiState,
    onRefresh: () -> Unit,
    onArticleClick: (ArticleWithoutBody) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Feed") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        // TODO Refresh icon should go here
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                is FeedUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is FeedUiState.Success -> if (uiState.articles.isEmpty()) {
                    Text(text = "No articles found in your feed", modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.articles) { article ->
                            ArticleItem(article = article, onClick = { onArticleClick(article) })
                        }
                    }
                }

                is FeedUiState.Error ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.message}",
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
            }
        }
    }
}