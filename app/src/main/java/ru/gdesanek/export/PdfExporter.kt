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

    private data class Room(val name: String, val area: Float, val cx: Float, val cy: Float)

    fun export(
        context: Context, projectName: String, projectId: Long,
        walls: List<Wall>, objects: List<PlanObject>, tracks: List<CableTrack>,
        mono: Boolean = false, passport: List<String> = listOf("", "", "", "")
    ): File {
        val document = PdfDocument()
        val pw = 1123; val ph = 794; val M = 30f
        val W = pw.toFloat(); val H = ph.toFloat()
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (wl in walls) {
            if (wl.x1 < minX) minX = wl.x1; if (wl.x2 < minX) minX = wl.x2
            if (wl.y1 < minY) minY = wl.y1; if (wl.y2 < minY) minY = wl.y2
            if (wl.x1 > maxX) maxX = wl.x1; if (wl.x2 > maxX) maxX = wl.x2
            if (wl.y1 > maxY) maxY = wl.y1; if (wl.y2 > maxY) maxY = wl.y2
        }
        for (o in objects) { if (o.x < minX) minX = o.x; if (o.x > maxX) maxX = o.x; if (o.y < minY) minY = o.y; if (o.y > maxY) maxY = o.y }
        if (minX > maxX) { minX = 0f; minY = 0f; maxX = 100f; maxY = 100f }
        val spanX = (maxX - minX).coerceAtLeast(1f); val spanY = (maxY - minY).coerceAtLeast(1f)
        val scale = minOf((W - 2 * M - 380f) / spanX, (H - 2 * M - 80f) / spanY)
        val ox = M + 70f; val oy = M + 50f
        val tx = { x: Float -> ox + (x - minX) * scale }
        val ty = { y: Float -> oy + (y - minY) * scale }
        val org = passport.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "ГдеСанёк"
        val doc = passport.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "ЭОМ"
        val aut = passport.getOrNull(2) ?: ""
        val authorSuffix = if (aut.isNotBlank()) "   Разраб. $aut" else ""
        val totalSheets = 8
        val trackSys = OneLineDiagram.trackSystems(tracks, objects)
        val groups = OneLineDiagram.buildGroups(tracks, objects)
        val rooms = computeRooms(walls, objects, minX, maxX, minY, maxY)

        // === 1.1 ОБЩИЕ ДАННЫЕ ===
        val pA = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 1).create()); val cA = pA.canvas
        drawFrame(cA, M, W, H)
        cA.drawText("ОБЩИЕ ДАННЫЕ", M, M + 8f, titlePaint())
        val tpA = Paint().apply { color = Color.BLACK; textSize = 14f }
        var yA = M + 40f
        cA.drawText("ВЕДОМОСТЬ РАБОЧИХ ЧЕРТЕЖЕЙ:", M, yA, tpA); yA += 24f
        listOf("1.1  Общие данные", "2.1  Однолинейная схема щита ЩР-1", "3.1  План освещения", "3.2  План розеток и силового оборудования", "3.3  План слаботочных сетей", "3.4  Ведомости выключателей и помещений", "4.1  Спецификация оборудования и материалов", "5.1  Заземление и ДСУП").forEach { cA.drawText(it, M, yA, tpA); yA += 24f }
        yA += 12f
        cA.drawText("ПОЯСНИТЕЛЬНАЯ ЗАПИСКА:", M, yA, tpA); yA += 24f
        listOf("1. Проект разработан на основании технического задания заказчика.", "2. Согласно СП 31-110-2003 объект относится к III категории по степени обеспечения надежности электроснабжения.", "3. Располагаемые потери напряжения не более 2%.", "4. Групповые сети предусмотрены трехпроводными и пятипроводными с отдельным защитным проводником PE (гл. 7.1 ПУЭ).", "5. Прокладка кабелей выполняется медным кабелем ВВГнг-LS: скрыто в штробе, открыто по плите перекрытия в гофрированной ПВХ трубе.", "6. Щит должен иметь отдельную шину для подключения защитного проводника.", "7. Все элементы электросетей выполнены с учетом ГОСТ Р 50462-92 (цветовая идентификация жил).", "8. Вся электрическая сеть рассчитана на длительно допустимую нагрузку и проверена по потере напряжения.", "9. Соединение жил в ответвительных коробках методом скрутки не допускается; рекомендуется клеммниками WAGO.", "10. Весь монтаж должен быть выполнен в соответствии с ПУЭ и СП 76.13330.2011.").forEach { cA.drawText(it, M, yA, tpA); yA += 24f }
        yA += 12f
        cA.drawText("ВЕДОМОСТЬ ССЫЛОЧНЫХ ДОКУМЕНТОВ:", M, yA, tpA); yA += 24f
        listOf("ПУЭ  Правила устройства электроустановок. - 7-е изд. - М., 2002.", "СП 31-110-2003  Электрооборудование жилых и общественных зданий. Нормы проектирования", "СП 76.13330.2011  Электротехнические устройства", "СП 52.13330.2010  Естественное и искусственное освещение. Нормы проектирования", "ГОСТ Р 50571.1-2009  Электроустановки зданий", "ГОСТ Р 50571.5.52-2011  Выбор и монтаж электрооборудования", "ГОСТ Р 50462-2009  Идентификация проводников посредством цветов и буквенно-цифровых обозначений").forEach { cA.drawText(it, M, yA, tpA); yA += 24f }
        drawStamp(cA, W, H, M, org, doc, "Лист 1.1   Общие данные", projectName, authorSuffix, totalSheets, aut)
        document.finishPage(pA)

        // === 2.1 ОДНОЛИНЕЙНАЯ ===
        val pB = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 2).create())
        OneLineDiagram.render(pB.canvas, projectName, groups, pw, ph, totalSheets, aut)
        document.finishPage(pB)

        // === 3.1 ОСВЕЩЕНИЕ ===
        val pC = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 3).create()); val cC = pC.canvas
        drawFrame(cC, M, W, H)
        cC.drawText("ПЛАН ОСВЕЩЕНИЯ — $projectName", M, M + 5f, titlePaint())
        drawWalls(cC, walls, tx, ty, scale)
        drawTracks(cC, tracks, trackSys, "light", tx, ty, mono)
        drawObjects(cC, objects, walls, tx, ty, mono, "light", trackSys, tracks) { o -> o.type.contains("lamp") || o.type.contains("switch") || o.type.contains("panel") }
        drawSwitchLampLines(cC, objects, tx, ty)
        drawRoomLabels(cC, rooms, tx, ty)
        drawChains(cC, walls, tx, ty, minX, maxX, minY, maxY)
        drawLegend(cC, M, H, mono, listOf("Освещение" to "lamp_lust", "Выключатели" to "switch_1"))
        drawStamp(cC, W, H, M, org, doc, "Лист 3.1   План освещения", projectName, authorSuffix, totalSheets, aut)
        document.finishPage(pC)

        // === 3.2 РОЗЕТКИ ===
        val pD = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 4).create()); val cD = pD.canvas
        drawFrame(cD, M, W, H)
        cD.drawText("ПЛАН РОЗЕТОК И СИЛОВОГО ОБОРУДОВАНИЯ — $projectName", M, M + 5f, titlePaint())
        drawWalls(cD, walls, tx, ty, scale)
        drawTracks(cD, tracks, trackSys, "socket", tx, ty, mono)
        drawObjects(cD, objects, walls, tx, ty, mono, "socket", trackSys, tracks) { o -> o.type.contains("socket") || o.type.contains("panel") || o.type.contains("cons") }
        drawPrivyazki(cD, objects, walls, tx, ty)
        rooms.firstOrNull { it.name == "Санузел" }?.let { r ->
            val dp = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1.5f }
            cD.drawRect(tx(r.cx) - 17f, ty(r.cy) - 11f, tx(r.cx) + 17f, ty(r.cy) + 11f, dp)
            cD.drawText("ДСУП", tx(r.cx) - 14f, ty(r.cy) + 4f, Paint().apply { color = Color.DKGRAY; textSize = 9f })
            cD.drawText("(лист 5.1)", tx(r.cx) - 17f, ty(r.cy) + 22f, Paint().apply { color = Color.DKGRAY; textSize = 8f })
        }
        drawRoomLabels(cD, rooms, tx, ty)
        drawChains(cD, walls, tx, ty, minX, maxX, minY, maxY)
        drawLegend(cD, M, H, mono, listOf("Розетки 220В" to "socket_b1", "Силовое оборудование" to "panel_shr"))
        drawStamp(cD, W, H, M, org, doc, "Лист 3.2   План розеток", projectName, authorSuffix, totalSheets, aut)
        document.finishPage(pD)

        // === 3.3 СЛАБОТОЧКА ===
        val pE = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 5).create()); val cE = pE.canvas
        drawFrame(cE, M, W, H)
        cE.drawText("ПЛАН СЛАБОТОЧНЫХ СЕТЕЙ — $projectName", M, M + 5f, titlePaint())
        drawWalls(cE, walls, tx, ty, scale)
        drawTracks(cE, tracks, trackSys, "weak", tx, ty, mono)
        drawObjects(cE, objects, walls, tx, ty, mono, "weak", trackSys, tracks) { o -> o.type.contains("sks") || o.type.contains("tv") || o.type.contains("rj45") || o.type.contains("panel") }
        drawLegend(cE, M, H, mono, listOf("Слаботочка (ТВ/Интернет)" to "sks_tv"))
        drawStamp(cE, W, H, M, org, doc, "Лист 3.3   Слаботочка", projectName, authorSuffix, totalSheets, aut)
        document.finishPage(pE)

        // === 3.4 ВЕДОМОСТИ ===
        val pF = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 6).create()); val cF = pF.canvas
        drawFrame(cF, M, W, H)
        cF.drawText("ВЕДОМОСТЬ ВЫКЛЮЧАТЕЛЕЙ; ЭКСПЛИКАЦИЯ ПОМЕЩЕНИЙ — $projectName", M, M + 8f, titlePaint().apply { textSize = 20f })
        drawVedomosti(cF, M, W, H, objects, tracks, trackSys, rooms, org, doc, projectName, authorSuffix, totalSheets, aut)
        document.finishPage(pF)

        // === 4.1 СПЕЦИФИКАЦИЯ ===
        val pG = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 7).create()); val cG = pG.canvas
        drawFrame(cG, M, W, H)
        cG.drawText("СПЕЦИФИКАЦИЯ ОБОРУДОВАНИЯ И МАТЕРИАЛОВ — $projectName", M, M + 8f, titlePaint().apply { textSize = 20f })
        drawSpec(cG, M, W, H, objects, groups, org, doc, projectName, authorSuffix, totalSheets, aut)
        document.finishPage(pG)

        // === 5.1 ЗАЗЕМЛЕНИЕ ===
        val pH = document.startPage(PdfDocument.PageInfo.Builder(pw, ph, 8).create()); val cH = pH.canvas
        drawFrame(cH, M, W, H)
        cH.drawText("ЗАЗЕМЛЕНИЕ И СИСТЕМА ДОПОЛНИТЕЛЬНОГО УРАВНИВАНИЯ ПОТЕНЦИАЛОВ — $projectName", M, M + 8f, titlePaint().apply { textSize = 20f })
        drawGrounding(cH, M, W, H, org, doc, projectName, authorSuffix, totalSheets, aut)
        document.finishPage(pH)

        val file = File(context.cacheDir, "GdeSanek_$projectId.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun titlePaint() = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }

    private fun drawFrame(c: Canvas, M: Float, W: Float, H: Float) {
        c.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f })
    }

    private fun drawWalls(c: Canvas, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float, scale: Float) {
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

    private fun drawTracks(c: Canvas, tracks: List<CableTrack>, trackSys: List<String>, sys: String, tx: (Float) -> Float, ty: (Float) -> Float, mono: Boolean) {
        val tp = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f) }
        val lbl = Paint().apply { textSize = 13f; isFakeBoldText = true }
        for ((idx, t) in tracks.withIndex()) {
            if (t.points.isEmpty() || trackSys.getOrElse(idx) { "" } != sys) continue
            tp.color = if (mono) Color.DKGRAY else t.color; lbl.color = tp.color
            val path = Path(); path.moveTo(tx(t.points[0].x), ty(t.points[0].y))
            for (i in 1 until t.points.size) path.lineTo(tx(t.points[i].x), ty(t.points[i].y))
            c.drawPath(path, tp)
            val mid = t.points[t.points.size / 2]
            c.drawText("Гр.${idx + 1} ВВГнг-LS " + t.cable, tx(mid.x) + 6f, ty(mid.y) - 10f, lbl)
        }
    }

    private fun drawObjects(c: Canvas, objects: List<PlanObject>, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float, mono: Boolean, sys: String, trackSys: List<String>, tracks: List<CableTrack>, filter: (PlanObject) -> Boolean) {
        val sp = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3f }
        val lp = Paint().apply { textSize = 12f }
        val np = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        val mp = Paint().apply { textSize = 9f; color = Color.GRAY }
        var oi = 0
        for (o in objects) {
            if (!filter(o)) continue
            val isPanel = o.type.contains("panel")
            val col = if (mono) Color.BLACK else CategoryPalette.color(o.type)
            sp.color = col; lp.color = col
            if (ArchTypes.isArch(o.type)) {
                var th = 100f
                for (wl in walls) { if (distToSeg(o.x, o.y, wl) < 30f) th = wl.thickness }
                GostSymbols.draw(c, o.type, tx(o.x), ty(o.y), o.rotation, sp, th / 10f)
            } else GostSymbols.draw(c, o.type, tx(o.x), ty(o.y), o.rotation, sp)
            val hh = if (o.height >= 0) o.height else (SymbolPalette.height(o.type) ?: -1)
            if (hh >= 0) c.drawText("h=" + hh, tx(o.x) + 8f, ty(o.y) - 8f, lp)
            SymbolPalette.power(o.type)?.let { w -> c.drawText(w.toString() + " Вт", tx(o.x) + 8f, ty(o.y) + 16f, lp) }
            if (o.name.isNotBlank()) c.drawText(o.name.take(28), tx(o.x) + 8f, ty(o.y) + 30f + (oi % 2) * 14f, np)
            if (!isPanel && !ArchTypes.isArch(o.type)) {
                val gi = nearestTrackIdx(o, tracks, trackSys, sys)
                if (gi >= 0) c.drawText("гр.${gi + 1}", tx(o.x) - 26f, ty(o.y) - 8f, mp)
            }
            oi++
        }
    }

    private fun nearestTrackIdx(o: PlanObject, tracks: List<CableTrack>, trackSys: List<String>, sys: String): Int {
        var best = -1; var bd = 300f
        for (ti in tracks.indices) {
            if (trackSys.getOrElse(ti) { "" } != sys || tracks[ti].points.isEmpty()) continue
            val d = distToTrackPts(o.x, o.y, tracks[ti])
            if (d < bd) { bd = d; best = ti }
        }
        return best
    }

    private fun drawSwitchLampLines(c: Canvas, objects: List<PlanObject>, tx: (Float) -> Float, ty: (Float) -> Float) {
        val p = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f) }
        val lamps = objects.filter { it.type.contains("lamp") }
        for (sw in objects.filter { it.type.contains("switch") }) {
            var bl: PlanObject? = null; var bd = 600f
            for (l in lamps) { val d = kotlin.math.sqrt((l.x - sw.x) * (l.x - sw.x) + (l.y - sw.y) * (l.y - sw.y)); if (d < bd) { bd = d; bl = l } }
            bl?.let { c.drawLine(tx(sw.x), ty(sw.y), tx(it.x), ty(it.y), p) }
        }
    }

    private fun drawRoomLabels(c: Canvas, rooms: List<Room>, tx: (Float) -> Float, ty: (Float) -> Float) {
        val rp = Paint().apply { color = 0xFFB0BEC5.toInt(); textSize = 11f; isFakeBoldText = true }
        for (r in rooms) { c.drawText(r.name, tx(r.cx) - 20f, ty(r.cy), rp); c.drawText(String.format("%.1f м²", r.area), tx(r.cx) - 16f, ty(r.cy) + 14f, rp) }
    }

    private fun drawPrivyazki(c: Canvas, objects: List<PlanObject>, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float) {
        val p = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f; textSize = 9f }
        for (o in objects) {
            if (!o.type.contains("socket")) continue
            var bestW: Wall? = null; var bestD = 1e9f
            for (w in walls) { val d = distToSeg(o.x, o.y, w); if (d < bestD) { bestD = d; bestW = w } }
            val w = bestW ?: continue
            val dx = w.x2 - w.x1; val dy = w.y2 - w.y1; val len2 = dx * dx + dy * dy
            val t = if (len2 > 0f) ((o.x - w.x1) * dx + (o.y - w.y1) * dy) / len2 else 0f
            val tc = t.coerceIn(0f, 1f); val fx = w.x1 + tc * dx; val fy = w.y1 + tc * dy
            c.drawLine(tx(fx), ty(fy), tx(o.x), ty(o.y), p)
            c.drawText((bestD * 10).toInt().toString(), tx(fx) + 6f, ty(fy) - 4f, p)
        }
    }

    private fun drawChains(c: Canvas, walls: List<Wall>, tx: (Float) -> Float, ty: (Float) -> Float, minX: Float, maxX: Float, minY: Float, maxY: Float) {
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
            c.drawLine(a - 4f, yChain + 4f, a + 4f, yChain - 4f, dp); c.drawLine(b - 4f, yChain + 4f, b + 4f, yChain - 4f, dp)
            c.drawText(((xl[i + 1] - xl[i]) * 10).toInt().toString(), (a + b) / 2f - 12f, yChain - 4f, dp)
        }
        val xChain = tx(minX) - 40f
        for (i in 0 until yl.size - 1) {
            val a = ty(yl[i]); val b = ty(yl[i + 1])
            if (b - a < 8f) continue
            c.drawLine(xChain, a, xChain, b, dp)
            c.drawLine(xChain - 4f, a - 4f, xChain + 4f, a + 4f, dp); c.drawLine(xChain - 4f, b - 4f, xChain + 4f, b + 4f, dp)
            c.drawText(((yl[i + 1] - yl[i]) * 10).toInt().toString(), xChain - 32f, (a + b) / 2f, dp)
        }
    }

    private fun drawLegend(c: Canvas, M: Float, H: Float, mono: Boolean, items: List<Pair<String, String>>) {
        val lp = Paint().apply { textSize = 13f }
        var lx = M; val ly = H - M - 10f
        for (it in items) {
            lp.color = if (mono) Color.BLACK else CategoryPalette.color(it.second)
            c.drawLine(lx, ly - 4f, lx + 22f, ly - 4f, lp); c.drawText(it.first, lx + 28f, ly, lp)
            lx += 28f + lp.measureText(it.first) + 30f
        }
    }

    private fun drawStamp(c: Canvas, W: Float, H: Float, M: Float, org: String, doc: String, sheet: String, projectName: String, authorSuffix: String, totalSheets: Int, author: String) {
        val sw = 320f; val sh = 90f; val sx = W - M - sw; val sy = H - M - sh
        val sp = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        c.drawRect(sx, sy, sx + sw, sy + sh, sp)
        c.drawLine(sx, sy + 30f, sx + sw, sy + 30f, sp); c.drawLine(sx, sy + 60f, sx + sw, sy + 60f, sp)
        val st = Paint().apply { color = Color.BLACK; textSize = 13f }
        c.drawText(org, sx + 6f, sy + 20f, st)
        c.drawText("$doc   $sheet", sx + 6f, sy + 50f, st)
        c.drawText(projectName + authorSuffix, sx + 6f, sy + 80f, st)
        c.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sx + sw - 115f, sy + 80f, st)
        c.drawText("Листов $totalSheets", sx + sw - 70f, sy + 18f, st)
        c.drawText("Формат А4", sx + sw - 70f, sy + 45f, st)
        val sig = Paint().apply { color = Color.BLACK; textSize = 11f }
        c.drawText("Разраб.: " + author.ifBlank { "______" } + "   Пров.: ______   Н.контр.: ______", sx, sy - 26f, sig)
        c.drawText("Дата: ______", sx + sw - 90f, sy - 10f, sig)
    }

    private fun drawVedomosti(c: Canvas, M: Float, W: Float, H: Float, objects: List<PlanObject>, tracks: List<CableTrack>, trackSys: List<String>, rooms: List<Room>, org: String, doc: String, projectName: String, authorSuffix: String, totalSheets: Int, author: String) {
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
            listOf("В${si + 1}", swObj.name.take(30), keys, if (bestTr >= 0) "Гр.${bestTr + 1}" else "—").forEachIndexed { i, v -> c.drawText(v, swCols[i] + 4f, yy + 17f, tp); c.drawLine(swCols[i], yy, swCols[i], yy + 24f, cp) }
            yy += 24f
        }
        yy += 30f
        c.drawText("Экспликация помещений", M, yy, tb); yy += 24f
        val rmCols = floatArrayOf(M, M + 60f, M + 360f, M + 520f)
        c.drawRect(rmCols[0], yy, rmCols[3], yy + 24f, cp)
        listOf("№", "Наименование", "Площадь, м²").forEachIndexed { i, h -> c.drawText(h, rmCols[i] + 4f, yy + 17f, tb); c.drawLine(rmCols[i], yy, rmCols[i], yy + 24f, cp) }
        yy += 24f
        rooms.sortedByDescending { it.area }.forEachIndexed { i, r ->
            c.drawRect(rmCols[0], yy, rmCols[3], yy + 24f, cp)
            listOf("${i + 1}", r.name, String.format("%.1f", r.area)).forEachIndexed { vi, v -> c.drawText(v, rmCols[vi] + 4f, yy + 17f, tp); c.drawLine(rmCols[vi], yy, rmCols[vi], yy + 24f, cp) }
            yy += 24f
        }
        c.drawText("Итого площадь: " + String.format("%.1f", rooms.sumOf { it.area.toDouble() }.toFloat()) + " м²", M, yy + 20f, tb)
        drawStamp(c, W, H, M, org, doc, "Лист 3.4   Ведомости", projectName, authorSuffix, totalSheets, author)
    }

    private fun drawSpec(c: Canvas, M: Float, W: Float, H: Float, objects: List<PlanObject>, groups: List<OneLineDiagram.Group>, org: String, doc: String, projectName: String, authorSuffix: String, totalSheets: Int, author: String) {
        val t5 = Paint().apply { color = Color.BLACK; textSize = 12f }
        val tb5 = Paint().apply { color = Color.BLACK; textSize = 12f; isFakeBoldText = true }
        val cp5 = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val cableMap = mutableMapOf<String, Int>()
        for (g in groups) cableMap[g.cable] = (cableMap[g.cable] ?: 0) + (g.length * 1.15f).toInt() + 1
        val isEl = { o: PlanObject -> !ArchTypes.isArch(o.type) && !ArchTypes.isFurn(o.type) }
        val nSockAll = objects.count { isEl(it) && it.type.contains("socket") }
        val nIp44 = objects.count { isEl(it) && it.type.contains("socket") && (it.name.lowercase().contains("ip44") || it.type.lowercase().contains("ip44")) }
        val nSock = nSockAll - nIp44
        val nSwAll = objects.count { it.type.contains("switch") }
        val nSw2 = objects.count { it.type.contains("switch") && it.type.contains("2") }
        val nSw = nSwAll - nSw2
        val nLamp = objects.count { it.type.contains("lamp") }
        val nWeak = objects.count { it.type.contains("sks") || it.type.contains("tv") || it.type.contains("rj45") }
        val rows = mutableListOf<List<String>>()
        var pos = 1
        rows.add(listOf("", "Раздел А. Кабели и проводники", "", "", ""))
        for ((cab, len) in cableMap) { rows.add(listOf("$pos", "Кабель ВВГнг-LS $cab", "м", "$len", "с запасом 15%")); pos++ }
        rows.add(listOf("", "Раздел Б. Коробки и комплектующие", "", "", ""))
        rows.add(listOf("$pos", "Коробка установочная (подрозетник)", "шт", "$nSockAll", "по числу розеток")); pos++
        rows.add(listOf("$pos", "Коробка распаячная 80x80", "шт", "${groups.size + nSwAll}", "")); pos++
        rows.add(listOf("$pos", "Клемма WAGO 221-413", "шт", "${(groups.size + nSwAll) * 3}", "3 шт на коробку")); pos++
        rows.add(listOf("$pos", "Труба гофр. ПВХ д20", "м", "${cableMap.values.sum() / 2}", "открытая прокладка")); pos++
        rows.add(listOf("", "Раздел В. Щит ЩР-1", "", "", ""))
        rows.add(listOf("$pos", "Щит навесной ЩРН-П-12", "шт", "1", "")); pos++
        rows.add(listOf("$pos", "Автомат вводной C25/1", "шт", "1", "")); pos++
        rows.add(listOf("$pos", "УЗО 40/2 300мА", "шт", "1", "противопожарное")); pos++
        val brMap = mutableMapOf<String, Int>()
        for (g in groups) brMap[g.breaker] = (brMap[g.breaker] ?: 0) + 1
        for ((br, cnt) in brMap) { rows.add(listOf("$pos", "Автомат $br", "шт", "$cnt", "групповые")); pos++ }
        rows.add(listOf("$pos", "УЗО 25/2 30мА", "шт", "${groups.count { it.rcd != "—" }}", "розетки и влажные")); pos++
        rows.add(listOf("", "Раздел Г. Изделия", "", "", ""))
        if (nSock > 0) { rows.add(listOf("$pos", "Розетки 220В накладные/врезные", "шт", "$nSock", "")); pos++ }
        if (nIp44 > 0) { rows.add(listOf("$pos", "Розетка IP44 (влажные помещения)", "шт", "$nIp44", "")); pos++ }
        if (nSw > 0) { rows.add(listOf("$pos", "Выключатель 1-кл", "шт", "$nSw", "")); pos++ }
        if (nSw2 > 0) { rows.add(listOf("$pos", "Выключатель 2-кл", "шт", "$nSw2", "")); pos++ }
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
        drawStamp(c, W, H, M, org, doc, "Лист 4.1   Спецификация", projectName, authorSuffix, totalSheets, author)
    }

    private fun drawGrounding(c: Canvas, M: Float, W: Float, H: Float, org: String, doc: String, projectName: String, authorSuffix: String, totalSheets: Int, author: String) {
        val t6 = Paint().apply { color = Color.BLACK; textSize = 13f }
        val tb6 = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        val ln6 = Paint().apply { color = Color.BLACK; strokeWidth = 2f; style = Paint.Style.STROKE }
        val bx6 = M + 60f; val by6 = M + 70f
        c.drawRect(bx6, by6, bx6 + 150f, by6 + 60f, ln6)
        c.drawText("Щит ЩР-1", bx6 + 10f, by6 + 25f, t6); c.drawText("шина PE (ГЗШ)", bx6 + 10f, by6 + 45f, t6)
        c.drawLine(bx6 + 150f, by6 + 30f, bx6 + 280f, by6 + 30f, ln6)
        c.drawRect(bx6 + 280f, by6 + 10f, bx6 + 400f, by6 + 50f, ln6)
        c.drawText("Коробка КВП", bx6 + 290f, by6 + 35f, t6)
        val trunkX = bx6 + 340f
        c.drawLine(trunkX, by6 + 50f, trunkX, by6 + 330f, ln6)
        listOf("Стояк водоснабжения" to "ПУ 4 мм²", "Ванна / душевой поддон" to "ПУ 2,5 мм² в гофре", "Стиральная машина" to "ПУ 2,5 мм² в гофре", "Вентиляция вытяжная" to "ПУ 2,5 мм² в гофре", "PE розеточных групп санузла" to "ПУ 4 мм²").forEachIndexed { i, pr ->
            val yy = by6 + 110f + i * 55f
            c.drawLine(trunkX, yy, trunkX + 100f, yy, ln6)
            c.drawRect(trunkX + 100f, yy - 20f, trunkX + 380f, yy + 20f, ln6)
            c.drawText(pr.first, trunkX + 110f, yy + 5f, t6); c.drawText(pr.second, trunkX + 10f, yy - 6f, t6)
        }
        val ty6 = by6 + 420f
        c.drawText("Примечания:", M, ty6, tb6)
        listOf("1. Система заземления TN-C-S: PEN-проводник разделен на вводе в щите ЩР-1 на шины PE и N.", "2. Главную заземляющую шину (ГЗШ) установить в щите ЩР-1; к ней подключить PE и PEN проводники.", "3. В санузле выполнить дополнительную систему уравнивания потенциалов (ДСУП) через коробку КВП.", "4. К КВП подключить: стояки воды, ванну, стиральную машину, вентиляцию и PE розеток проводниками ПУ 2,5-4 мм².", "5. Соединения защитных проводников выполнять опрессовкой или болтовым соединением; скрутка и пайка не допускаются.", "6. Для коттеджа: контур заземления из полосы 40x4 мм, 3x5 м, вертикальные электроды 50 м; Rрасч ≤ 30 Ом.").forEachIndexed { i, s6 -> c.drawText(s6, M, ty6 + 24f + i * 22f, t6) }
        drawStamp(c, W, H, M, org, doc, "Лист 5.1   Заземление и ДСУП", projectName, authorSuffix, totalSheets, author)
    }

    private fun computeRooms(walls: List<Wall>, objects: List<PlanObject>, minX: Float, maxX: Float, minY: Float, maxY: Float): List<Room> {
        val cell = 20f
        val gx = ((maxX - minX) / cell).toInt() + 1
        val gy = ((maxY - minY) / cell).toInt() + 1
        if (gx < 2 || gy < 2) return emptyList()
        val blocked = Array(gy) { BooleanArray(gx) }
        val thr = 220f
        for (wl in walls) {
            val wlen = kotlin.math.sqrt((wl.x2 - wl.x1) * (wl.x2 - wl.x1) + (wl.y2 - wl.y1) * (wl.y2 - wl.y1))
            val steps = (wlen / (cell / 2f)).toInt() + 1
            for (si in 0..steps) {
                val x = wl.x1 + (wl.x2 - wl.x1) * si / steps; val y = wl.y1 + (wl.y2 - wl.y1) * si / steps
                val ci = ((x - minX) / cell).toInt(); val ri = ((y - minY) / cell).toInt()
                if (ri in 0 until gy && ci in 0 until gx) blocked[ri][ci] = true
            }
        }
        for (o in objects) {
            if (!o.type.contains("door")) continue
            val ci = ((o.x - minX) / cell).toInt(); val ri = ((o.y - minY) / cell).toInt()
            for (dr in -2..2) for (dc in -2..2) { val r2 = ri + dr; val c2 = ci + dc; if (r2 in 0 until gy && c2 in 0 until gx) blocked[r2][c2] = true }
        }
        for (w in walls) {
            for (p in listOf(w.x1 to w.y1, w.x2 to w.y2)) {
                var bestD = 1e9f; var fx = 0f; var fy = 0f
                for (w2 in walls) {
                    if (w2 === w) continue
                    val dx = w2.x2 - w2.x1; val dy = w2.y2 - w2.y1; val len2 = dx * dx + dy * dy
                    val t = if (len2 > 0f) ((p.first - w2.x1) * dx + (p.second - w2.y1) * dy) / len2 else 0f
                    val tc = t.coerceIn(0f, 1f); val cx = w2.x1 + tc * dx; val cy = w2.y1 + tc * dy
                    val d = kotlin.math.sqrt((p.first - cx) * (p.first - cx) + (p.second - cy) * (p.second - cy))
                    if (d < bestD) { bestD = d; fx = cx; fy = cy }
                }
                if (bestD > 1f && bestD < thr) {
                    val steps = (bestD / (cell / 2f)).toInt() + 1
                    for (si in 0..steps) {
                        val x = p.first + (fx - p.first) * si / steps; val y = p.second + (fy - p.second) * si / steps
                        val ci = ((x - minX) / cell).toInt(); val ri = ((y - minY) / cell).toInt()
                        if (ri in 0 until gy && ci in 0 until gx) blocked[ri][ci] = true
                    }
                }
            }
        }
        val roomIds = Array(gy) { IntArray(gx) { -1 } }
        val cnt = mutableListOf<Int>(); val sx = mutableListOf<Float>(); val sy = mutableListOf<Float>()
        var rc = 0
        for (r in 0 until gy) for (cc in 0 until gx) {
            if (blocked[r][cc] || roomIds[r][cc] >= 0) continue
            val id = rc++; var n = 0; var ax = 0f; var ay = 0f
            val q = java.util.ArrayDeque<IntArray>(); q.add(intArrayOf(r, cc)); roomIds[r][cc] = id
            while (q.isNotEmpty()) {
                val cur = q.poll(); n++; ax += cur[1]; ay += cur[0]
                for (d in 0..3) {
                    val dr = if (d == 0) 1 else if (d == 1) -1 else 0
                    val dc = if (d == 2) 1 else if (d == 3) -1 else 0
                    val r2 = cur[0] + dr; val c2 = cur[1] + dc
                    if (r2 in 0 until gy && c2 in 0 until gx && !blocked[r2][c2] && roomIds[r2][c2] < 0) { roomIds[r2][c2] = id; q.add(intArrayOf(r2, c2)) }
                }
            }
            cnt.add(n); sx.add(ax / n); sy.add(ay / n)
        }
        val out = mutableListOf<Room>()
        for (id in 0 until rc) {
            if (cnt[id] < 40) continue
            val cxp = minX + sx[id] * cell; val cyp = minY + sy[id] * cell
            out.add(Room(roomName(cxp, cyp, objects, roomIds, id, minX, minY, cell), cnt[id] * 0.04f, cxp, cyp))
        }
        return out
    }

    private fun roomName(cx: Float, cy: Float, objects: List<PlanObject>, roomIds: Array<IntArray>, id: Int, minX: Float, minY: Float, cell: Float): String {
        var kitchen = 0; var living = 0; var bath = 0; var bed = 0; var hall = 0
        for (o in objects) {
            val ci = ((o.x - minX) / cell).toInt(); val ri = ((o.y - minY) / cell).toInt()
            if (ri !in roomIds.indices || ci !in roomIds[0].indices || roomIds[ri][ci] != id) continue
            val t = o.type.lowercase(); val n = o.name.lowercase()
            if (t.contains("sofa") || n.contains("диван")) living += 3
            if (t.contains("bed") || n.contains("кровать")) bed += 3
            if (t.contains("toilet") || t.contains("bath") || n.contains("унитаз") || n.contains("ванна")) bath += 3
            if (t.contains("wardrobe") || n.contains("шкаф")) hall += 2
            when {
                t.contains("плит") || t.contains("вытяж") || n.contains("кухн") -> kitchen++
                t.contains("диван") || t.contains("тв") || n.contains("гостин") -> living++
                t.contains("унитаз") || t.contains("ванн") || t.contains("стир") || n.contains("санузел") || n.contains("стирал") -> bath++
                t.contains("кроват") || n.contains("спальн") -> bed++
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
        val dx = wl.x2 - wl.x1; val dy = wl.y2 - wl.y1; val len2 = dx * dx + dy * dy
        val t = if (len2 > 0f) ((px - wl.x1) * dx + (py - wl.y1) * dy) / len2 else 0f
        val tc = t.coerceIn(0f, 1f); val cx = wl.x1 + tc * dx; val cy = wl.y1 + tc * dy
        return kotlin.math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
    }

    private fun distToTrackPts(px: Float, py: Float, tr: CableTrack): Float {
        if (tr.points.size < 2) return 1e9f
        var min = 1e9f
        for (i in 1 until tr.points.size) {
            val a = tr.points[i - 1]; val b = tr.points[i]
            val dx = b.x - a.x; val dy = b.y - a.y; val len2 = dx * dx + dy * dy
            val t = if (len2 > 0f) ((px - a.x) * dx + (py - a.y) * dy) / len2 else 0f
            val tc = t.coerceIn(0f, 1f); val cx = a.x + tc * dx; val cy = a.y + tc * dy
            val d = kotlin.math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
            if (d < min) min = d
        }
        return min
    }
}
