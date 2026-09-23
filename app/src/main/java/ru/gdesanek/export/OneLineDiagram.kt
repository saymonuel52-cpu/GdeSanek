package ru.gdesanek.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument

/**
 * Однолинейная схема щита ЩР + таблица групп (ГОСТ 21.614-style).
 * Рисует на готовой странице PdfDocument (лист 2.1).
 */
object OneLineDiagram {

    data class Group(
        val num: String,          // "1с", "4о", ...
        val name: String,         // "Свет коридор"
        val pKw: Float,           // кВт
        val length: Int,          // м
        val cable: String,        // "ВВГнг-LS 3x1.5"
        val breaker: String,      // "C10/1"
        val rcd: String,          // "—" или "УЗО 25/2 30мА"
        val phase: String         // "L1" / "L2" / "L3" / "L"
    )

    /**
     * Группирует объекты проекта в расчётные группы щита.
     * Логика:
     *  - каждый CableTrack = отдельная группа (у неё уже есть номер Гр.N и кабель)
     *  - мощность = сумма мощностей подключённых PlanObject по близости к трассе
     *  - автомат подбирается по Ip: <=10A -> C10, <=16A -> C16, <=25A -> C25, >25A -> C32
     *  - УЗО 30мА ставится на влажные (санузел, стиралка) и на все розеточные по ПУЭ 7.1.71
     */
    fun buildGroups(
        tracks: List<ru.gdesanek.model.CableTrack>,
        objects: List<ru.gdesanek.model.PlanObject>
    ): List<Group> {
        if (tracks.isEmpty()) return emptyList()
        val result = mutableListOf<Group>()
        tracks.forEachIndexed { idx, tr ->
            val gnum = "Гр.${idx + 1}"
            // ищем ближайшие объекты к трассе (до 250 ед. в плане)
            var powerW = 0f
            var name = "Группа ${idx + 1}"
            var isWet = false
            var hasSocket = false
            var hasLight = false
            for (o in objects) {
                val d = distToTrack(o.x, o.y, tr)
                if (d < 250f) {
                    val w = (ru.gdesanek.theme.SymbolPalette.power(o.type)?.toFloat() ?: 0f)
                    powerW += w
                    name = o.name.ifBlank { name }
                    val t = o.type
                    if (t.contains("socket", true) || t.contains("roz") || t.contains("плит")
                        || t.contains("стир") || t.contains("стир") || t.contains("washer")) hasSocket = true
                    if (t.contains("lamp", true) || t.contains("lustr") || t.contains("свет")
                        || t.contains("люст") || t.contains("bra") || t.contains("spot")) hasLight = true
                    if (t.contains("сан") || t.contains("ван") || t.contains("дух")
                        || t.contains("wash") || t.contains("стир")) isWet = true
                }
            }
            val pKw = powerW / 1000f
            val ip = if (pKw > 0f) (pKw * 0.8f * 1000f / 220f) else 1f // Кс=0.8
            val breaker = when {
                ip <= 10f -> "C10/1"
                ip <= 16f -> "C16/1"
                ip <= 25f -> "C25/1"
                else -> "C32/1"
            }
            val cable = tr.cable.ifBlank {
                if (hasLight) "ВВГнг-LS 3x1.5" else "ВВГнг-LS 3x2.5"
            }
            val rcd = if (hasSocket || isWet) "УЗО 25/2 30мА" else "—"
            val suffix = if (hasLight && !hasSocket) "с" else if (hasSocket && !hasLight) "о" else ""
            val num = "${idx + 1}$suffix"
            val length = tr.points.foldRight(0f) { p, acc ->
                if (acc == 0f) 0f else acc
            }.let {
                // длина как сумма сегментов
                var len = 0f
                for (i in 1 until tr.points.size) {
                    val dx = tr.points[i].x - tr.points[i - 1].x
                    val dy = tr.points[i].y - tr.points[i - 1].y
                    len += kotlin.math.sqrt(dx * dx + dy * dy)
                }
                (len / 100f).toInt().coerceAtLeast(5) // грубый масштаб 100px = 1м
            }
            val phase = "L${((idx % 3) + 1)}" // балансировка L1/L2/L3
            result.add(
                Group(
                    num = gnum + suffix,
                    name = name.take(30),
                    pKw = pKw,
                    length = length,
                    cable = cable,
                    breaker = breaker,
                    rcd = rcd,
                    phase = phase
                )
            )
        }
        return result
    }

