package ru.gdesanek.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import ru.gdesanek.core.EstimateCalculator
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import ru.gdesanek.model.Wall
import ru.gdesanek.render.GostSymbols
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun export(context: Context, projectName: String, projectId: Long, walls: List<Wall>, objects: List<PlanObject>, tracks: List<CableTrack>): File {
        val prefs = context.getSharedPreferences("estimate", Context.MODE_PRIVATE)
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val document = PdfDocument()

        val framePaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f; style = Paint.Style.STROKE }
        val stampPaint = Paint().apply { color = Color.GRAY; textSize = 10f }
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 18f; isFakeBoldText = true }

        // ===== ЛИСТ 1: ПЛАН (А4 альбомный) =====
        val page1 = document.startPage(PdfDocument.PageInfo.Builder(842, 595, 1).create())
        val c1 = page1.canvas
        c1.drawColor(Color.WHITE)
        c1.drawRect(15f, 15f, 827f, 580f, framePaint)
        c1.drawText("ГдеСанёк · План электропроводки · $projectName", 30f, 40f, titlePaint)

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        fun add(x: Float, y: Float) { if (x < minX) minX = x; if (y < minY) minY = y; if (x > maxX) maxX = x; if (y > maxY) maxY = y }
        walls.forEach { add(it.x1, it.y1); add(it.x2, it.y2) }
        objects.forEach { add(it.x, it.y) }
        tracks.forEach { t -> t.points.forEach { add(it.x, it.y) } }
        if (minX > maxX) { minX = 0f; minY = 0f; maxX = 100f; maxY = 100f }

        val scale = minOf((842 - 120f) / (maxX - minX + 1), (595 - 160f) / (maxY - minY + 1))
        c1.save()
        c1.translate(60f, 70f)
        c1.scale(scale, scale)
        c1.translate(-minX, -minY)

        val trackPaint = Paint().apply { color = Color.parseColor("#2E7D32"); strokeWidth = 4f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val wallPaint = Paint().apply { color = Color.BLACK; strokeWidth = 8f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val symPaint = Paint().apply { color = Color.BLACK; strokeWidth = 4f }

        for (t in tracks) for (i in 0 until t.points.size - 1) c1.drawLine(t.points[i].x, t.points[i].y, t.points[i+1].x, t.points[i+1].y, trackPaint)
        for (w in walls) c1.drawLine(w.x1, w.y1, w.x2, w.y2, wallPaint)
        for (o in objects) GostSymbols.draw(c1, o.type, o.x, o.y, o.rotation, symPaint)
        c1.restore()

        c1.drawText("Масштаб: 1 клетка = 0.5 м · Трассы зеленым · Дата: $date", 30f, 570f, stampPaint)
        document.finishPage(page1)

        // ===== ЛИСТ 2: СМЕТА (А4 книжный) =====
        val page2 = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 2).create())
        val c2 = page2.canvas
        c2.drawColor(Color.WHITE)
        c2.drawRect(15f, 15f, 580f, 827f, framePaint)
        c2.drawText("СМЕТА · $projectName", 30f, 50f, titlePaint)
        c2.drawText("Дата: $date", 30f, 72f, stampPaint)

        val hPaint = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        val tPaint = Paint().apply { color = Color.BLACK; textSize = 12f }
        val totalPaint = Paint().apply { color = Color.BLACK; textSize = 18f; isFakeBoldText = true }

        var y = 120f
        c2.drawText("Наименование", 30f, y, hPaint)
        c2.drawText("Кол-во", 320f, y, hPaint)
        c2.drawText("Цена", 410f, y, hPaint)
        c2.drawText("Сумма", 480f, y, hPaint)
        y += 12f; c2.drawLine(30f, y, 565f, y, tPaint); y += 28f

        var total = 0f
        for (r in EstimateCalculator.rows(objects, tracks)) {
            val price = prefs.getFloat(r.key, 0f)
            val sum = r.qty * price
            total += sum
            c2.drawText(r.name, 30f, y, tPaint)
            c2.drawText(String.format("%.1f %s", r.qty, r.unit), 320f, y, tPaint)
            c2.drawText(String.format("%.0f", price), 410f, y, tPaint)
            c2.drawText(String.format("%.0f", sum), 480f, y, tPaint)
            y += 26f
        }
        y += 24f
        c2.drawText(String.format("ИТОГО: %.0f руб.", total), 30f, y, totalPaint)
        c2.drawText("ГдеСанёк · Лист 2 (Смета)", 430f, 815f, stampPaint)
        document.finishPage(page2)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
        val file = File(dir, "GdeSanek_${projectName.replace(Regex("[^A-Za-zА-Яа-я0-9]"), "_")}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }
}
