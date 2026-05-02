package com.shestikpetr.meteoapp.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.ThemeMode
import com.shestikpetr.meteoapp.presentation.settings.dialogs.AddStationDialog
import com.shestikpetr.meteoapp.presentation.settings.dialogs.ChangePasswordDialog
import com.shestikpetr.meteoapp.presentation.settings.dialogs.DeleteStationDialog
import com.shestikpetr.meteoapp.presentation.settings.dialogs.EditProfileDialog
import com.shestikpetr.meteoapp.presentation.settings.dialogs.RenameStationDialog
import com.shestikpetr.meteoapp.ui.components.AppButton
import com.shestikpetr.meteoapp.ui.components.AppButtonStyle
import com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles
import com.shestikpetr.meteoapp.ui.theme.appColors

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val palette = MaterialTheme.appColors
    val context = LocalContext.current

    var showAddStation by remember { mutableStateOf(false) }
    var showDeleteStation by remember { mutableStateOf<Station?>(null) }
    var showRenameStation by remember { mutableStateOf<Station?>(null) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.Toast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                SettingsEffect.NavigateToAuth -> onLogout()
            }
        }
    }

    if (showAddStation) {
        AddStationDialog(
            onDismiss = { showAddStation = false },
            onConfirm = { number, name ->
                viewModel.onAddStation(number, name)
                showAddStation = false
            }
        )
    }
    showDeleteStation?.let { station ->
        DeleteStationDialog(
            stationName = station.name,
            onDismiss = { showDeleteStation = null },
            onConfirm = {
                viewModel.onDeleteStation(station.stationNumber)
                showDeleteStation = null
            }
        )
    }
    showRenameStation?.let { station ->
        RenameStationDialog(
            currentName = station.name,
            onDismiss = { showRenameStation = null },
            onConfirm = { newName ->
                viewModel.onRenameStation(station.stationNumber, newName)
                showRenameStation = null
            }
        )
    }
    if (showEditProfile) {
        EditProfileDialog(
            currentUsername = state.user?.username.orEmpty(),
            currentEmail = state.user?.email.orEmpty(),
            onDismiss = { showEditProfile = false },
            onConfirm = { newUsername, newEmail ->
                viewModel.onUpdateProfile(newUsername, newEmail)
                showEditProfile = false
            }
        )
    }
    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onConfirm = { current, new ->
                viewModel.onChangePassword(current, new)
                showChangePassword = false
            }
        )
    }

    val scroll = rememberScrollState()
    Surface(color = palette.bg, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(onNavigateBack = onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .widthIn(max = 880.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileHero(
                    username = state.user?.username ?: "—",
                    email = state.user?.email,
                    role = state.user?.role?.name?.lowercase() ?: "user",
                    isAdmin = state.user?.role?.name == "ADMIN",
                    onLogout = viewModel::onLogout
                )

                ProfileSection(title = "Аккаунт", subtitle = null) {
                    Column {
                        AccountActionRow(
                            icon = Icons.Default.Person,
                            title = "Изменить профиль",
                            subtitle = null,
                            enabled = state.user != null,
                            onClick = { showEditProfile = true }
                        )
                        HorizontalDivider(color = palette.line)
                        AccountActionRow(
                            icon = Icons.Default.Lock,
                            title = "Сменить пароль",
                            subtitle = null,
                            onClick = { showChangePassword = true }
                        )
                    }
                }

                ProfileSection(title = "Внешний вид", subtitle = null) {
                    Column {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            ThemeModeRow(
                                mode = mode,
                                selected = state.settings.themeMode == mode,
                                onSelect = { viewModel.onSetThemeMode(mode) }
                            )
                            if (index < ThemeMode.entries.lastIndex) {
                                HorizontalDivider(color = palette.line)
                            }
                        }
                    }
                }

                ProfileSection(
                    title = "Станции",
                    subtitle = null,
                    headerActions = {
                        AppButton(
                            text = "Привязать",
                            onClick = { showAddStation = true },
                            style = AppButtonStyle.Primary,
                            icon = Icons.Default.Add
                        )
                    }
                ) {
                    if (state.stations.isEmpty()) {
                        Text(
                            text = "Список пуст",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.ink3
                        )
                    } else {
                        Column {
                            state.stations.forEachIndexed { index, station ->
                                val isHidden = station.stationNumber in state.settings.hiddenStations
                                StationRow(
                                    station = station,
                                    hidden = isHidden,
                                    onToggleHidden = { viewModel.onToggleStationHidden(station.stationNumber) },
                                    onRename = { showRenameStation = station },
                                    onDelete = { showDeleteStation = station }
                                )
                                if (index < state.stations.size - 1) {
                                    HorizontalDivider(color = palette.line)
                                }
                            }
                        }
                    }
                }

                if (state.parameters.isNotEmpty()) {
                    ProfileSection(title = "Параметры", subtitle = null) {
                        // Группировка по name. Если у группы несколько датчиков —
                        // блок раскрывается, и каждый датчик скрывается/показывается
                        // отдельно. Можно переключить всю группу одним тапом по
                        // главной строке.
                        val grouped = remember(state.parameters) {
                            val byName = LinkedHashMap<String, MutableList<com.shestikpetr.meteoapp.domain.model.ParameterMeta>>()
                            state.parameters.forEach { p -> byName.getOrPut(p.name) { mutableListOf() }.add(p) }
                            byName.values.toList()
                        }
                        Column {
                            grouped.forEachIndexed { index, group ->
                                ParameterHideRow(
                                    group = group,
                                    hidden = state.settings.hiddenParameters,
                                    onToggle = viewModel::onToggleParameterHidden
                                )
                                if (index < grouped.size - 1) {
                                    HorizontalDivider(color = palette.line)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "MeteoApp v1.0",
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    val palette = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.bgElev)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = palette.ink
                )
            }
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.ink
            )
        }
        HorizontalDivider(color = palette.line)
    }
}

