package ru.gdesanek.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import ru.gdesanek.theme.SymbolPalette

object OneLineDiagram {

    data class Group(
        val num: String, val name: String, val pKw: Float, val length: Int,
        val cable: String, val breaker: String, val rcd: String, val phase: String
    )

    fun buildGroups(tracks: List<CableTrack>, objects: List<PlanObject>): List<Group> {
        if (tracks.isEmpty()) return emptyList()
        val assign = mutableMapOf<Long, Int>()
        for (o in objects) {
            if (ru.gdesanek.core.ArchTypes.isArch(o.type) || ru.gdesanek.core.ArchTypes.isFurn(o.type)) continue
            var best = -1; var bd = 300f
            for (ti in tracks.indices) { val d = distToTrack(o.x, o.y, tracks[ti]); if (d < bd) { bd = d; best = ti } }
            if (best >= 0) assign[o.id] = best
        }
        val powers = FloatArray(tracks.size); val lights = IntArray(tracks.size); val socks = IntArray(tracks.size); val weaks = IntArray(tracks.size)
        val mine = Array(tracks.size) { mutableListOf<PlanObject>() }
        for (o in objects) {
            val idx = assign[o.id] ?: continue
            mine[idx].add(o)
            powers[idx] += SymbolPalette.power(o.type)?.toFloat() ?: 0f
            val t = o.type.lowercase()
            if (t.contains("sks") || t.contains("tv") || t.contains("rj45")) weaks[idx]++
            else if (t.contains("lamp") || t.contains("switch")) lights[idx]++
            else socks[idx]++
        }
        val totalKw = powers.sum() / 1000f
        val threePhase = totalKw > 5f
        val result = mutableListOf<Group>()
        tracks.forEachIndexed { idx, tr ->
            val sorted = mine[idx].sortedByDescending { SymbolPalette.power(it.type)?.toFloat() ?: 0f }
            val names = sorted.mapNotNull { it.name.takeIf { n -> n.isNotBlank() } }.distinct().take(2)
            val name = if (names.isEmpty()) "Группа ${idx + 1}" else names.joinToString(", ") + (if (sorted.size > 2) " и др." else "")
            val pKw = powers[idx] / 1000f
            val ip = if (pKw > 0f) pKw * 0.8f * 1000f / 220f else 1f
            val hasSocket = socks[idx] > 0; val hasLight = lights[idx] > 0; val hasWeak = weaks[idx] > 0
            val breaker = when {
                ip <= 6f -> "C10/1"
                ip <= 10f -> if (hasSocket) "C16/1" else "C10/1"
                ip <= 16f -> "C16/1"
                ip <= 25f -> "C25/1"
                else -> "C32/1"
            }
            val cable = if (hasLight && !hasSocket) "3x1.5" else tr.cable.ifBlank { "3x2.5" }
            val wet = mine[idx].any { val n = it.name.lowercase(); n.contains("стир") || n.contains("сан") || n.contains("ван") }
            val rcd = if (hasSocket || hasWeak || wet) "УЗО 25/2 30мА" else "—"
            val suffix = when { hasSocket && !hasLight -> "с"; hasLight && !hasSocket -> "о"; else -> "" }
            var len = 0f
            for (i in 1 until tr.points.size) { val dx = tr.points[i].x - tr.points[i - 1].x; val dy = tr.points[i].y - tr.points[i - 1].y; len += kotlin.math.sqrt(dx * dx + dy * dy) }
            result.add(Group("Гр.${idx + 1}$suffix", name.take(40), pKw, (len / 100f).toInt().coerceAtLeast(5), cable, breaker, rcd, if (threePhase) "L${(idx % 3) + 1}" else "L1"))
        }
        return result
    }

    fun trackSystems(tracks: List<CableTrack>, objects: List<PlanObject>): List<String> {
        val assign = mutableMapOf<Long, Int>()
        for (o in objects) {
            if (ru.gdesanek.core.ArchTypes.isArch(o.type) || ru.gdesanek.core.ArchTypes.isFurn(o.type)) continue
            var best = -1; var bd = 300f
            for (ti in tracks.indices) { val d = distToTrack(o.x, o.y, tracks[ti]); if (d < bd) { bd = d; best = ti } }
            if (best >= 0) assign[o.id] = best
        }
        return tracks.indices.map { idx ->
            var light = 0; var sock = 0; var weak = 0
            for (o in objects) {
                if (assign[o.id] != idx) continue
                val t = o.type.lowercase()
                if (t.contains("sks") || t.contains("tv") || t.contains("rj45")) weak++
                else if (t.contains("lamp") || t.contains("switch")) light++
                else sock++
            }
            when { weak > 0 && weak >= light && weak >= sock -> "weak"; light > 0 && light >= sock -> "light"; else -> "socket" }
        }
    }

