package com.example.smartwindowsill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * График индекса комфорта в стиле биржевого: ось Y — 0–100 %, ось X — дни.
 * Линия — индекс по дневным средним, полупрозрачная полоса — диапазон внутри дня
 * (худший..лучший случай по min/max), под линией — градиентная заливка.
 * Касание или перетаскивание выбирает день (на телефоне нет hover).
 *
 * days и points выровнены по индексам; points[i] == null — за этот день данных нет.
 */
@Composable
fun ComfortChart(
    days: List<Long>,
    points: List<ComfortPoint?>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val dateFormat = remember(Unit) { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    val labelStyle = TextStyle(fontSize = 12.sp, color = labelColor, fontFeatureSettings = "tnum")
    val markerColor = MaterialTheme.colorScheme.onSurface

    fun nearestWithData(x: Float, left: Float, right: Float): Int? {
        val n = days.size
        if (n == 0) return null
        val idx = if (n <= 1) 0 else ((x - left) / (right - left) * (n - 1)).roundToInt().coerceIn(0, n - 1)
        return points.indices.filter { points[it] != null }.minByOrNull { abs(it - idx) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(days, points) {
                val left = 40.dp.toPx()
                val right = size.width - 12.dp.toPx()
                detectTapGestures { offset -> nearestWithData(offset.x, left, right)?.let(onSelect) }
            }
            .pointerInput(days, points) {
                val left = 40.dp.toPx()
                val right = size.width - 12.dp.toPx()
                detectHorizontalDragGestures { change, _ ->
                    nearestWithData(change.position.x, left, right)?.let(onSelect)
                }
            }
    ) {
        val left = 40.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val n = days.size

        fun xOf(i: Int) = if (n <= 1) (left + right) / 2 else left + (right - left) * i / (n - 1)
        fun yOf(v: Float) = top + (1f - v.coerceIn(0f, 100f) / 100f) * (bottom - top)

        // Сетка и подписи оси Y
        for (v in listOf(0, 25, 50, 75, 100)) {
            val y = yOf(v.toFloat())
            drawLine(labelColor.copy(alpha = 0.15f), Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            val layout = measurer.measure("$v%", labelStyle)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
        }

        // Подписи оси X: до 5 равномерно расставленных дат
        if (n > 0) {
            val labelCount = minOf(5, n)
            for (k in 0 until labelCount) {
                val i = if (labelCount == 1) 0 else (k * (n - 1).toFloat() / (labelCount - 1)).roundToInt()
                val layout = measurer.measure(dateFormat.format(Date(days[i])), labelStyle)
                val x = (xOf(i) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(x, bottom + 6.dp.toPx()))
            }
        }

        val idxWithData = points.indices.filter { points[it] != null }
        if (idxWithData.isEmpty()) return@Canvas

        // Полоса диапазона внутри дня
        val band = Path().apply {
            idxWithData.forEachIndexed { k, i ->
                val x = xOf(i); val y = yOf(points[i]!!.high)
                if (k == 0) moveTo(x, y) else lineTo(x, y)
            }
            idxWithData.asReversed().forEach { i -> lineTo(xOf(i), yOf(points[i]!!.low)) }
            close()
        }
        drawPath(band, lineColor.copy(alpha = 0.16f))

        // Заливка под линией
        val firstX = xOf(idxWithData.first())
        val lastX = xOf(idxWithData.last())
        val area = Path().apply {
            idxWithData.forEachIndexed { k, i ->
                val x = xOf(i); val y = yOf(points[i]!!.index)
                if (k == 0) moveTo(x, y) else lineTo(x, y)
            }
            lineTo(lastX, bottom)
            lineTo(firstX, bottom)
            close()
        }
        drawPath(area, lineColor.copy(alpha = 0.10f))

        // Линия индекса
        val line = Path().apply {
            idxWithData.forEachIndexed { k, i ->
                val x = xOf(i); val y = yOf(points[i]!!.index)
                if (k == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx()))

        if (n <= 31) {
            idxWithData.forEach { i -> drawCircle(lineColor, 3.dp.toPx(), Offset(xOf(i), yOf(points[i]!!.index))) }
        }

        // Выбранный день: перекрестие и маркер
        selectedIndex?.takeIf { it in points.indices && points[it] != null }?.let { i ->
            val x = xOf(i)
            drawLine(
                markerColor.copy(alpha = 0.5f), Offset(x, top), Offset(x, bottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
            drawCircle(markerColor, 6.dp.toPx(), Offset(x, yOf(points[i]!!.index)))
            drawCircle(lineColor, 4.dp.toPx(), Offset(x, yOf(points[i]!!.index)))
        }
    }
}
