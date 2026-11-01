package com.example.smartwindowsill

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

@Composable
fun PlantDetailsDialog(plant: Plant, viewModel: PlantsViewModel, onDismiss: () -> Unit) {
    val error by viewModel.error.collectAsState()
    val photoSource = rememberPhotoSource { viewModel.changePhoto(plant, it) }
    var showComfort by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Данные микроклимата за последние 7 дней для поправки графика полива
    val recentHistory by remember(plant) {
        viewModel.dailyHistory(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
    }.collectAsState(initial = emptyList())

    val wateringStatus = remember(plant, recentHistory) {
        WateringAdvisor.computeStatus(plant, recentHistory)
    }

    if (showComfort) {
        PlantComfortDialog(plant = plant, viewModel = viewModel, onDismiss = { showComfort = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить растение?") },
            text = {
                Text("Вы уверены, что хотите удалить «${plant.nickname.ifBlank { plant.name }}»? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deletePlant(plant)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    val dismiss = {
        viewModel.clearError()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Column {
                Text(plant.nickname.ifBlank { plant.name }, fontWeight = FontWeight.Bold)
                if (plant.nickname.isNotBlank()) {
                    Text(
                        plant.name,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                plant.scientificName?.takeIf { it.isNotBlank() }?.let { sci ->
                    Text(
                        sci,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(plant.imageUri) {
                        decodeSampledBitmap(plant.imageUri)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = plant.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("🌱", fontSize = 64.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = photoSource.takePhoto, modifier = Modifier.weight(1f)) {
                        Text("Камера")
                    }
                    OutlinedButton(onClick = photoSource.pickFromGallery, modifier = Modifier.weight(1f)) {
                        Text("Галерея")
                    }
                }

                plant.scientificName?.takeIf { it.isNotBlank() }?.let { sci ->
                    InfoRow("Научное название", sci)
                }

                InfoRow("Добавлено", formatDate(plant.addedAt))

                // Полив растения (упрощенный блок вместо вложенной карточки)
                InfoRow("Частота полива", "раз в ${wateringStatus.intervalDays} дн.")
                InfoRow("Последний полив", wateringStatus.lastWateredText)
                InfoRow("Следующий полив", wateringStatus.nextWateringText)

                OutlinedButton(
                    onClick = { viewModel.markWatered(plant) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💧 Отметить полив")
                }

                InfoRow("Температура", "${plant.minTemp.fmt()}–${plant.maxTemp.fmt()} °C (идеал ${plant.optTemp.fmt()})")
                InfoRow("Влажность", "${plant.minHumidity.fmt()}–${plant.maxHumidity.fmt()} % (идеал ${plant.optHumidity.fmt()})")
                InfoRow("Свет", "${plant.minLight.fmt()}–${plant.maxLight.fmt()} lx (идеал ${plant.optLight.fmt()})")

                OutlinedButton(
                    onClick = { showComfort = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📈 История комфорта")
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("🗑️ Удалить растение")
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = dismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp)
    }
}

private fun Float.fmt(): String = if (this % 1f == 0f) toInt().toString() else "%.1f".format(this)

// addedAt == 0 — запись из старой версии БД без даты
private fun formatDate(millis: Long): String =
    if (millis <= 0L) "неизвестно"
    else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
