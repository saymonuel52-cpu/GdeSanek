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
        canvas3.drawText("3.1  План освещения", M, y, textPaint3); y += 24f
        canvas3.drawText("3.2  План розеток и силового оборудования", M, y, textPaint3); y += 24f
        canvas3.drawText("3.3  План слаботочных сетей", M, y, textPaint3); y += 48f
        
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

        // === СТРАНИЦА 1: План ОСВЕЩЕНИЯ (лист 3.1) ===
        val page1 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c1 = page1.canvas
        val framePaint1 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        c1.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint1)
        val titlePaint1 = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        c1.drawText("ПЛАН ОСВЕЩЕНИЯ — $projectName", M, M + 5f, titlePaint1)

        val wallPaint1 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        for (wl in walls) {
            val ddx = wl.x2 - wl.x1; val ddy = wl.y2 - wl.y1
            val ll = kotlin.math.sqrt(ddx * ddx + ddy * ddy).coerceAtLeast(0.001f)
            val gap = kotlin.math.min((wl.thickness / 10f) * scale, 6f) / 2f
            val nx = -ddy / ll * gap; val ny = ddx / ll * gap
            c1.drawLine(tx(wl.x1) + nx, ty(wl.y1) + ny, tx(wl.x2) + nx, ty(wl.y2) + ny, wallPaint1)
            c1.drawLine(tx(wl.x1) - nx, ty(wl.y1) - ny, tx(wl.x2) - nx, ty(wl.y2) - ny, wallPaint1)
        }

        val symPaint1 = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3f }
        val labelPaint1 = Paint().apply { textSize = 12f }
        val namePaint1 = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        
        // Только светильники и выключатели
        val lightObjects = filterBySystem(objects, "light")
        for ((oi, o) in lightObjects.withIndex()) {
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
        val legendItems1 = listOf("Освещение" to CategoryPalette.color("lamp_lust"), "Выключатели" to CategoryPalette.color("switch_1"))
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
        c1.drawText("$doc1   Лист 3.1   План освещения", sx1 + 6f, sy1 + 50f, stPaint1)
        c1.drawText(projectName + (if (aut1.isNotBlank()) "   Разраб. $aut1" else ""), sx1 + 6f, sy1 + 80f, stPaint1)
        c1.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sx1 + sw1 - 115f, sy1 + 50f, stPaint1)
        document.finishPage(page1)

        // === СТРАНИЦА 2: План РОЗЕТОК (лист 3.2) ===
        val page1b = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c1b = page1b.canvas
        val framePaint1b = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        c1b.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint1b)
        val titlePaint1b = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        c1b.drawText("ПЛАН РОЗЕТОК И СИЛОВОГО ОБОРУДОВАНИЯ — $projectName", M, M + 5f, titlePaint1b)

        val wallPaint1b = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        for (wl in walls) {
            val ddx = wl.x2 - wl.x1; val ddy = wl.y2 - wl.y1
            val ll = kotlin.math.sqrt(ddx * ddx + ddy * ddy).coerceAtLeast(0.001f)
            val gap = kotlin.math.min((wl.thickness / 10f) * scale, 6f) / 2f
            val nx = -ddy / ll * gap; val ny = ddx / ll * gap
            c1b.drawLine(tx(wl.x1) + nx, ty(wl.y1) + ny, tx(wl.x2) + nx, ty(wl.y2) + ny, wallPaint1b)
            c1b.drawLine(tx(wl.x1) - nx, ty(wl.y1) - ny, tx(wl.x2) - nx, ty(wl.y2) - ny, wallPaint1b)
        }

        val symPaint1b = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3f }
        val labelPaint1b = Paint().apply { textSize = 12f }
        val namePaint1b = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        
        // Только розетки и силовое
        val socketObjects = filterBySystem(objects, "socket")
        for ((oi, o) in socketObjects.withIndex()) {
            val col = if (mono) Color.BLACK else CategoryPalette.color(o.type)
            symPaint1b.color = col; labelPaint1b.color = col
            if (ArchTypes.isArch(o.type)) {
                var th = 100f
                for (wl in walls) { if (distToSeg(o.x, o.y, wl) < 30f) th = wl.thickness }
                GostSymbols.draw(c1b, o.type, tx(o.x), ty(o.y), o.rotation, symPaint1b, th / 10f)
            } else {
                GostSymbols.draw(c1b, o.type, tx(o.x), ty(o.y), o.rotation, symPaint1b)
            }
            val hh = if (o.height >= 0) o.height else (SymbolPalette.height(o.type) ?: -1)
            if (hh >= 0) c1b.drawText("h=" + hh, tx(o.x) + 8f, ty(o.y) - 8f, labelPaint1b)
            SymbolPalette.power(o.type)?.let { w -> c1b.drawText(w.toString() + " Вт", tx(o.x) + 8f, ty(o.y) + 16f, labelPaint1b) }
            if (o.name.isNotBlank()) c1b.drawText(o.name.take(28), tx(o.x) + 8f, ty(o.y) + 30f + (oi % 2) * 14f, namePaint1b)
        }

        val legendPaint1b = Paint().apply { textSize = 13f }
        val legendItems1b = listOf("Розетки 220В" to CategoryPalette.color("socket_b1"), "Силовое оборудование" to CategoryPalette.color("panel_shr"))
        var lx1b = M; val ly1b = H - M - 10f
        for (it in legendItems1b) {
            legendPaint1b.color = if (mono) Color.BLACK else it.second
            c1b.drawLine(lx1b, ly1b - 4f, lx1b + 22f, ly1b - 4f, legendPaint1b)
            c1b.drawText(it.first, lx1b + 28f, ly1b, legendPaint1b)
            lx1b += 28f + legendPaint1b.measureText(it.first) + 30f
        }

        val sw1b = 320f; val sh1b = 90f
        val sx1b = W - M - sw1b; val sy1b = H - M - sh1b
        val stampPaint1b = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        c1b.drawRect(sx1b, sy1b, sx1b + sw1b, sy1b + sh1b, stampPaint1b)
        c1b.drawLine(sx1b, sy1b + 30f, sx1b + sw1b, sy1b + 30f, stampPaint1b)
        c1b.drawLine(sx1b, sy1b + 60f, sx1b + sw1b, sy1b + 60f, stampPaint1b)
        val stPaint1b = Paint().apply { color = Color.BLACK; textSize = 13f }
        c1b.drawText(org1, sx1b + 6f, sy1b + 20f, stPaint1b)
        c1b.drawText("$doc1   Лист 3.2   План розеток", sx1b + 6f, sy1b + 50f, stPaint1b)
        c1b.drawText(projectName, sx1b + 6f, sy1b + 80f, stPaint1b)
        c1b.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sx1b + sw1b - 115f, sy1b + 50f, stPaint1b)
        document.finishPage(page1b)

        // === СТРАНИЦА 3: План СЛАБОТОЧКИ (лист 3.3) ===
        val page1c = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c1c = page1c.canvas
        val framePaint1c = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        c1c.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint1c)
        val titlePaint1c = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        c1c.drawText("ПЛАН СЛАБОТОЧНЫХ СЕТЕЙ — $projectName", M, M + 5f, titlePaint1c)

        val wallPaint1c = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        for (wl in walls) {
            val ddx = wl.x2 - wl.x1; val ddy = wl.y2 - wl.y1
            val ll = kotlin.math.sqrt(ddx * ddx + ddy * ddy).coerceAtLeast(0.001f)
            val gap = kotlin.math.min((wl.thickness / 10f) * scale, 6f) / 2f
            val nx = -ddy / ll * gap; val ny = ddx / ll * gap
            c1c.drawLine(tx(wl.x1) + nx, ty(wl.y1) + ny, tx(wl.x2) + nx, ty(wl.y2) + ny, wallPaint1c)
            c1c.drawLine(tx(wl.x1) - nx, ty(wl.y1) - ny, tx(wl.x2) - nx, ty(wl.y2) - ny, wallPaint1c)
        }

        val symPaint1c = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3f }
        val labelPaint1c = Paint().apply { textSize = 12f }
        val namePaint1c = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        
        // Только слаботочка
        val weakObjects = filterBySystem(objects, "weak")
        for ((oi, o) in weakObjects.withIndex()) {
            val col = if (mono) Color.BLACK else CategoryPalette.color(o.type)
            symPaint1c.color = col; labelPaint1c.color = col
            if (ArchTypes.isArch(o.type)) {
                var th = 100f
                for (wl in walls) { if (distToSeg(o.x, o.y, wl) < 30f) th = wl.thickness }
                GostSymbols.draw(c1c, o.type, tx(o.x), ty(o.y), o.rotation, symPaint1c, th / 10f)
            } else {
                GostSymbols.draw(c1c, o.type, tx(o.x), ty(o.y), o.rotation, symPaint1c)
            }
            val hh = if (o.height >= 0) o.height else (SymbolPalette.height(o.type) ?: -1)
            if (hh >= 0) c1c.drawText("h=" + hh, tx(o.x) + 8f, ty(o.y) - 8f, labelPaint1c)
            if (o.name.isNotBlank()) c1c.drawText(o.name.take(28), tx(o.x) + 8f, ty(o.y) + 30f + (oi % 2) * 14f, namePaint1c)
        }

        val legendPaint1c = Paint().apply { textSize = 13f }
        val legendItems1c = listOf("Слаботочка (ТВ/Интернет)" to CategoryPalette.color("sks_tv"))
        var lx1c = M; val ly1c = H - M - 10f
        for (it in legendItems1c) {
            legendPaint1c.color = if (mono) Color.BLACK else it.second
            c1c.drawLine(lx1c, ly1c - 4f, lx1c + 22f, ly1c - 4f, legendPaint1c)
            c1c.drawText(it.first, lx1c + 28f, ly1c, legendPaint1c)
            lx1c += 28f + legendPaint1c.measureText(it.first) + 30f
        }

        val sw1c = 320f; val sh1c = 90f
        val sx1c = W - M - sw1c; val sy1c = H - M - sh1c
        val stampPaint1c = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        c1c.drawRect(sx1c, sy1c, sx1c + sw1c, sy1c + sh1c, stampPaint1c)
        c1c.drawLine(sx1c, sy1c + 30f, sx1c + sw1c, sy1c + 30f, stampPaint1c)
        c1c.drawLine(sx1c, sy1c + 60f, sx1c + sw1c, sy1c + 60f, stampPaint1c)
        val stPaint1c = Paint().apply { color = Color.BLACK; textSize = 13f }
        c1c.drawText(org1, sx1c + 6f, sy1c + 20f, stPaint1c)
        c1c.drawText("$doc1   Лист 3.3   Слаботочка", sx1c + 6f, sy1c + 50f, stPaint1c)
        c1c.drawText(projectName, sx1c + 6f, sy1c + 80f, stPaint1c)
        c1c.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sx1c + sw1c - 115f, sy1c + 50f, stPaint1c)
        document.finishPage(page1c)


        val file = File(context.cacheDir, "GdeSanek_$projectId.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }


    /**
     * Фильтр объектов по системе:
     * - light: светильники, выключатели
     * - socket: розетки, силовое оборудование (плита, стиралка)
     * - weak: слаботочка (ТВ, интернет, домофон)
     */
    private fun filterBySystem(objects: List<PlanObject>, system: String): List<PlanObject> {
        return objects.filter { o ->
            val type = o.type.lowercase()
            when (system) {
                "light" -> type.contains("lamp") || type.contains("light") || type.contains("switch") || 
                           type.contains("выкл") || type.contains("люстр") || type.contains("бра")
                "socket" -> type.contains("socket") || type.contains("panel") || type.contains("розетк") ||
                            type.contains("плит") || type.contains("стир") || type.contains("вытяж")
                "weak" -> type.contains("sks") || type.contains("tv") || type.contains("rj45") || 
                          type.contains("интернет") || type.contains("тв") || type.contains("домофон")
                else -> true
            }
        }
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
