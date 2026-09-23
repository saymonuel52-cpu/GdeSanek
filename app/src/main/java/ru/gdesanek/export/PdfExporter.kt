package ru.gdesanek.export

import android.content.Context
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
import ru.gdesanek.export.OneLineDiagram

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
            if (wl.x1 < minX) minX = wl.x1
            if (wl.x2 < minX) minX = wl.x2
            if (wl.y1 < minY) minY = wl.y1
            if (wl.y2 < minY) minY = wl.y2
            if (wl.x1 > maxX) maxX = wl.x1
            if (wl.x2 > maxX) maxX = wl.x2
            if (wl.y1 > maxY) maxY = wl.y1
            if (wl.y2 > maxY) maxY = wl.y2
        }
        for (o in objects) {
            if (o.x < minX) minX = o.x
            if (o.x > maxX) maxX = o.x
            if (o.y < minY) minY = o.y
            if (o.y > maxY) maxY = o.y
        }
        if (minX > maxX) { minX = 0f; minY = 0f; maxX = 100f; maxY = 100f }
        val spanX = (maxX - minX).coerceAtLeast(1f)
        val spanY = (maxY - minY).coerceAtLeast(1f)
        val scale = minOf((W - 2 * M - 300f) / spanX, (H - 2 * M - 80f) / spanY)
        val ox = M
        val oy = M + 50f
        val tx = { x: Float -> ox + (x - minX) * scale }
        val ty = { y: Float -> oy + (y - minY) * scale }

        // === Страница 1: план ===
        val page = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 1).create())
        val c = page.canvas
        val framePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        c.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint)
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        c.drawText("ПЛАН РАСПОЛОЖЕНИЯ ЭО И ОСВЕЩЕНИЯ — $projectName", M, M + 5f, titlePaint)

        val wallPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE }
        for (wl in walls) {
            wallPaint.strokeWidth = (wl.thickness * scale / 2f).coerceAtLeast(1.5f)
            c.drawLine(tx(wl.x1), ty(wl.y1), tx(wl.x2), ty(wl.y2), wallPaint)
        }

        val trackPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f) }
        val trackLabel = Paint().apply { textSize = 13f; isFakeBoldText = true }
        for (t in tracks) {
            if (t.points.isEmpty()) continue
            trackPaint.color = if (mono) Color.DKGRAY else t.color
            trackLabel.color = trackPaint.color
            val path = Path()
            path.moveTo(tx(t.points[0].x), ty(t.points[0].y))
            for (i in 1 until t.points.size) path.lineTo(tx(t.points[i].x), ty(t.points[i].y))
            c.drawPath(path, trackPaint)
            val p0 = t.points[0]
            c.drawText("Гр." + (tracks.indexOf(t) + 1) + " ВВГнг-LS " + t.cable, tx(p0.x) + 6f, ty(p0.y) - 6f, trackLabel)
        }

        val symPaint = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3f }
        val labelPaint = Paint().apply { textSize = 12f }
        val namePaint = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        for (o in objects) {
            val col = if (mono) Color.BLACK else CategoryPalette.color(o.type)
            symPaint.color = col
            labelPaint.color = col
            if (ArchTypes.isArch(o.type)) {
                var th = 100f
                for (wl in walls) { if (distToSeg(o.x, o.y, wl) < 30f) th = wl.thickness }
                GostSymbols.draw(c, o.type, tx(o.x), ty(o.y), o.rotation, symPaint, th / 10f)
            } else {
                GostSymbols.draw(c, o.type, tx(o.x), ty(o.y), o.rotation, symPaint)
            }
            val hh = if (o.height >= 0) o.height else (SymbolPalette.height(o.type) ?: -1)
            if (hh >= 0) c.drawText("h=" + hh, tx(o.x) + 8f, ty(o.y) - 8f, labelPaint)
            SymbolPalette.power(o.type)?.let { w -> c.drawText(w.toString() + " Вт", tx(o.x) + 8f, ty(o.y) + 16f, labelPaint) }
            if (o.name.isNotBlank()) c.drawText(o.name.take(28), tx(o.x) + 8f, ty(o.y) + 30f, namePaint)
        }

        val legendPaint = Paint().apply { textSize = 13f }
        val legendItems = listOf(
            "Розетки 220В" to CategoryPalette.color("socket_b1"),
            "Выключатели" to CategoryPalette.color("switch_1"),
            "Освещение" to CategoryPalette.color("lamp_lust"),
            "Слаботочка" to CategoryPalette.color("sks_tv"),
            "Щиты/короба" to CategoryPalette.color("panel_shr"),
            "Нагрузка" to CategoryPalette.color("cons_hood")
        )
        var lx = M
        val ly = H - M - 10f
        for (it in legendItems) {
            legendPaint.color = if (mono) Color.BLACK else it.second
            c.drawLine(lx, ly - 4f, lx + 22f, ly - 4f, legendPaint)
            c.drawText(it.first, lx + 28f, ly, legendPaint)
            lx += 28f + legendPaint.measureText(it.first) + 30f
        }

        val sw = 320f; val sh = 90f
        val sx = W - M - sw; val sy = H - M - sh
        val stampPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        c.drawRect(sx, sy, sx + sw, sy + sh, stampPaint)
        c.drawLine(sx, sy + 30f, sx + sw, sy + 30f, stampPaint)
        c.drawLine(sx, sy + 60f, sx + sw, sy + 60f, stampPaint)
        val stPaint = Paint().apply { color = Color.BLACK; textSize = 13f }
        val org = passport.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "ГдеСанёк"
        val doc = passport.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "ЭОМ"
        val aut = passport.getOrNull(2) ?: ""
        c.drawText(org, sx + 6f, sy + 20f, stPaint)
        c.drawText("$doc   Лист 2.1   Масштаб 1:100", sx + 6f, sy + 50f, stPaint)
        c.drawText(projectName + (if (aut.isNotBlank()) "   Разраб. $aut" else ""), sx + 6f, sy + 80f, stPaint)
        c.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sx + sw - 115f, sy + 50f, stPaint)
        document.finishPage(page)

        
        // === Страница 2: однолинейная схема щита ЩР (лист 2.1) ===
        val pageSch = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 2).create())
        val groups = OneLineDiagram.buildGroups(tracks, objects)
        OneLineDiagram.render(pageSch.canvas, projectName, groups, pw, ph)
        document.finishPage(pageSch)

