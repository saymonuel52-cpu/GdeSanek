package ru.gdesanek.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import ru.gdesanek.model.ElectricObject
import ru.gdesanek.model.ObjectType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfExporter {

    fun exportToPdf(
        objects: List<ElectricObject>,
        outputDir: File,
        projectName: String,
        masterName: String = "ГдеСанёк",
        mono: Boolean = false,
        passport: List<String> = listOf("", "", "", "")
    ): File {
        val document = PdfDocument()

        // === Страницы 1.1-1.3 (общие данные) ===
        run {
            val a4l = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            val m = 842f / 297f
            val titleP = Paint().apply { color = Color.BLACK; textSize = 7f * m; isAntiAlias = true }
            val subtitleP = Paint().apply { color = Color.BLACK; textSize = 6f * m; isAntiAlias = true }
            val textP = Paint().apply { color = Color.BLACK; textSize = 5f * m; isAntiAlias = true }
            val smallP = Paint().apply { color = Color.BLACK; textSize = 4.2f * m; isAntiAlias = true }

            val pg1 = document.startPage(a4l); val c1 = pg1.canvas
            c1.drawText("ОБЩИЕ ДАННЫЕ", 20f * m, 25f * m, titleP)
            c1.drawText("Комплект рабочих чертежей: " + projectName, 20f * m, 40f * m, subtitleP)
            if (passport.getOrNull(3)?.isNotBlank() == true) c1.drawText("Адрес объекта: " + passport[3], 20f * m, 50f * m, textP)
            if (passport.getOrNull(1)?.isNotBlank() == true) c1.drawText("Проектная организация: " + passport[1], 20f * m, 60f * m, textP)
            c1.drawText("Ведомость рабочих чертежей:", 20f * m, 75f * m, textP)
            listOf("1.1   Общие данные", "1.2   Пояснительная записка", "1.3   Ведомость ссылочных документов", "2.1   План расположения ЭО и освещения").forEachIndexed { i, t -> c1.drawText(t, 26f * m, (85 + i * 8) * m, textP) }
            document.finishPage(pg1)

            val pg2 = document.startPage(a4l); val c2 = pg2.canvas
            c2.drawText("ПОЯСНИТЕЛЬНАЯ ЗАПИСКА", 20f * m, 25f * m, titleP)
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
            ).forEachIndexed { i, t -> c2.drawText(t, 20f * m, (40 + i * 9) * m, smallP) }
            document.finishPage(pg2)

            val pg3 = document.startPage(a4l); val c3 = pg3.canvas
            c3.drawText("ВЕДОМОСТЬ ССЫЛОЧНЫХ ДОКУМЕНТОВ", 20f * m, 25f * m, titleP)
            listOf(
                "ПУЭ" to "Правила устройства электроустановок. - 7-е изд. - М., 2002.",
                "СП 31-110-2003" to "Электрооборудование жилых и общественных зданий. Нормы проектирования",
                "СП 76.13330.2011" to "Электротехнические устройства",
                "СП 52.13330.2010" to "Естественное и искусственное освещение. Нормы проектирования",
                "ГОСТ Р 50571.1-2009" to "Электроустановки зданий",
                "ГОСТ Р 50571.5.52-2011" to "Выбор и монтаж электрооборудования",
                "ГОСТ Р 50462-2009" to "Идентификация проводников посредством цветов и буквенно-цифровых обозначений"
            ).forEachIndexed { i, (n, t) -> c3.drawText(n + "   " + t, 20f * m, (40 + i * 9) * m, smallP) }
            document.finishPage(pg3)
        }

        // === Страница 2.1 — План ===
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 4).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val mm = canvas.width / 297f
            val mLeft = 20f * mm
            val mTop = 20f * mm
            val mRight = canvas.width - 20f * mm
            val mBottom = canvas.height - 20f * mm

            val framePaint = Paint().apply {
                color = if (mono) Color.BLACK else Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 0.5f * mm
                isAntiAlias = true
            }
            canvas.drawRect(mLeft, mTop, mRight, mBottom, framePaint)

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 6f * mm
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText("ПЛАН РАСПОЛОЖЕНИЯ ЭО И ОСВЕЩЕНИЯ", canvas.width / 2f - 80f * mm, mTop + 12f * mm, titlePaint)

            val subtitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 4f * mm
                isAntiAlias = true
            }
            canvas.drawText(projectName, canvas.width / 2f - 50f * mm, mTop + 20f * mm, subtitlePaint)

            val planLeft = mLeft + 5f * mm
            val planTop = mTop + 30f * mm
            val planRight = mRight - 100f * mm
            val planBottom = mBottom - 10f * mm

            var minX = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE
            for (o in objects) {
                if (o.x < minX) minX = o.x
                if (o.x > maxX) maxX = o.x
                if (o.y < minY) minY = o.y
                if (o.y > maxY) maxY = o.y
            }

            val planW = planRight - planLeft
            val planH = planBottom - planTop
            val scaleX = if (maxX > minX) planW / (maxX - minX) else 1f
            val scaleY = if (maxY > minY) planH / (maxY - minY) else 1f
            val scale = minOf(scaleX, scaleY) * 0.9f

            fun tx(x: Float): Float = planLeft + (x - minX) * scale
            fun ty(y: Float): Float = planTop + (y - minY) * scale

            val linePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 0.3f * mm
                isAntiAlias = true
            }
            for (o in objects) {
                if (o.cableGroupId.isNotBlank()) {
                    for (o2 in objects) {
                        if (o !== o2 && o.cableGroupId == o2.cableGroupId) {
                            canvas.drawLine(tx(o.x), ty(o.y), tx(o2.x), ty(o2.y), linePaint)
                        }
                    }
                }
            }

            val symbolPaint = Paint().apply {
                color = Color.BLACK
                textSize = 5f * mm
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val labelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 3f * mm
                isAntiAlias = true
            }

            for ((oi, o) in objects.withIndex()) {
                val cx = tx(o.x)
                val cy = ty(o.y)
                val symbol = when (o.type) {
                    ObjectType.SOCKET -> "⏚"
                    ObjectType.SWITCH -> "⏛"
                    ObjectType.LIGHT -> "✕"
                    ObjectType.SHIELD -> "▣"
                    ObjectType.JUNCTION_BOX -> "◯"
                    else -> "•"
                }
                canvas.drawText(symbol, cx, cy + 1.5f * mm, symbolPaint)

                if (o.name.isNotBlank()) {
                    val dyN = 3.0f * mm + labelPaint.textSize * 1.15f + (oi % 2) * 3.5f * mm
                    canvas.drawText(o.name.take(18), cx + 2.2f * mm, cy + dyN, labelPaint)
                }
            }

            val legendLeft = mRight - 90f * mm
            val legendTop = mTop + 30f * mm
            val legendPaint = Paint().apply {
                color = Color.BLACK
                textSize = 3.5f * mm
                isAntiAlias = true
            }
            canvas.drawText("Условные обозначения:", legendLeft, legendTop, legendPaint)
            listOf("⏚  Розетки 220В", "⏛  Выключатели", "✕  Освещение", "▣  Щиты/короба", "•  Нагрузка").forEachIndexed { i, item ->
                canvas.drawText(item, legendLeft, legendTop + (i + 1) * 7f * mm, legendPaint)
            }

            // ГОСТ-штамп
            run {
                val mmPx = canvas.width / 297f
                val sl = canvas.width - 195f * mmPx
                val st = canvas.height - 65f * mmPx
                val sr = canvas.width - 5f * mmPx
                val sb = canvas.height - 5f * mmPx
                val stampFrame = Paint(framePaint)
                stampFrame.strokeWidth = 0.6f * mmPx
                canvas.drawRect(sl, st, sr, sb, stampFrame)
                canvas.drawLine(sl + 60f * mmPx, st, sl + 60f * mmPx, sb, stampFrame)
                canvas.drawLine(sl, st + 40f * mmPx, sr, st + 40f * mmPx, stampFrame)
                val ts = Paint().apply { color = Color.BLACK; isAntiAlias = true }
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

            document.finishPage(page)
        }

        val outputFile = File(outputDir, projectName + ".pdf")
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
        return outputFile
    }
}
