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
import com.shestikpetr.meteo.localization.compose.stringResource
import com.shestikpetr.meteo.localization.interfaces.StringKey
import com.shestikpetr.meteo.config.utils.ValidationUtils

/**
 * Station Management Screen - allows users to add, remove, and edit stations
 * Available in API v1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationManagementScreen(
    viewModel: StationManagementViewModel = hiltViewModel(),
    validationUtils: ValidationUtils,
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
                text = stringResource(StringKey.StationManagement),
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(StringKey.Back))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add station section
        AddStationSection(
            onAddStation = { stationNumber, customName ->
                viewModel.addStation(stationNumber, customName)
                keyboardController?.hide()
            },
            isLoading = uiState.isLoading,
            validationUtils = validationUtils
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stations list
        Text(
            text = stringResource(StringKey.MyStations),
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
                        text = stringResource(StringKey.NoStationsAdded),
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
    isLoading: Boolean,
    validationUtils: ValidationUtils
) {
    var stationNumber by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(StringKey.AddStation),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = stationNumber,
                onValueChange = { input ->
                    val filteredInput = validationUtils.getStationNumberInputFilter()(input)
                    if (validationUtils.validateStationNumberInput(filteredInput)) {
                        stationNumber = filteredInput
                    }
                },
                label = { Text(stringResource(StringKey.StationNumber)) },
                placeholder = { Text(stringResource(StringKey.StationNumberPlaceholder)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                supportingText = { Text(stringResource(StringKey.StationNumberHelp)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text(stringResource(StringKey.StationNameOptional)) },
                placeholder = { Text(stringResource(StringKey.StationNamePlaceholder)) },
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
                Text(stringResource(StringKey.AddStationButton))
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
                        text = stringResource(StringKey.Parameters, station.parameters.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (station.is_favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (station.is_favorite) stringResource(StringKey.RemoveFromFavorites) else stringResource(StringKey.AddToFavorites),
                            tint = if (station.is_favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(StringKey.EditStation))
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(StringKey.DeleteStation))
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
            title = { Text(stringResource(StringKey.DeleteStationConfirmTitle)) },
            text = { Text(stringResource(StringKey.DeleteStationConfirmMessage, station.display_name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text(stringResource(StringKey.Delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(StringKey.Cancel))
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
        title = { Text(stringResource(StringKey.EditStationTitle)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(StringKey.StationName)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(StringKey.Save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(StringKey.Cancel))
            }
        }
    )
}