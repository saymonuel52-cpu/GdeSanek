package ru.gdesanek.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.graphics.pdf.PdfDocument
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import ru.gdesanek.model.Wall
import ru.gdesanek.render.GostSymbols
import ru.gdesanek.theme.SymbolPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

object PdfExporter {
    fun export(context: Context, projectName: String, projectId: Long, walls: List<Wall>, objects: List<PlanObject>, tracks: List<CableTrack>): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(842, 595, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        val labelPaint = Paint().apply { color = Color.BLACK; textSize = 5f }

        val framePaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f; style = Paint.Style.STROKE }
        val thinPaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 10f }
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 13f }

        val L = 57f; val T = 14f; val R = 828f; val B = 581f
        canvas.drawRect(L, T, R, B, framePaint)

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        fun add(x: Float, y: Float) { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y }
        walls.forEach { add(it.x1, it.y1); add(it.x2, it.y2) }
        objects.forEach { add(it.x, it.y) }
        tracks.forEach { t -> t.points.forEach { add(it.x, it.y) } }
        if (minX > maxX) { minX = 0f; minY = 0f; maxX = 1000f; maxY = 1000f }
        minX -= 100f; minY -= 100f; maxX += 100f; maxY += 100f

        val aL = L + 10f; val aT = T + 25f; val aR = R - 10f; val aB = B - 80f
        val scale = min((aR - aL) / (maxX - minX), (aB - aT) / (maxY - minY))
        fun tx(x: Float) = aL + (x - minX) * scale
        fun ty(y: Float) = aT + (y - minY) * scale

        canvas.drawText("ПЛАН РАСПОЛОЖЕНИЯ ЭО И ОСВЕЩЕНИЯ — $projectName", L + 10f, T + 16f, titlePaint)

        val wallPaint = Paint().apply { color = Color.BLACK; strokeWidth = 1.5f }
        for (w in walls) {
            val dx = w.x2 - w.x1; val dy = w.y2 - w.y1
            val len = sqrt(dx * dx + dy * dy); if (len < 1f) continue
            val ux = dx / len; val uy = dy / len; val px = -uy; val py = ux
            val half = w.thickness / 10f / 2f * scale
            canvas.drawLine(tx(w.x1) + px * half, ty(w.y1) + py * half, tx(w.x2) + px * half, ty(w.y2) + py * half, wallPaint)
            canvas.drawLine(tx(w.x1) - px * half, ty(w.y1) - py * half, tx(w.x2) - px * half, ty(w.y2) - py * half, wallPaint)
        }

        val trPaint = Paint().apply { strokeWidth = 1.5f }
        for (t in tracks) {
            trPaint.color = t.color; trPaint.pathEffect = when (t.wiring) { "shtroba" -> DashPathEffect(floatArrayOf(6f, 4f), 0f); "gofra" -> DashPathEffect(floatArrayOf(6f, 3f, 2f, 3f), 0f); "truba" -> DashPathEffect(floatArrayOf(2f, 3f), 0f); "lotok" -> DashPathEffect(floatArrayOf(8f, 3f), 0f); else -> null }
            for (i in 0 until t.points.size - 1) canvas.drawLine(tx(t.points[i].x), ty(t.points[i].y), tx(t.points[i+1].x), ty(t.points[i+1].y), trPaint)
            if (t.points.isNotEmpty()) { val p0 = t.points[0]; labelPaint.color = t.color; canvas.drawText("Гр." + (tracks.indexOf(t) + 1) + " ВВГнг-LS " + t.cable, tx(p0.x) + 4f, ty(p0.y) - 3f, labelPaint) }
        }

        labelPaint.color = Color.BLACK
        val symPaint = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 8f }
        for (o in objects) {
            symPaint.color = SymbolPalette.color(o.type)
            canvas.save()
            canvas.translate(tx(o.x), ty(o.y))
            canvas.scale(scale, scale)
            canvas.translate(-o.x, -o.y)
            GostSymbols.draw(canvas, o.type, o.x, o.y, o.rotation, symPaint)
            canvas.restore()
            SymbolPalette.height(o.type)?.let { h -> canvas.drawText("H=$h", tx(o.x) + 6f, ty(o.y) - 4f, labelPaint) }
            SymbolPalette.power(o.type)?.let { w -> canvas.drawText(w.toString() + " Вт", tx(o.x) + 6f, ty(o.y) + 8f, labelPaint) }
        }

        val legend = listOf(
            "Розетки 220В" to Color.parseColor("#FF5252"),
            "Выключатели" to Color.parseColor("#FF7043"),
            "Освещение" to Color.parseColor("#FFCA28"),
            "Слаботочка" to Color.parseColor("#40C4FF"),
            "Щиты/короба" to Color.parseColor("#26A69A"),
            "Нагрузка" to Color.parseColor("#AB47BC")
        )
        var ly = B - 38f
        textPaint.textSize = 8f
        for (i in legend.indices) {
            val (name, c) = legend[i]
            val col = if (i < 3) 0 else 1
            val row = if (i < 3) i else i - 3
            val x0 = L + 10f + col * 200f
            val yy = B - 38f + row * 11f
            trPaint.color = c; trPaint.strokeWidth = 2f
            canvas.drawLine(x0, yy - 3f, x0 + 20f, yy - 3f, trPaint)
            textPaint.color = Color.BLACK
            canvas.drawText(name, x0 + 25f, yy, textPaint)
        }

        val sL = R - 185f; val sT = B - 55f
        canvas.drawRect(sL, sT, R, B, framePaint)
        canvas.drawLine(sL, sT + 18f, R, sT + 18f, thinPaint)
        canvas.drawLine(sL, sT + 36f, R, sT + 36f, thinPaint)
        canvas.drawLine(sL + 95f, sT + 18f, sL + 95f, B, thinPaint)
        textPaint.textSize = 11f
        canvas.drawText("ГдеСанёк", sL + 6f, sT + 12f, textPaint)
        textPaint.textSize = 9f
        canvas.drawText("Лист Э1    Масштаб 1:100", sL + 6f, sT + 30f, textPaint)
        canvas.drawText(projectName, sL + 6f, sT + 48f, textPaint)
        canvas.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sL + 100f, sT + 30f, textPaint)
        canvas.drawText("Разработал: Электромонтажник", sL + 100f, sT + 48f, textPaint)

        doc.finishPage(page)
        val file = File(context.cacheDir, "GdeSanek_$projectId.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
