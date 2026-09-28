package ru.gdesanek.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import ru.gdesanek.core.ArchTypes

object GostElectro {

    fun draw(c: Canvas, type: String, x: Float, y: Float, rot: Float, p: Paint, thickness: Float = 1f, scale: Float = 1f) {
        if (ArchTypes.isArch(type) || ArchTypes.isFurn(type)) { GostSymbols.draw(c, type, x, y, rot, p, thickness); return }
        val r = 12f * scale
        val pw = Paint(p).apply { strokeWidth = maxOf(p.strokeWidth, r * 0.13f) }
        c.save(); c.translate(x, y); c.rotate(rot)
        when (type) {
            "socket_b1" -> drawSocket(c, pw, r, 1, false, false)
            "socket_b2" -> drawSocket(c, pw, r, 2, false, false)
            "socket_b4" -> drawSocket(c, pw, r, 3, false, false)
            "socket_k"  -> drawSocketK(c, pw, r)
            "socket_b3" -> drawSocket(c, pw, r, 1, true, false)
            "socket_380" -> drawSocket(c, pw, r, 3, false, true)
            "socket_block2" -> drawBlock(c, pw, r, 2)
            "socket_block3" -> drawBlock(c, pw, r, 3)
            "socket_block4" -> drawBlock(c, pw, r, 4)
            "switch_1" -> drawSwitch(c, pw, r, 1)
            "switch_2" -> drawSwitch(c, pw, r, 2)
            "switch_3" -> drawSwitch(c, pw, r, 3)
            "switch_pass" -> drawPassSwitch(c, pw, r)
            "switch_dim" -> drawDimmer(c, pw, r)
            "switch_move" -> drawSensor(c, pw, r)
            "lamp_lust" -> drawChandelier(c, pw, r)
            "lamp_grig" -> drawSpot(c, pw, r)
            "lamp_titan" -> drawLinear(c, pw, r, "600")
            "lamp_flame" -> drawLinear(c, pw, r, "1200")
            "lamp_bra" -> drawBra(c, pw, r)
            "lamp_led" -> drawLedStrip(c, pw, r)
            "lamp_street" -> drawStreetLamp(c, pw, r)
            "lamp_ao" -> drawEmergency(c, pw, r)
            "lamp_exit" -> drawExitSign(c, pw, r)
            "rj45" -> drawWeakBox(c, pw, r, "RJ")
            "rj45x2" -> drawWeakBox(c, pw, r, "2RJ")
            "sks_tv" -> drawWeakBox(c, pw, r, "ТВ")
            "sks_phone" -> drawWeakBox(c, pw, r, "ТЕЛ")
            "sks_intercom" -> drawWeakBox(c, pw, r, "ДОМ")
            "sks_cam" -> drawWeakBox(c, pw, r, "КАМ")
            "sks_smoke" -> drawSmoke(c, pw, r)
            "sks_sec" -> drawSec(c, pw, r)
            "panel_shr" -> drawPanel(c, pw, r, "ЩР")
            "panel_sks" -> drawPanel(c, pw, r, "ЩС")
            "box_rk" -> drawJunctionBox(c, pw, r)
            "input_220" -> drawInput(c, pw, r)
            "ground" -> drawGround(c, pw, r)
            "cond_vk" -> drawCond(c, pw, r)
            "cons_hood" -> drawFan(c, pw, r)
            "cons_boiler" -> drawBoiler(c, pw, r)
            "cons_stove" -> drawStove(c, pw, r)
            "cons_pump" -> drawPump(c, pw, r)
            else -> { c.drawCircle(0f, 0f, r * 0.6f, pw) }
        }
        c.restore()
    }

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
    private fun drawSocketK(c: Canvas, p: Paint, r: Float) {
        drawSocket(c, p, r, 1, false, false)
        c.drawLine(0f, r * 0.15f, 0f, r * 0.6f, p)
        c.drawLine(-r * 0.25f, r * 0.6f, r * 0.25f, r * 0.6f, p)
    }
    private fun drawBlock(c: Canvas, p: Paint, r: Float, n: Int) {
        val step = r * 0.7f; val startX = -(n - 1) * step / 2f
        for (i in 0 until n) {
            val cx = startX + i * step
            val dome = Path().apply { moveTo(cx - r * 0.35f, 0f); arcTo(RectF(cx - r * 0.35f, -r * 0.35f, cx + r * 0.35f, r * 0.35f), 180f, 180f); close() }
            c.drawPath(dome, p); c.drawLine(cx, -r * 0.35f, cx + r * 0.3f, -r * 0.65f, p)
        }
    }
    private fun drawSwitch(c: Canvas, p: Paint, r: Float, keys: Int) {
        c.drawCircle(0f, 0f, r * 0.45f, p); c.drawLine(r * 0.32f, -r * 0.32f, r * 1.05f, -r * 1.05f, p)
        for (i in 0 until keys) { val d = r * 0.55f + i * r * 0.26f; c.drawLine(d - r * 0.22f, -d - r * 0.22f, d + r * 0.22f, -d + r * 0.22f, p) }
    }
    private fun drawPassSwitch(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.45f, p)
        c.drawLine(r * 0.32f, -r * 0.32f, r * 1.0f, -r * 1.0f, p)
        c.drawLine(r * 0.32f, r * 0.32f, r * 1.0f, r * 1.0f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.5f; textAlign = Paint.Align.CENTER }
        c.drawText("П", 0f, r * 0.18f, tp)
    }
    private fun drawDimmer(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.55f, p); c.drawLine(r * 0.38f, -r * 0.38f, r * 1.0f, -r * 1.0f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.6f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("N", 0f, r * 0.22f, tp)
    }
    private fun drawSensor(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.45f, p)
        c.drawArc(RectF(-r * 0.2f, -r * 0.9f, r * 1.3f, r * 0.9f), -40f, 80f, false, p)
        c.drawArc(RectF(0f, -r * 0.55f, r * 0.95f, r * 0.55f), -40f, 80f, false, p)
    }
    private fun drawChandelier(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.8f, p)
        c.drawLine(-r * 0.57f, -r * 0.57f, r * 0.57f, r * 0.57f, p); c.drawLine(-r * 0.57f, r * 0.57f, r * 0.57f, -r * 0.57f, p)
    }
    private fun drawSpot(c: Canvas, p: Paint, r: Float) { val fp = Paint(p).apply { style = Paint.Style.FILL }; c.drawCircle(0f, 0f, r * 0.35f, fp) }
    private fun drawLinear(c: Canvas, p: Paint, r: Float, len: String) {
        c.drawRect(-r * 0.9f, -r * 0.28f, r * 0.9f, r * 0.28f, p)
        c.drawLine(-r * 0.45f, r * 0.28f, -r * 0.15f, -r * 0.28f, p); c.drawLine(r * 0.15f, r * 0.28f, r * 0.45f, -r * 0.28f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.45f; textAlign = Paint.Align.CENTER }
        c.drawText(len, 0f, r * 0.85f, tp)
    }
    private fun drawBra(c: Canvas, p: Paint, r: Float) { c.drawCircle(0f, 0f, r * 0.5f, p); c.drawLine(r * 0.5f, 0f, r * 0.95f, 0f, p) }
    private fun drawLedStrip(c: Canvas, p: Paint, r: Float) {
        c.drawLine(-r, 0f, r, 0f, p)
        c.drawLine(-r, 0f, -r * 0.7f, -r * 0.2f, p); c.drawLine(-r, 0f, -r * 0.7f, r * 0.2f, p)
        c.drawLine(r, 0f, r * 0.7f, -r * 0.2f, p); c.drawLine(r, 0f, r * 0.7f, r * 0.2f, p)
        val fp = Paint(p).apply { style = Paint.Style.FILL }
        for (i in 0..3) c.drawCircle(-r * 0.6f + i * r * 0.4f, 0f, r * 0.1f, fp)
    }
    private fun drawStreetLamp(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.5f, p)
        for (a in 0..7) { val ang = Math.toRadians((a * 45).toDouble()); c.drawLine((kotlin.math.cos(ang) * r * 0.65).toFloat(), (kotlin.math.sin(ang) * r * 0.65).toFloat(), (kotlin.math.cos(ang) * r).toFloat(), (kotlin.math.sin(ang) * r).toFloat(), p) }
    }
    private fun drawEmergency(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.7f, p)
        val bolt = Path().apply { moveTo(-r * 0.15f, -r * 0.45f); lineTo(r * 0.15f, -r * 0.05f); lineTo(-r * 0.05f, -r * 0.05f); lineTo(r * 0.15f, r * 0.45f); lineTo(-r * 0.15f, r * 0.05f); lineTo(r * 0.05f, r * 0.05f); close() }
        val fp = Paint(p).apply { style = Paint.Style.FILL }; c.drawPath(bolt, fp)
    }
    private fun drawExitSign(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.95f, -r * 0.35f, r * 0.95f, r * 0.35f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.5f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("Вых", 0f, r * 0.17f, tp)
    }
    private fun drawWeakBox(c: Canvas, p: Paint, r: Float, label: String) {
        c.drawRect(-r * 0.7f, -r * 0.5f, r * 0.7f, r * 0.5f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.45f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText(label, 0f, r * 0.16f, tp)
    }
    private fun drawSmoke(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.7f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("S", 0f, r * 0.25f, tp)
    }
    private fun drawSec(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.42f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("ОХР", 0f, r * 0.15f, tp)
    }
    private fun drawPanel(c: Canvas, p: Paint, r: Float, label: String) {
        c.drawRect(-r * 0.9f, -r * 0.65f, r * 0.9f, r * 0.65f, p); c.drawLine(-r * 0.9f, r * 0.65f, r * 0.9f, -r * 0.65f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.55f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText(label, 0f, r * 0.2f, tp)
    }
    private fun drawJunctionBox(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.65f, -r * 0.65f, r * 0.65f, r * 0.65f, p)
        c.drawLine(-r * 0.65f, -r * 0.65f, r * 0.65f, r * 0.65f, p); c.drawLine(-r * 0.65f, r * 0.65f, r * 0.65f, -r * 0.65f, p)
    }
    private fun drawInput(c: Canvas, p: Paint, r: Float) {
        c.drawLine(-r, 0f, r * 0.7f, 0f, p); c.drawLine(r * 0.7f, 0f, r * 0.4f, -r * 0.25f, p); c.drawLine(r * 0.7f, 0f, r * 0.4f, r * 0.25f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.5f; textAlign = Paint.Align.CENTER }
        c.drawText("220", 0f, r * 0.9f, tp)
    }
    private fun drawGround(c: Canvas, p: Paint, r: Float) {
        c.drawLine(0f, -r * 0.6f, 0f, 0f, p); c.drawLine(-r * 0.5f, 0f, r * 0.5f, 0f, p)
        c.drawLine(-r * 0.3f, r * 0.2f, r * 0.3f, r * 0.2f, p); c.drawLine(-r * 0.15f, r * 0.4f, r * 0.15f, r * 0.4f, p)
    }
    private fun drawCond(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.9f, -r * 0.4f, r * 0.9f, r * 0.4f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.6f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("К", 0f, r * 0.22f, tp)
    }
    private fun drawFan(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        for (a in 0..2) { val ang = Math.toRadians((a * 120).toDouble()); c.drawLine(0f, 0f, (kotlin.math.cos(ang) * r * 0.5).toFloat(), (kotlin.math.sin(ang) * r * 0.5).toFloat(), p) }
        c.drawLine(r * 0.6f, 0f, r * 1.1f, 0f, p); c.drawLine(r * 1.1f, 0f, r * 0.9f, -r * 0.18f, p); c.drawLine(r * 1.1f, 0f, r * 0.9f, r * 0.18f, p)
    }
    private fun drawBoiler(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.7f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.8f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("Т", 0f, r * 0.28f, tp)
    }
    private fun drawStove(c: Canvas, p: Paint, r: Float) {
        c.drawRect(-r * 0.85f, -r * 0.85f, r * 0.85f, r * 0.85f, p)
        for (dx in listOf(-0.45f, 0.45f)) for (dy in listOf(-0.45f, 0.45f)) c.drawCircle(dx * r, dy * r, r * 0.22f, p)
    }
    private fun drawPump(c: Canvas, p: Paint, r: Float) {
        c.drawCircle(0f, 0f, r * 0.6f, p)
        c.drawLine(-r * 1.1f, 0f, -r * 0.6f, 0f, p); c.drawLine(r * 0.6f, 0f, r * 1.1f, 0f, p)
        c.drawLine(r * 1.1f, 0f, r * 0.85f, -r * 0.2f, p); c.drawLine(r * 1.1f, 0f, r * 0.85f, r * 0.2f, p)
        val tp = Paint(p).apply { style = Paint.Style.FILL; textSize = r * 0.5f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText("Н", 0f, r * 0.18f, tp)
    }
}
