package com.shestikpetr.meteoapp.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteoapp.ui.components.AppButton
import com.shestikpetr.meteoapp.ui.components.AppButtonStyle
import com.shestikpetr.meteoapp.ui.components.AppInput
import com.shestikpetr.meteoapp.ui.components.BrandMark
import com.shestikpetr.meteoapp.ui.components.SegmentedTabsEqual
import com.shestikpetr.meteoapp.ui.theme.appColors

/** Экран входа/регистрации. Двухколоночный layout (`auth-shell`) на широких экранах. */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val palette = MaterialTheme.appColors

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            if (effect is AuthEffect.NavigateToMain) onAuthSuccess()
        }
    }

    val isWide = LocalConfiguration.current.screenWidthDp >= 720
    Surface(color = palette.bg, modifier = Modifier.fillMaxSize()) {
        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                AuthAside(modifier = Modifier.weight(1f).fillMaxHeight())
                Box(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                    AuthFormContainer(state, viewModel)
                }
            }
        } else {
            AuthFormContainer(state, viewModel)
        }
    }
}

@Composable
private fun AuthAside(modifier: Modifier = Modifier) {
    val palette = MaterialTheme.appColors
    Box(
        modifier = modifier
            .background(palette.ink)
            .padding(horizontal = 32.dp, vertical = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrandMark(size = 24.dp, bg = palette.bgElev, glyph = palette.ink)
            Text(
                text = "Meteo·App",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.bgElev
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = "Метеоданные с ваших станций — в одном дашборде.",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                color = palette.bgElev,
                modifier = Modifier.widthIn(max = 380.dp)
            )
        }
    }
}

@Composable
private fun AuthFormContainer(state: AuthUiState, vm: AuthViewModel) {
    val palette = MaterialTheme.appColors
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        contentAlignment = BiasAlignment(0f, -0.2f)
    ) {
        Column(modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
            SegmentedTabsEqual(
                options = listOf("Вход", "Регистрация"),
                selectedIndex = if (state.mode == AuthUiState.Mode.LOGIN) 0 else 1,
                onSelected = { index ->
                    vm.setMode(if (index == 0) AuthUiState.Mode.LOGIN else AuthUiState.Mode.REGISTER)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            state.errorMessage?.let { error ->
                Surface(
                    color = palette.danger.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = palette.danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            AppInput(
                value = state.username,
                onValueChange = vm::setUsername,
                label = "Имя пользователя",
                placeholder = "username",
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(14.dp))

            if (state.mode == AuthUiState.Mode.REGISTER) {
                AppInput(
                    value = state.email,
                    onValueChange = vm::setEmail,
                    label = "Email",
                    placeholder = "you@example.com",
                    enabled = !state.isLoading,
                    error = if (state.email.isNotEmpty() && !AuthFormValidator.isEmailValid(state.email))
                        "Введите корректный email" else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(Modifier.height(14.dp))
            }

            AppInput(
                value = state.password,
                onValueChange = vm::setPassword,
                label = "Пароль",
                placeholder = "••••••••",
                enabled = !state.isLoading,
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { vm.submit() }),
                error = if (state.password.isNotEmpty() && state.password.length < 6)
                    "Минимум 6 символов" else null,
                trailingIcon = {
                    IconButton(onClick = vm::togglePasswordVisible) {
                        Icon(
                            imageVector = if (state.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (state.passwordVisible) "Скрыть пароль" else "Показать пароль",
                            tint = palette.ink3
                        )
                    }
                }
            )

            Spacer(Modifier.height(22.dp))

            AppButton(
                text = if (state.mode == AuthUiState.Mode.LOGIN) "Войти" else "Зарегистрироваться",
                onClick = vm::submit,
                style = AppButtonStyle.Primary,
                enabled = vm.isFormValid(),
                loading = state.isLoading,
                fullWidth = true
            )

            Spacer(Modifier.height(20.dp))

            val hint = if (state.mode == AuthUiState.Mode.LOGIN)
                "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти"
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        vm.setMode(
                            if (state.mode == AuthUiState.Mode.LOGIN) AuthUiState.Mode.REGISTER
                            else AuthUiState.Mode.LOGIN
                        )
                    }
                    .padding(top = 4.dp)
            )
        }
    }
}
