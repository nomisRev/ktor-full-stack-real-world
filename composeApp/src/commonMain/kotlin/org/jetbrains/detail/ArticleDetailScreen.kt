package org.jetbrains.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.realworld.article.Article

@Composable
fun ArticleDetailContent(
    uiState: ArticleDetailUiState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Article") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // TODO Back icon should go here
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is ArticleDetailUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is ArticleDetailUiState.Success -> ArticleDetail(article = uiState.article)
                is ArticleDetailUiState.Error ->
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
                    }
            }
        }
    }
}

@Composable
fun ArticleDetail(article: Article) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = article.description,
            style = MaterialTheme.typography.subtitle1
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "By ${article.author.username}",
            style = MaterialTheme.typography.caption
        )
        if (article.tagList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tags: ${article.tagList.joinToString(", ")}",
                style = MaterialTheme.typography.caption
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = article.body,
            style = MaterialTheme.typography.body1
        )
    }
}
