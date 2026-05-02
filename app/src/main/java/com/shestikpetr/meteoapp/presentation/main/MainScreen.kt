package com.shestikpetr.meteoapp.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteoapp.presentation.main.components.DetailPanel
import com.shestikpetr.meteoapp.presentation.main.components.OsmMapView
import com.shestikpetr.meteoapp.presentation.main.components.StationListPanel
import com.shestikpetr.meteoapp.ui.components.BrandMark
import com.shestikpetr.meteoapp.ui.components.SegmentedTabsEqual
import com.shestikpetr.meteoapp.ui.theme.appColors
import com.shestikpetr.meteoapp.util.LocationProvider
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val palette = MaterialTheme.appColors
    val context = LocalContext.current
    val locationProvider = remember { LocationProvider(context) }
    val scope = rememberCoroutineScope()

    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var initialCameraSet by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) scope.launch {
            locationProvider.getLastKnownLocation()?.let { userLocation = it }
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            locationProvider.getLastKnownLocation()?.let { userLocation = it }
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    // Верхний отступ ручки шторки должен «включаться» только когда верх
    // шторки реально заезжает под статус-бар — иначе в свёрнутом состоянии
    // над ручкой появляется бесполезная пустота высотой в системный бар.
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val sheetTopInsetDp by remember(statusBarPx) {
        derivedStateOf {
            val offset = runCatching { bottomSheetState.requireOffset() }
                .getOrDefault(Float.MAX_VALUE)
            val pad = (statusBarPx - offset).coerceAtLeast(10f)
            with(density) { pad.toDp() }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 200.dp,
        sheetContainerColor = palette.bgElev,
        sheetContentColor = palette.ink,
        sheetShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 0.dp,
        sheetDragHandle = {
            // Верхний отступ появляется только тогда, когда верх шторки
            // заехал под статус-бар — в свёрнутом состоянии он равен 0.
            // Фон шторки при полностью раскрытой продолжает доходить до
            // самого верха экрана.
            Box(
                modifier = Modifier
                    .padding(top = sheetTopInsetDp)
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(36.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.line2)
            )
        },
        sheetContent = {
            BottomSheet(
                state = state,
                onParameterSelected = viewModel::selectParameter,
                onStationSelected = { st ->
                    viewModel.selectStation(st.stationNumber)
                    scope.launch { bottomSheetState.partialExpand() }
                }
            )
        },
        topBar = {
            MainTopBar(
                onSettingsClick = onNavigateToSettings,
                onStatsClick = onNavigateToStatistics,
                isRefreshing = state.isRefreshing,
                onRefreshClick = viewModel::refresh
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(palette.bg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.ink, strokeWidth = 2.dp)
                }
            } else {
                OsmMapView(
                    stations = state.stations,
                    latestByStation = state.latestByStation,
                    selectedParameter = state.selectedParameter,
                    activeStationNumber = state.selectedStationNumber,
                    onStationClick = { station ->
                        viewModel.selectStation(station.stationNumber)
                        scope.launch { bottomSheetState.partialExpand() }
                    },
                    initialCameraSet = initialCameraSet,
                    onInitialCameraSet = { initialCameraSet = true },
                    userLocation = userLocation,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating detail panel поверх карты
            val activeLatest by remember(state.selectedStationNumber, state.latestByStation) {
                derivedStateOf {
                    state.selectedStationNumber?.let { state.latestByStation[it] }
                }
            }
            AnimatedVisibility(
                visible = activeLatest != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                // Карточка прижимается под топбар (paddingValues уже учитывает
                // высоту топбара + статус-бара), без повторного statusBarsPadding.
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .widthIn(max = 420.dp)
            ) {
                activeLatest?.let { latest ->
                    DetailPanel(
                        latest = latest,
                        hiddenParameterCodes = state.settings.hiddenParameters,
                        onClose = { viewModel.selectStation(null) },
                        onOpenStatistics = onNavigateToStatistics,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MainTopBar(
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit
) {
    val palette = MaterialTheme.appColors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bgElev)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrandMark(size = 22.dp, bg = palette.ink, glyph = palette.bgElev)
                Text(
                    text = "Meteo·App",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.ink
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRefreshClick) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 1.5.dp,
                        color = palette.ink2
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = palette.ink2
                    )
                }
            }
            IconButton(onClick = onStatsClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = "Статистика",
                    tint = palette.ink2
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = palette.ink2
                )
            }
        }
        HorizontalDivider(color = palette.line)
    }
}

@Composable
private fun BottomSheet(
    state: VisibleMainUiState,
    onParameterSelected: (com.shestikpetr.meteoapp.domain.model.ParameterMeta?) -> Unit,
    onStationSelected: (com.shestikpetr.meteoapp.domain.model.Station) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Учитываем навигационную панель Android внизу, чтобы последний
            // параметр или станция не оказывались под ней.
            .navigationBarsPadding()
    ) {
        Surface(color = MaterialTheme.appColors.bgElev) {
            Column {
                SegmentedTabsEqual(
                    options = listOf("Параметры", "Станции"),
                    selectedIndex = tab,
                    onSelected = { tab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                when (tab) {
                    0 -> ParametersTab(state, onParameterSelected)
                    1 -> StationListPanel(
                        stations = state.stations,
                        latestByStation = state.latestByStation,
                        activeStationNumber = state.selectedStationNumber,
                        onStationClick = onStationSelected
                    )
                }
                // Запас снизу, чтобы последняя плитка/строка не упиралась в
                // нижний край шторки.
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ParametersTab(
    state: VisibleMainUiState,
    onParameterSelected: (com.shestikpetr.meteoapp.domain.model.ParameterMeta?) -> Unit
) {
    val palette = MaterialTheme.appColors
    val scroll = rememberScrollState()

    // Параметры с одинаковым name группируем — на станции может быть несколько
    // датчиков «Температура» (воздух, грунт, вода). Внутри группы сохраняем
    // исходный порядок, между группами — порядок появления первого элемента.
    val groups: List<List<com.shestikpetr.meteoapp.domain.model.ParameterMeta>> = remember(state.allParameters) {
        val byName = LinkedHashMap<String, MutableList<com.shestikpetr.meteoapp.domain.model.ParameterMeta>>()
        state.allParameters.forEach { p ->
            byName.getOrPut(p.name) { mutableListOf() }.add(p)
        }
        byName.values.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Bottom sheet с verticalScroll позволяет скроллить контент,
            // когда параметров больше, чем умещается в полностью раскрытой шторке.
            .verticalScroll(scroll)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = state.selectedParameter?.let { "Выбран: ${it.name}${it.unit?.let { u -> " · $u" } ?: ""}" }
                ?: "Выберите параметр для отображения на карте",
            style = MaterialTheme.typography.bodySmall,
            color = if (state.selectedParameter != null) palette.ink else palette.ink3
        )
        Spacer(Modifier.size(8.dp))

        if (state.allParameters.isEmpty()) {
            Text(
                text = "Нет параметров",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink3,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            return@Column
        }

        groups.forEach { group ->
            ParameterGroupItem(
                group = group,
                selected = state.selectedParameter,
                onSelect = onParameterSelected
            )
            Spacer(Modifier.size(6.dp))
        }
    }
}

/**
 * Один элемент списка параметров. Если в группе один параметр — это обычная
 * плитка-фильтр. Если несколько (например, разные датчики температуры) — это
 * раскрывающийся список с теми же сабтайтлами.
 */
@Composable
private fun ParameterGroupItem(
    group: List<com.shestikpetr.meteoapp.domain.model.ParameterMeta>,
    selected: com.shestikpetr.meteoapp.domain.model.ParameterMeta?,
    onSelect: (com.shestikpetr.meteoapp.domain.model.ParameterMeta?) -> Unit
) {
    val palette = MaterialTheme.appColors
    val isGroup = group.size > 1
    val anySelectedInGroup = group.any { it.code == selected?.code }
    val first = group.first()
    var expanded by rememberSaveable(first.name) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                if (isGroup) {
                    expanded = !expanded
                } else {
                    val same = selected?.code == first.code
                    onSelect(if (same) null else first)
                }
            },
        color = if (anySelectedInGroup) palette.bgSunken else palette.bgElev,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (anySelectedInGroup) palette.line2 else palette.line
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = first.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(6.dp))
                if (isGroup) {
                    // Бейдж с количеством датчиков и стрелка раскрытия
                    Text(
                        text = "${group.size}",
                        style = com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles.MonoSmall,
                        color = palette.ink4
                    )
                    Spacer(Modifier.size(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть",
                        tint = palette.ink3,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    first.unit?.takeIf { it.isNotBlank() }?.let { unit ->
                        Text(
                            text = unit,
                            style = com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles.MonoSmall,
                            color = palette.ink4,
                            maxLines = 1
                        )
                    }
                }
            }
            // У одиночного параметра description показываем сразу, у группы —
            // только в раскрывающихся пунктах (там у каждого датчика своё).
            if (!isGroup) {
                first.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink3,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }

    if (isGroup) {
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
                group.forEach { p ->
                    val isSelected = selected?.code == p.code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val same = selected?.code == p.code
                                onSelect(if (same) null else p)
                            },
                        color = if (isSelected) palette.bgSunken else palette.bgElev,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) palette.line2 else palette.line
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Имя «дочерки»: используем description как уточнение
                                // (например, «Воздух», «Грунт»). Если description нет —
                                // показываем код параметра как подпись.
                                Text(
                                    text = p.description?.takeIf { it.isNotBlank() } ?: "Датчик #${p.code}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                p.unit?.takeIf { it.isNotBlank() }?.let { unit ->
                                    Spacer(Modifier.size(6.dp))
                                    Text(
                                        text = unit,
                                        style = com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles.MonoSmall,
                                        color = palette.ink4,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