// === Страница 2: общие данные ===
        val page2 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c2 = page2.canvas
        c2.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint)
        val h2 = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        val t2 = Paint().apply { color = Color.BLACK; textSize = 14f }
        c2.drawText("ОБЩИЕ ДАННЫЕ", M, M + 5f, h2)
        val lines = listOf(
            "ВЕДОМОСТЬ РАБОЧИХ ЧЕРТЕЖЕЙ:",
            "1.1  Общие данные",
            "2.1  План расположения ЭО и освещения",
            "",
            "ПОЯСНИТЕЛЬНАЯ ЗАПИСКА:",
            "1. Проект разработан на основании технического задания заказчика.",
            "2. Согласно СП 31-110-2003 объект относится к III категории по степени обеспечения надежности электроснабжения.",
            "3. Располагаемые потери напряжения не более 2%.",
            "4. Групповые сети предусмотрены трехпроводными и пятипроводными с отдельным защитным проводником PE (гл. 7.1 ПУЭ).",
            "5. Прокладка кабелей выполняется медным кабелем ВВГнг-LS: скрыто в штробе, открыто по плите перекрытия в гофрированной ПВХ трубе.",
            "6. Щит должен иметь отдельную шину для подключения защитного проводника.",
            "7. Все элементы электросетей выполнены с учетом ГОСТ Р 50462-92 (цветовая идентификация жил).",
            "8. Вся электрическая сеть рассчитана на длительно допустимую нагрузку и проверена по потере напряжения.",
            "9. Соединение жил в ответвительных коробках методом скрутки не допускается; рекомендуется клеммниками WAGO.",
            "10. Весь монтаж должен быть выполнен в соответствии с ПУЭ и СП 76.13330.2011.",
            "",
            "ВЕДОМОСТЬ ССЫЛОЧНЫХ ДОКУМЕНТОВ:",
            "ПУЭ  Правила устройства электроустановок. - 7-е изд. - М., 2002.",
            "СП 31-110-2003  Электрооборудование жилых и общественных зданий. Нормы проектирования",
            "СП 76.13330.2011  Электротехнические устройства",
            "СП 52.13330.2010  Естественное и искусственное освещение. Нормы проектирования",
            "ГОСТ Р 50571.1-2009  Электроустановки зданий",
            "ГОСТ Р 50571.5.52-2011  Выбор и монтаж электрооборудования",
            "ГОСТ Р 50462-2009  Идентификация проводников посредством цветов и буквенно-цифровых обозначений"
        )
        var yy = M + 40f
        for (ln in lines) { c2.drawText(ln, M, yy, t2); yy += 24f }
        document.finishPage(page2)

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