    private fun distToTrack(px: Float, py: Float, tr: ru.gdesanek.model.CableTrack): Float {
        if (tr.points.size < 2) return 1e9f
        var min = 1e9f
        for (i in 1 until tr.points.size) {
            val a = tr.points[i - 1]; val b = tr.points[i]
            val dx = b.x - a.x; val dy = b.y - a.y
            val len2 = dx * dx + dy * dy
            val t = if (len2 > 0f) ((px - a.x) * dx + (py - a.y) * dy) / len2 else 0f
            val tc = t.coerceIn(0f, 1f)
            val cx = a.x + tc * dx; val cy = a.y + tc * dy
            val ddx = px - cx; val ddy = py - cy
            val d = kotlin.math.sqrt(ddx * ddx + ddy * ddy)
            if (d < min) min = d
        }
        return min
    }

    /**
     * Рисует страницу схемы щита (лист 2.1, альбомная A3-подобная).
     */
    fun render(
        canvas: android.graphics.Canvas,
        projectName: String,
        groups: List<Group>,
        pw: Int, ph: Int
    ) {
        val M = 40f
        val W = pw.toFloat(); val H = ph.toFloat()

        val framePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(M - 15f, M - 15f, W - M + 15f, H - M + 15f, framePaint)

        val h2 = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
        canvas.drawText("СХЕМА ЭЛЕКТРИЧЕСКАЯ ОДНОЛИНЕЙНАЯ ЩИТА ЩР-1", M, M + 8f, h2)

        // === Суммарные мощности ===
        val totalKw = groups.map { it.pKw.toDouble() }.sum().toFloat()
        val kc = 0.8f
        val pRas = totalKw * kc
        val ipRas = pRas * 1000f / 220f

        // === Блок щита (левая верхняя часть) ===
        val boxX = M + 30f
        val boxY = M + 60f
        val boxW = W - M * 2 - 60f
        val boxH = 220f

        val bx = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(boxX, boxY, boxX + boxW, boxY + boxH, bx)

        val t = Paint().apply { color = Color.BLACK; textSize = 14f }
        val tb = Paint().apply { color = Color.BLACK; textSize = 14f; isFakeBoldText = true }
        canvas.drawText("ЩР-1   ~220В   ${"%.2f".format(totalKw)} кВт   Кс=0.8   Pр=${"%.2f".format(pRas)} кВт   Iр=${"%.1f".format(ipRas)} А",
            boxX + 12f, boxY + 22f, tb)

        // === Вводной автомат ===
        val inX = boxX + 20f
        val inY = boxY + 60f
        val mainBreaker = if (ipRas <= 25f) "C25/1" else if (ipRas <= 40f) "C40/1" else "C63/1"
        canvas.drawRect(inX, inY, inX + 60f, inY + 40f, bx)
        canvas.drawText("QF", inX + 8f, inY + 16f, t)
        canvas.drawText(mainBreaker, inX + 8f, inY + 32f, t)

        // === Счётчик ===
        val meterX = inX + 90f
        canvas.drawRect(meterX, inY, meterX + 120f, inY + 40f, bx)
        canvas.drawText("Счётчик", meterX + 6f, inY + 16f, t)
        canvas.drawText("Меркурий 231", meterX + 6f, inY + 32f, t)

        // === УЗО на вводе (противопожарное) ===
        val uzoX = meterX + 140f
        canvas.drawRect(uzoX, inY, uzoX + 100f, inY + 40f, bx)
        canvas.drawText("УЗО 40/2", uzoX + 6f, inY + 16f, t)
        canvas.drawText("300 мА", uzoX + 6f, inY + 32f, t)

        // === Шины L, N, PE ===
        val busY = inY + 90f
        val busPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        val busLen = boxW - 60f
        canvas.drawLine(boxX + 30f, busY, boxX + 30f + busLen, busY, busPaint)
        canvas.drawText("L", boxX + 36f, busY - 6f, t)

        canvas.drawLine(boxX + 30f, busY + 20f, boxX + 30f + busLen, busY + 20f, busPaint)
        canvas.drawText("N", boxX + 36f, busY + 14f, t)

        val pePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawLine(boxX + 30f, busY + 40f, boxX + 30f + busLen, busY + 40f, pePaint)
        canvas.drawText("PE", boxX + 36f, busY + 34f, t)

        // Заземление
        val gndX = boxX + 50f
        canvas.drawLine(gndX, busY + 40f, gndX, busY + 60f, pePaint)
        canvas.drawLine(gndX - 12f, busY + 60f, gndX + 12f, busY + 60f, pePaint)
        canvas.drawLine(gndX - 8f, busY + 65f, gndX + 8f, busY + 65f, pePaint)
        canvas.drawLine(gndX - 4f, busY + 70f, gndX + 4f, busY + 70f, pePaint)

        // === Таблица групп (ниже схемы) ===
        val tableTop = boxY + boxH + 30f
        val rowH = 26f
        val cols = floatArrayOf(
            M + 10f,              // № гр.
            M + 90f,              // Наименование
            M + 420f,             // Pуст
            M + 500f,             // Ip
            M + 570f,             // L
            M + 640f,             // Кабель
            M + 790f,             // Автомат
            M + 890f,             // УЗО
            M + 1020f,            // Фаза
            W - M - 10f           // (граница)
        )
        val hdrPaint = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        val cellPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 12f }

