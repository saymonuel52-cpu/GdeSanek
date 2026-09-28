package ru.gdesanek.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import ru.gdesanek.core.ArchTypes

/**
 * УГО электро по ГОСТ 21.614-88, ГОСТ 2.721-74, ГОСТ 21.404-85, ГОСТ 21.406-88
 * и типовым блокам 5.407.1-155.
 * Каждому типу из Catalog соответствует уникальный знак.
 * Архитектура и мебель делегируются в прежний GostSymbols.
 */
object GostElectro {

    fun draw(c: Canvas, type: String, x: Float, y: Float, rot: Float, p: Paint, thickness: Float = 1f, scale: Float = 1f) {
        if (ArchTypes.isArch(type) || ArchTypes.isFurn(type)) {
            GostSymbols.draw(c, type, x, y, rot, p, thickness)
            return
        }
        val r = 12f * scale
        val pw = Paint(p).apply { strokeWidth = maxOf(p.strokeWidth, r * 0.13f) }
        c.save(); c.translate(x, y); c.rotate(rot)
        when (type) {
            // ===== РОЗЕТКИ 220В (ГОСТ 21.614-88) =====
            "socket_b1" -> drawSocket(c, pw, r, 1, false, false)
            "socket_b2" -> drawSocket(c, pw, r, 2, false, false)
            "socket_b4" -> drawSocket(c, pw, r, 3, false, false)
            "socket_k"  -> drawSocket(c, pw, r, 1, false, false) // с заземлением — базовый символ
            "socket_b3" -> drawSocket(c, pw, r, 1, true, false) // IP44
            "socket_380" -> drawSocket(c, pw, r, 3, false, true) // 380В
            "socket_block2" -> drawBlock(c, pw, r, 2)
            "socket_block3" -> drawBlock(c, pw, r, 3)
            "socket_block4" -> drawBlock(c, pw, r, 4)

            // ===== ВЫКЛЮЧАТЕЛИ (ГОСТ 21.614-88) =====
            "switch_1" -> drawSwitch(c, pw, r, 1)
            "switch_2" -> drawSwitch(c, pw, r, 2)
            "switch_3" -> drawSwitch(c, pw, r, 3)
            "switch_pass" -> drawPassSwitch(c, pw, r) // проходной — 2 ручки
            "switch_dim"  -> drawDimmer(c, pw, r)    // диммер — круг с N
            "switch_move" -> drawSensor(c, pw, r)    // датчик движения

            // ===== СВЕТИЛЬНИКИ (ГОСТ 21.614-88) =====
            "lamp_lust"  -> drawChandelier(c, pw, r) // круг с Х
            "lamp_grig"  -> drawSpot(c, pw, r)      // залитая точка
            "lamp_titan" -> drawLinear(c, pw, r, "600")
            "lamp_flame" -> drawLinear(c, pw, r, "1200")
            "lamp_bra"   -> drawBra(c, pw, r)       // полукруг на ножке
            "lamp_led"   -> drawLedStrip(c, pw, r)  // отрезок со стрелками
            "lamp_street"-> drawStreetLamp(c, pw, r)// круг с лучами
            "lamp_ao"    -> drawEmergency(c, pw, r) // круг с молнией
            "lamp_exit"  -> drawExitSign(c, pw, r)  // прямоугольник с "Вых"

            // ===== СЛАБОТОЧКА (ГОСТ 21.406-88) =====
            "rj45"   -> drawWeakBox(c, pw, r, "RJ")
            "rj45x2" -> drawWeakBox(c, pw, r, "2RJ")
            "sks_tv"       -> drawWeakBox(c, pw, r, "ТВ")
            "sks_phone"    -> drawWeakBox(c, pw, r, "ТЕЛ")
            "sks_intercom" -> drawWeakBox(c, pw, r, "ДОМ")
            "sks_cam"      -> drawWeakBox(c, pw, r, "КАМ")
            "sks_smoke"    -> drawSmokeDetector(c, pw, r) // круг с S
            "sks_sec"      -> drawSecSensor(c, pw, r)     // круг с ОХР

            // ===== ЩИТЫ И КОРОБКИ (ГОСТ 21.614-88) =====
            "panel_shr" -> drawPanel(c, pw, r, "ЩР")
            "panel_sks" -> drawPanel(c, pw, r, "ЩС")
            "box_rk"    -> drawJunctionBox(c, pw, r) // квадрат с крестом
            "input_220" -> drawInput(c, pw, r)       // линия со стрелкой
            "ground"    -> drawGround(c, pw, r)      // 3 горизонтальные линии

            // ===== КЛИМАТ И НАГРУЗКА (ГОСТ 2.721-74, 21.404-85) =====
            "cond_vk"    -> drawConditioner(c, pw, r) // прямоугольник с К
            "cons_hood"  -> drawFan(c, pw, r)          // вентилятор
            "cons_boiler"-> drawBoiler(c, pw, r)       // круг с Т
            "cons_stove" -> drawStove(c, pw, r)        // квадрат с 4 кружками
            "cons_pump"  -> drawPump(c, pw, r)         // круг со стрелкой

            else -> {
                // fallback — пустой круг, чтобы не падать
                c.drawCircle(0f, 0f, r * 0.6f, pw)
                val tp = Paint(pw).apply { style = Paint.Style.FILL; textSize = r * 0.7f; textAlign = Paint.Align.CENTER }
                c.drawText("?", 0f, r * 0.25f, tp)
            }
        }
        c.restore()
    }

