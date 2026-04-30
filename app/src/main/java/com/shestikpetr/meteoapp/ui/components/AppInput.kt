package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles
import com.shestikpetr.meteoapp.ui.theme.appColors

private val InputShape = RoundedCornerShape(6.dp)

/**
 * Текстовое поле в стиле meteo-web: bg-elev фон, line2 рамка, accent-фокус.
 * Лейбл показывается снаружи поля (не плавающий) — как в .label CSS.
 */
@Composable
fun AppInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val palette = MaterialTheme.appColors
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            AppLabel(text = label)
            Spacer(Modifier.height(5.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            placeholder = placeholder?.let { { Text(it, color = palette.ink4) } },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = error != null,
            shape = InputShape,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.bgElev,
                unfocusedContainerColor = palette.bgElev,
                disabledContainerColor = palette.bgSunken,
                errorContainerColor = palette.bgElev,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.line2,
                disabledBorderColor = palette.line,
                errorBorderColor = palette.danger,
                cursorColor = palette.accent,
                errorCursorColor = palette.danger,
                focusedTextColor = palette.ink,
                unfocusedTextColor = palette.ink,
                disabledTextColor = palette.ink3,
                errorTextColor = palette.ink,
                focusedLabelColor = palette.accent,
                unfocusedLabelColor = palette.ink3,
                disabledLabelColor = palette.ink4
            ),
            modifier = Modifier.fillMaxWidth()
        )
        when {
            error != null -> {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.danger
                )
            }
            helper != null -> {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink4
                )
            }
        }
    }
}

/** Маленький UPPERCASE-лейбл над полем формы. */
@Composable
fun AppLabel(text: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.appColors
    Text(
        text = text.uppercase(),
        style = MeteoTextStyles.Label,
        color = palette.ink3,
        modifier = modifier
    )
}
