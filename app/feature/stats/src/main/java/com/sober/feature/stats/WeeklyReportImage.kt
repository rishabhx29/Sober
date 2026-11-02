package com.sober.feature.stats

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.sober.core.domain.usecase.ExtremeDay
import com.sober.core.domain.usecase.weekOverWeekLabel
import com.sober.core.ui.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Draws the shareable 7-day card.
 *
 * Android canvas rather than a Compose capture: the report must fit one image
 * even when parts of the stats screen are scrolled out of view, and a capture
 * would also drag in whatever theme the device currently has.
 */
internal fun renderWeeklyReport(context: Context, data: WeeklyReportData): Bitmap {
    val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(CREAM)

    val inter = runCatching { ResourcesCompat.getFont(context, R.font.inter) }
        .getOrNull() ?: Typeface.SANS_SERIF
    val mono = runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono) }
        .getOrNull() ?: Typeface.MONOSPACE
    val regular = Typeface.create(inter, Typeface.NORMAL)
    val semibold = Typeface.create(inter, Typeface.BOLD)

    fun paint(
        typeface: Typeface,
        size: Float,
        color: Int,
        letterSpacing: Float = 0f
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
        this.letterSpacing = letterSpacing
    }

    val eyebrow = paint(mono, 26f, MUTED, 0.18f)
    val rangePaint = paint(regular, 38f, SECONDARY)
    val totalPaint = paint(semibold, 148f, INK)
    val caption = paint(regular, 32f, SECONDARY)
    val labelPaint = paint(mono, 24f, MUTED, 0.14f)
    val valuePaint = paint(semibold, 46f, INK)
    val footerPaint = paint(mono, 22f, MUTED, 0.04f)

    val margin = 88f
    val right = WIDTH - margin

    canvas.drawText("SOBER — WEEKLY REPORT", margin, 132f, eyebrow)
    canvas.drawText(data.rangeLabel, margin, 206f, rangePaint)

    canvas.drawText(formatMinutes(data.totalMinutes), margin, 392f, totalPaint)
    canvas.drawText("on screen this week", margin, 452f, caption)

    drawComparisonChip(canvas, data.comparisonPercent, margin, 508f, semibold)

    canvas.drawRect(margin, 604f, right, 606f, Paint().apply { color = BORDER })

    val columnWidth = (right - margin) / 2f
    drawMetric(canvas, margin, 700f, "QUIETEST DAY", data.bestDay.describe(), labelPaint, valuePaint, columnWidth)
    drawMetric(canvas, margin + columnWidth, 700f, "HEAVIEST DAY", data.worstDay.describe(), labelPaint, valuePaint, columnWidth)

    val unlocks = data.unlocks?.let { "$it" } ?: "—"
    drawMetric(canvas, margin, 892f, "PHONE UNLOCKS", unlocks, labelPaint, valuePaint, columnWidth)
    val adherence = if (data.daysMeasured > 0) {
        "${data.daysUnderLimit} of ${data.daysMeasured}"
    } else {
        "No limit set"
    }
    drawMetric(canvas, margin + columnWidth, 892f, "DAYS UNDER LIMIT", adherence, labelPaint, valuePaint, columnWidth)

    drawMetric(canvas, margin, 1084f, "FOCUS SESSIONS", "${data.focusCompleted} completed", labelPaint, valuePaint, columnWidth)
    drawMetric(canvas, margin + columnWidth, 1084f, "DAILY AVERAGE", formatMinutes(data.averageMinutes), labelPaint, valuePaint, columnWidth)

    canvas.drawRect(margin, 1196f, right, 1198f, Paint().apply { color = BORDER })
    canvas.drawText("Made on-device. Nothing in this report left your phone.", margin, 1258f, footerPaint)

    return bitmap
}

private fun drawComparisonChip(
    canvas: Canvas,
    percent: Int?,
    x: Float,
    top: Float,
    semibold: Typeface
) {
    val significant = percent != null && abs(percent) >= 3
    val worse = significant && percent!! > 0
    val text = weekOverWeekLabel(percent).uppercase(Locale.US)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = semibold
        textSize = 30f
        letterSpacing = 0.04f
        color = if (worse) DANGER else INK
    }
    val paddingX = 30f
    val height = 84f
    val width = textPaint.measureText(text) + paddingX * 2
    val rect = RectF(x, top, x + width, top + height)
    canvas.drawRoundRect(rect, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (worse) DANGER_TINT else TINT
    })
    val baseline = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
    canvas.drawText(text, x + paddingX, baseline, textPaint)
}

private fun drawMetric(
    canvas: Canvas,
    x: Float,
    top: Float,
    label: String,
    value: String,
    labelPaint: Paint,
    valuePaint: Paint,
    columnWidth: Float
) {
    canvas.drawText(label, x, top, labelPaint)
    // Copy the paint: shrinking for one value must not leak into the next.
    val fitted = Paint(valuePaint).apply {
        val max = columnWidth - 24f
        val measured = measureText(value)
        if (measured > max) textSize = (46f * (max / measured)).coerceAtLeast(26f)
    }
    canvas.drawText(value, x, top + 62f, fitted)
}

private fun ExtremeDay?.describe(): String = if (this == null) {
    "No usage"
} else {
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    val label = day?.format(DateTimeFormatter.ofPattern("EEE", Locale.US)) ?: date
    "$label · ${formatMinutes(minutes)}"
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

private const val WIDTH = 1080
private const val HEIGHT = 1350
private const val CREAM = 0xFFF7F4EF.toInt()
private const val INK = 0xFF1A1A2E.toInt()
private const val SECONDARY = 0xFF6B6560.toInt()
private const val MUTED = 0xFF726C67.toInt()
private const val BORDER = 0xFFE8E0D5.toInt()
private const val TINT = 0xFFEDEAE4.toInt()
private const val DANGER = 0xFFC0392B.toInt()
private const val DANGER_TINT = 0xFFF9ECEB.toInt()