    // ==================== РОЗЕТКИ ====================
    /** Базовая розетка: полукруг хордой вниз + N штрихов под 45°; IP44 = залитый; 380 = с полкой */
    private fun drawSocket(c: Canvas, p: Paint, r: Float, n: Int, ip44: Boolean, v380: Boolean) {
        val dome = Path().apply { moveTo(-r, 0f); arcTo(RectF(-r, -r, r, r), 180f, 180f); close() }
        if (ip44) { val fp = Paint(p).apply { style = Paint.Style.FILL }; c.drawPath(dome, fp) } else c.drawPath(dome, p)
        for (i in 0 until n) {
            val sx = -r * 0.5f + i * r * 0.5f
            val sy = -kotlin.math.sqrt(kotlin.math.max(0f, r * r - sx * sx)) * 0.9f
            c.drawLine(sx, sy, sx + r * 0.7f, sy - r * 0.7f, p)
        }
        if (v380) c.drawLine(-r, r * 0.35f, r, r * 0.35f, p)
    }

    /** Блок розеток — несколько полукругов подряд */
    private fun drawBlock(c: Canvas, p: Paint, r: Float, n: Int) {
        val step = r * 0.7f
        val startX = -(n - 1) * step / 2f
        for (i in 0 until n) {
            val cx = startX + i * step
            val dome = Path().apply { moveTo(cx - r * 0.35f, 0f); arcTo(RectF(cx - r * 0.35f, -r * 0.35f, cx + r * 0.35f, r * 0.35f), 180f, 180f); close() }
            c.drawPath(dome, p)
            c.drawLine(cx, -r * 0.35f, cx + r * 0.3f, -r * 0.65f, p)
        }
    }

    // ==================== ВЫКЛЮЧАТЕЛИ ====================
    private fun drawSwitch(c: Canvas, p: Paint, r: Float, keys: Int) {
        c.drawCircle(0f, 0f, r * 0.45f, p)
        c.drawLine(r * 0.32f, -r * 0.32f, r * 1.05f, -r * 1.05f, p)
        for (i in 0 until keys) {
            val d = r * 0.55f + i * r * 0.26f
            c.drawLine(d - r * 0.22f, -d - r * 0.22f, d + r * 0.22f, -d + r * 0.22f, p)
        }
    }

