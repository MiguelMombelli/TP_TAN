package com.tan.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.presentation.components.TanChip
import com.tan.presentation.components.TanTopBar
import com.tan.ui.theme.TANTheme
import com.tan.ui.theme.TanNaranja
import com.tan.ui.theme.TanTexto
import com.tan.ui.theme.TanTurquesa

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    HistoryContent(state, onBack, onItemClick, query, viewModel::onQueryChange)
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    query: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    Scaffold(topBar = { TanTopBar("Historial", onBack) }) { padding ->
      Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        // El buscador solo tiene sentido si hay transcripciones que buscar.
        if (state is HistoryUiState.Content || state is HistoryUiState.NoResults) {
            SearchField(query, onQueryChange)
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                HistoryUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                HistoryUiState.Empty ->
                    Text("Todavía no hay transcripciones", Modifier.align(Alignment.Center))

                HistoryUiState.NoResults ->
                    Text("No hay transcripciones que coincidan con la búsqueda", Modifier.align(Alignment.Center))

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
                                modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) },
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = item.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TanTexto
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TanChip(
                                            text = item.sourceLabel,
                                            color = if (item.sourceLabel == "Grabación") TanNaranja else TanTurquesa
                                        )
                                        TanChip(text = item.model, color = TanTexto)
                                    }
                                    Text(
                                        text = item.preview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
            }
        }
      }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Buscar en el historial") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun HistoryLoadingPreview() {
    TANTheme { HistoryContent(HistoryUiState.Loading, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun HistoryEmptyPreview() {
    TANTheme { HistoryContent(HistoryUiState.Empty, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    TANTheme { HistoryContent(HistoryUiState.Content(fakeItems()), {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun HistoryErrorPreview() {
    TANTheme { HistoryContent(HistoryUiState.Error("No se pudo cargar"), {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun HistoryNoResultsPreview() {
    TANTheme { HistoryContent(HistoryUiState.NoResults, {}, {}, query = "xyz") }
}
