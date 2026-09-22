package ru.gdesanek.export

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.graphics.pdf.PdfDocument
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import ru.gdesanek.model.Wall
import ru.gdesanek.render.GostSymbols
import ru.gdesanek.theme.SymbolPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

object PdfExporter {
    fun export(context: Context, projectName: String, projectId: Long, walls: List<Wall>, objects: List<PlanObject>, tracks: List<CableTrack>, mono: Boolean = false, passport: List<String> = listOf("", "", "", "")): File {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val masterName = prefs.getString("masterName", "ГдеСанёк").orEmpty()
        val masterPhone = prefs.getString("masterPhone", "").orEmpty()
        val masterInn = prefs.getString("masterInn", "").orEmpty()
        val doc = PdfDocument()
        run {
            val a4l = android.graphics.pdf.PdfDocument.PageInfo.Builder(842, 595, 1).create()
            val pg1 = doc.startPage(a4l); val c1 = pg1.canvas; val m1 = c1.width / 297f
            val tp = Paint(textPaint)
            c1.drawText("ОБЩИЕ ДАННЫЕ", 20f * m1, 25f * m1, Paint(titlePaint))
            tp.textSize = 6f * m1
            c1.drawText("Комплект рабочих чертежей: " + projectName, 20f * m1, 40f * m1, tp)
            if (passport.getOrNull(3)?.isNotBlank() == true) c1.drawText("Адрес объекта: " + passport[3], 20f * m1, 50f * m1, tp)
            if (passport.getOrNull(1)?.isNotBlank() == true) c1.drawText("Проектная организация: " + passport[1], 20f * m1, 60f * m1, tp)
            tp.textSize = 5f * m1
            c1.drawText("Ведомость рабочих чертежей:", 20f * m1, 75f * m1, tp)
            listOf("1.1   Общие данные", "1.2   Пояснительная записка", "1.3   Ведомость ссылочных документов", "2.1   План расположения ЭО и освещения").forEachIndexed { i, t -> c1.drawText(t, 26f * m1, (85 + i * 8) * m1, tp) }
            doc.finishPage(pg1)
            val pg2 = doc.startPage(a4l); val c2 = pg2.canvas
            val tp2 = Paint(textPaint); tp2.textSize = 4.2f * m1
            c2.drawText("ПОЯСНИТЕЛЬНАЯ ЗАПИСКА", 20f * m1, 25f * m1, Paint(titlePaint))
            listOf(
                "1. Проект разработан на основании технического задания заказчика.",
                "2. Согласно СП 31-110-2003 объект относится к III категории по степени обеспечения надежности электроснабжения.",
                "3. Располагаемые потери напряжения не более 2%.",
                "4. В соответствии с гл. 7.1 ПУЭ седьмого издания групповые сети предусмотрены трехпроводными и пятипроводными с отдельным защитным проводником PE.",
                "5. Прокладка кабелей выполняется медным кабелем ВВГнг-LS: скрыто в штробе, открыто по плите перекрытия в гофрированной ПВХ трубе.",
                "6. Щит должен иметь отдельную шину для подключения защитного проводника.",
                "7. Все элементы электросетей выполнены с учетом ГОСТ Р 50462-92: цветовая идентификация жил кабелей и проводников.",
                "8. Вся электрическая сеть рассчитана на длительно допустимую нагрузку и проверена по потере напряжения.",
                "9. Соединение жил в ответвительных коробках методом скрутки не допускается; рекомендуется клеммниками быстрого соединения типа WAGO.",
                "10. Весь монтаж должен быть выполнен в соответствии с ПУЭ и СП 76.13330.2011."
            ).forEachIndexed { i, t -> c2.drawText(t, 20f * m1, (40 + i * 9) * m1, tp2) }
            doc.finishPage(pg2)
            val pg3 = doc.startPage(a4l); val c3 = pg3.canvas
            val tp3 = Paint(textPaint); tp3.textSize = 4.2f * m1
            c3.drawText("ВЕДОМОСТЬ ССЫЛОЧНЫХ ДОКУМЕНТОВ", 20f * m1, 25f * m1, Paint(titlePaint))
            listOf(
                "ПУЭ" to "Правила устройства электроустановок. - 7-е изд. - М., 2002.",
                "СП 31-110-2003" to "Электрооборудование жилых и общественных зданий. Нормы проектирования",
                "СП 76.13330.2011" to "Электротехнические устройства",
                "СП 52.13330.2010" to "Естественное и искусственное освещение. Нормы проектирования",
                "ГОСТ Р 50571.1-2009" to "Электроустановки зданий",
                "ГОСТ Р 50571.5.52-2011" to "Выбор и монтаж электрооборудования",
                "ГОСТ Р 50462-2009" to "Идентификация проводников посредством цветов и буквенно-цифровых обозначений"
            ).forEachIndexed { i, (n, t) -> c3.drawText(n + "   " + t, 20f * m1, (40 + i * 9) * m1, tp3) }
            doc.finishPage(pg3)
        }
        val page = doc.startPage(PdfDocument.PageInfo.Builder(842, 595, 1).create())
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        val labelPaint = Paint().apply { color = Color.BLACK; textSize = 3.0f }

        val framePaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f; style = Paint.Style.STROKE }
        val thinPaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 10f }
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 13f }

        val L = 57f; val T = 14f; val R = 828f; val B = 581f
        canvas.drawRect(L, T, R, B, framePaint)

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        fun add(x: Float, y: Float) { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y }
        walls.forEach { add(it.x1, it.y1); add(it.x2, it.y2) }
        objects.forEach { add(it.x, it.y) }
        tracks.forEach { t -> t.points.forEach { add(it.x, it.y) } }
        if (minX > maxX) { minX = 0f; minY = 0f; maxX = 1000f; maxY = 1000f }
        minX -= 100f; minY -= 100f; maxX += 100f; maxY += 100f

        val aL = L + 10f; val aT = T + 25f; val aR = R - 10f; val aB = B - 80f
        val scale = min((aR - aL) / (maxX - minX), (aB - aT) / (maxY - minY))
        val mm = 2.8346f
        labelPaint.textSize = 2.5f * mm
        fun tx(x: Float) = aL + (x - minX) * scale
        fun ty(y: Float) = aT + (y - minY) * scale

        canvas.drawText("ПЛАН РАСПОЛОЖЕНИЯ ЭО И ОСВЕЩЕНИЯ — $projectName", L + 10f, T + 16f, titlePaint)

        val wallPaint = Paint().apply { color = Color.BLACK; strokeWidth = 0.8f * mm / scale }
        for (w in walls) {
            val dx = w.x2 - w.x1; val dy = w.y2 - w.y1
            val len = sqrt(dx * dx + dy * dy); if (len < 1f) continue
            val ux = dx / len; val uy = dy / len; val px = -uy; val py = ux
            val half = w.thickness / 10f / 2f * scale
            canvas.drawLine(tx(w.x1) + px * half, ty(w.y1) + py * half, tx(w.x2) + px * half, ty(w.y2) + py * half, wallPaint)
            canvas.drawLine(tx(w.x1) - px * half, ty(w.y1) - py * half, tx(w.x2) - px * half, ty(w.y2) - py * half, wallPaint)
        }

        val trPaint = Paint().apply { strokeWidth = 0.6f * mm / scale }
        for (t in tracks) {
            trPaint.color = if (mono) 0xFF000000.toInt() else t.color; trPaint.pathEffect = when (t.wiring) { "shtroba" -> DashPathEffect(floatArrayOf(6f, 4f), 0f); "gofra" -> DashPathEffect(floatArrayOf(6f, 3f, 2f, 3f), 0f); "truba" -> DashPathEffect(floatArrayOf(2f, 3f), 0f); "lotok" -> DashPathEffect(floatArrayOf(8f, 3f), 0f); else -> null }
            for (i in 0 until t.points.size - 1) canvas.drawLine(tx(t.points[i].x), ty(t.points[i].y), tx(t.points[i+1].x), ty(t.points[i+1].y), trPaint)
            if (t.points.isNotEmpty()) { val p0 = t.points[0]; labelPaint.color = if (mono) 0xFF000000.toInt() else t.color; canvas.drawText("Гр." + (tracks.indexOf(t) + 1) + " ВВГнг-LS " + t.cable, tx(p0.x) + 2.0f * mm, ty(p0.y) - 2.0f * mm, labelPaint) }
        }

        labelPaint.color = Color.BLACK
        val symPaint = Paint().apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 0.6f * mm / scale }
        if (mono) symPaint.strokeWidth = 0.5f * mm / scale
        for ((oi, o) in objects.withIndex()) {
            symPaint.color = if (mono) (if (ru.gdesanek.core.ArchTypes.isFurn(o.type) || ru.gdesanek.core.ArchTypes.isArch(o.type)) 0xFF9E9E9E.toInt() else 0xFF000000.toInt()) else SymbolPalette.color(o.type)
            canvas.save()
            canvas.translate(tx(o.x), ty(o.y))
            canvas.scale(scale, scale)
            canvas.translate(-o.x, -o.y)
            if (ru.gdesanek.core.ArchTypes.isArch(o.type)) {
                val ap = android.graphics.Paint(symPaint).apply { color = android.graphics.Color.parseColor("#9E9E9E"); strokeWidth = 0.5f * mm / scale }
                GostSymbols.draw(canvas, o.type, o.x, o.y, o.rotation, ap)
            } else {
                canvas.save(); canvas.translate(o.x, o.y); canvas.scale(0.7f, 0.7f); canvas.translate(-o.x, -o.y)
                GostSymbols.draw(canvas, o.type, o.x, o.y, o.rotation, symPaint)
                canvas.restore()
            }
            canvas.restore()
            val hh = if (o.height >= 0) o.height else SymbolPalette.height(o.type)
            val ipm = SymbolPalette.ip(o.type)
            val mark = (if (hh != null) "h=$hh" else "") + (if (ipm != null) (if (hh != null) " " else "") + ipm else "")
            if (mark.isNotEmpty()) { labelPaint.color = if (mono) Color.BLACK else 0xFFD32F2F.toInt(); canvas.drawText(mark, tx(o.x) + 2.2f * mm, ty(o.y) - 2.2f * mm, labelPaint); labelPaint.color = Color.BLACK }
            if (o.name.isNotBlank()) { val dyN = 3.0f * mm + labelPaint.textSize * 1.15f + (oi % 2) * 3.5f * mm; canvas.drawText(o.name.take(18), tx(o.x) + 2.2f * mm, ty(o.y) + dyN, labelPaint) }
            SymbolPalette.power(o.type)?.let { w -> canvas.drawText(w.toString() + " Вт", tx(o.x) + 2.2f * mm, ty(o.y) + 3.0f * mm, labelPaint) }
        }

        val legend = listOf(
            "Розетки 220В" to Color.parseColor("#FF5252"),
            "Выключатели" to Color.parseColor("#FF7043"),
            "Освещение" to Color.parseColor("#FFCA28"),
            "Слаботочка" to Color.parseColor("#40C4FF"),
            "Щиты/короба" to Color.parseColor("#26A69A"),
            "Нагрузка" to Color.parseColor("#AB47BC")
        )
        var ly = B - 38f
        textPaint.textSize = 8f
        for (i in legend.indices) {
            val (name, c) = legend[i]
            val col = if (i < 3) 0 else 1
            val row = if (i < 3) i else i - 3
            val x0 = L + 10f + col * 200f
            val yy = B - 38f + row * 11f
            trPaint.color = c; trPaint.strokeWidth = 2f
            canvas.drawLine(x0, yy - 3f, x0 + 20f, yy - 3f, trPaint)
            textPaint.color = Color.BLACK
            canvas.drawText(name, x0 + 25f, yy, textPaint)
        }

        val sL = R - 185f; val sT = B - 55f
        canvas.drawRect(sL, sT, R, B, framePaint)
        canvas.drawLine(sL, sT + 18f, R, sT + 18f, thinPaint)
        canvas.drawLine(sL, sT + 36f, R, sT + 36f, thinPaint)
        canvas.drawLine(sL + 95f, sT + 18f, sL + 95f, B, thinPaint)
        textPaint.textSize = 11f
        textPaint.textSize = 9f
        run {
            val mmPx = canvas.width / 297f
            val sl = canvas.width - 195f * mmPx; val st = canvas.height - 65f * mmPx; val sr = canvas.width - 5f * mmPx; val sb = canvas.height - 5f * mmPx
            val fp = Paint(framePaint); fp.strokeWidth = 0.6f * mmPx
            canvas.drawRect(sl, st, sr, sb, fp)
            canvas.drawLine(sl + 60f * mmPx, st, sl + 60f * mmPx, sb, fp)
            canvas.drawLine(sl, st + 40f * mmPx, sr, st + 40f * mmPx, fp)
            val ts = Paint(textPaint); ts.color = Color.BLACK
            ts.textSize = 3.5f * mmPx
            canvas.drawText(passport.getOrNull(1)?.ifBlank { "ГдеСанёк" } ?: "ГдеСанёк", sl + 3f * mmPx, st + 6f * mmPx, ts)
            ts.textSize = 5f * mmPx
            canvas.drawText(passport.getOrNull(0)?.ifBlank { "ЭОМ" } ?: "ЭОМ", sl + 63f * mmPx, st + 8f * mmPx, ts)
            canvas.drawText("План расположения ЭО и освещения", sl + 63f * mmPx, st + 20f * mmPx, ts)
            ts.textSize = 4f * mmPx
            canvas.drawText("Стадия Р", sl + 63f * mmPx, st + 30f * mmPx, ts)
            canvas.drawText("Лист 2.1", sl + 90f * mmPx, st + 30f * mmPx, ts)
            canvas.drawText("Листов 4", sl + 115f * mmPx, st + 30f * mmPx, ts)
            canvas.drawText("Разраб. " + (passport.getOrNull(2) ?: ""), sl + 3f * mmPx, st + 46f * mmPx, ts)
            canvas.drawText("Дата: " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()), sl + 63f * mmPx, st + 46f * mmPx, ts)
        }
        if (masterInn.isNotEmpty()) canvas.drawText("ИНН: $masterInn", sL + 100f, sT + 62f, textPaint)

        doc.finishPage(page)
        val file = File(context.cacheDir, "GdeSanek_$projectId.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