@Composable
private fun ProfileHero(
    username: String,
    email: String?,
    role: String,
    isAdmin: Boolean,
    onLogout: () -> Unit
) {
    val palette = MaterialTheme.appColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.line)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.ink),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (username.firstOrNull() ?: '?').uppercase(),
                    style = MeteoTextStyles.Mono.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = palette.bgElev
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    RoleChip(role = role, isAdmin = isAdmin)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = email ?: "email не указан",
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AppButton(text = "Выйти", onClick = onLogout, style = AppButtonStyle.Primary)
        }
    }
}

@Composable
private fun RoleChip(role: String, isAdmin: Boolean) {
    val palette = MaterialTheme.appColors
    val bg = if (isAdmin) palette.accentSoft else palette.bgSunken
    val ink = if (isAdmin) palette.accentInk else palette.ink3
    val border = if (isAdmin) palette.accent.copy(alpha = 0.25f) else palette.line
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Text(
            text = role.uppercase(),
            style = MeteoTextStyles.Label.copy(color = ink),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    subtitle: String?,
    headerActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val palette = MaterialTheme.appColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.line)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.ink
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink3,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                headerActions()
            }
            HorizontalDivider(color = palette.line)
            Column(modifier = Modifier.padding(18.dp)) { content() }
        }
    }
}

/**
 * Строка-действие в секции «Аккаунт». Иконка слева, заголовок + подпись,
 * шеврон справа. По тапу открывается соответствующий диалог.
 */
@Composable
private fun AccountActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) palette.ink2 else palette.ink4,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) palette.ink else palette.ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = palette.ink3,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Строка выбора темы. Радио слева, заголовок темы справа. Вся строка
 * кликабельна и выступает как один элемент группы radio.
 */
@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val palette = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = palette.ink,
                unselectedColor = palette.line2
            )
        )
        Text(
            text = mode.label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            ),
            color = if (selected) palette.ink else palette.ink2
        )
    }
}

/**
 * Строка-параметр в Настройках. Если в группе один параметр — обычная строка
 * с глазиком, по тапу скрывается/показывается. Если несколько — раскрывается
 * по тапу на тело строки, а глаз справа переключает группу целиком: если хотя бы
 * один датчик группы скрыт, тап делает все видимыми; если все видимы — все скрытыми.
 */
@Composable
private fun ParameterHideRow(
    group: List<com.shestikpetr.meteoapp.domain.model.ParameterMeta>,
    hidden: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val palette = MaterialTheme.appColors
    val first = group.first()
    val isGroup = group.size > 1
    val anyHidden = group.any { it.code in hidden }
    val allHidden = group.all { it.code in hidden }
    var expanded by androidx.compose.runtime.saveable.rememberSaveable(first.name) {
        androidx.compose.runtime.mutableStateOf(false)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isGroup) expanded = !expanded
                    else onToggle(first.code)
                }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = first.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (allHidden) palette.ink3 else palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isGroup) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${group.size}",
                            style = MeteoTextStyles.MonoSmall,
                            color = palette.ink4
                        )
                    } else {
                        first.unit?.takeIf { it.isNotBlank() }?.let { unit ->
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = unit,
                                style = MeteoTextStyles.MonoSmall,
                                color = palette.ink4,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (!isGroup) {
                    first.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink3,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            if (isGroup) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = palette.ink3,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        // Если хотя бы один скрыт — открываем все; иначе скрываем все.
                        val targetHide = !allHidden
                        group.forEach { p ->
                            val isHiddenNow = p.code in hidden
                            if (isHiddenNow != targetHide) onToggle(p.code)
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (allHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (allHidden) "Показать все" else "Скрыть все",
                        tint = if (anyHidden) palette.ink4 else palette.ink2,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = if (allHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (allHidden) "Скрыт" else "Виден",
                    tint = if (allHidden) palette.ink4 else palette.ink2,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (isGroup) {
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)) {
                    group.forEach { p ->
                        val ph = p.code in hidden
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(p.code) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = p.description?.takeIf { it.isNotBlank() } ?: "Датчик #${p.code}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (ph) palette.ink3 else palette.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    p.unit?.takeIf { it.isNotBlank() }?.let { unit ->
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = unit,
                                            style = MeteoTextStyles.MonoSmall,
                                            color = palette.ink4,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (ph) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (ph) "Скрыт" else "Виден",
                                tint = if (ph) palette.ink4 else palette.ink2,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    station: Station,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (hidden) palette.ink3 else palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = station.stationNumber + (station.location?.let { " · $it" } ?: ""),
                style = MeteoTextStyles.MonoSmall,
                color = palette.ink4,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggleHidden, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (hidden) "Показать" else "Скрыть",
                tint = palette.ink3,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Переименовать",
                tint = palette.ink2,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = palette.danger,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

