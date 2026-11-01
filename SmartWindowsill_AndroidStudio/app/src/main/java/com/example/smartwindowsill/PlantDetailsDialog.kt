package com.example.smartwindowsill

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.example.smartwindowsill.ui.theme.NumberSmallStyle
import com.example.smartwindowsill.ui.theme.appColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    var showEditNickname by remember { mutableStateOf(false) }

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

    if (showEditNickname) {
        var editNicknameText by remember(plant.nickname) { mutableStateOf(plant.nickname) }
        AlertDialog(
            onDismissRequest = { showEditNickname = false },
            title = { Text("Изменить никнейм") },
            text = {
                OutlinedTextField(
                    value = editNicknameText,
                    onValueChange = { editNicknameText = InputSanitizer.filterInput(it, 60) },
                    label = { Text("Никнейм") },
                    supportingText = { Text("${editNicknameText.length}/60") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editNicknameText.isNotBlank(),
                    onClick = {
                        viewModel.updateNickname(plant, editNicknameText)
                        showEditNickname = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNickname = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить растение?") },
            text = {
                Text("Вы уверены, что хотите удалить «${plant.nickname}»? Это действие нельзя отменить.")
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
                    Text("Удалить")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.nickname, style = MaterialTheme.typography.titleLarge)
                    plant.scientificName?.takeIf { it.isNotBlank() }?.let { sci ->
                        Text(
                            sci,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { showEditNickname = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Изменить никнейм", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(plant.imageUri) {
                        decodeSampledBitmap(plant.imageUri)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = plant.nickname,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Outlined.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
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

                // Карточка "Режим полива" с индикатором высыхания влаги
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (wateringStatus.isUrgent)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Режим полива",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = wateringStatus.nextWateringText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (wateringStatus.isUrgent)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }

                        // Линия остатка воды: заполнена слева направо, проценты справа
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                if (wateringStatus.waterFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = wateringStatus.waterFraction)
                                            .align(Alignment.CenterStart)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.appColors.water)
                                    )
                                }
                            }
                            Text(
                                text = "${wateringStatus.waterPercent}%",
                                style = NumberSmallStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Частота", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("раз в ${wateringStatus.intervalDays} дн.", style = MaterialTheme.typography.labelLarge)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Последний полив", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(wateringStatus.lastWateredText, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.markWatered(plant) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
Spacer(modifier = Modifier.width(8.dp))
Text("Отметить полив")
                        }
                    }
                }

                plant.scientificName?.takeIf { it.isNotBlank() }?.let { sci ->
                    InfoRow("Вид растения", sci)
                }

                InfoRow("Добавлено", formatDate(plant.addedAt))

                InfoRow("Температура", "${plant.minTemp.fmt()}–${plant.maxTemp.fmt()} °C (идеал ${plant.optTemp.fmt()})")
                InfoRow("Влажность", "${plant.minHumidity.fmt()}–${plant.maxHumidity.fmt()} % (идеал ${plant.optHumidity.fmt()})")
                InfoRow("Свет", "${plant.minLight.fmt()}–${plant.maxLight.fmt()} lx (идеал ${plant.optLight.fmt()})")

                OutlinedButton(
                    onClick = { showComfort = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
Spacer(modifier = Modifier.width(8.dp))
Text("История комфорта")
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
Spacer(modifier = Modifier.width(8.dp))
Text("Удалить растение")
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
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun Float.fmt(): String = if (this % 1f == 0f) toInt().toString() else "%.1f".format(this)

// addedAt == 0 — запись из старой версии БД без даты
private fun formatDate(millis: Long): String =
    if (millis <= 0L) "неизвестно"
    else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
