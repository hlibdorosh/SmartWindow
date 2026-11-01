package com.example.smartwindowsill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class SensorChartMetric(val label: String, val unit: String, val icon: String, val color: Color) {
    TEMPERATURE("Температура", "°C", "🌡️", Color(0xFFFF7043)),
    HUMIDITY("Влажность", "%", "💧", Color(0xFF42A5F5)),
    LIGHT("Свет", "lx", "☀️", Color(0xFFFFB300))
}

@Composable
fun SensorHistoryChart(
    history: List<SensorHistory>,
    metric: SensorChartMetric,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📊 Накапливаем данные датчика... Появятся в течение минуты",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val values = remember(history, metric) {
        history.map {
            when (metric) {
                SensorChartMetric.TEMPERATURE -> it.temperature
                SensorChartMetric.HUMIDITY -> it.humidity
                SensorChartMetric.LIGHT -> it.light.coerceIn(0f, 20000f)
            }
        }
    }

    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var selectedIndex by remember(history, metric) { mutableStateOf<Int?>(null) }
    val activeIndex = selectedIndex?.coerceIn(0, history.size - 1) ?: (history.size - 1)
    val activeHistory = history.getOrNull(activeIndex)
    val activeVal = values.getOrNull(activeIndex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Подсказка/значение в выбранной точке
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${metric.icon} Динамика (60 мин): ${metric.label}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (activeHistory != null && activeVal != null) {
                Text(
                    text = "${timeFormat.format(Date(activeHistory.timestamp))} → ${formatValue(activeVal)} ${metric.unit}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = metric.color
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .pointerInput(history, metric) {
                    val left = 38.dp.toPx()
                    val right = size.width - 8.dp.toPx()
                    detectTapGestures { offset ->
                        selectedIndex = nearestIndex(offset.x, left, right, history.size)
                    }
                }
                .pointerInput(history, metric) {
                    val left = 38.dp.toPx()
                    val right = size.width - 8.dp.toPx()
                    detectHorizontalDragGestures { change, _ ->
                        selectedIndex = nearestIndex(change.position.x, left, right, history.size)
                    }
                }
        ) {
            val left = 38.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val n = values.size

            val minRaw = values.minOrNull() ?: 0f
            val maxRaw = values.maxOrNull() ?: 100f
            val spread = if (maxRaw - minRaw < 1e-2f) 1f else (maxRaw - minRaw)
            val minY = minRaw - spread * 0.1f
            val maxY = maxRaw + spread * 0.1f
            val rangeY = if (maxY - minY < 1e-2f) 1f else (maxY - minY)

            fun xOf(i: Int) = if (n <= 1) (left + right) / 2f else left + (right - left) * i / (n - 1)
            fun yOf(v: Float) = top + (1f - (v - minY) / rangeY).coerceIn(0f, 1f) * (bottom - top)

            // Сетка по Y (3 линии: минимум, середина, максимум)
            val ySteps = listOf(minRaw, (minRaw + maxRaw) / 2f, maxRaw)
            for (v in ySteps) {
                val y = yOf(v)
                drawLine(
                    color = labelColor.copy(alpha = 0.12f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx()
                )
                val layout = measurer.measure(formatValue(v), labelStyle)
                drawText(layout, topLeft = Offset(left - layout.size.width - 4.dp.toPx(), y - layout.size.height / 2f))
            }

            // Подписи по оси X (до 4 отметок времени)
            if (n > 1) {
                val stepCount = minOf(4, n)
                for (k in 0 until stepCount) {
                    val i = (k * (n - 1).toFloat() / (stepCount - 1)).roundToInt()
                    val layout = measurer.measure(timeFormat.format(Date(history[i].timestamp)), labelStyle)
                    val x = (xOf(i) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                    drawText(layout, topLeft = Offset(x, bottom + 4.dp.toPx()))
                }
            }

            if (n >= 2) {
                // Построение линии и градиентной заливки
                val linePath = Path()
                val fillPath = Path()

                fillPath.moveTo(xOf(0), bottom)
                linePath.moveTo(xOf(0), yOf(values[0]))
                fillPath.lineTo(xOf(0), yOf(values[0]))

                for (i in 1 until n) {
                    val x = xOf(i)
                    val y = yOf(values[i])
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                fillPath.lineTo(xOf(n - 1), bottom)
                fillPath.close()

                // Заливка под графиком
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(metric.color.copy(alpha = 0.30f), metric.color.copy(alpha = 0.02f)),
                        startY = top,
                        endY = bottom
                    )
                )

                // Линия графика
                drawPath(
                    path = linePath,
                    color = metric.color,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Маркер активной точки
                val sel = activeIndex.coerceIn(0, n - 1)
                val selX = xOf(sel)
                val selY = yOf(values[sel])
                drawCircle(color = metric.color, radius = 5.dp.toPx(), center = Offset(selX, selY))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(selX, selY))
            } else if (n == 1) {
                val cx = (left + right) / 2f
                val cy = yOf(values[0])
                drawCircle(color = metric.color, radius = 5.dp.toPx(), center = Offset(cx, cy))
            }
        }
    }
}

private fun nearestIndex(x: Float, left: Float, right: Float, count: Int): Int {
    if (count <= 1) return 0
    return ((x - left) / (right - left) * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

private fun formatValue(v: Float): String =
    if (v >= 1000f) "${(v / 1000f).let { if (it % 1f == 0f) it.toInt() else "%.1f".format(it) }}k"
    else if (v % 1f == 0f) v.toInt().toString()
    else "%.1f".format(v)
