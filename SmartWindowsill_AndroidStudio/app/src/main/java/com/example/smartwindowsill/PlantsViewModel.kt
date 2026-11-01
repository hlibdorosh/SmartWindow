package com.example.smartwindowsill

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PlantsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = (app as App).database.plantDao()

    val plants: StateFlow<List<Plant>> = dao.getAllPlants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Путь к сохраненному в приложении временному файлу фото нового растения (или null)
    private val _photoPath = MutableStateFlow<String?>(null)
    val photoPath: StateFlow<String?> = _photoPath

    private val _isAdding = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = _isAdding

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val newPath = withContext(Dispatchers.IO) { savePhoto(uri) }
            if (newPath == null) {
                _error.value = "Не удалось открыть фото"
                return@launch
            }
            _photoPath.value?.let { File(it).delete() }
            _photoPath.value = newPath
            _error.value = null
        }
    }

    // Диалог закрыт без добавления: удаляем временное фото
    fun cancelDraft() {
        _photoPath.value?.let { File(it).delete() }
        _photoPath.value = null
        _error.value = null
    }

    fun addPlant(name: String, nickname: String, onAdded: () -> Unit) {
        val photo = _photoPath.value ?: return
        viewModelScope.launch {
            _isAdding.value = true
            _error.value = null
            try {
                val jpeg = withContext(Dispatchers.IO) { File(photo).readBytes() }
                // Название растения используется только чтобы Claude понял, что это за растение
                val info = PlantCareClient.fetchCareInfo(name.trim(), jpeg)

                // Если никнейм не задан, сохраняем введенное название как никнейм
                val effectiveNickname = nickname.trim().ifBlank { name.trim() }

                dao.insertPlant(
                    Plant(
                        nickname = effectiveNickname,
                        scientificName = info.scientificName,
                        imageUri = photo,
                        minTemp = info.minTemp, optTemp = info.optTemp, maxTemp = info.maxTemp,
                        minHumidity = info.minHumidity, optHumidity = info.optHumidity, maxHumidity = info.maxHumidity,
                        minLight = info.minLight, optLight = info.optLight, maxLight = info.maxLight,
                        wateringIntervalDays = info.wateringIntervalDays
                    )
                )
                _photoPath.value = null // фото теперь принадлежит растению, не удаляем
                onAdded()
            } catch (e: Exception) {
                _error.value = e.message ?: "Не удалось получить данные от Claude"
            } finally {
                _isAdding.value = false
            }
        }
    }

    fun updateNickname(plant: Plant, newNickname: String) {
        val trimmed = newNickname.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            dao.updatePlant(plant.copy(nickname = trimmed))
        }
    }

    // Заменяем фото существующего растения; старый файл удаляем
    fun changePhoto(plant: Plant, uri: Uri) {
        viewModelScope.launch {
            val newPath = withContext(Dispatchers.IO) { savePhoto(uri) }
            if (newPath == null) {
                _error.value = "Не удалось открыть фото"
                return@launch
            }
            dao.updatePlant(plant.copy(imageUri = newPath))
            plant.imageUri?.let { old -> withContext(Dispatchers.IO) { File(old).delete() } }
            _error.value = null
        }
    }

    fun markWatered(plant: Plant) {
        viewModelScope.launch {
            dao.updatePlant(plant.copy(lastWatered = System.currentTimeMillis()))
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            dao.deletePlant(plant)
            plant.imageUri?.let { path ->
                withContext(Dispatchers.IO) {
                    try {
                        File(path).delete()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Дневные агрегаты условий начиная с момента since (мс), по возрастанию даты
    fun dailyHistory(since: Long) = (getApplication<Application>() as App).database
        .dailyConditionDao().getSince(since)

    // Уменьшаем фото, корректируем EXIF-ориентацию и сохраняем как JPEG в хранилище приложения
    private fun savePhoto(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver

        val orientation = try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                ExifInterface(pfd.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_PX) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postRotate(270f)
                    postScale(-1f, 1f)
                }
            }
        }

        val rotated = if (!matrix.isIdentity) {
            val res = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (res !== decoded) decoded.recycle()
            res
        } else {
            decoded
        }

        val scaled = if (maxOf(rotated.width, rotated.height) > MAX_PHOTO_PX) {
            val ratio = MAX_PHOTO_PX.toFloat() / maxOf(rotated.width, rotated.height)
            Bitmap.createScaledBitmap(
                rotated,
                (rotated.width * ratio).toInt(),
                (rotated.height * ratio).toInt(),
                true
            ).also { if (it !== rotated) rotated.recycle() }
        } else {
            rotated
        }

        val dir = File(getApplication<Application>().filesDir, "plants").apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.jpg")

        return try {
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            scaled.recycle()
            target.absolutePath
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    companion object {
        private const val MAX_PHOTO_PX = 1024
    }
}