    /** Проходной — круг с двумя ручками под 45° */
    private fun drawPassSwitch(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.45f, p)
        c.drawLine(r * 0.32f, -r * 0.32f, r * 1.0f, -r * 1.0f, p)
        c.drawLine(r * 0.32f, r * 0.32f, r * 1.0f, r * 1.0f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.55f; textAlign = Paint.Align.CENTER }
        c.drawText("П", 0f, r * 0.18f, tp)
    }

    /** Диммер — круг с ручкой и литерой N внутри */
    private fun drawDimmer(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.55f, p)
        c.drawLine(r * 0.38f, -r * 0.38f, r * 1.0f, -r * 1.0f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.6f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("N", 0f, r * 0.22f, tp)
    }

    /** Датчик движения — круг + две дуги-волны */
    private fun drawSensor(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.45f, p)
        c.drawArc(RectF(-r * 0.2f, -r * 0.9f, r * 1.3f, r * 0.9f), -40f, 80f, false, p)
        c.drawArc(RectF(0f, -r * 0.55f, r * 0.95f, r * 0.55f), -40f, 80f, false, p)
    }

    // ==================== СВЕТИЛЬНИКИ ====================
    /** Люстра — круг с вписанным Х (накаливание) */
    private fun drawChandelier(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.8f, p)
        c.drawLine(-r * 0.57f, -r * 0.57f, r * 0.57f, r * 0.57f, p)
        c.drawLine(-r * 0.57f, r * 0.57f, r * 0.57f, -r * 0.57f, p)
    }

    /** Спот — залитая точка */
    private fun drawSpot(c: Canvas, p: Paint, r: Float) {
        val fp = Paint(p).apply { style = Paint.Style.FILL }
        c.drawCircle(0f, 0f, r * 0.35f, fp)
    }

    /** Линейный светильник (ЛДС) — прямоугольник со штрихами и подписью длины */
    private fun drawLinear(c: Canvas, p: Paint, r: Float, len: String) {
        c.drawRect(-r * 0.9f, -r * 0.28f, r * 0.9f, r * 0.28f, p)
        c.drawLine(-r * 0.45f, r * 0.28f, -r * 0.15f, -r * 0.28f, p)
        c.drawLine(r * 0.15f, r * 0.28f, r * 0.45f, -r * 0.28f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.45f; textAlign = Paint.Align.CENTER }
        c.drawText(len, 0f, r * 0.8f, tp)
    }

    /** Бра — полукруг на ножке */
    private fun drawBra(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.5f, p)
        c.drawLine(r * 0.5f, 0f, r * 0.95f, 0f, p)
    }

    /** LED-лента — отрезок со стрелками по концам */
    private fun drawLedStrip(c: Canvas, p: Paint, r: Float) {
        c.drawLine(-r, 0f, r, 0f, p)
        c.drawLine(-r, 0f, -r * 0.7f, -r * 0.2f, p); c.drawLine(-r, 0f, -r * 0.7f, r * 0.2f, p)
        c.drawLine(r, 0f, r * 0.7f, -r * 0.2f, p);  c.drawLine(r, 0f, r * 0.7f, r * 0.2f, p)
        for (i in 0..3) {
            val xx = -r * 0.6f + i * r * 0.4f
            val fp = Paint(p).apply { style = Paint.Style.FILL }
            c.drawCircle(xx, 0f, r * 0.1f, fp)
        }
    }

    /** Уличный фонарь — круг с лучами */
    private fun drawStreetLamp(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.5f, p)
        for (a in 0..7) {
            val ang = Math.toRadians((a * 45).toDouble())
            c.drawLine((kotlin.math.cos(ang) * r * 0.65).toFloat(), (kotlin.math.sin(ang) * r * 0.65).toFloat(),
                       (kotlin.math.cos(ang) * r * 1.0f).toFloat(), (kotlin.math.sin(ang) * r * 1.0f).toFloat(), p)
        }
    }

    /** Аварийный свет — круг с молнией */
    private fun drawEmergency(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.7f, p)
        val bolt = Path().apply {
            moveTo(-r * 0.15f, -r * 0.45f); lineTo(r * 0.15f, -r * 0.05f); lineTo(-r * 0.05f, -r * 0.05f)
            lineTo(r * 0.15f, r * 0.45f); lineTo(-r * 0.15f, r * 0.05f); lineTo(r * 0.05f, r * 0.05f); close()
        }
        val fp = Paint(p).apply { style = Paint.Style.FILL }
        c.drawPath(bolt, fp)
    }

    /** Табло ВЫХОД — прямоугольник с текстом */
    private fun drawExitSign(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.95f, -r * 0.35f, r * 0.95f, r * 0.35f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.55f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("Вых", 0f, r * 0.18f, tp)
    }

    // ==================== СЛАБОТОЧКА ====================
    /** Универсальный слаботочный прямоугольник с литерой */
    private fun drawWeakBox(c: Canvas, p: Paint, r: Float, label: String) {
        c.drawRect(-r * 0.7f, -r * 0.5f, r * 0.7f, r * 0.5f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.5f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText(label, 0f, r * 0.18f, tp)
    }

    /** Дымовой датчик — круг с S */
    private fun drawSmokeDetector(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.7f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("S", 0f, r * 0.25f, tp)
    }

    /** Охранный датчик — круг с ОХР */
    private fun drawSecSensor(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.45f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("ОХР", 0f, r * 0.18f, tp)
    }

    // ==================== ЩИТЫ И КОРОБКИ ====================
    /** Щит — прямоугольник с диагональю и подписью */
    private fun drawPanel(c: Canvas, p: Paint, r: Float, label: String) {
        c.drawRect(-r * 0.9f, -r * 0.65f, r * 0.9f, r * 0.65f, p)
        c.drawLine(-r * 0.9f, r * 0.65f, r * 0.9f, -r * 0.65f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.55f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText(label, 0f, r * 0.2f, tp)
    }

    /** Распаячная коробка — квадрат с крестом */
    private fun drawJunctionBox(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.65f, -r * 0.65f, r * 0.65f, r * 0.65f, p)
        c.drawLine(-r * 0.65f, -r * 0.65f, r * 0.65f, r * 0.65f, p)
        c.drawLine(-r * 0.65f, r * 0.65f, r * 0.65f, -r * 0.65f, p)
    }

    /** Ввод 220В — линия со стрелкой и подписью */
    private fun drawInput(c: Canvas, p: Paint, r: Float) {
        c.drawLine(-r, 0f, r * 0.7f, 0f, p)
        c.drawLine(r * 0.7f, 0f, r * 0.4f, -r * 0.25f, p)
        c.drawLine(r * 0.7f, 0f, r * 0.4f, r * 0.25f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.5f; textAlign = Paint.Align.CENTER }
        c.drawText("220", 0f, r * 0.9f, tp)
    }

    /** Заземление — три горизонтальные линии */
    private fun drawGround(c: Canvas, p: Paint, r: Float) {
        c.drawLine(0f, -r * 0.6f, 0f, 0f, p)
        c.drawLine(-r * 0.5f, 0f, r * 0.5f, 0f, p)
        c.drawLine(-r * 0.3f, r * 0.2f, r * 0.3f, r * 0.2f, p)
        c.drawLine(-r * 0.15f, r * 0.4f, r * 0.15f, r * 0.4f, p)
    }

    // ==================== КЛИМАТ И НАГРУЗКА ====================
    /** Кондиционер — прямоугольник с К */
    private fun drawConditioner(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.9f, -r * 0.4f, r * 0.9f, r * 0.4f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.7f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("К", 0f, r * 0.25f, tp)
    }

    /** Вентилятор — круг с лопастями и стрелкой выброса (ГОСТ 2.721-74) */
    private fun drawFan(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        for (a in 0..2) {
            val ang = Math.toRadians((a * 120).toDouble())
            c.drawLine(0f, 0f, (kotlin.math.cos(ang) * r * 0.5).toFloat(), (kotlin.math.sin(ang) * r * 0.5).toFloat(), p)
        }
        c.drawLine(r * 0.6f, 0f, r * 1.1f, 0f, p)
        c.drawLine(r * 1.1f, 0f, r * 0.9f, -r * 0.18f, p)
        c.drawLine(r * 1.1f, 0f, r * 0.9f, r * 0.18f, p)
    }

    /** Бойлер — круг с Т внутри */
    private fun drawBoiler(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.7f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.85f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("Т", 0f, r * 0.3f, tp)
    }

    /** Плита — квадрат с 4 кружками (конфорки) */
    private fun drawStove(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.85f, -r * 0.85f, r * 0.85f, r * 0.85f, p)
        val rr = r * 0.22f
        for (dx in listOf(-0.45f, 0.45f)) for (dy in listOf(-0.45f, 0.45f)) {
            c.drawCircle(dx * r, dy * r, rr, p)
        }
    }

    /** Насос — круг со стрелкой (ГОСТ 2.721-74) */
    private fun drawPump(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        c.drawLine(-r * 1.1f, 0f, -r * 0.6f, 0f, p)
        c.drawLine(r * 0.6f, 0f, r * 1.1f, 0f, p)
        c.drawLine(r * 1.1f, 0f, r * 0.85f, -r * 0.2f, p)
        c.drawLine(r * 1.1f, 0f, r * 0.85f, r * 0.2f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.55f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("Н", 0f, r * 0.2f, tp)
    }
}
