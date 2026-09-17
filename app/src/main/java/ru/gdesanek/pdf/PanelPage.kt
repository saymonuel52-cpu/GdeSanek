package ru.gdesanek.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PdfDocument
import android.graphics.Typeface
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import ru.gdesanek.model.TrackPoint
import kotlin.math.sqrt

object PanelPage {
    private data class Group(val track: CableTrack?, val objects: List<PlanObject>)

    fun generate(doc: PdfDocument, pageInfo: PdfDocument.PageInfo, tracks: List<CableTrack>, objects: List<PlanObject>, projectName: String) {
        val page = doc.startPage(pageInfo)
        val c = page.canvas
        c.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK; isAntiAlias = true }

        paint.textSize = 20f; paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText("ЩИТ И ГРУППЫ", 40f, 40f, paint)
        paint.textSize = 12f; paint.typeface = Typeface.DEFAULT
        c.drawText("Проект: $projectName", 40f, 58f, paint)

        val cols = intArrayOf(40, 65, 215, 285, 345, 395, 445)
        val headers = listOf("№", "Назначение", "Кабель", "Длина", "Автомат", "УЗО", "Мощность")
        paint.textSize = 11f; paint.typeface = Typeface.DEFAULT_BOLD
        for ((i, h) in headers.withIndex()) c.drawText(h, cols[i].toFloat(), 85f, paint)

        paint.color = Color.GRAY; c.drawLine(40f, 90f, 515f, 90f, paint); paint.color = Color.BLACK

        val groups = groupByTrack(tracks, objects)
        var y = 105f
        paint.textSize = 10f; paint.typeface = Typeface.DEFAULT
        var totalPower = 0.0; var totalLength = 0.0; var idx = 1

        for (g in groups) {
            val lengthM = if (g.track != null) trackLength(g.track.points) / 100f * 1.10f else 0f
            val power = g.objects.sumOf { defaultPower(it.type) }
            val cable = g.track?.cable ?: "—"
            val breaker = breakerBySection(parseSection(cable))
            val hasWet = g.objects.any { isWetZone(it.type) }
            val uzo = if (hasWet) "30 мА" else "—"
            val name = if (g.objects.isEmpty()) "(пусто)" else g.objects.first().name.ifEmpty { g.objects.first().type }

            c.drawText(idx.toString(), cols[0].toFloat(), y, paint)
            c.drawText(name.take(16), cols[1].toFloat(), y, paint)
            c.drawText(cable, cols[2].toFloat(), y, paint)
            c.drawText(String.format("%.1f м", lengthM), cols[3].toFloat(), y, paint)
            c.drawText("C$breaker", cols[4].toFloat(), y, paint)
            c.drawText(uzo, cols[5].toFloat(), y, paint)
            c.drawText(String.format("%.1f кВт", power), cols[6].toFloat(), y, paint)

            totalPower += power; totalLength += lengthM; idx++; y += 18f
            if (y > 780f) { doc.finishPage(page); return }
        }

        y += 8f
        paint.color = Color.GRAY; c.drawLine(40f, y - 8f, 515f, y - 8f, paint); paint.color = Color.BLACK
        paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText("ИТОГО", cols[1].toFloat(), y, paint)
        c.drawText(String.format("%.1f м", totalLength), cols[3].toFloat(), y, paint)
        c.drawText(String.format("%.1f кВт", totalPower), cols[6].toFloat(), y, paint)

        y += 30f; paint.typeface = Typeface.DEFAULT; paint.textSize = 9f; paint.color = Color.DKGRAY
        c.drawText("Примечание:", 40f, y, paint); y += 14f
        c.drawText("• Вводное УЗО 100 мА тип S (селективное).", 50f, y, paint); y += 12f
        c.drawText("• Групповые УЗО 30 мА тип А (для стиральных) / АС (для остальных).", 50f, y, paint); y += 12f
        c.drawText("• Автомат подбирается по допустимому току кабеля (ПУЭ табл. 1.3.4).", 50f, y, paint); y += 12f
        c.drawText("• Длина кабеля указана с запасом ×1.10 на запас и спуски.", 50f, y, paint)

        doc.finishPage(page)
    }

    private fun groupByTrack(tracks: List<CableTrack>, objects: List<PlanObject>): List<Group> {
        val result = mutableListOf<Group>(); val used = mutableSetOf<Long>()
        for (tr in tracks) {
            val attached = objects.filter { minDistToTrack(it, tr) < 150f }
            if (attached.isNotEmpty()) { attached.forEach { used.add(it.id) }; result.add(Group(tr, attached)) }
        }
        val orphans = objects.filter { it.id !in used }
        if (orphans.isNotEmpty()) result.add(Group(null, orphans))
        return result
    }

    private fun minDistToTrack(obj: PlanObject, tr: CableTrack): Float {
        var min = Float.MAX_VALUE
        val pts = tr.points
        for (i in 0 until pts.size - 1) {
            val d = distToSegment(obj.x, obj.y, pts[i].x, pts[i].y, pts[i+1].x, pts[i+1].y)
            if (d < min) min = d
        }
        return min
    }

    private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1; val dy = y2 - y1
        if (dx == 0f && dy == 0f) return sqrt((px-x1)*(px-x1)+(py-y1)*(py-y1))
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val tc = t.coerceIn(0f, 1f)
        val nx = x1 + tc * dx; val ny = y1 + tc * dy
        return sqrt((px - nx) * (px - nx) + (py - ny) * (py - ny))
    }

    private fun trackLength(pts: List<TrackPoint>): Float {
        var s = 0f
        for (i in 0 until pts.size - 1) { val dx = pts[i+1].x-pts[i].x; val dy = pts[i+1].y-pts[i].y; s += sqrt(dx*dx+dy*dy) }
        return s
    }

    private fun parseSection(cable: String): Float {
        val m = Regex("(\\d+)[xхХ×*](\\d+(?:\\.\\d+)?)").find(cable)
        return m?.groupValues?.get(2)?.toFloatOrNull() ?: 2.5f
    }

    private fun breakerBySection(s: Float): Int = when {
        s < 1.5f -> 10; s < 2.5f -> 10; s < 4f -> 16; s < 6f -> 25; s < 10f -> 32; s < 16f -> 40; s < 25f -> 63; else -> 80
    }

    private fun isWetZone(t: String): Boolean = t == "socket_b3" || t == "cons_boiler" || t == "sks_smoke"

    private fun defaultPower(t: String): Double = when (t) {
        "socket_b1", "socket_b2", "socket_b4", "socket_k", "socket_double",
        "socket_block2", "socket_block3", "socket_block4", "socket_b3" -> 2.2
        "socket_380" -> 10.0
        "switch_1", "switch_2", "switch_3", "switch_pass", "switch_dim", "switch_move", "switch_o" -> 0.3
        "lamp_grig", "lamp_titan", "lamp_flame", "lamp_lust", "lamp_bra",
        "lamp_led", "lamp_street", "lamp_ao", "lamp_exit" -> 0.1
        "sks_tv", "sks_phone", "sks_intercom", "sks_cam", "sks_smoke", "sks_sec" -> 0.1
        "rj45", "rj45x2" -> 0.05
        "cond_vk" -> 2.5; "cons_hood" -> 0.25; "cons_boiler" -> 2.5
        "cons_stove" -> 10.0; "cons_pump" -> 1.5
        else -> 0.0
    }
}
