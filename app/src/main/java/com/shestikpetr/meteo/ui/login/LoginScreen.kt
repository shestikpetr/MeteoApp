package com.shestikpetr.meteo.ui.login

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteo.network.LoginResult

enum class AuthMode {
    LOGIN, REGISTER
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val loginState = viewModel.loginState.collectAsState()

    LaunchedEffect(loginState.value) {
        when (loginState.value) {
            is LoginResult.Success -> {
                isLoading = false
                onLoginSuccess()
            }

            is LoginResult.Error -> {
                isLoading = false
                errorMessage = when (authMode) {
                    AuthMode.LOGIN -> "Неверные учетные данные. Проверьте логин и пароль."
                    AuthMode.REGISTER -> "Ошибка регистрации. Пользователь уже существует или данные некорректны."
                }
            }

            null -> {
                isLoading = false
                errorMessage = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo and title
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Метео",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Метеорологическая система",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Auth form card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            authMode = AuthMode.LOGIN
                            errorMessage = null
                            confirmPassword = ""
                            email = ""
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (authMode == AuthMode.LOGIN)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            "Вход",
                            fontWeight = if (authMode == AuthMode.LOGIN) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Text(
                        " | ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )

                    TextButton(
                        onClick = {
                            authMode = AuthMode.REGISTER
                            errorMessage = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (authMode == AuthMode.REGISTER)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            "Регистрация",
                            fontWeight = if (authMode == AuthMode.REGISTER) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Animated form content
                AnimatedContent(
                    targetState = authMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "auth_form"
                ) { mode ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Email field
                        if (mode == AuthMode.REGISTER) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )
                        }

                        // Username field
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Имя пользователя") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль") },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            trailingIcon = {
                                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(
                                        text = if (passwordVisible) "Скрыть" else "Показать",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (mode == AuthMode.REGISTER) 16.dp else 24.dp)
                        )

                        // Confirm password field (only for registration)
                        if (mode == AuthMode.REGISTER) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Подтвердите пароль") },
                                visualTransformation = if (confirmPasswordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                trailingIcon = {
                                    TextButton(onClick = {
                                        confirmPasswordVisible = !confirmPasswordVisible
                                    }) {
                                        Text(
                                            text = if (confirmPasswordVisible) "Скрыть" else "Показать",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )

                            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                Text(
                                    text = "Пароли не совпадают",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Action button
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true

                                when (mode) {
                                    AuthMode.LOGIN -> {
                                        viewModel.login(username, password)
                                    }

                                    AuthMode.REGISTER -> {
                                        if (password == confirmPassword) {
                                            viewModel.register(username, password, email)
                                        } else {
                                            isLoading = false
                                            errorMessage = "Пароли не совпадают"
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading && when (mode) {
                                AuthMode.LOGIN -> username.isNotBlank() && password.isNotBlank()
                                AuthMode.REGISTER -> username.isNotBlank() &&
                                        password.isNotBlank() &&
                                        confirmPassword.isNotBlank() &&
                                        email.isNotBlank() &&
                                        password == confirmPassword
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    when (mode) {
                                        AuthMode.LOGIN -> "Войти"
                                        AuthMode.REGISTER -> "Зарегистрироваться"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo mode button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Демо режим",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "Попробуйте приложение без регистрации",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        viewModel.login("user", "user")
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Войти в демо режим")
                    }
                }
            }
        }

        // Error message
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Log.d("Login Error", message)
        }
    }
}