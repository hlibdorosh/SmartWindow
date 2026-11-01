package com.example.smartwindowsill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                onClick = { showAddDialog = true }
            ) {
                Text("+")
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
    onSelectMetric: (SensorChartMetric) -> Unit,
    raw: String = ""
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Smart Windowsill",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Значения сенсоров идут друг за другом.
            // Клик по любому сенсору переключает график внизу на этот параметр!
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusSensorRow(
                    label = "Температура",
                    value = temp,
                    icon = "🌡️",
                    isSelected = selectedMetric == SensorChartMetric.TEMPERATURE,
                    onClick = { onSelectMetric(SensorChartMetric.TEMPERATURE) }
                )
                StatusSensorRow(
                    label = "Влажность воздуха",
                    value = humidity,
                    icon = "💧",
                    isSelected = selectedMetric == SensorChartMetric.HUMIDITY,
                    onClick = { onSelectMetric(SensorChartMetric.HUMIDITY) }
                )
                StatusSensorRow(
                    label = "Освещенность",
                    value = light,
                    icon = "☀️",
                    isSelected = selectedMetric == SensorChartMetric.LIGHT,
                    onClick = { onSelectMetric(SensorChartMetric.LIGHT) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Простой динамический график за последние 60 минут
            SensorHistoryChart(
                history = history60Min,
                metric = selectedMetric
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Поиск") },
                modifier = Modifier.fillMaxWidth()
            )

            // Отладочный вывод: последний полученный сырой буфер (короткая версия)
            if (raw.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "RAW: ${raw.replace("\n", " ").take(200)}", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatusSensorRow(
    label: String,
    value: String,
    icon: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Text("📈", fontSize = 14.sp)
                }
            }
        }
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
        contentPadding = PaddingValues(16.dp)
    ) {
        items(plants, key = { it.id }) { plant ->
            val wateringStatus = remember(plant, history) {
                WateringAdvisor.computeStatus(plant, history)
            }
            PlantCard(
                plant = plant,
                waterFraction = wateringStatus.waterFraction,
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
    onClick: () -> Unit,
    onWaterClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
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
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(text = "🌱", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.nickname,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                plant.scientificName?.takeIf { it.isNotBlank() }?.let { sci ->
                    Text(
                        text = sci,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Статус: Подключено",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Кнопка полива с круговым индикатором остатка влаги вокруг (убывает по часовой стрелке)
            val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
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
                            color = Color(0xFF4FC3F7),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onWaterClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("💧", fontSize = 18.sp)
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
