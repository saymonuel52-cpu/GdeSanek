package ru.gdesanek.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import ru.gdesanek.core.ArchTypes
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import ru.gdesanek.model.Wall
import ru.gdesanek.render.CategoryPalette
import ru.gdesanek.render.GostSymbols
import ru.gdesanek.theme.SymbolPalette
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun export(
        context: Context,
        projectName: String,
        projectId: Long,
        walls: List<Wall>,
        objects: List<PlanObject>,
        tracks: List<CableTrack>,
        mono: Boolean = false,
        passport: List<String> = listOf("", "", "", "")
    ): File {
        val document = PdfDocument()
        val pw = 1123
        val ph = 794
        val M = 30f
        val W = pw.toFloat()
        val H = ph.toFloat()

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (wl in walls) {
            if (wl.x1 < minX) minX = wl.x1; if (wl.x2 < minX) minX = wl.x2
            if (wl.y1 < minY) minY = wl.y1; if (wl.y2 < minY) minY = wl.y2
            if (wl.x1 > maxX) maxX = wl.x1; if (wl.x2 > maxX) maxX = wl.x2
            if (wl.y1 > maxY) maxY = wl.y1; if (wl.y2 > maxY) maxY = wl.y2
        }
        for (o in objects) {
            if (o.x < minX) minX = o.x; if (o.x > maxX) maxX = o.x
            if (o.y < minY) minY = o.y; if (o.y > maxY) maxY = o.y
        }
        if (minX > maxX) { minX = 0f; minY = 0f; maxX = 100f; maxY = 100f }
        val spanX = (maxX - minX).coerceAtLeast(1f)
        val spanY = (maxY - minY).coerceAtLeast(1f)
        val scale = minOf((W - 2 * M - 300f) / spanX, (H - 2 * M - 80f) / spanY)
        val ox = M; val oy = M + 50f
        val tx = { x: Float -> ox + (x - minX) * scale }
        val ty = { y: Float -> oy + (y - minY) * scale }

        // === СТРАНИЦА 3: Общие данные (лист 1.1) ===
        val page3 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 1).create())
        val canvas3 = page3.canvas
        val framePaint3 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas3.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint3)
        
        val titlePaint3 = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        canvas3.drawText("ОБЩИЕ ДАННЫЕ", M, M + 8f, titlePaint3)
        
        val textPaint3 = Paint().apply { color = Color.BLACK; textSize = 14f }
        var y = M + 40f
        
        canvas3.drawText("ВЕДОМОСТЬ РАБОЧИХ ЧЕРТЕЖЕЙ:", M, y, textPaint3); y += 24f
        canvas3.drawText("1.1  Общие данные", M, y, textPaint3); y += 24f
        canvas3.drawText("2.1  Однолинейная схема щита ЩР-1", M, y, textPaint3); y += 24f
        canvas3.drawText("3.1  План расположения ЭО и освещения", M, y, textPaint3); y += 48f
        
        canvas3.drawText("ПОЯСНИТЕЛЬНАЯ ЗАПИСКА:", M, y, textPaint3); y += 24f
        val notes = listOf(
            "1. Проект разработан на основании технического задания заказчика.",
            "2. Согласно СП 31-110-2003 объект относится к III категории по степени обеспечения надежности электроснабжения.",
            "3. Располагаемые потери напряжения не более 2%.",
            "4. Групповые сети предусмотрены трехпроводными и пятипроводными с отдельным защитным проводником PE (гл. 7.1 ПУЭ).",
            "5. Прокладка кабелей выполняется медным кабелем ВВГнг-LS: скрыто в штробе, открыто по плите перекрытия в гофрированной ПВХ трубе.",
            "6. Щит должен иметь отдельную шину для подключения защитного проводника.",
            "7. Все элементы электросетей выполнены с учетом ГОСТ Р 50462-92 (цветовая идентификация жил).",
            "8. Вся электрическая сеть рассчитана на длительно допустимую нагрузку и проверена по потере напряжения.",
            "9. Соединение жил в ответвительных коробках методом скрутки не допускается; рекомендуется клеммниками WAGO.",
            "10. Весь монтаж должен быть выполнен в соответствии с ПУЭ и СП 76.13330.2011."
        )
        for (note in notes) { canvas3.drawText(note, M, y, textPaint3); y += 24f }
        y += 24f
        
        canvas3.drawText("ВЕДОМОСТЬ ССЫЛОЧНЫХ ДОКУМЕНТОВ:", M, y, textPaint3); y += 24f
        val docs = listOf(
            "ПУЭ  Правила устройства электроустановок. - 7-е изд. - М., 2002.",
            "СП 31-110-2003  Электрооборудование жилых и общественных зданий. Нормы проектирования",
            "СП 76.13330.2011  Электротехнические устройства",
            "СП 52.13330.2010  Естественное и искусственное освещение. Нормы проектирования",
            "ГОСТ Р 50571.1-2009  Электроустановки зданий",
            "ГОСТ Р 50571.5.52-2011  Выбор и монтаж электрооборудования",
            "ГОСТ Р 50462-2009  Идентификация проводников посредством цветов и буквенно-цифровых обозначений"
        )
        for (doc in docs) { canvas3.drawText(doc, M, y, textPaint3); y += 24f }
        
        val sw3 = 360f; val sh3 = 80f
        val sx3 = W - M - sw3; val sy3 = H - M - sh3
        val stampPaint3 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas3.drawRect(sx3, sy3, sx3 + sw3, sy3 + sh3, stampPaint3)
        canvas3.drawLine(sx3, sy3 + 25f, sx3 + sw3, sy3 + 25f, stampPaint3)
        canvas3.drawLine(sx3, sy3 + 50f, sx3 + sw3, sy3 + 50f, stampPaint3)
        val st3 = Paint().apply { color = Color.BLACK; textSize = 13f }
        val org3 = passport.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "ГдеСанёк"
        val doc3 = passport.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "ЭОМ"
        canvas3.drawText(org3, sx3 + 6f, sy3 + 18f, st3)
        canvas3.drawText("$doc3   Лист 1.1   Общие данные", sx3 + 6f, sy3 + 42f, st3)
        canvas3.drawText(projectName, sx3 + 6f, sy3 + 70f, st3)
        
        document.finishPage(page3)

        // === СТРАНИЦА 2: Однолинейная схема (лист 2.1) ===
        val page2 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 2).create())
        val groups = OneLineDiagram.buildGroups(tracks, objects)
        OneLineDiagram.render(page2.canvas, projectName, groups, pw, ph)
        document.finishPage(page2)

        // === СТРАНИЦА 1: План (лист 3.1) ===
        val page1 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c1 = page1.canvas
        val framePaint1 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        c1.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint1)
        val titlePaint1 = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        c1.drawText("ПЛАН РАСПОЛОЖЕНИЯ ЭО И ОСВЕЩЕНИЯ — $projectName", M, M + 5f, titlePaint1)

        val wallPaint1 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        for (wl in walls) {
            val ddx = wl.x2 - wl.x1; val ddy = wl.y2 - wl.y1
            val ll = kotlin.math.sqrt(ddx * ddx + ddy * ddy).coerceAtLeast(0.001f)
            val gap = kotlin.math.min((wl.thickness / 10f) * scale, 6f) / 2f
            val nx = -ddy / ll * gap; val ny = ddx / ll * gap
            c1.drawLine(tx(wl.x1) + nx, ty(wl.y1) + ny, tx(wl.x2) + nx, ty(wl.y2) + ny, wallPaint1)
            c1.drawLine(tx(wl.x1) - nx, ty(wl.y1) - ny, tx(wl.x2) - nx, ty(wl.y2) - ny, wallPaint1)
        }

        val trackPaint1 = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f) }
        val trackLabel1 = Paint().apply { textSize = 13f; isFakeBoldText = true }
        for (t in tracks) {
            if (t.points.isEmpty()) continue
            trackPaint1.color = if (mono) Color.DKGRAY else t.color
            trackLabel1.color = trackPaint1.color
            val path = Path()
            path.moveTo(tx(t.points[0].x), ty(t.points[0].y))
            for (i in 1 until t.points.size) path.lineTo(tx(t.points[i].x), ty(t.points[i].y))
            c1.drawPath(path, trackPaint1)
            val p0 = t.points[0]
            c1.drawText("Гр." + (tracks.indexOf(t) + 1) + " ВВГнг-LS " + t.cable, tx(p0.x) + 6f, ty(p0.y) - 6f, trackLabel1)
        }

        val symPaint1 = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3f }
        val labelPaint1 = Paint().apply { textSize = 12f }
        val namePaint1 = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        for ((oi, o) in objects.withIndex()) {
            val col = if (mono) Color.BLACK else CategoryPalette.color(o.type)
            symPaint1.color = col; labelPaint1.color = col
            if (ArchTypes.isArch(o.type)) {
                var th = 100f
                for (wl in walls) { if (distToSeg(o.x, o.y, wl) < 30f) th = wl.thickness }
                GostSymbols.draw(c1, o.type, tx(o.x), ty(o.y), o.rotation, symPaint1, th / 10f)
            } else {
                GostSymbols.draw(c1, o.type, tx(o.x), ty(o.y), o.rotation, symPaint1)
            }
            val hh = if (o.height >= 0) o.height else (SymbolPalette.height(o.type) ?: -1)
            if (hh >= 0) c1.drawText("h=" + hh, tx(o.x) + 8f, ty(o.y) - 8f, labelPaint1)
            SymbolPalette.power(o.type)?.let { w -> c1.drawText(w.toString() + " Вт", tx(o.x) + 8f, ty(o.y) + 16f, labelPaint1) }
            if (o.name.isNotBlank()) c1.drawText(o.name.take(28), tx(o.x) + 8f, ty(o.y) + 30f + (oi % 2) * 14f, namePaint1)
        }

        val legendPaint1 = Paint().apply { textSize = 13f }
        val legendItems1 = listOf(
            "Розетки 220В" to CategoryPalette.color("socket_b1"),
            "Выключатели" to CategoryPalette.color("switch_1"),
            "Освещение" to CategoryPalette.color("lamp_lust"),
            "Слаботочка" to CategoryPalette.color("sks_tv"),
            "Щиты/короба" to CategoryPalette.color("panel_shr"),
            "Нагрузка" to CategoryPalette.color("cons_hood")
        )
        var lx1 = M; val ly1 = H - M - 10f
        for (it in legendItems1) {
            legendPaint1.color = if (mono) Color.BLACK else it.second
            c1.drawLine(lx1, ly1 - 4f, lx1 + 22f, ly1 - 4f, legendPaint1)
            c1.drawText(it.first, lx1 + 28f, ly1, legendPaint1)
            lx1 += 28f + legendPaint1.measureText(it.first) + 30f
        }

        val sw1 = 320f; val sh1 = 90f
        val sx1 = W - M - sw1; val sy1 = H - M - sh1
        val stampPaint1 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        c1.drawRect(sx1, sy1, sx1 + sw1, sy1 + sh1, stampPaint1)
        c1.drawLine(sx1, sy1 + 30f, sx1 + sw1, sy1 + 30f, stampPaint1)
        c1.drawLine(sx1, sy1 + 60f, sx1 + sw1, sy1 + 60f, stampPaint1)
        val stPaint1 = Paint().apply { color = Color.BLACK; textSize = 13f }
        val org1 = passport.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "ГдеСанёк"
        val doc1 = passport.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "ЭОМ"
        val aut1 = passport.getOrNull(2) ?: ""
        c1.drawText(org1, sx1 + 6f, sy1 + 20f, stPaint1)
        c1.drawText("$doc1   Лист 3.1   Масштаб 1:100", sx1 + 6f, sy1 + 50f, stPaint1)
        c1.drawText(projectName + (if (aut1.isNotBlank()) "   Разраб. $aut1" else ""), sx1 + 6f, sy1 + 80f, stPaint1)
        c1.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sx1 + sw1 - 115f, sy1 + 50f, stPaint1)
        document.finishPage(page1)

        val file = File(context.cacheDir, "GdeSanek_$projectId.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun distToSeg(px: Float, py: Float, wl: Wall): Float {
        val dx = wl.x2 - wl.x1; val dy = wl.y2 - wl.y1
        val len2 = dx * dx + dy * dy
        val t = if (len2 > 0f) ((px - wl.x1) * dx + (py - wl.y1) * dy) / len2 else 0f
        val tc = t.coerceIn(0f, 1f)
        val cx = wl.x1 + tc * dx; val cy = wl.y1 + tc * dy
        val ddx = px - cx; val ddy = py - cy
        return kotlin.math.sqrt(ddx * ddx + ddy * ddy)
    }
}
