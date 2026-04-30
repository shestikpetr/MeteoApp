package com.shestikpetr.meteoapp.presentation.settings.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.components.AppInput
import com.shestikpetr.meteoapp.ui.theme.appColors

@Composable
fun AddStationDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var stationNumber by remember { mutableStateOf("") }
    val palette = MaterialTheme.appColors
    AlertDialog(
        containerColor = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        title = { Text("Привязать станцию", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Введите серийный номер метеостанции",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink3
                )
                AppInput(
                    value = stationNumber,
                    onValueChange = { stationNumber = it },
                    placeholder = "MS-12345"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(stationNumber.trim()) }, enabled = stationNumber.isNotBlank()) {
                Text("Привязать", color = palette.ink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = palette.ink3)
            }
        }
    )
}

@Composable
fun DeleteStationDialog(stationName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val palette = MaterialTheme.appColors
    AlertDialog(
        containerColor = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        title = { Text("Отвязать станцию", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                text = "Отвязать «$stationName»? Сами данные станции на сервере не удаляются.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink2
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Отвязать", color = palette.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = palette.ink3)
            }
        }
    )
}

@Composable
fun RenameStationDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newName by remember { mutableStateOf(currentName) }
    val palette = MaterialTheme.appColors
    AlertDialog(
        containerColor = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        title = { Text("Переименовать станцию", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AppInput(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Новое имя"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName.trim()) }, enabled = newName.isNotBlank()) {
                Text("Сохранить", color = palette.ink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = palette.ink3)
            }
        }
    )
}
