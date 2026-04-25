package com.shestikpetr.meteoapp.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.UserStationResponse
import com.shestikpetr.meteoapp.data.repository.AuthRepository
import com.shestikpetr.meteoapp.data.repository.StationDataAggregator
import com.shestikpetr.meteoapp.data.repository.StationRepository
import com.shestikpetr.meteoapp.ui.theme.SkyBlue40
import com.shestikpetr.meteoapp.ui.theme.SkyBlue80
import com.shestikpetr.meteoapp.util.SettingsManager
import com.shestikpetr.meteoapp.util.ThemeMode
import com.shestikpetr.meteoapp.util.TokenStore
import com.shestikpetr.meteoapp.util.UserSessionStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val userSessionStore = remember { UserSessionStore(context) }
    val authRepository = remember { AuthRepository(tokenStore, userSessionStore) }
    val stationRepository = remember { StationRepository(tokenStore) }
    val stationDataAggregator = remember { StationDataAggregator(stationRepository) }
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    val themeMode by settingsManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val tooltipsEnabled by settingsManager.tooltipsEnabled.collectAsState(initial = true)
    val hiddenStations by settingsManager.hiddenStations.collectAsState(initial = emptySet())
    val hiddenParameters by settingsManager.hiddenParameters.collectAsState(initial = emptySet())
    val username by userSessionStore.username.collectAsState(initial = null)

    var stations by remember { mutableStateOf<List<UserStationResponse>>(emptyList()) }
    var allParameters by remember { mutableStateOf<List<ParameterMetadata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog states
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showDeleteStationDialog by remember { mutableStateOf<UserStationResponse?>(null) }
    var showRenameStationDialog by remember { mutableStateOf<UserStationResponse?>(null) }

    fun reloadStations() {
        scope.launch {
            isLoading = true
            stationRepository.getUserStations().getOrNull()?.let { loaded ->
                stations = loaded
                stationDataAggregator.getAllParameters(loaded).getOrNull()?.let { params ->
                    allParameters = params
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reloadStations()
    }

    // Add station dialog
    if (showAddStationDialog) {
        AddStationDialog(
            onDismiss = { showAddStationDialog = false },
            onConfirm = { stationNumber ->
                scope.launch {
                    val result = stationRepository.addStation(stationNumber)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Станция добавлена", Toast.LENGTH_SHORT).show()
                        reloadStations()
                    } else {
                        Toast.makeText(context, "Ошибка: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                showAddStationDialog = false
            }
        )
    }

    // Delete station dialog
    showDeleteStationDialog?.let { station ->
        DeleteStationDialog(
            stationName = station.customName ?: station.station?.name ?: "Станция",
            onDismiss = { showDeleteStationDialog = null },
            onConfirm = {
                scope.launch {
                    val result = stationRepository.deleteStation(station.id)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Станция удалена", Toast.LENGTH_SHORT).show()
                        reloadStations()
                    } else {
                        Toast.makeText(context, "Ошибка: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                showDeleteStationDialog = null
            }
        )
    }

    // Rename station dialog
    showRenameStationDialog?.let { station ->
        RenameStationDialog(
            currentName = station.customName ?: station.station?.name ?: "",
            onDismiss = { showRenameStationDialog = null },
            onConfirm = { newName ->
                scope.launch {
                    val result = stationRepository.renameStation(station.id, newName)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Станция переименована", Toast.LENGTH_SHORT).show()
                        reloadStations()
                    } else {
                        Toast.makeText(context, "Ошибка: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                showRenameStationDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Настройки",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBlue40
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile section
            SectionCard(title = "Профиль", icon = Icons.Default.Person) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = SkyBlue40
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = username ?: "Пользователь",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${stations.size} станций подключено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Appearance section
            SectionCard(title = "Оформление", icon = Icons.Default.DarkMode) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settingsManager.setThemeMode(mode) }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == mode,
                            onClick = {
                                scope.launch { settingsManager.setThemeMode(mode) }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = SkyBlue40)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = null,
                            tint = if (themeMode == mode) SkyBlue40 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Tooltips toggle
            SectionCard(title = "Подсказки", icon = Icons.AutoMirrored.Filled.HelpOutline) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Показывать подсказки",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = tooltipsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsManager.setTooltipsEnabled(enabled) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = SkyBlue40)
                    )
                }
            }

            // Stations management section
            SectionCard(title = "Мои станции", icon = Icons.Default.LocationOn) {
                // Add station button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddStationDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = SkyBlue40.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SkyBlue40,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Добавить станцию",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = SkyBlue40
                        )
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SkyBlue40,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    stations.forEachIndexed { index, station ->
                        Spacer(modifier = Modifier.height(if (index == 0) 12.dp else 0.dp))
                        val stationNumber = station.station?.stationNumber ?: ""
                        val isHidden = stationNumber in hiddenStations

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = if (isHidden) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                else SkyBlue80.copy(alpha = 0.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                                    else SkyBlue40,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.customName ?: station.station?.name ?: "Станция",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "№ $stationNumber",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Hide/show button
                            IconButton(
                                onClick = {
                                    scope.launch { settingsManager.toggleStationHidden(stationNumber) }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isHidden) "Показать" else "Скрыть",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Rename button
                            IconButton(
                                onClick = { showRenameStationDialog = station },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Переименовать",
                                    tint = SkyBlue40,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Delete button
                            IconButton(
                                onClick = { showDeleteStationDialog = station },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < stations.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            // Hidden parameters section
            if (allParameters.isNotEmpty()) {
                SectionCard(title = "Скрытие параметров", icon = Icons.Default.Tune) {
                    allParameters.forEach { param ->
                        val isHidden = param.code in hiddenParameters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { settingsManager.toggleParameterHidden(param.code) }
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = param.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                param.unit?.let { unit ->
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "($unit)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isHidden) "Скрыт" else "Виден",
                                tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else SkyBlue40,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Logout button
            Button(
                onClick = {
                    scope.launch {
                        authRepository.logout()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выйти из аккаунта",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // App info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MeteoApp v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SkyBlue40,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
