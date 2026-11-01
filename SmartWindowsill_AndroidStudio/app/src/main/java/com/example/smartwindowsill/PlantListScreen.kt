package com.example.smartwindowsill

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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

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

    var showAddDialog by remember { mutableStateOf(false) }
    // Храним id, а не объект, чтобы окно обновлялось после изменений в БД
    var selectedPlantId by remember { mutableStateOf<Int?>(null) }
    val selectedPlant = plants.firstOrNull { it.id == selectedPlantId }

    var plantToDelete by remember { mutableStateOf<Plant?>(null) }

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
                onPlantClick = { selectedPlantId = it.id },
                onWaterClick = { plantsViewModel.markWatered(it) },
                onDeleteClick = { plantToDelete = it }
            )
        }
    } // Конец Scaffold

    if (plantToDelete != null) {
        val target = plantToDelete!!
        AlertDialog(
            onDismissRequest = { plantToDelete = null },
            title = { Text("Удалить растение?") },
            text = {
                Text("Вы уверены, что хотите удалить «${target.nickname.ifBlank { target.name }}»? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        plantsViewModel.deletePlant(target)
                        plantToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { plantToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

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
    onPlantClick: (Plant) -> Unit,
    onWaterClick: (Plant) -> Unit,
    onDeleteClick: (Plant) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(plants, key = { it.id }) { plant ->
            PlantCard(
                plant = plant,
                onClick = { onPlantClick(plant) },
                onWaterClick = { onWaterClick(plant) },
                onDeleteClick = { onDeleteClick(plant) }
            )
        }
    }
}

@Composable
fun PlantCard(
    plant: Plant,
    onClick: () -> Unit,
    onWaterClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                    contentDescription = plant.name,
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
                    text = plant.nickname.ifBlank { plant.name },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (plant.nickname.isNotBlank()) {
                    Text(
                        text = plant.name,
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

            // Кнопка полива прямо на карточке растения справа
            FilledTonalIconButton(
                onClick = onWaterClick,
                modifier = Modifier.size(42.dp)
            ) {
                Text("💧", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Кнопка удаления растения с запросом подтверждения
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(38.dp)
            ) {
                Text("🗑️", fontSize = 18.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlantListScreenPreview() {
    PlantListScreen()
}
