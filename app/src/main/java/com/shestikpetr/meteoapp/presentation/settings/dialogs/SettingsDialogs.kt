package com.shestikpetr.meteoapp.presentation.settings.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.presentation.auth.AuthFormValidator
import com.shestikpetr.meteoapp.ui.components.AppInput
import com.shestikpetr.meteoapp.ui.theme.appColors

@Composable
fun AddStationDialog(onDismiss: () -> Unit, onConfirm: (number: String, customName: String?) -> Unit) {
    var stationNumber by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    val palette = MaterialTheme.appColors
    AlertDialog(
        containerColor = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        title = { Text("Привязать станцию", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AppInput(
                    value = stationNumber,
                    onValueChange = { stationNumber = it },
                    label = "Номер станции",
                    placeholder = "MS-12345"
                )
                AppInput(
                    value = customName,
                    onValueChange = { customName = it },
                    label = "Имя (необязательно)",
                    placeholder = "Например, «Дача»",
                    helper = "Можно задать своё имя — оно будет видно вместо номера"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(stationNumber.trim(), customName.trim().ifBlank { null })
                },
                enabled = stationNumber.isNotBlank()
            ) {
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

/**
 * Диалог редактирования профиля. Отдаёт только те поля, что реально изменились.
 * API требует хотя бы одно поле — подтверждение заблокировано, пока нет изменений
 * или что-то из полей невалидно.
 */
@Composable
fun EditProfileDialog(
    currentUsername: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (username: String?, email: String?) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var email by remember { mutableStateOf(currentEmail) }
    val palette = MaterialTheme.appColors

    val trimmedUsername = username.trim()
    val trimmedEmail = email.trim()
    val usernameChanged = trimmedUsername != currentUsername
    val emailChanged = trimmedEmail != currentEmail

    val usernameValid = trimmedUsername.isBlank() ||
        (trimmedUsername.length in 3..50 && trimmedUsername.matches(Regex("^[a-zA-Z0-9_]+$")))
    val emailValid = AuthFormValidator.isEmailValid(trimmedEmail)
    val usernameError = when {
        !usernameChanged -> null
        trimmedUsername.isBlank() -> "Не может быть пустым"
        !usernameValid -> "3–50 символов: латиница, цифры, _"
        else -> null
    }
    val emailError = when {
        !emailChanged -> null
        trimmedEmail.isBlank() -> "Не может быть пустым"
        !emailValid -> "Некорректный email"
        else -> null
    }
    val canConfirm = (usernameChanged || emailChanged) &&
        usernameError == null && emailError == null

    AlertDialog(
        containerColor = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        title = { Text("Изменить профиль", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AppInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "Логин",
                    placeholder = "username",
                    error = usernameError
                )
                AppInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    error = emailError
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        trimmedUsername.takeIf { usernameChanged },
                        trimmedEmail.takeIf { emailChanged }
                    )
                },
                enabled = canConfirm
            ) {
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

/**
 * Диалог смены пароля. Поля `currentPassword` и `newPassword` обязательны;
 * `confirmPassword` — UX-проверка, наружу не уходит.
 */
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newPassword: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val palette = MaterialTheme.appColors

    val newValid = AuthFormValidator.isPasswordValid(newPassword)
    val matches = confirmPassword.isEmpty() || confirmPassword == newPassword
    val newError = when {
        newPassword.isEmpty() -> null
        !newValid -> "Минимум 6 символов"
        else -> null
    }
    val confirmError = when {
        confirmPassword.isEmpty() -> null
        !matches -> "Пароли не совпадают"
        else -> null
    }
    val canConfirm = currentPassword.isNotEmpty() &&
        newValid &&
        confirmPassword == newPassword

    AlertDialog(
        containerColor = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        title = { Text("Сменить пароль", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AppInput(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = "Текущий пароль",
                    placeholder = "••••••",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                AppInput(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Новый пароль",
                    placeholder = "не меньше 6 символов",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    error = newError
                )
                AppInput(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Повторите пароль",
                    placeholder = "повторите",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    error = confirmError
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = canConfirm
            ) {
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