    private fun distToTrack(px: Float, py: Float, tr: CableTrack): Float {
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

    fun render(canvas: Canvas, projectName: String, groups: List<Group>, pw: Int, ph: Int, totalSheets: Int = 8, author: String = "") {
        val M = 40f; val W = pw.toFloat(); val H = ph.toFloat()
        val frame = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, frame)
        val h2 = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        canvas.drawText("СХЕМА ЭЛЕКТРИЧЕСКАЯ ОДНОЛИНЕЙНАЯ ЩИТА ЩР-1", M, M + 8f, h2)
        val totalKw = groups.map { it.pKw.toDouble() }.sum().toFloat()
        val pRas = totalKw * 0.8f; val ipRas = pRas * 1000f / 220f
        val threePhase = groups.any { it.phase != "L1" }
        val volt = if (threePhase) "~380/220В" else "~220В"
        val meter = if (threePhase) "Меркурий 231" else "Меркурий 201"
        val mainRcd = if (threePhase) "УЗО 40/4" else "УЗО 40/2"
        val inputCab = if (threePhase) "ВВГнг-LS 5x4" else "ВВГнг-LS 3x4"
        val boxX = M + 30f; val boxY = M + 60f; val boxW = W - M * 2 - 60f; val boxH = 200f
        val bx = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(boxX, boxY, boxX + boxW, boxY + boxH, bx)
        val t = Paint().apply { color = Color.BLACK; textSize = 14f }
        val tb = Paint().apply { color = Color.BLACK; textSize = 14f; isFakeBoldText = true }
        canvas.drawText("ЩР-1  $volt  ${"%.2f".format(totalKw)} кВт  Кс=0.8  Pр=${"%.2f".format(pRas)} кВт  Iр=${"%.1f".format(ipRas)} А", boxX + 12f, boxY + 22f, tb)
        val inX = boxX + 20f; val inY = boxY + 50f
        val mainBreaker = if (ipRas <= 25f) "C25/1" else if (ipRas <= 40f) "C40/1" else "C63/1"
        canvas.drawRect(inX, inY, inX + 60f, inY + 40f, bx)
        canvas.drawText("QF", inX + 8f, inY + 16f, t); canvas.drawText(mainBreaker, inX + 8f, inY + 32f, t)
        val meterX = inX + 90f
        canvas.drawRect(meterX, inY, meterX + 120f, inY + 40f, bx)
        canvas.drawText("Счётчик", meterX + 6f, inY + 16f, t); canvas.drawText(meter, meterX + 6f, inY + 32f, t)
        val uzoX = meterX + 140f
        canvas.drawRect(uzoX, inY, uzoX + 100f, inY + 40f, bx)
        canvas.drawText(mainRcd, uzoX + 6f, inY + 16f, t); canvas.drawText("300 мА", uzoX + 6f, inY + 32f, t)
        val busY = inY + 80f
        val bus = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        val busLen = boxW - 60f
        canvas.drawLine(boxX + 30f, busY, boxX + 30f + busLen, busY, bus); canvas.drawText("L", boxX + 36f, busY - 6f, t)
        canvas.drawLine(boxX + 30f, busY + 20f, boxX + 30f + busLen, busY + 20f, bus); canvas.drawText("N", boxX + 36f, busY + 14f, t)
        canvas.drawLine(boxX + 30f, busY + 40f, boxX + 30f + busLen, busY + 40f, bus); canvas.drawText("PE", boxX + 36f, busY + 34f, t)
        val gndX = boxX + 50f
        canvas.drawLine(gndX, busY + 40f, gndX, busY + 60f, bus)
        canvas.drawLine(gndX - 12f, busY + 60f, gndX + 12f, busY + 60f, bus)
        canvas.drawLine(gndX - 8f, busY + 65f, gndX + 8f, busY + 65f, bus)
        canvas.drawLine(gndX - 4f, busY + 70f, gndX + 4f, busY + 70f, bus)
        canvas.drawText("Ввод: $inputCab от ВРУ", boxX + 12f, boxY + boxH + 20f, t)
        val tableTop = boxY + boxH + 50f; val rowH = 26f
        val cols = floatArrayOf(M + 10f, M + 90f, M + 420f, M + 500f, M + 570f, M + 640f, M + 790f, M + 890f, M + 1020f, W - M - 10f)
        val hdr = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        val cell = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val tp = Paint().apply { color = Color.BLACK; textSize = 12f }
        val headers = listOf("№ гр.", "Наименование", "Pуст,кВт", "Ip,А", "L,м", "Кабель", "Автомат", "УЗО", "Фаза")
        canvas.drawRect(M, tableTop, W - M, tableTop + rowH, cell)
        for (i in 0 until cols.size - 1) { canvas.drawLine(cols[i], tableTop, cols[i], tableTop + rowH, cell); canvas.drawText(headers[i], cols[i] + 4f, tableTop + 18f, hdr) }
        val show = groups.take(14)
        show.forEachIndexed { idx, g ->
            val y = tableTop + rowH * (idx + 1)
            canvas.drawRect(M, y, W - M, y + rowH, cell)
            for (i in 0 until cols.size - 1) canvas.drawLine(cols[i], y, cols[i], y + rowH, cell)
            canvas.drawText(g.num, cols[0] + 4f, y + 18f, tp); canvas.drawText(g.name, cols[1] + 4f, y + 18f, tp)
            canvas.drawText("%.2f".format(g.pKw), cols[2] + 4f, y + 18f, tp); canvas.drawText("%.1f".format(g.pKw * 0.8f * 1000f / 220f), cols[3] + 4f, y + 18f, tp)
            canvas.drawText(g.length.toString(), cols[4] + 4f, y + 18f, tp); canvas.drawText(g.cable, cols[5] + 4f, y + 18f, tp)
            canvas.drawText(g.breaker, cols[6] + 4f, y + 18f, tp); canvas.drawText(g.rcd, cols[7] + 4f, y + 18f, tp); canvas.drawText(g.phase, cols[8] + 4f, y + 18f, tp)
        }
        val xT = cols[6] - 20f
        canvas.drawLine(xT, boxY + boxH, xT, tableTop + rowH * (show.size + 1), bx)
        show.forEachIndexed { idx, g ->
            val ym = tableTop + rowH * (idx + 1) + rowH / 2f
            canvas.drawLine(xT, ym, cols[6], ym, bx)
            canvas.drawRect(xT + 4f, ym - 9f, xT + 14f, ym + 9f, bx)
        }
        val sumY = tableTop + rowH * (show.size + 1) + 6f
        val sumP = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        canvas.drawText("Итого по щиту: Pуст=${"%.2f".format(totalKw)} кВт, Pр=${"%.2f".format(pRas)} кВт, Iр=${"%.1f".format(ipRas)} А, групп: ${groups.size}", M, sumY + 14f, sumP)
        val noteY = sumY + 40f
        listOf("Примечания:", "1. Степень защиты щита ЩР-1 не ниже IP31.", "2. Вводной автоматический выключатель опломбировать.", "3. Группы с УЗО 30мА — розеточные и влажные помещения (ПУЭ 7.1.71).", "4. Соединение жил в расп. коробках — клеммниками WAGO, скрутка запрещена.", "5. Все розетки — с защитным контактом, двухполюсные (ПУЭ 7.1.49).")
            .forEachIndexed { idx, s2 -> canvas.drawText(s2, M, noteY + idx * 18f, Paint().apply { color = Color.BLACK; textSize = 12f }) }
        val sw = 360f; val sh = 80f; val sx = W - M - sw; val sy = H - M - sh
        val sp = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas.drawRect(sx, sy, sx + sw, sy + sh, sp)
        canvas.drawLine(sx, sy + 25f, sx + sw, sy + 25f, sp); canvas.drawLine(sx, sy + 50f, sx + sw, sy + 50f, sp)
        val st = Paint().apply { color = Color.BLACK; textSize = 13f }
        canvas.drawText("ГдеСанёк", sx + 6f, sy + 18f, st)
        canvas.drawText("ЭОМ   Лист 2.1   Схема однолинейная", sx + 6f, sy + 42f, st)
        canvas.drawText(projectName + (if (author.isNotBlank()) "   Разраб. $author" else ""), sx + 6f, sy + 70f, st)
        canvas.drawText("Листов $totalSheets", sx + sw - 70f, sy + 18f, st)
        canvas.drawText("Формат А4", sx + sw - 70f, sy + 42f, st)
    }
}
