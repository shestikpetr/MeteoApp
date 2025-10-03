package com.shestikpetr.meteo.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteo.model.ParameterConfig

/**
 * Dynamic dropdown menu for selecting weather parameters.
 * Replaces the static ParametersDropdownMenu with API-driven parameter selection.
 *
 * @param availableParameters List of parameters available for selection (from API)
 * @param selectedParameter Currently selected parameter configuration
 * @param onParameterSelected Callback when a parameter is selected
 * @param modifier Modifier for styling
 * @param isEnabled Whether the dropdown is enabled (default: true)
 * @param placeholder Text to show when no parameter is selected
 */
@Composable
fun DynamicParametersDropdownMenu(
    availableParameters: List<ParameterConfig>,
    selectedParameter: ParameterConfig?,
    onParameterSelected: (ParameterConfig) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    placeholder: String = "Выберите параметр"
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "dropdownRotation"
    )

    // Display text for the selected parameter or placeholder
    val displayText = selectedParameter?.displayText ?: placeholder
    val hasSelection = selectedParameter != null

    Box(
        modifier = modifier
            .widthIn(min = 150.dp, max = 250.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = if (isEnabled) 2.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    if (isEnabled) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    RoundedCornerShape(8.dp)
                )
                .clickable(enabled = isEnabled && availableParameters.isNotEmpty()) {
                    expanded = true
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        !hasSelection -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = if (expanded) "Скрыть" else "Раскрыть",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationState),
                    tint = if (isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
            }
        }

        // Dropdown menu with dynamic parameters
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 150.dp, max = 300.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (availableParameters.isEmpty()) {
                // Show placeholder when no parameters available
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Параметры недоступны",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    onClick = { expanded = false },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Show available parameters
                availableParameters.forEach { parameter ->
                    val isSelected = selectedParameter?.code == parameter.code

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = parameter.displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1
                            )
                        },
                        onClick = {
                            expanded = false
                            if (!isSelected) {
                                onParameterSelected(parameter)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                    )
                }
            }
        }
    }
}

