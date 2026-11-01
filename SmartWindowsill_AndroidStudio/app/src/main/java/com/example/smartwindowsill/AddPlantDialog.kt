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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AddPlantDialog(viewModel: PlantsViewModel, onDismiss: () -> Unit) {
    val photoPath by viewModel.photoPath.collectAsState()
    val isAdding by viewModel.isAdding.collectAsState()
    val error by viewModel.error.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    val photoSource = rememberPhotoSource { viewModel.onPhotoPicked(it) }
    val onCameraClick = photoSource.takePhoto

    val dismiss = {
        viewModel.cancelDraft()
        onDismiss()
    }

    val onAttemptLeave = {
        if (photoPath != null || name.isNotBlank() || nickname.isNotBlank()) {
            showLeaveConfirm = true
        } else {
            dismiss()
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Выйти без сохранения?") },
            text = { Text("Вы уверены, что хотите выйти? Введенные данные будут потеряны.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirm = false
                        dismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("Остаться")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isAdding) onAttemptLeave() },
        title = { Text("Новое растение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(photoPath) {
                        decodeSampledBitmap(photoPath)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Фото растения",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("Добавьте фото растения")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCameraClick,
                        enabled = !isAdding,
                        modifier = Modifier.weight(1f)
                    ) { Text("Камера") }
                    OutlinedButton(
                        onClick = photoSource.pickFromGallery,
                        enabled = !isAdding,
                        modifier = Modifier.weight(1f)
                    ) { Text("Галерея") }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace("\n", "").replace("\r", "").replace("\t", "") },
                    label = { Text("Название растения (для Claude)") },
                    supportingText = { Text("Используется только для подбора условий ухода") },
                    singleLine = true,
                    enabled = !isAdding,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it.replace("\n", "").replace("\r", "").replace("\t", "") },
                    label = { Text("Никнейм (необязательно)") },
                    supportingText = { Text("Если пусто, будет использовано название") },
                    singleLine = true,
                    enabled = !isAdding,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                if (isAdding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("Claude подбирает условия...")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isAdding && photoPath != null && name.isNotBlank(),
                onClick = { viewModel.addPlant(name, nickname, onAdded = onDismiss) }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(enabled = !isAdding, onClick = onAttemptLeave) {
                Text("Отмена")
            }
        }
    )
}
