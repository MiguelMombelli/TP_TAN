package com.tan.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.presentation.components.TanTopBar
import com.tan.ui.theme.TANTheme

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    HistoryContent(state, onBack)
}

@Composable
fun HistoryContent(state: HistoryUiState, onBack: () -> Unit) {
    Scaffold(topBar = { TanTopBar("Historial", onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                HistoryUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                HistoryUiState.Empty ->
                    Text("Todavía no hay transcripciones", Modifier.align(Alignment.Center))

                is HistoryUiState.Error ->
                    Text(
                        state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )

                is HistoryUiState.Content ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    Text(item.date, style = MaterialTheme.typography.bodySmall)
                                    Text(item.preview, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryLoadingPreview() {
    TANTheme { HistoryContent(HistoryUiState.Loading) {} }
}

@Preview(showBackground = true)
@Composable
private fun HistoryEmptyPreview() {
    TANTheme { HistoryContent(HistoryUiState.Empty) {} }
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    TANTheme { HistoryContent(HistoryUiState.Content(fakeItems())) {} }
}

@Preview(showBackground = true)
@Composable
private fun HistoryErrorPreview() {
    TANTheme { HistoryContent(HistoryUiState.Error("No se pudo cargar")) {} }
}