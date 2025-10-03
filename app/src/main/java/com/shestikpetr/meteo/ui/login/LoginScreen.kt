package com.shestikpetr.meteo.ui.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteo.BuildConfig
import com.shestikpetr.meteo.common.error.MeteoResult
import com.shestikpetr.meteo.common.logging.MeteoLogger
import com.shestikpetr.meteo.network.AuthTokens
import com.shestikpetr.meteo.ui.components.MeteoLogo
import com.shestikpetr.meteo.ui.components.MinimalTextField
import com.shestikpetr.meteo.ui.components.MinimalButton
import com.shestikpetr.meteo.ui.components.MinimalCard
import com.shestikpetr.meteo.localization.compose.stringResource
import com.shestikpetr.meteo.localization.interfaces.StringKey
import com.shestikpetr.meteo.config.interfaces.DemoConfigRepository

/**
 * Authentication modes for the login screen.
 */
enum class AuthMode {
    LOGIN, REGISTER
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    demoConfigViewModel: DemoConfigViewModel = hiltViewModel()
) {
    val logger = MeteoLogger.forTag("LoginScreen")
    // Get localized error messages
    val loginErrorMessage = stringResource(StringKey.LoginErrorInvalidCredentials)
    val registerErrorMessage = stringResource(StringKey.RegisterErrorUserExists)
    val passwordMismatchMessage = stringResource(StringKey.PasswordMismatch)
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isDemoLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var demoCredentials by remember { mutableStateOf<DemoConfigRepository.DemoCredentials?>(null) }

    // Load demo credentials
    LaunchedEffect(Unit) {
        try {
            demoCredentials = demoConfigViewModel.demoConfigRepository.getDemoCredentials().getOrElse {
                demoConfigViewModel.demoConfigRepository.getDefaultDemoCredentials()
            }
        } catch (e: Exception) {
            logger.e("Failed to load demo credentials", e)
            demoCredentials = demoConfigViewModel.demoConfigRepository.getDefaultDemoCredentials()
        }
    }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val loginState = viewModel.loginState.collectAsState()
    val scrollState = rememberScrollState()

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    val contentOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 50.dp,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "content_offset"
    )

    LaunchedEffect(loginState.value) {
        when (loginState.value) {
            is MeteoResult.Success -> {
                isLoading = false
                isDemoLoading = false
                onLoginSuccess()
            }

            is MeteoResult.Error -> {
                isLoading = false
                isDemoLoading = false
                errorMessage = when (authMode) {
                    AuthMode.LOGIN -> loginErrorMessage
                    AuthMode.REGISTER -> registerErrorMessage
                }
            }

            is MeteoResult.Loading -> {
                errorMessage = null
            }

            null -> {
                isLoading = false
                isDemoLoading = false
                errorMessage = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
                .alpha(contentAlpha)
                .offset(y = contentOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Animated logo
            MeteoLogo(
                size = 100.dp,
                animated = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(StringKey.AppTitle),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(StringKey.AppSubtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Auth form card
            MinimalCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode toggle with improved design
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ModeToggleButton(
                                text = stringResource(StringKey.LoginMode),
                                isSelected = authMode == AuthMode.LOGIN,
                                onClick = {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                    confirmPassword = ""
                                    email = ""
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ModeToggleButton(
                                text = stringResource(StringKey.RegisterMode),
                                isSelected = authMode == AuthMode.REGISTER,
                                onClick = {
                                    authMode = AuthMode.REGISTER
                                    errorMessage = null
                                },
                                modifier = Modifier.weight(1f)
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
                            MinimalTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = stringResource(StringKey.Email),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                            )
                        }

                        // Username field
                        MinimalTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = stringResource(StringKey.Username),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        )

                        // Password field
                        MinimalTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(StringKey.Password),
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = if (passwordVisible) stringResource(StringKey.HidePassword) else stringResource(StringKey.ShowPassword),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = if (passwordVisible) 1f else 0.5f)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (mode == AuthMode.REGISTER) 20.dp else 32.dp)
                        )

                        // Confirm password field (only for registration)
                        if (mode == AuthMode.REGISTER) {
                            MinimalTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = stringResource(StringKey.ConfirmPassword),
                                visualTransformation = if (confirmPasswordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        confirmPasswordVisible = !confirmPasswordVisible
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = if (confirmPasswordVisible) stringResource(StringKey.HidePassword) else stringResource(StringKey.ShowPassword),
                                            tint = if (confirmPassword.isNotEmpty() && password != confirmPassword)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (confirmPasswordVisible) 1f else 0.5f)
                                        )
                                    }
                                },
                                supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                    {
                                        Text(
                                            text = passwordMismatchMessage,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            )
                        }

                        // Action button
                        MinimalButton(
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
                                            errorMessage = passwordMismatchMessage
                                        }
                                    }
                                }
                            },
                            enabled = when (mode) {
                                AuthMode.LOGIN -> username.isNotBlank() && password.isNotBlank()
                                AuthMode.REGISTER -> username.isNotBlank() &&
                                        password.isNotBlank() &&
                                        confirmPassword.isNotBlank() &&
                                        email.isNotBlank() &&
                                        password == confirmPassword
                            },
                            loading = isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(
                                text = when (mode) {
                                    AuthMode.LOGIN -> stringResource(StringKey.LoginButton)
                                    AuthMode.REGISTER -> stringResource(StringKey.RegisterButton)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // Demo mode button
            MinimalCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(StringKey.DemoMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(StringKey.DemoModeDescription),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            isDemoLoading = true
                            // Use dynamic demo credentials instead of hardcoded values
                            val credentials = demoCredentials
                            if (credentials != null && credentials.enabled) {
                                viewModel.login(credentials.username, credentials.password)
                            } else {
                                errorMessage = "Demo mode is disabled"
                                isDemoLoading = false
                            }
                        },
                        enabled = !isDemoLoading && !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDemoLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                stringResource(StringKey.DemoModeButton),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Error message with animation
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = slideInVertically(animationSpec = tween(300)) + fadeIn(),
                exit = slideOutVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    logger.d("Login error displayed: $message")
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ModeToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(300),
        label = "button_background"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "button_content"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    TextButton(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}