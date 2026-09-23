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

        val org = passport.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "ГдеСанёк"
        val doc = passport.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "ЭОМ"
        val aut = passport.getOrNull(2) ?: ""
        val authorSuffix = if (aut.isNotBlank()) "   Разраб. $aut" else ""
        val totalSheets = 8

        val trackSys = OneLineDiagram.trackSystems(tracks, objects)

        // === СТРАНИЦА 1: План ОСВЕЩЕНИЯ (лист 3.1) ===
        val page1 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c1 = page1.canvas
        drawFrame(c1, M, W, H)
        c1.drawText("ПЛАН ОСВЕЩЕНИЯ — $projectName", M, M + 5f, titlePaint())
        drawWalls(c1, walls, tx, ty, scale)
        drawTracksBySystem(c1, tracks, trackSys, "light", tx, ty, mono)
        drawObjectsByFilter(c1, objects, walls, tx, ty, scale, mono) { o -> o.type.contains("lamp") || o.type.contains("switch") || o.type.contains("light") }
        drawLegend(c1, M, H, mono, listOf("Освещение" to "lamp_lust", "Выключатели" to "switch_1"))
        drawChains(c1, walls, tx, ty, minX, maxX, minY, maxY)
        drawStamp(c1, W, H, M, org, doc, "Лист 3.1   План освещения", projectName, authorSuffix, totalSheets)
        document.finishPage(page1)

        // === СТРАНИЦА 2: План РОЗЕТОК (лист 3.2) ===
        val page1b = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c1b = page1b.canvas
        drawFrame(c1b, M, W, H)
        c1b.drawText("ПЛАН РОЗЕТОК И СИЛОВОГО ОБОРУДОВАНИЯ — $projectName", M, M + 5f, titlePaint())
        drawWalls(c1b, walls, tx, ty, scale)
        drawTracksBySystem(c1b, tracks, trackSys, "socket", tx, ty, mono)
        drawObjectsByFilter(c1b, objects, walls, tx, ty, scale, mono) { o -> o.type.contains("socket") || o.type.contains("panel") || o.type.contains("cons") }
        drawPrivyazki(c1b, objects, walls, tx, ty)
        drawLegend(c1b, M, H, mono, listOf("Розетки 220В" to "socket_b1", "Силовое оборудование" to "panel_shr"))
        drawChains(c1b, walls, tx, ty, minX, maxX, minY, maxY)
        drawStamp(c1b, W, H, M, org, doc, "Лист 3.2   План розеток", projectName, authorSuffix, totalSheets)
        document.finishPage(page1b)

        // === СТРАНИЦА 3: План СЛАБОТОЧКИ (лист 3.3) ===
        val page1c = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create())
        val c1c = page1c.canvas
        drawFrame(c1c, M, W, H)
        c1c.drawText("ПЛАН СЛАБОТОЧНЫХ СЕТЕЙ — $projectName", M, M + 5f, titlePaint())
        drawWalls(c1c, walls, tx, ty, scale)
        drawTracksBySystem(c1c, tracks, trackSys, "weak", tx, ty, mono)
        drawObjectsByFilter(c1c, objects, walls, tx, ty, scale, mono) { o -> o.type.contains("sks") || o.type.contains("tv") || o.type.contains("rj45") }
        drawLegend(c1c, M, H, mono, listOf("Слаботочка (ТВ/Интернет)" to "sks_tv"))
        drawStamp(c1c, W, H, M, org, doc, "Лист 3.3   Слаботочка", projectName, authorSuffix, totalSheets)
        document.finishPage(page1c)

        // === СТРАНИЦА 4: Ведомости (лист 3.4) ===
        val page4 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 4).create())
        val c4 = page4.canvas
        drawFrame(c4, M, W, H)
        c4.drawText("ВЕДОМОСТЬ ВЫКЛЮЧАТЕЛЕЙ; ЭКСПЛИКАЦИЯ ПОМЕЩЕНИЙ — $projectName", M, M + 8f, titlePaint().apply { textSize = 20f })
        drawVedomosti(c4, M, W, H, objects, tracks, trackSys, walls, minX, maxX, minY, maxY, org, doc, projectName, authorSuffix, totalSheets)
        document.finishPage(page4)

        // === СТРАНИЦА 5: Спецификация (лист 4.1) ===
        val page5 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 6).create())
        val c5 = page5.canvas
        drawFrame(c5, M, W, H)
        c5.drawText("СПЕЦИФИКАЦИЯ ОБОРУДОВАНИЯ И МАТЕРИАЛОВ — $projectName", M, M + 8f, titlePaint().apply { textSize = 20f })
        drawSpec(c5, M, W, H, objects, tracks, org, doc, projectName, authorSuffix, totalSheets)
        document.finishPage(page5)

        // === СТРАНИЦА 6: Однолинейная (лист 2.1) ===
        val page2 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 2).create())
        val groups = OneLineDiagram.buildGroups(tracks, objects)
        OneLineDiagram.render(page2.canvas, projectName, groups, pw, ph, totalSheets, aut)
        document.finishPage(page2)

        // === СТРАНИЦА 7: Общие данные (лист 1.1) ===
        val page3 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 1).create())
        val c3 = page3.canvas
        drawFrame(c3, M, W, H)
        c3.drawText("ОБЩИЕ ДАННЫЕ", M, M + 8f, titlePaint())
        val tp3 = Paint().apply { color = Color.BLACK; textSize = 14f }
        var y = M + 40f
        c3.drawText("ВЕДОМОСТЬ РАБОЧИХ ЧЕРТЕЖЕЙ:", M, y, tp3); y += 24f
        listOf(
            "1.1  Общие данные",
            "2.1  Однолинейная схема щита ЩР-1",
            "3.1  План освещения",
            "3.2  План розеток и силового оборудования",
            "3.3  План слаботочных сетей",
            "3.4  Ведомости выключателей и помещений",
            "4.1  Спецификация оборудования и материалов",
            "5.1  Заземление и ДСУП"
        ).forEach { c3.drawText(it, M, y, tp3); y += 24f }
        y += 12f
        c3.drawText("ПОЯСНИТЕЛЬНАЯ ЗАПИСКА:", M, y, tp3); y += 24f
        listOf(
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
        ).forEach { c3.drawText(it, M, y, tp3); y += 24f }
        y += 12f
        c3.drawText("ВЕДОМОСТЬ ССЫЛОЧНЫХ ДОКУМЕНТОВ:", M, y, tp3); y += 24f
        listOf(
            "ПУЭ  Правила устройства электроустановок. - 7-е изд. - М., 2002.",
            "СП 31-110-2003  Электрооборудование жилых и общественных зданий. Нормы проектирования",
            "СП 76.13330.2011  Электротехнические устройства",
            "СП 52.13330.2010  Естественное и искусственное освещение. Нормы проектирования",
            "ГОСТ Р 50571.1-2009  Электроустановки зданий",
            "ГОСТ Р 50571.5.52-2011  Выбор и монтаж электрооборудования",
            "ГОСТ Р 50462-2009  Идентификация проводников посредством цветов и буквенно-цифровых обозначений"
        ).forEach { c3.drawText(it, M, y, tp3); y += 24f }
        drawStamp(c3, W, H, M, org, doc, "Лист 1.1   Общие данные", projectName, authorSuffix, totalSheets)
        document.finishPage(page3)

        // === СТРАНИЦА 8: Заземление и ДСУП (лист 5.1) ===
        val page6 = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 7).create())
        val c6 = page6.canvas
        drawFrame(c6, M, W, H)
        c6.drawText("ЗАЗЕМЛЕНИЕ И ДСУП — $projectName", M, M + 8f, titlePaint().apply { textSize = 20f })
        drawGrounding(c6, M, W, H, org, doc, projectName, authorSuffix, totalSheets)
        document.finishPage(page6)

        val file = File(context.cacheDir, "GdeSanek_$projectId.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun titlePaint(): Paint = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }

    private fun drawFrame(c: android.graphics.Canvas, M: Float, W: Float, H: Float) {
        val fp = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        c.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, fp)
    }

    private fun drawWalls(c: android.graphics.Canvas, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float, scale: Float) {
        val wp = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        for (wl in walls) {
            val ddx = wl.x2 - wl.x1; val ddy = wl.y2 - wl.y1
            val ll = kotlin.math.sqrt(ddx * ddx + ddy * ddy).coerceAtLeast(0.001f)
            val gap = kotlin.math.min((wl.thickness / 10f) * scale, 6f) / 2f
            val nx = -ddy / ll * gap; val ny = ddx / ll * gap
            c.drawLine(tx(wl.x1) + nx, ty(wl.y1) + ny, tx(wl.x2) + nx, ty(wl.y2) + ny, wp)
            c.drawLine(tx(wl.x1) - nx, ty(wl.y1) - ny, tx(wl.x2) - nx, ty(wl.y2) - ny, wp)
        }
    }

    private fun drawTracksBySystem(c: android.graphics.Canvas, tracks: List<CableTrack>, trackSys: List<String>, sys: String, tx: (Float) -> Float, ty: (Float) -> Float, mono: Boolean) {
        val tp = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f) }
        val lbl = Paint().apply { textSize = 13f; isFakeBoldText = true }
        for ((idx, t) in tracks.withIndex()) {
            if (t.points.isEmpty()) continue
            if (trackSys.getOrElse(idx) { "" } != sys) continue
            tp.color = if (mono) Color.DKGRAY else t.color
            lbl.color = tp.color
            val path = Path()
            path.moveTo(tx(t.points[0].x), ty(t.points[0].y))
            for (i in 1 until t.points.size) path.lineTo(tx(t.points[i].x), ty(t.points[i].y))
            c.drawPath(path, tp)
            val mid = t.points[t.points.size / 2]
            c.drawText("Гр.${idx + 1} ВВГнг-LS " + t.cable, tx(mid.x) + 6f, ty(mid.y) - 6f, lbl)
        }
    }

    private fun drawObjectsByFilter(c: android.graphics.Canvas, objects: List<PlanObject>, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float, scale: Float, mono: Boolean, filter: (PlanObject) -> Boolean) {
        val sp = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3f }
        val lp = Paint().apply { textSize = 12f }
        val np = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        var oi = 0
        for (o in objects) {
            if (!filter(o)) continue
            val col = if (mono) Color.BLACK else CategoryPalette.color(o.type)
            sp.color = col; lp.color = col
            if (ArchTypes.isArch(o.type)) {
                var th = 100f
                for (wl in walls) { if (distToSeg(o.x, o.y, wl) < 30f) th = wl.thickness }
                GostSymbols.draw(c, o.type, tx(o.x), ty(o.y), o.rotation, sp, th / 10f)
            } else {
                GostSymbols.draw(c, o.type, tx(o.x), ty(o.y), o.rotation, sp)
            }
            val hh = if (o.height >= 0) o.height else (SymbolPalette.height(o.type) ?: -1)
            if (hh >= 0) c.drawText("h=" + hh, tx(o.x) + 8f, ty(o.y) - 8f, lp)
            SymbolPalette.power(o.type)?.let { w -> c.drawText(w.toString() + " Вт", tx(o.x) + 8f, ty(o.y) + 16f, lp) }
            if (o.name.isNotBlank()) c.drawText(o.name.take(28), tx(o.x) + 8f, ty(o.y) + 30f + (oi % 2) * 14f, np)
            oi++
        }
    }

    private fun drawLegend(c: android.graphics.Canvas, M: Float, H: Float, mono: Boolean, items: List<Pair<String, String>>) {
        val lp = Paint().apply { textSize = 13f }
        var lx = M; val ly = H - M - 10f
        for (it in items) {
            lp.color = if (mono) Color.BLACK else CategoryPalette.color(it.second)
            c.drawLine(lx, ly - 4f, lx + 22f, ly - 4f, lp)
            c.drawText(it.first, lx + 28f, ly, lp)
            lx += 28f + lp.measureText(it.first) + 30f
        }
    }

    private fun drawPrivyazki(c: android.graphics.Canvas, objects: List<PlanObject>, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float) {
        val p = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f; textSize = 9f }
        for (o in objects) {
            if (!o.type.contains("socket")) continue
            var bestW: Wall? = null; var bestD = 1e9f
            for (w in walls) { val d = distToSeg(o.x, o.y, w); if (d < bestD) { bestD = d; bestW = w } }
            val w = bestW ?: continue
            val dx = w.x2 - w.x1; val dy = w.y2 - w.y1
            val len2 = dx * dx + dy * dy
            val t = if (len2 > 0f) ((o.x - w.x1) * dx + (o.y - w.y1) * dy) / len2 else 0f
            val tc = t.coerceIn(0f, 1f)
            val fx = w.x1 + tc * dx; val fy = w.y1 + tc * dy
            c.drawLine(tx(fx), ty(fy), tx(o.x), ty(o.y), p)
            c.drawText((bestD * 10).toInt().toString(), tx(fx) + 6f, ty(fy) - 4f, p)
        }
    }

    private fun drawChains(c: android.graphics.Canvas, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float, minX: Float, maxX: Float, minY: Float, maxY: Float) {
        val dp = Paint().apply { color = Color.BLACK; strokeWidth = 1f; textSize = 10f }
        val xsRaw = mutableListOf<Float>(); val ysRaw = mutableListOf<Float>()
        for (w in walls) { xsRaw.add(w.x1); xsRaw.add(w.x2); ysRaw.add(w.y1); ysRaw.add(w.y2) }
        xsRaw.sort(); ysRaw.sort()
        val xl = mutableListOf<Float>(); val yl = mutableListOf<Float>()
        for (v in xsRaw) if (xl.isEmpty() || v - xl.last() > 15f) xl.add(v)
        for (v in ysRaw) if (yl.isEmpty() || v - yl.last() > 15f) yl.add(v)
        val yChain = ty(maxY) + 40f
        for (i in 0 until xl.size - 1) {
            val a = tx(xl[i]); val b = tx(xl[i + 1])
            if (b - a < 8f) continue
            c.drawLine(a, yChain, b, yChain, dp)
            c.drawLine(a - 4f, yChain + 4f, a + 4f, yChain - 4f, dp)
            c.drawLine(b - 4f, yChain + 4f, b + 4f, yChain - 4f, dp)
            c.drawText(((xl[i + 1] - xl[i]) * 10).toInt().toString(), (a + b) / 2f - 12f, yChain - 4f, dp)
        }
        val xChain = tx(minX) - 40f
        for (i in 0 until yl.size - 1) {
            val a = ty(yl[i]); val b = ty(yl[i + 1])
            if (b - a < 8f) continue
            c.drawLine(xChain, a, xChain, b, dp)
            c.drawLine(xChain - 4f, a - 4f, xChain + 4f, a + 4f, dp)
            c.drawLine(xChain - 4f, b - 4f, xChain + 4f, b + 4f, dp)
            c.drawText(((yl[i + 1] - yl[i]) * 10).toInt().toString(), xChain - 32f, (a + b) / 2f, dp)
        }
    }

    private fun drawVedomosti(c: android.graphics.Canvas, M: Float, W: Float, H: Float, objects: List<PlanObject>, tracks: List<CableTrack>, trackSys: List<String>, walls: List<Wall>, minX: Float, maxX: Float, minY: Float, maxY: Float, org: String, doc: String, projectName: String, authorSuffix: String, totalSheets: Int) {
        val tp = Paint().apply { color = Color.BLACK; textSize = 13f }
        val tb = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        val cp = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val switches = objects.filter { it.type.contains("switch") }.sortedWith(compareBy({ it.y }, { it.x }))
        val lightTrIdx = tracks.indices.filter { trackSys.getOrElse(it) { "" } == "light" }
        var yy = M + 40f
        c.drawText("Ведомость выключателей", M, yy, tb); yy += 24f
        val swCols = floatArrayOf(M, M + 60f, M + 300f, M + 420f, M + 560f)
        c.drawRect(swCols[0], yy, swCols[4], yy + 24f, cp)
        listOf("Марка", "Наименование", "Клавиш", "Группа").forEachIndexed { i, h -> c.drawText(h, swCols[i] + 4f, yy + 17f, tb); c.drawLine(swCols[i], yy, swCols[i], yy + 24f, cp) }
        yy += 24f
        switches.forEachIndexed { si, swObj ->
            var bestTr = -1; var bestD = 1e9f
            for (ti in lightTrIdx) { val d = distToTrackPts(swObj.x, swObj.y, tracks[ti]); if (d < bestD) { bestD = d; bestTr = ti } }
            val keys = when { swObj.type.contains("3") -> "3-кл"; swObj.type.contains("2") -> "2-кл"; else -> "1-кл" }
            c.drawRect(swCols[0], yy, swCols[4], yy + 24f, cp)
            val vals = listOf("В${si + 1}", swObj.name.take(30), keys, if (bestTr >= 0) "Гр.${bestTr + 1}" else "—")
            vals.forEachIndexed { i, v -> c.drawText(v, swCols[i] + 4f, yy + 17f, tp); c.drawLine(swCols[i], yy, swCols[i], yy + 24f, cp) }
            yy += 24f
        }
        yy += 30f
        c.drawText("Экспликация помещений", M, yy, tb); yy += 24f
        val cell = 20f
        val gx = ((maxX - minX) / cell).toInt() + 1
        val gy = ((maxY - minY) / cell).toInt() + 1
        val blocked = Array(gy) { BooleanArray(gx) }
        for (wl in walls) {
            val wlen = kotlin.math.sqrt((wl.x2 - wl.x1) * (wl.x2 - wl.x1) + (wl.y2 - wl.y1) * (wl.y2 - wl.y1))
            val steps = (wlen / (cell / 2f)).toInt() + 1
            for (si in 0..steps) {
                val x = wl.x1 + (wl.x2 - wl.x1) * si / steps
                val y = wl.y1 + (wl.y2 - wl.y1) * si / steps
                val ci = ((x - minX) / cell).toInt(); val ri = ((y - minY) / cell).toInt()
                if (ri in 0 until gy && ci in 0 until gx) blocked[ri][ci] = true
            }
        }
        for (o in objects) {
            if (!o.type.contains("door")) continue
            val ci = ((o.x - minX) / cell).toInt(); val ri = ((o.y - minY) / cell).toInt()
            for (dr in -2..2) for (dc in -2..2) {
                val r2 = ri + dr; val c2 = ci + dc
                if (r2 in 0 until gy && c2 in 0 until gx) blocked[r2][c2] = true
            }
        }
        for (w in walls) {
            for (p in listOf(w.x1 to w.y1, w.x2 to w.y2)) {
                var bestD = 1e9f; var fx = 0f; var fy = 0f
                for (w2 in walls) {
                    if (w2 === w) continue
                    val dx = w2.x2 - w2.x1; val dy = w2.y2 - w2.y1
                    val len2 = dx * dx + dy * dy
                    val t = if (len2 > 0f) ((p.first - w2.x1) * dx + (p.second - w2.y1) * dy) / len2 else 0f
                    val tc = t.coerceIn(0f, 1f)
                    val cx = w2.x1 + tc * dx; val cy = w2.y1 + tc * dy
                    val d = kotlin.math.sqrt((p.first - cx) * (p.first - cx) + (p.second - cy) * (p.second - cy))
                    if (d < bestD) { bestD = d; fx = cx; fy = cy }
                }
                if (bestD > 1f && bestD < 150f) {
                    val steps = (bestD / (cell / 2f)).toInt() + 1
                    for (si in 0..steps) {
                        val x = p.first + (fx - p.first) * si / steps
                        val y = p.second + (fy - p.second) * si / steps
                        val ci = ((x - minX) / cell).toInt(); val ri = ((y - minY) / cell).toInt()
                        if (ri in 0 until gy && ci in 0 until gx) blocked[ri][ci] = true
                    }
                }
            }
        }
        val roomIds = Array(gy) { IntArray(gx) { -1 } }
        val roomCnt = mutableListOf<Int>(); val roomSx = mutableListOf<Float>(); val roomSy = mutableListOf<Float>()
        var rc = 0
        for (r in 0 until gy) for (cc in 0 until gx) {
            if (blocked[r][cc] || roomIds[r][cc] >= 0) continue
            val id = rc++; var cnt = 0; var sx = 0f; var sy = 0f
            val q = java.util.ArrayDeque<IntArray>(); q.add(intArrayOf(r, cc)); roomIds[r][cc] = id
            while (q.isNotEmpty()) {
                val cur = q.poll(); cnt++; sx += cur[1]; sy += cur[0]
                for (d in 0..3) {
                    val dr = if (d == 0) 1 else if (d == 1) -1 else 0
                    val dc = if (d == 2) 1 else if (d == 3) -1 else 0
                    val r2 = cur[0] + dr; val c2 = cur[1] + dc
                    if (r2 in 0 until gy && c2 in 0 until gx && !blocked[r2][c2] && roomIds[r2][c2] < 0) { roomIds[r2][c2] = id; q.add(intArrayOf(r2, c2)) }
                }
            }
            roomCnt.add(cnt); roomSx.add(sx / cnt); roomSy.add(sy / cnt)
        }
        val roomRows = mutableListOf<Triple<String, Float, Float>>()
        for (id in 0 until rc) {
            if (roomCnt[id] < 40) continue
            val cxp = minX + roomSx[id] * cell; val cyp = minY + roomSy[id] * cell
            roomRows.add(Triple(roomName(cxp, cyp, objects, roomIds, id, minX, minY, cell), roomCnt[id] * 0.04f, cxp))
        }
        roomRows.sortByDescending { it.second }
        val rmCols = floatArrayOf(M, M + 60f, M + 360f, M + 520f)
        c.drawRect(rmCols[0], yy, rmCols[3], yy + 24f, cp)
        listOf("№", "Наименование", "Площадь, м²").forEachIndexed { i, h -> c.drawText(h, rmCols[i] + 4f, yy + 17f, tb); c.drawLine(rmCols[i], yy, rmCols[i], yy + 24f, cp) }
        yy += 24f
        roomRows.forEachIndexed { i, rw ->
            c.drawRect(rmCols[0], yy, rmCols[3], yy + 24f, cp)
            val vals = listOf("${i + 1}", rw.first, String.format("%.1f", rw.second))
            vals.forEachIndexed { vi, v -> c.drawText(v, rmCols[vi] + 4f, yy + 17f, tp); c.drawLine(rmCols[vi], yy, rmCols[vi], yy + 24f, cp) }
            yy += 24f
        }
        c.drawText("Итого площадь: " + String.format("%.1f", roomRows.sumOf { it.second.toDouble() }.toFloat()) + " м²", M, yy + 20f, tb)
        drawStamp(c, W, H, M, org, doc, "Лист 3.4   Ведомости", projectName, authorSuffix, totalSheets)
    }

    private fun drawSpec(c: android.graphics.Canvas, M: Float, W: Float, H: Float, objects: List<PlanObject>, tracks: List<CableTrack>, org: String, doc: String, projectName: String, authorSuffix: String, totalSheets: Int) {
        val t5 = Paint().apply { color = Color.BLACK; textSize = 12f }
        val tb5 = Paint().apply { color = Color.BLACK; textSize = 12f; isFakeBoldText = true }
        val cp5 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val groups5 = OneLineDiagram.buildGroups(tracks, objects)
        val cableMap = mutableMapOf<String, Int>()
        for (g in groups5) cableMap[g.cable] = (cableMap[g.cable] ?: 0) + (g.length * 1.15f).toInt() + 1
        val nSock = objects.count { !ArchTypes.isArch(it.type) && !ArchTypes.isFurn(it.type) && it.type.contains("socket") }
        val nSw = objects.count { it.type.contains("switch") }
        val nLamp = objects.count { it.type.contains("lamp") }
        val nWeak = objects.count { it.type.contains("sks") || it.type.contains("tv") || it.type.contains("rj45") }
        val rows = mutableListOf<List<String>>()
        var pos = 1
        rows.add(listOf("", "Раздел А. Кабели и проводники", "", "", ""))
        for ((cab, len) in cableMap) { rows.add(listOf("$pos", "Кабель ВВГнг-LS $cab", "м", "$len", "с запасом 15%")); pos++ }
        rows.add(listOf("", "Раздел Б. Коробки и комплектующие", "", "", ""))
        rows.add(listOf("$pos", "Коробка установочная (подрозетник)", "шт", "$nSock", "по числу розеток")); pos++
        rows.add(listOf("$pos", "Коробка распаячная 80x80", "шт", "${groups5.size + nSw}", "")); pos++
        rows.add(listOf("$pos", "Клемма WAGO 221-413", "шт", "${(groups5.size + nSw) * 3}", "3 шт на коробку")); pos++
        rows.add(listOf("$pos", "Труба гофр. ПВХ д20", "м", "${cableMap.values.sum() / 2}", "открытая прокладка")); pos++
        rows.add(listOf("", "Раздел В. Щит ЩР-1", "", "", ""))
        rows.add(listOf("$pos", "Щит навесной ЩРН-П-12", "шт", "1", "")); pos++
        rows.add(listOf("$pos", "Автомат вводной C25/1", "шт", "1", "")); pos++
        rows.add(listOf("$pos", "УЗО 40/2 300мА", "шт", "1", "противопожарное")); pos++
        val brMap = mutableMapOf<String, Int>()
        for (g in groups5) brMap[g.breaker] = (brMap[g.breaker] ?: 0) + 1
        for ((br, cnt) in brMap) { rows.add(listOf("$pos", "Автомат $br", "шт", "$cnt", "групповые")); pos++ }
        rows.add(listOf("$pos", "УЗО 25/2 30мА", "шт", "${groups5.count { it.rcd != "—" }}", "розетки и влажные")); pos++
        rows.add(listOf("", "Раздел Г. Изделия", "", "", ""))
        rows.add(listOf("$pos", "Розетки 220В накладные/врезные", "шт", "$nSock", "")); pos++
        rows.add(listOf("$pos", "Выключатели", "шт", "$nSw", "")); pos++
        rows.add(listOf("$pos", "Светильники", "шт", "$nLamp", "")); pos++
        rows.add(listOf("$pos", "Розетки слаботочные", "шт", "$nWeak", "")); pos++
        val cols5 = floatArrayOf(M, M + 50f, M + 340f, M + 620f, M + 700f, W - M - 10f)
        var y5 = M + 36f
        c.drawRect(cols5[0], y5, cols5[5], y5 + 24f, cp5)
        listOf("№", "Обозначение, наименование и тип", "Ед.", "Кол.", "Примечание").forEachIndexed { i, h -> c.drawText(h, cols5[i] + 4f, y5 + 17f, tb5); c.drawLine(cols5[i], y5, cols5[i], y5 + 24f, cp5) }
        y5 += 24f
        for (r in rows) {
            val isSec = r[0].isEmpty()
            c.drawRect(cols5[0], y5, cols5[5], y5 + 24f, cp5)
            r.forEachIndexed { i, v -> c.drawText(v, cols5[i] + 4f, y5 + 17f, if (isSec) tb5 else t5); c.drawLine(cols5[i], y5, cols5[i], y5 + 24f, cp5) }
            y5 += 24f
        }
        drawStamp(c, W, H, M, org, doc, "Лист 4.1   Спецификация", projectName, authorSuffix, totalSheets)
    }

    private fun drawGrounding(c: android.graphics.Canvas, M: Float, W: Float, H: Float, org: String, doc: String, projectName: String, authorSuffix: String, totalSheets: Int) {
        val t6 = Paint().apply { color = Color.BLACK; textSize = 13f }
        val tb6 = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        val ln6 = Paint().apply { color = Color.BLACK; strokeWidth = 2f; style = Paint.Style.STROKE }
        val bx6 = M + 60f; val by6 = M + 70f
        c.drawRect(bx6, by6, bx6 + 150f, by6 + 60f, ln6)
        c.drawText("Щит ЩР-1", bx6 + 10f, by6 + 25f, t6)
        c.drawText("шина PE (ГЗШ)", bx6 + 10f, by6 + 45f, t6)
        c.drawLine(bx6 + 150f, by6 + 30f, bx6 + 280f, by6 + 30f, ln6)
        c.drawRect(bx6 + 280f, by6 + 10f, bx6 + 400f, by6 + 50f, ln6)
        c.drawText("Коробка КВП", bx6 + 290f, by6 + 35f, t6)
        val trunkX = bx6 + 340f
        c.drawLine(trunkX, by6 + 50f, trunkX, by6 + 330f, ln6)
        val cons6 = listOf("Стояк водоснабжения" to "ПУ 4 мм²", "Ванна / душевой поддон" to "ПУ 2,5 мм² в гофре", "Стиральная машина" to "ПУ 2,5 мм² в гофре", "Вентиляция вытяжная" to "ПУ 2,5 мм² в гофре", "PE розеточных групп санузла" to "ПУ 4 мм²")
        cons6.forEachIndexed { i, pr ->
            val yy = by6 + 110f + i * 55f
            c.drawLine(trunkX, yy, trunkX + 100f, yy, ln6)
            c.drawRect(trunkX + 100f, yy - 20f, trunkX + 380f, yy + 20f, ln6)
            c.drawText(pr.first, trunkX + 110f, yy + 5f, t6)
            c.drawText(pr.second, trunkX + 10f, yy - 6f, t6)
        }
        val ty6 = by6 + 420f
        c.drawText("Примечания:", M, ty6, tb6)
        listOf(
            "1. Система заземления TN-C-S: PEN-проводник разделен на вводе в щите ЩР-1 на шины PE и N.",
            "2. Главную заземляющую шину (ГЗШ) установить в щите ЩР-1; к ней подключить PE и PEN проводники.",
            "3. В санузле выполнить дополнительную систему уравнивания потенциалов (ДСУП) через коробку КВП.",
            "4. К КВП подключить: стояки воды, ванну, стиральную машину, вентиляцию и PE розеток проводниками ПУ 2,5-4 мм².",
            "5. Соединения защитных проводников выполнять опрессовкой или болтовым соединением; скрутка и пайка не допускаются.",
            "6. Для коттеджа: контур заземления из полосы 40x4 мм, 3x5 м, вертикальные электроды 50 м; Rрасч ≤ 30 Ом."
        ).forEachIndexed { i, s6 -> c.drawText(s6, M, ty6 + 24f + i * 22f, t6) }
        drawStamp(c, W, H, M, org, doc, "Лист 5.1   Заземление и ДСУП", projectName, authorSuffix, totalSheets)
    }

    private fun drawStamp(c: android.graphics.Canvas, W: Float, H: Float, M: Float, org: String, doc: String, sheet: String, projectName: String, authorSuffix: String, totalSheets: Int) {
        val sw = 320f; val sh = 90f
        val sx = W - M - sw; val sy = H - M - sh
        val sp = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        c.drawRect(sx, sy, sx + sw, sy + sh, sp)
        c.drawLine(sx, sy + 30f, sx + sw, sy + 30f, sp)
        c.drawLine(sx, sy + 60f, sx + sw, sy + 60f, sp)
        val st = Paint().apply { color = Color.BLACK; textSize = 13f }
        c.drawText(org, sx + 6f, sy + 20f, st)
        c.drawText("$doc   $sheet", sx + 6f, sy + 50f, st)
        c.drawText(projectName + authorSuffix, sx + 6f, sy + 80f, st)
        c.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sx + sw - 115f, sy + 80f, st)
        c.drawText("Листов $totalSheets", sx + sw - 70f, sy + 18f, st)
    }

    private fun roomName(cx: Float, cy: Float, objects: List<PlanObject>, roomIds: Array<IntArray>, id: Int, minX: Float, minY: Float, cell: Float): String {
        var kitchen = 0; var living = 0; var bath = 0; var bed = 0; var hall = 0
        for (o in objects) {
            val ci = ((o.x - minX) / cell).toInt(); val ri = ((o.y - minY) / cell).toInt()
            if (ri !in roomIds.indices || ci !in roomIds[0].indices) continue
            if (roomIds[ri][ci] != id) continue
            val t = o.type.lowercase(); val n = o.name.lowercase()
            if (t.contains("sofa") || n.contains("диван")) living += 2
            if (t.contains("bed") || n.contains("кровать")) bed += 2
            if (t.contains("toilet") || t.contains("bath") || n.contains("унитаз") || n.contains("ванна")) bath += 2
            if (t.contains("wardrobe") || n.contains("шкаф")) hall += 1
            when {
                t.contains("плит") || t.contains("вытяж") || n.contains("кухн") -> kitchen++
                t.contains("диван") || t.contains("тв") || n.contains("гостин") || n.contains("диван") -> living++
                t.contains("унитаз") || t.contains("ванн") || t.contains("стир") || n.contains("санузел") || n.contains("стирал") -> bath++
                t.contains("кроват") || n.contains("кроват") || n.contains("спальн") -> bed++
                t.contains("шкаф") || n.contains("коридор") || n.contains("прихож") -> hall++
            }
        }
        return when {
            kitchen > 0 && kitchen >= living && kitchen >= bath && kitchen >= bed -> "Кухня"
            living > 0 && living >= bath && living >= bed -> "Гостиная"
            bath > 0 && bath >= bed -> "Санузел"
            bed > 0 -> "Спальня"
            hall > 0 -> "Прихожая"
            else -> "Помещение"
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

    private fun distToTrackPts(px: Float, py: Float, tr: CableTrack): Float {
        if (tr.points.size < 2) return 1e9f
        var min = 1e9f
        for (i in 1 until tr.points.size) {
            val a = tr.points[i - 1]; val b = tr.points[i]
            val dx = b.x - a.x; val dy = b.y - a.y
            val len2 = dx * dx + dy * dy
            val t = if (len2 > 0f) ((px - a.x) * dx + (py - a.y) * dy) / len2 else 0f
            val tc = t.coerceIn(0f, 1f)
            val cx = a.x + tc * dx; val cy = a.y + tc * dy
            val d = kotlin.math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
            if (d < min) min = d
        }
        return min
    }
}
