package com.example.smartwindowsill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartwindowsill.ui.theme.NumberStyle
import com.example.smartwindowsill.ui.theme.appColors

@Composable
fun PlantListScreen(
    mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    plantsViewModel: PlantsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {

    val temp by mainViewModel.temp.collectAsState()
    val humidity by mainViewModel.humidity.collectAsState()
    val light by mainViewModel.light.collectAsState()
    val history60Min by mainViewModel.history60Min.collectAsState()

    var selectedMetric by remember { mutableStateOf(SensorChartMetric.TEMPERATURE) }

    val plants by plantsViewModel.plants.collectAsState()

    // История для расчета графика и остатка влаги
    val recentHistory by remember {
        plantsViewModel.dailyHistory(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
    }.collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    // Храним id, а не объект, чтобы окно обновлялось после изменений в БД
    var selectedPlantId by remember { mutableStateOf<Int?>(null) }
    val selectedPlant = plants.firstOrNull { it.id == selectedPlantId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Добавить растение", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Header(
                temp = temp,
                humidity = humidity,
                light = light,
                history60Min = history60Min,
                selectedMetric = selectedMetric,
                onSelectMetric = { selectedMetric = it }
            )
            PlantList(
                plants = plants,
                history = recentHistory,
                onPlantClick = { selectedPlantId = it.id },
                onWaterClick = { plantsViewModel.markWatered(it) }
            )
        }
    } // Конец Scaffold

    if (selectedPlant != null) {
        PlantDetailsDialog(
            plant = selectedPlant,
            viewModel = plantsViewModel,
            onDismiss = { selectedPlantId = null }
        )
    }

    if (showAddDialog) {
        AddPlantDialog(
            viewModel = plantsViewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}


@Composable
fun Header(
    temp: String,
    humidity: String,
    light: String,
    history60Min: List<SensorHistory>,
    selectedMetric: SensorChartMetric,
    onSelectMetric: (SensorChartMetric) -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Smart Windowsill",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Значения сенсоров одной группой.
                // Клик по любому сенсору переключает график внизу на этот параметр
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        StatusSensorRow(
                            label = "Температура",
                            value = temp,
                            icon = SensorChartMetric.TEMPERATURE.icon,
                            isSelected = selectedMetric == SensorChartMetric.TEMPERATURE,
                            onClick = { onSelectMetric(SensorChartMetric.TEMPERATURE) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StatusSensorRow(
                            label = "Влажность воздуха",
                            value = humidity,
                            icon = SensorChartMetric.HUMIDITY.icon,
                            isSelected = selectedMetric == SensorChartMetric.HUMIDITY,
                            onClick = { onSelectMetric(SensorChartMetric.HUMIDITY) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StatusSensorRow(
                            label = "Освещенность",
                            value = light,
                            icon = SensorChartMetric.LIGHT.icon,
                            isSelected = selectedMetric == SensorChartMetric.LIGHT,
                            onClick = { onSelectMetric(SensorChartMetric.LIGHT) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Динамический график за последние 60 минут
                SensorHistoryChart(
                    history = history60Min,
                    metric = selectedMetric
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun StatusSensorRow(
    label: String,
    value: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = NumberStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PlantList(
    plants: List<Plant>,
    history: List<DailyCondition> = emptyList(),
    onPlantClick: (Plant) -> Unit,
    onWaterClick: (Plant) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
    ) {
        items(plants, key = { it.id }) { plant ->
            val wateringStatus = remember(plant, history) {
                WateringAdvisor.computeStatus(plant, history)
            }
            PlantCard(
                plant = plant,
                waterFraction = wateringStatus.waterFraction,
                wateringText = wateringStatus.nextWateringText,
                isUrgent = wateringStatus.isUrgent,
                onClick = { onPlantClick(plant) },
                onWaterClick = { onWaterClick(plant) }
            )
        }
    }
}

@Composable
fun PlantCard(
    plant: Plant,
    waterFraction: Float = 0f,
    wateringText: String = "",
    isUrgent: Boolean = false,
    onClick: () -> Unit,
    onWaterClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val photo = remember(plant.imageUri) {
                decodeSampledBitmap(plant.imageUri)?.asImageBitmap()
            }
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = plant.nickname,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                plant.scientificName?.takeIf { it.isNotBlank() }?.let { sci ->
                    Text(
                        text = sci,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (wateringText.isNotBlank()) {
                    Text(
                        text = "Полив: $wateringText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка полива с круговым индикатором остатка влаги вокруг (убывает по часовой стрелке)
            val trackColor = MaterialTheme.colorScheme.outlineVariant
            val waterColor = MaterialTheme.appColors.water
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(halfStroke, halfStroke)

                    // Фоновый трек круга
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Индикатор остатка воды: высыхает/убывает по часовой стрелке от 12 часов
                    if (waterFraction > 0f) {
                        val startAngle = -90f + (1f - waterFraction) * 360f
                        val sweepAngle = (waterFraction * 360f).coerceIn(0.1f, 360f)
                        drawArc(
                            color = waterColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                IconButton(
                    onClick = onWaterClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = "Отметить полив",
                        tint = waterColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlantListScreenPreview() {
    PlantListScreen()
}
