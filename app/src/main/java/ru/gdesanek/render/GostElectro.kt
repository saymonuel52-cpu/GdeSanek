package ru.gdesanek.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import ru.gdesanek.core.ArchTypes

/**
 * УГО электро по ГОСТ 21.614-88 и ГОСТ 2.721-74 (как в эталонных проектах
 * Ватсон / ТЦ "Небо" / Чеченино и типовых блоках 5.407.1-155).
 * Архитектура и мебель делегируются в прежний GostSymbols.
 */
object GostElectro {

    fun draw(c: Canvas, type: String, x: Float, y: Float, rot: Float, p: Paint, thickness: Float = 1f) {
        if (ArchTypes.isArch(type) || ArchTypes.isFurn(type)) {
            GostSymbols.draw(c, type, x, y, rot, p, thickness)
            return
        }
        val r = 12f
        val t = type.lowercase()
        c.save(); c.translate(x, y); c.rotate(rot)
        when {
            t.contains("junction") || t.contains("box") -> drawBox(c, p, r)
            t.contains("motion") || t.contains("sensor") || t.contains("датчик") -> drawSensor(c, p, r)
            t.startsWith("socket_") -> drawSocket(c, p, r, t)
            t.startsWith("switch_") -> drawSwitch(c, p, r, t)
            t.startsWith("lamp_") -> drawLamp(c, p, r, t)
            t.startsWith("sks_") || t.startsWith("tv_") || t.startsWith("rj45") -> drawWeak(c, p, r, t)
            t.startsWith("panel_") || t.startsWith("box_") -> drawPanel(c, p, r)
            t.startsWith("cons_") -> drawFan(c, p, r)
            else -> c.drawCircle(0f, 0f, r * 0.5f, p)
        }
        c.restore()
    }

    // Распаячная коробка: квадратик с крестом (ГОСТ 21.614-88)
    private fun drawBox(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.7f, -r * 0.7f, r * 0.7f, r * 0.7f, p)
        c.drawLine(-r * 0.7f, -r * 0.7f, r * 0.7f, r * 0.7f, p)
        c.drawLine(-r * 0.7f, r * 0.7f, r * 0.7f, -r * 0.7f, p)
    }

    // Розетка: полукруг хордой вниз + штрихи 45° по числу гнёзд; IP44 — залитый
    private fun drawSocket(c: Canvas, p: Paint, r: Float, t: String) {
        val dome = Path()
        dome.moveTo(-r, 0f)
        dome.arcTo(RectF(-r, -r, r, r), 180f, 180f)
        dome.close()
        if (t.contains("ip44")) {
            val fp = Paint(p); fp.style = Paint.Style.FILL
            c.drawPath(dome, fp)
        } else c.drawPath(dome, p)
        val n = when {
            t.contains("380") || t.contains("b3") || t.contains("3") -> 3
            t.contains("b2") || t.contains("2") -> 2
            else -> 1
        }
        for (i in 0 until n) {
            val sx = -r * 0.5f + i * r * 0.5f
            val sy = -kotlin.math.sqrt(kotlin.math.max(0f, r * r - sx * sx)) * 0.9f
            c.drawLine(sx, sy, sx + r * 0.55f, sy - r * 0.55f, p)
        }
    }

    // Выключатель: круг + ручка 45° с рисками по числу клавиш
    private fun drawSwitch(c: Canvas, p: Paint, r: Float, t: String) {
        c.drawCircle(0f, 0f, r * 0.45f, p)
        c.drawLine(r * 0.32f, -r * 0.32f, r * 1.05f, -r * 1.05f, p)
        val n = when { t.contains("3") -> 3; t.contains("2") -> 2; else -> 1 }
        for (i in 0 until n) {
            val d = r * 0.55f + i * r * 0.26f
            c.drawLine(d - r * 0.14f, -d - r * 0.14f, d + r * 0.14f, -d + r * 0.14f, p)
        }
    }

    // Светильники: круг с Х (накаливание), прямоугольник со штрихами (ЛДС), бра, спот
    private fun drawLamp(c: Canvas, p: Paint, r: Float, t: String) {
        when {
            t.contains("lum") || t.contains("lds") -> {
                c.drawRect(-r * 0.9f, -r * 0.28f, r * 0.9f, r * 0.28f, p)
                c.drawLine(-r * 0.45f, r * 0.28f, -r * 0.15f, -r * 0.28f, p)
                c.drawLine(r * 0.15f, r * 0.28f, r * 0.45f, -r * 0.28f, p)
            }
            t.contains("spot") -> {
                val fp = Paint(p); fp.style = Paint.Style.FILL
                c.drawCircle(0f, 0f, r * 0.35f, fp)
            }
            t.contains("br") || t.contains("wall") -> {
                c.drawCircle(0f, 0f, r * 0.5f, p)
                c.drawLine(r * 0.5f, 0f, r * 0.95f, 0f, p)
            }
            else -> {
                c.drawCircle(0f, 0f, r * 0.8f, p)
                c.drawLine(-r * 0.57f, -r * 0.57f, r * 0.57f, r * 0.57f, p)
                c.drawLine(-r * 0.57f, r * 0.57f, r * 0.57f, -r * 0.57f, p)
            }
        }
    }

    // Слаботочка: прямоугольник с литерой
    private fun drawWeak(c: Canvas, p: Paint, r: Float, t: String) {
        c.drawRect(-r * 0.7f, -r * 0.5f, r * 0.7f, r * 0.5f, p)
        val tp = Paint(p); tp.style = Paint.Style.FILL; tp.textSize = r * 0.62f; tp.textAlign = Paint.Align.CENTER
        c.drawText(if (t.contains("rj45") || t.contains("inet") || t.contains("интернет")) "RJ" else "ТВ", 0f, r * 0.22f, tp)
    }

    // Щит/короб: прямоугольник с диагональю
    private fun drawPanel(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.9f, -r * 0.65f, r * 0.9f, r * 0.65f, p)
        c.drawLine(-r * 0.9f, r * 0.65f, r * 0.9f, -r * 0.65f, p)
    }

    // Вентилятор/вытяжка: круг с лопастями + стрелка выброса (ГОСТ 2.721-74)
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

    // Датчик движения: круг + две дуги-волны
    private fun drawSensor(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.45f, p)
        c.drawArc(RectF(-r * 0.2f, -r * 0.9f, r * 1.3f, r * 0.9f), -40f, 80f, false, p)
        c.drawArc(RectF(0f, -r * 0.55f, r * 0.95f, r * 0.55f), -40f, 80f, false, p)
    }
}
