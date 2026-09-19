package ru.gdesanek.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import kotlin.math.ceil
import kotlin.math.sqrt

object SpecificationPage {
    fun generate(doc: PdfDocument, pageInfo: PdfDocument.PageInfo, tracks: List<CableTrack>, objects: List<PlanObject>, projectName: String, masterName: String) {
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        val title = Paint().apply { color = Color.BLACK; textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
        val head = Paint().apply { color = Color.BLACK; textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val text = Paint().apply { color = Color.BLACK; textSize = 10f }
        val lineP = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
        canvas.drawText("СПЕЦИФИКАЦИЯ ОБОРУДОВАНИЯ И МАТЕРИАЛОВ — $projectName", 40f, 40f, title)
        if (masterName.isNotEmpty()) canvas.drawText("Исполнитель: $masterName", 40f, 56f, text)
        fun segLen(t: CableTrack): Float { var s = 0f; for (i in 0 until t.points.size - 1) s += sqrt((t.points[i+1].x - t.points[i].x).let { it * it } + (t.points[i+1].y - t.points[i].y).let { it * it }); return s }
        fun cableM(c: String): Float { var s = 0f; for (t in tracks) if (t.cable == c) s += segLen(t); return ceil(s / 100f * 1.1f) }
        fun wiringM(w: String): Float { var s = 0f; for (t in tracks) if (t.wiring == w) s += segLen(t); return ceil(s / 100f * 1.1f) }
        val points = objects.count { it.type.startsWith("socket") || it.type.startsWith("switch") }
        val lamps = objects.count { it.type.startsWith("lamp") }
        val rk = ceil(points / 6f).toInt() + 1
        val podrozet = points + lamps
        val gofra = wiringM("gofra") + wiringM("truba")
        val clamps = ceil(gofra / 2f).toInt()
        val wago = rk * 4
        val rows = mutableListOf<Array<String>>()
        fun add(name: String, unit: String, qty: Int) { if (qty > 0) rows.add(arrayOf(name, unit, qty.toString())) }
        add("Кабель ВВГнг(А)-LS 3х1,5 (освещение)", "м", cableM("3x1.5").toInt())
        add("Кабель ВВГнг(А)-LS 3х2,5 (розетки)", "м", cableM("3x2.5").toInt())
        add("Кабель ВВГнг(А)-LS 3х4", "м", cableM("3x4").toInt())
        add("Кабель ВВГнг(А)-LS 3х6", "м", cableM("3x6").toInt())
        add("Труба гофрированная ПВХ 20 мм", "м", gofra.toInt())
        add("Коробка установочная 68х45 мм", "шт", podrozet)
        add("Коробка распределительная 80х80х40 IP44", "шт", rk)
        add("Клемма WAGO 221-413", "шт", wago)
        add("Держатель трубы 20 мм", "шт", clamps)
        val groups = PanelPage.groupByTrack(tracks, objects)
        val brk = mutableMapOf<Int, Int>()
        for (g in groups) { val b = PanelPage.breakerBySection(PanelPage.parseSection(g.track?.cable ?: "")); brk[b] = (brk[b] ?: 0) + 1 }
        for ((b, c) in brk.toSortedMap()) add("Автоматический выключатель 1P C$b 6кА", "шт", c)
        if (groups.isNotEmpty()) add("Щит распределительный навесной", "шт", 1)
        var y = 80f
        canvas.drawText("№", 40f, y, head); canvas.drawText("Наименование и характеристика", 60f, y, head); canvas.drawText("Ед.", 430f, y, head); canvas.drawText("Кол-во", 480f, y, head)
        y += 4f; canvas.drawLine(40f, y, 555f, y, lineP); y += 16f
        var i = 1
        for (r in rows) {
            canvas.drawText(i.toString(), 40f, y, text)
            canvas.drawText(r[0], 60f, y, text)
            canvas.drawText(r[1], 430f, y, text)
            canvas.drawText(r[2], 480f, y, text)
            y += 14f; i++
        }
        canvas.drawText("Примечание: длины кабелей с запасом 10%. Спецификация сформирована автоматически приложением ГдеСанёк.", 40f, y + 12f, text)
        doc.finishPage(page)
    }
}
