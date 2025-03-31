package org.jetbrains.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.realworld.article.ArticleWithoutBody

@Composable
fun ArticleListContent(
    uiState: ArticleListUiState,
    onRefresh: () -> Unit,
    onArticleClick: (ArticleWithoutBody) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Articles") },
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
                is ArticleListUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is ArticleListUiState.Success -> if (uiState.articles.isEmpty()) {
                    Text(text = "No articles found", modifier = Modifier.align(Alignment.Center))
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

                is ArticleListUiState.Error ->
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

@Composable
fun ArticleItem(
    article: ArticleWithoutBody,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.description,
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${article.author.username}",
                    style = MaterialTheme.typography.caption
                )
                if (article.tagList.isNotEmpty()) {
                    Text(
                        text = article.tagList.joinToString(", "),
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}
