package ru.gdesanek.export

import android.graphics.Color
import android.graphics.Paint
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject

object OneLineDiagram {

    data class Group(
        val num: String, val name: String, val pKw: Float, val length: Int,
        val cable: String, val breaker: String, val rcd: String, val phase: String
    )

    // Надежный хардкод мощностей вместо SymbolPalette
    private fun getPower(type: String, name: String): Float {
        val t = type.lowercase(); val n = name.lowercase()
        return when {
            t.contains("плита") || n.contains("плита") -> 3500f
            t.contains("вытяжка") || n.contains("вытяжка") -> 250f
            t.contains("стир") || n.contains("стир") -> 2000f
            t.contains("бойлер") || n.contains("нагрев") -> 1500f
            t.contains("кондиц") -> 1500f
            t.contains("посудомой") -> 2000f
            t.contains("духовка") -> 2500f
            t.contains("микроволновка") -> 1000f
            t.contains("чайник") || t.contains("утюг") || t.contains("пылесос") || t.contains("фен") -> 1500f
            t.contains("компьютер") || t.contains("пк") || n.contains("пк") -> 500f
            t.contains("тв") || n.contains("тв") -> 200f
            t.contains("холодильник") -> 300f
            t.contains("роутер") -> 20f
            t.contains("зарядка") -> 60f
            t.contains("люстра") || n.contains("люстра") -> 150f
            t.contains("бра") || n.contains("бра") -> 60f
            t.contains("точечн") || t.contains("спот") -> 50f
            t.contains("лампа") || n.contains("лампа") || t.contains("свет") || n.contains("свет") -> 100f
            t.contains("розетка") || t.contains("socket") -> 100f
            else -> 60f
        }
    }

    // Надежная проверка архитектуры вместо ArchTypes
    private fun isArchOrFurn(type: String): Boolean {
        val t = type.lowercase()
        return t.contains("arch") || t.contains("furn") || t.contains("window") || t.contains("door") || t.contains("стена")
    }