        // заголовок
        val headers = listOf("№ гр.", "Наименование", "Pуст,кВт", "Ip,А", "L,м", "Кабель", "Автомат", "УЗО", "Фаза")
        canvas.drawRect(M, tableTop, W - M, tableTop + rowH, cellPaint)
        for (i in 0 until cols.size - 1) {
            canvas.drawLine(cols[i], tableTop, cols[i], tableTop + rowH, cellPaint)
            canvas.drawText(headers[i], cols[i] + 4f, tableTop + 18f, hdrPaint)
        }

        // строки
        val maxRows = 14
        val show = groups.take(maxRows)
        show.forEachIndexed { idx, g ->
            val y = tableTop + rowH * (idx + 1)
            canvas.drawRect(M, y, W - M, y + rowH, cellPaint)
            for (i in 0 until cols.size - 1) {
                canvas.drawLine(cols[i], y, cols[i], y + rowH, cellPaint)
            }
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

        // Итого
        val sumY = tableTop + rowH * (show.size + 1) + 6f
        val sumPaint = Paint().apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }
        canvas.drawText(
            "Итого по щиту: Pуст=${"%.2f".format(totalKw)} кВт, Pр=${"%.2f".format(pRas)} кВт, Iр=${"%.1f".format(ipRas)} А, групп: ${groups.size}",
            M, sumY + 14f, sumPaint
        )

        // Примечание
        val noteY = sumY + 40f
        val notes = listOf(
            "Примечания:",
            "1. Степень защиты щита ЩР-1 не ниже IP31.",
            "2. Вводной автоматический выключатель опломбировать.",
            "3. Группы с УЗО 30мА — розеточные и влажные помещения (ПУЭ 7.1.71).",
            "4. Соединение жил в расп. коробках — клеммниками WAGO, скрутка запрещена.",
            "5. Все розетки — с защитным контактом, двухполюсные (ПУЭ 7.1.49)."
        )
        val notePaint = Paint().apply { color = Color.BLACK; textSize = 12f }
        notes.forEachIndexed { idx, s ->
            canvas.drawText(s, M, noteY + idx * 18f, notePaint)
        }

        // Штамп внизу
        val sw = 360f; val sh = 80f
        val sx = W - M - sw; val sy = H - M - sh
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
