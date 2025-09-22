package com.shestikpetr.meteo.ui.stations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.shestikpetr.meteo.network.StationInfo

/**
 * Station Management Screen - allows users to add, remove, and edit stations
 * Available in API v1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationManagementScreen(
    viewModel: StationManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.loadUserStations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Управление станциями",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add station section
        AddStationSection(
            onAddStation = { stationNumber, customName ->
                viewModel.addStation(stationNumber, customName)
                keyboardController?.hide()
            },
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stations list
        Text(
            text = "Мои станции",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading && uiState.stations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.stations.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "У вас пока нет добавленных станций",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.stations) { station ->
                        StationItem(
                            station = station,
                            onToggleFavorite = { viewModel.toggleFavorite(station.station_number) },
                            onEditName = { newName ->
                                viewModel.updateStationName(station.station_number, newName)
                            },
                            onRemove = { viewModel.removeStation(station.station_number) }
                        )
                    }
                }
            }
        }

        // Error message
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun AddStationSection(
    onAddStation: (String, String?) -> Unit,
    isLoading: Boolean
) {
    var stationNumber by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Добавить станцию",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = stationNumber,
                onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) stationNumber = it },
                label = { Text("Номер станции (8 цифр)") },
                placeholder = { Text("12345678") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                supportingText = { Text("Введите 8-значный номер станции") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text("Название (необязательно)") },
                placeholder = { Text("Моя станция") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (stationNumber.length == 8) {
                            onAddStation(stationNumber, customName.ifBlank { null })
                            stationNumber = ""
                            customName = ""
                        }
                    }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onAddStation(stationNumber, customName.ifBlank { null })
                    stationNumber = ""
                    customName = ""
                },
                enabled = stationNumber.length == 8 && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Добавить станцию")
            }
        }
    }
}

@Composable
private fun StationItem(
    station: StationInfo,
    onToggleFavorite: () -> Unit,
    onEditName: (String) -> Unit,
    onRemove: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.display_name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "№ ${station.station_number}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (station.location.isNotBlank()) {
                        Text(
                            text = station.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Параметры: ${station.parameters.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (station.is_favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (station.is_favorite) "Убрать из избранного" else "Добавить в избранное",
                            tint = if (station.is_favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }

    // Edit dialog
    if (showEditDialog) {
        EditStationDialog(
            currentName = station.custom_name ?: station.name,
            onConfirm = { newName ->
                onEditName(newName)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить станцию?") },
            text = { Text("Вы уверены, что хотите удалить станцию \"${station.display_name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun EditStationDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить название") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название станции") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}