    fun buildGroups(tracks: List<CableTrack>, objects: List<PlanObject>): List<Group> {
        if (tracks.isEmpty()) return emptyList()
        val assign = mutableMapOf<Long, Int>()
        for (o in objects) {
            if (isArchOrFurn(o.type)) continue
            var best = -1; var bd = 500f
            for (ti in tracks.indices) {
                val d = distToTrack(o.x, o.y, tracks[ti])
                if (d < bd) { bd = d; best = ti }
            }
            if (best >= 0) assign[o.id] = best
        }
        val result = mutableListOf<Group>()
        tracks.forEachIndexed { idx, tr ->
            val mine = objects.filter { assign[it.id] == idx }
            var powerW = 0f; var hasSocket = false; var hasLight = false; var isWet = false
            for (o in mine) {
                powerW += getPower(o.type, o.name)
                val t = o.type.lowercase(); val n = o.name.lowercase()
                if (t.contains("socket") || t.contains("розетка") || n.contains("розетка")) hasSocket = true
                if (t.contains("lamp") || t.contains("свет") || t.contains("люстра") || t.contains("бра") || n.contains("свет") || n.contains("люстра") || n.contains("бра")) hasLight = true
                if (n.contains("стир") || n.contains("сан") || n.contains("ван") || n.contains("душ") || t.contains("ван") || t.contains("сан")) isWet = true
            }
            if (mine.isEmpty()) { powerW = 100f; hasSocket = true }
            val top = mine.maxByOrNull { getPower(it.type, it.name) }
            val name = top?.name?.takeIf { it.isNotBlank() } ?: mine.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: "Группа ${idx + 1}"
            val pKw = powerW / 1000f
            val ip = if (pKw > 0f) pKw * 0.8f * 1000f / 220f else 1f
            val breaker = when { ip <= 10f -> "C10/1"; ip <= 16f -> "C16/1"; ip <= 25f -> "C25/1"; else -> "C32/1" }
            val cable = if (hasLight && !hasSocket) "3x1.5" else tr.cable.ifBlank { "3x2.5" }
            val rcd = if (hasSocket || isWet) "УЗО 25/2 30мА" else "—"
            val suffix = when { hasSocket && !hasLight -> "с"; hasLight && !hasSocket -> "о"; else -> "" }
            var len = 0f
            for (i in 1 until tr.points.size) {
                val dx = tr.points[i].x - tr.points[i - 1].x; val dy = tr.points[i].y - tr.points[i - 1].y
                len += kotlin.math.sqrt(dx * dx + dy * dy)
            }
            val length = (len / 100f).toInt().coerceAtLeast(5)
            result.add(Group("Гр.${idx + 1}$suffix", name.take(30), pKw, length, cable, breaker, rcd, "L${(idx % 3) + 1}"))
        }
        return result
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

    fun render(canvas: android.graphics.Canvas, projectName: String, groups: List<Group>, pw: Int, ph: Int) {
        val M = 40f; val W = pw.toFloat(); val H = ph.toFloat()
        val framePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint)
        val h2 = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        canvas.drawText("СХЕМА ЭЛЕКТРИЧЕСКАЯ ОДНОЛИНЕЙНАЯ ЩИТА ЩР-1", M, M + 8f, h2)
        val totalKw = groups.sumOf { it.pKw.toDouble() }.toFloat(); val pRas = totalKw * 0.8f; val ipRas = pRas * 1000f / 220f
        val boxX = M + 30f; val boxY = M + 60f; val boxW = W - M * 2 - 60f; val boxH = 220f
        val bx = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(boxX, boxY, boxX + boxW, boxY + boxH, bx)
        val t = Paint().apply { color = Color.BLACK; textSize = 14f }
        val tb = Paint().apply { color = Color.BLACK; textSize = 14f; isFakeBoldText = true }
        canvas.drawText("ЩР-1   ~220В   ${"%.2f".format(totalKw)} кВт   Кс=0.8   Pр=${"%.2f".format(pRas)} кВт   Iр=${"%.1f".format(ipRas)} А", boxX + 12f, boxY + 22f, tb)
        val inX = boxX + 20f; val inY = boxY + 60f
        val mainBreaker = if (ipRas <= 25f) "C25/1" else if (ipRas <= 40f) "C40/1" else "C63/1"
        canvas.drawRect(inX, inY, inX + 60f, inY + 40f, bx)
        canvas.drawText("QF", inX + 8f, inY + 16f, t); canvas.drawText(mainBreaker, inX + 8f, inY + 32f, t)
        val meterX = inX + 90f
        canvas.drawRect(meterX, inY, meterX + 120f, inY + 40f, bx)
        canvas.drawText("Счётчик", meterX + 6f, inY + 16f, t); canvas.drawText("Меркурий 231", meterX + 6f, inY + 32f, t)
        val uzoX = meterX + 140f
        canvas.drawRect(uzoX, inY, uzoX + 100f, inY + 40f, bx)
        canvas.drawText("УЗО 40/2", uzoX + 6f, inY + 16f, t); canvas.drawText("300 мА", uzoX + 6f, inY + 32f, t)
        val busY = inY + 90f; val busPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        val busLen = boxW - 60f
        canvas.drawLine(boxX + 30f, busY, boxX + 30f + busLen, busY, busPaint); canvas.drawText("L", boxX + 36f, busY - 6f, t)
        canvas.drawLine(boxX + 30f, busY + 20f, boxX + 30f + busLen, busY + 20f, busPaint); canvas.drawText("N", boxX + 36f, busY + 14f, t)
        canvas.drawLine(boxX + 30f, busY + 40f, boxX + 30f + busLen, busY + 40f, busPaint); canvas.drawText("PE", boxX + 36f, busY + 34f, t)
        val gndX = boxX + 50f
        canvas.drawLine(gndX, busY + 40f, gndX, busY + 60f, busPaint)
        canvas.drawLine(gndX - 12f, busY + 60f, gndX + 12f, busY + 60f, busPaint)
        canvas.drawLine(gndX - 8f, busY + 65f, gndX + 8f, busY + 65f, busPaint)
        canvas.drawLine(gndX - 4f, busY + 70f, gndX + 4f, busY + 70f, busPaint)
        val tableTop = boxY + boxH + 30f; val rowH = 26f
        val cols = floatArrayOf(M + 10f, M + 90f, M + 420f, M + 500f, M + 570f, M + 640f, M + 790f, M + 890f, M + 1020f, W - M - 10f)
        val hdrPaint = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        val cellPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 12f }
        val headers = listOf("№ гр.", "Наименование", "Pуст,кВт", "Ip,А", "L,м", "Кабель", "Автомат", "УЗО", "Фаза")
        canvas.drawRect(M, tableTop, W - M, tableTop + rowH, cellPaint)
        for (i in 0 until cols.size - 1) {
            canvas.drawLine(cols[i], tableTop, cols[i], tableTop + rowH, cellPaint)
            canvas.drawText(headers[i], cols[i] + 4f, tableTop + 18f, hdrPaint)
        }
        val show = groups.take(14)
        show.forEachIndexed { idx, g ->
            val y = tableTop + rowH * (idx + 1)
            canvas.drawRect(M, y, W - M, y + rowH, cellPaint)
            for (i in 0 until cols.size - 1) canvas.drawLine(cols[i], y, cols[i], y + rowH, cellPaint)
            canvas.drawText(g.num, cols[0] + 4f, y + 18f, textPaint)
            canvas.drawText(g.name, cols[1] + 4f, y + 18f, textPaint)
            canvas.drawText("%.2f".format(g.pKw), cols[2] + 4f, y + 18f, textPaint)
            canvas.drawText("%.1f".format(g.pKw * 0.8f * 1000f / 220f), cols[3] + 4f, y + 18f, textPaint)
            canvas.drawText(g.length.toString(), cols[4] + 4f, y + 18f, textPaint)
            canvas.drawText(g.cable, cols[5] + 4f, y + 18f, textPaint)
            canvas.drawText(g.breaker, cols[6] + 4f, y + 18f, textPaint)
            canvas.drawText(g.rcd, cols[7] + 4f, y + 18f, textPaint)
            canvas.drawText(g.phase, cols[8] + 4f, y + 18f, textPaint)
        }
        val sumY = tableTop + rowH * (show.size + 1) + 6f
        val sumPaint = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        canvas.drawText("Итого по щиту: Pуст=${"%.2f".format(totalKw)} кВт, Pр=${"%.2f".format(pRas)} кВт, Iр=${"%.1f".format(ipRas)} А, групп: ${groups.size}", M, sumY + 14f, sumPaint)
        val noteY = sumY + 40f
        val notes = listOf("Примечания:", "1. Степень защиты щита ЩР-1 не ниже IP31.", "2. Вводной автоматический выключатель опломбировать.", "3. Группы с УЗО 30мА — розеточные и влажные помещения (ПУЭ 7.1.71).", "4. Соединение жил в расп. коробках — клеммниками WAGO, скрутка запрещена.", "5. Все розетки — с защитным контактом, двухполюсные (ПУЭ 7.1.49).")
        val notePaint = Paint().apply { color = Color.BLACK; textSize = 12f }
        notes.forEachIndexed { idx, s2 -> canvas.drawText(s2, M, noteY + idx * 18f, notePaint) }
        val sw = 360f; val sh = 80f; val sx = W - M - sw; val sy = H - M - sh
        val stampPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas.drawRect(sx, sy, sx + sw, sy + sh, stampPaint)
        canvas.drawLine(sx, sy + 25f, sx + sw, sy + 25f, stampPaint)
        canvas.drawLine(sx, sy + 50f, sx + sw, sy + 50f, stampPaint)
        val st = Paint().apply { color = Color.BLACK; textSize = 13f }
        canvas.drawText("ГдеСанёк", sx + 6f, sy + 18f, st)
        canvas.drawText("ЭОМ   Лист 2.1   Схема однолинейная", sx + 6f, sy + 42f, st)
        canvas.drawText(projectName, sx + 6f, sy + 70f, st)
    }
}
