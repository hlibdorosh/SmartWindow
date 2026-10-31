package com.example.smartwindowsill

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class PlantsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as App).database.plantDao()
    private val photosDir = File(application.filesDir, "plants").apply { mkdirs() }

    val plants: StateFlow<List<Plant>> = dao.getAllPlants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Фото, выбранное в диалоге, пока растение не добавлено
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
                val info = PlantCareClient.fetchCareInfo(name.trim(), jpeg)

                dao.insertPlant(
                    Plant(
                        name = name.trim(),
                        nickname = nickname.trim(),
                        scientificName = info.scientificName,
                        imageUri = photo,
                        minTemp = info.minTemp, optTemp = info.optTemp, maxTemp = info.maxTemp,
                        minHumidity = info.minHumidity, optHumidity = info.optHumidity, maxHumidity = info.maxHumidity,
                        minLight = info.minLight, optLight = info.optLight, maxLight = info.maxLight,
                        minSoil = info.minSoil, optSoil = info.optSoil, maxSoil = info.maxSoil
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

    fun clearError() {
        _error.value = null
    }

    // Дневные агрегаты условий начиная с момента since (мс), по возрастанию даты
    fun dailyHistory(since: Long) = (getApplication<Application>() as App).database
        .dailyConditionDao().getSince(since)

    // Уменьшаем фото и сохраняем как JPEG в хранилище приложения (URI из пикера временные)
    private fun savePhoto(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_PX) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val target = File(photosDir, "plant_${System.currentTimeMillis()}.jpg")
        target.outputStream().use { decoded.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        decoded.recycle()
        return target.absolutePath
    }

    private companion object {
        const val MAX_PHOTO_PX = 1024
    }
}
