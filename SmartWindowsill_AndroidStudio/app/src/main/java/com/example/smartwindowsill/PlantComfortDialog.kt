package com.example.smartwindowsill

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartwindowsill.ui.theme.NumberStyle
import com.example.smartwindowsill.ui.theme.appColors
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val RANGE_OPTIONS = listOf(7, 30, 90)

@Composable
fun PlantComfortDialog(plant: Plant, viewModel: PlantsViewModel, onDismiss: () -> Unit) {
    var rangeDays by rememberSaveable { mutableStateOf(30) }
    // Включённые параметры: по умолчанию три доступных датчика
    var active by rememberSaveable { mutableStateOf(setOf(Metric.TEMPERATURE, Metric.HUMIDITY, Metric.LIGHT)) }
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }

    val days = remember(rangeDays) { windowDays(rangeDays) }
    val history by remember(days) { viewModel.dailyHistory(days.first()) }.collectAsState(initial = emptyList())
    val byDay = remember(history) { history.associateBy { it.day } }

    // Пересчёт мгновенный: зависит только от истории и набора включённых параметров
    val points = remember(days, byDay, active, plant) {
        days.map { d -> byDay[d]?.let { computeComfort(it, plant, active) } }
    }

    val selectedIndex = remember(points, selectedDay) {
        val bySelection = selectedDay?.let { sel -> days.indexOf(sel).takeIf { it >= 0 && points[it] != null } }
        bySelection ?: points.indexOfLast { it != null }.takeIf { it >= 0 }
    }
    val selectedPoint = selectedIndex?.let { points[it] }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Комфорт: ${plant.nickname}", style = MaterialTheme.typography.titleLarge)

                // Период
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RANGE_OPTIONS.forEach { d ->
                        FilterChip(selected = rangeDays == d, onClick = { rangeDays = d }, label = { Text("$d дн.") })
                    }
                }

                // Включение/выключение параметров
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Metric.entries.forEach { metric ->
                        val available = plant.idealRange(metric) != null
                        val label = if (available) metric.label else "${metric.label} (нет данных)"
                        FilterChip(
                            selected = metric in active && available,
                            enabled = available,
                            onClick = { active = if (metric in active) active - metric else active + metric },
                            label = { Text(label) }
                        )
                    }
                }

                if (points.none { it != null }) {
                    Text(
                        if (active.none { plant.idealRange(it) != null })
                            "Выберите хотя бы один параметр"
                        else
                            "Пока нет данных. Держите приложение подключённым к датчику — история копится автоматически.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ComfortChart(
                        days = days,
                        points = points,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedDay = days[it] },
                        modifier = Modifier.height(240.dp)
                    )
                    Text(
                        "Линия — индекс по средним за день, полоса — диапазон внутри дня. Коснитесь графика, чтобы увидеть детали.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    selectedPoint?.let { ComfortBreakdown(it, byDay[it.day]) }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
                    Text("Закрыть")
                }
            }
        }
    }
}

// Разбор выбранного дня: что тянет индекс вниз и насколько
@Composable
private fun ComfortBreakdown(point: ComfortPoint, daily: DailyCondition?) {
    val dateText = remember(point.day) {
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(point.day))
    }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Индекс комфорта: ${point.index.toInt()}%",
                style = NumberStyle.copy(fontSize = 20.sp, lineHeight = 28.sp), color = comfortColor(point.index)
            )
            Text(
                "Внутри дня: ${point.low.toInt()}–${point.high.toInt()}%" +
                    (daily?.let { " · замеров: ${it.sampleCount}" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            point.parts.forEach { part ->
                val diff = part.value - part.ideal.opt
                val where = when {
                    part.score >= 0.95f -> "в норме"
                    diff > 0 -> "выше идеала на ${fmt(diff)} ${part.metric.unit}"
                    else -> "ниже идеала на ${fmt(-diff)} ${part.metric.unit}"
                }
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(part.metric.label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (part.lossPoints >= 0.5f) "−${part.lossPoints.toInt()} п.п." else "0",
                            color = if (part.lossPoints >= 0.5f) comfortColor(part.score * 100f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${fmt(part.value)} ${part.metric.unit} (идеал ${fmt(part.ideal.opt)}, " +
                            "допустимо ${fmt(part.ideal.min)}–${fmt(part.ideal.max)}) — $where",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val worst = point.parts.firstOrNull()
            if (worst != null && worst.lossPoints >= 0.5f) {
                Text(
                    "Что улучшить в первую очередь: ${worst.metric.label.lowercase()}.",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun comfortColor(percent: Float): Color = when {
    percent >= 70f -> MaterialTheme.appColors.comfortGood
    percent >= 40f -> MaterialTheme.appColors.comfortWarn
    else -> MaterialTheme.appColors.comfortBad
}

private fun fmt(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

// n последних локальных суток, заканчивая сегодняшними, по возрастанию
private fun windowDays(n: Int): List<Long> {
    val today = dayStartMillis(System.currentTimeMillis())
    return (n - 1 downTo 0).map { back ->
        Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -back)
        }.timeInMillis
    }
}
