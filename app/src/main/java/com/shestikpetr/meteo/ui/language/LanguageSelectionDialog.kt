package com.shestikpetr.meteo.ui.language

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteo.localization.compose.LocalizationViewModel
import com.shestikpetr.meteo.localization.compose.stringResource
import com.shestikpetr.meteo.localization.interfaces.StringKey

/**
 * Language selection dialog component
 * Allows users to switch between supported languages
 */
@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    viewModel: LocalizationViewModel = hiltViewModel()
) {
    val currentLocale by viewModel.currentLocale
    val supportedLocales by viewModel.supportedLocales
    val isLoading by viewModel.isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(StringKey.Language))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedLocales.forEach { locale ->
                    LanguageOption(
                        locale = locale,
                        isSelected = locale == currentLocale,
                        isLoading = isLoading,
                        onSelect = { viewModel.changeLocale(locale) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(StringKey.Close))
            }
        }
    )
}

/**
 * Individual language option in the selection dialog
 */
@Composable
private fun LanguageOption(
    locale: String,
    isSelected: Boolean,
    isLoading: Boolean,
    onSelect: () -> Unit
) {
    val localeName = when (locale) {
        "ru" -> "Русский"
        "en" -> "English"
        else -> locale.uppercase()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = isSelected,
                enabled = !isLoading,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = localeName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        if (isLoading && isSelected) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * Floating Action Button for language switching
 * Can be placed in the app's main UI
 */
@Composable
fun LanguageSelectionFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = "🌐",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Menu item for language selection
 * Can be used in overflow menus or settings screens
 */
@Composable
fun LanguageMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenuItem(
        text = { Text(stringResource(StringKey.Language)) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = {
            Text(
                text = "🌐",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    )
}