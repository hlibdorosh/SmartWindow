package com.example.smartwindowsill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Инициализируем наш ультра-менеджер
    private val bluetoothManager = BluetoothManager(application.applicationContext)

    // Переменные для экрана (UI будет наблюдать за ними)
    private val _temp = MutableStateFlow("--°C")
    val temp: StateFlow<String> = _temp

    private val _humidity = MutableStateFlow("--%")
    val humidity: StateFlow<String> = _humidity

    private val _light = MutableStateFlow("--")
    val light: StateFlow<String> = _light

    // Отладочное поле: последний полученный сырый буфер/сообщение
    private val _raw = MutableStateFlow("")
    val raw: StateFlow<String> = _raw

    private val conditionRepository = ConditionRepository((application as App).database)
    private var lastRecordedAt = 0L
    private val RECORD_INTERVAL_MS = 60_000L

    init {
        // Начинаем слушать входящий сырой текст в фоновом режиме
        viewModelScope.launch {
            bluetoothManager.rawSensorData.collect { rawText ->
                // Сохраняем для отладки и парсим
                _raw.value = rawText
                parseData(rawText)
            }
        }
    }

    // Вызывается извне (например из Activity) после получения runtime-разрешений
    fun startConnect() {
        bluetoothManager.connect()
    }

    // Сохраняем не чаще раза в минуту: датчик шлёт данные каждые 5 с, для дневных агрегатов этого достаточно
    private fun recordSample(temperature: Float, humidity: Float, lux: Float) {
        val now = System.currentTimeMillis()
        if (now - lastRecordedAt < RECORD_INTERVAL_MS) return
        lastRecordedAt = now
        viewModelScope.launch {
            try {
                conditionRepository.record(temperature, humidity, lux, now = now)
            } catch (_: Exception) {
                // Сбой записи истории не должен ломать показ текущих значений
            }
        }
    }

    private fun parseData(text: String) {
        try {
            val trimmed = text.trim()

            // 1) Поддержка простого CSV формата: "25.0,60.0,4095"
            if (!trimmed.contains("Device UUID:", ignoreCase = true) && trimmed.contains(",")) {
                val csvParts = trimmed.split(",").map { it.trim() }
                if (csvParts.size >= 3) {
                    val t = csvParts[0].toFloatOrNull()
                    val h = csvParts[1].toFloatOrNull()
                    val l = csvParts[2].toFloatOrNull()
                    t?.let { _temp.value = "${it}°C" }
                    h?.let { _humidity.value = "${it}%" }
                    l?.let {
                        val adc = it.coerceIn(1f, 4094f)
                        val rLdr = 10000f * (4095f - adc) / adc
                        val lux = 500000f / rLdr
                        
                        _light.value = "${lux.toInt()} lx"
                        if (t != null && h != null) recordSample(t, h, lux)
                    }
                    return
                }
            }

            // 2) Общий парсинг по меткам внутри произвольного текста (например, "Humidity: 55.30%\nTemperature: 28.30°C\nLight: 2969.00")
            val tempRegex = Regex("Temperature\\D*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
            val humRegex = Regex("Humidity\\D*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
            val lightRegex = Regex("Light\\D*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)

            tempRegex.find(trimmed)?.groups?.get(1)?.value?.let { number ->
                _temp.value = "$number°C"
            }

            humRegex.find(trimmed)?.groups?.get(1)?.value?.let { number ->
                _humidity.value = "$number%"
            }

            lightRegex.find(trimmed)?.groups?.get(1)?.value?.let { number ->
                val rawLight = number.toFloatOrNull() ?: 0f
                // Ограничиваем значение, чтобы избежать деления на 0
                val adc = rawLight.coerceIn(1f, 4094f)
                
                // Фоторезистор подключен к 3.3В, а подтягивающий резистор 10 кОм — к земле
                // (так как ADC 4095 = максимум света).
                // Считаем сопротивление фоторезистора (RLDR) в Омах:
                val rLdr = 10000f * (4095f - adc) / adc
                
                // Приближенный расчет Люкс (Lux) для типичного фоторезистора:
                val lux = 500000f / rLdr
                
                _light.value = "${lux.toInt()} lx"
            }

        } catch (_: Exception) {
            // Игнорируем ошибки парсинга
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect() // Всегда отключаемся при закрытии приложения
    }
}
