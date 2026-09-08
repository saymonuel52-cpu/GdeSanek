package ru.gdesanek.render
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object GostSymbols {
    fun draw(canvas: Canvas, type: String, x: Float, y: Float, rotation: Float, paint: Paint) {
        when (type) {
            "socket_b1" -> drawSocketN(canvas, x, y, rotation, paint, 1, false)
            "socket_b2", "socket_double" -> drawSocketN(canvas, x, y, rotation, paint, 2, false)
            "socket_b4" -> drawSocketN(canvas, x, y, rotation, paint, 3, false)
            "socket_b3" -> drawSocketN(canvas, x, y, rotation, paint, 1, true)
            "socket_k" -> drawSocketGrounded(canvas, x, y, rotation, paint)
            "socket_380" -> drawSocket380(canvas, x, y, rotation, paint)
            "socket_block2" -> drawSocketBlock(canvas, x, y, rotation, paint, 2)
            "socket_block3" -> drawSocketBlock(canvas, x, y, rotation, paint, 3)
            "socket_block4" -> drawSocketBlock(canvas, x, y, rotation, paint, 4)
            "switch_o", "switch_1" -> drawSwitch(canvas, x, y, rotation, paint, 1)
            "switch_2" -> drawSwitch(canvas, x, y, rotation, paint, 2)
            "switch_3" -> drawSwitch(canvas, x, y, rotation, paint, 3)
            "switch_pass" -> drawSwitchPass(canvas, x, y, rotation, paint)
            "switch_dim" -> drawSwitchDimmer(canvas, x, y, rotation, paint)
            "switch_move" -> drawSwitchMotion(canvas, x, y, rotation, paint)
            "lamp_titan" -> drawLampLinear(canvas, x, y, rotation, paint, 20f)
            "lamp_flame" -> drawLampLinear(canvas, x, y, rotation, paint, 40f)
            "lamp_grig" -> drawLampSpot(canvas, x, y, rotation, paint)
            "lamp_lust" -> drawLampChandelier(canvas, x, y, rotation, paint)
            "lamp_bra" -> drawLampBra(canvas, x, y, rotation, paint)
            "lamp_led" -> drawLampLedStrip(canvas, x, y, rotation, paint)
            "lamp_street" -> drawLampStreet(canvas, x, y, rotation, paint)
            "lamp_ao" -> drawLampEmergency(canvas, x, y, rotation, paint)
            "lamp_exit" -> drawLampExit(canvas, x, y, rotation, paint)
            "rj45" -> drawWeakCurrent(canvas, x, y, rotation, paint, "RJ45")
            "rj45x2" -> drawWeakCurrent(canvas, x, y, rotation, paint, "2xRJ45")
            "sks_tv" -> drawWeakCurrent(canvas, x, y, rotation, paint, "ТВ")
            "sks_phone" -> drawWeakCurrent(canvas, x, y, rotation, paint, "Тел")
            "sks_intercom" -> drawWeakCurrent(canvas, x, y, rotation, paint, "Домоф")
            "sks_cam" -> drawCamera(canvas, x, y, rotation, paint)
            "sks_smoke" -> drawSmokeDetector(canvas, x, y, rotation, paint)
            "sks_sec" -> drawSecuritySensor(canvas, x, y, rotation, paint)
            "box_rk" -> drawJunctionBox(canvas, x, y, rotation, paint)
            "panel_shr" -> drawPanel(canvas, x, y, rotation, paint, "ЩР")
            "panel_sks" -> drawPanel(canvas, x, y, rotation, paint, "СКС")
            "input_220" -> drawInput(canvas, x, y, rotation, paint)
            "ground" -> drawGround(canvas, x, y, rotation, paint)
            "cond_vk" -> drawConditioner(canvas, x, y, rotation, paint)
            "cons_hood" -> drawHood(canvas, x, y, rotation, paint)
            "cons_boiler" -> drawConsumer(canvas, x, y, rotation, paint, "Бойлер")
            "cons_stove" -> drawConsumer(canvas, x, y, rotation, paint, "Плита")
            "cons_pump" -> drawConsumer(canvas, x, y, rotation, paint, "Насос")
        }
    }

    private fun stroke(paint: Paint, w: Float = 4f) = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = w; isAntiAlias = true }
    private fun fill(paint: Paint) = Paint(paint).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private fun text(paint: Paint, s: Float) = Paint(paint).apply { style = Paint.Style.FILL; textSize = s; textAlign = Paint.Align.CENTER; isAntiAlias = true }

    private fun drawSocketN(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint, n: Int, ip44: Boolean) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 14f + n * 5f
        if (ip44) canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, true, fill(paint))
        else canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, stroke(paint))
        val p = stroke(paint)
        for (i in 0 until n) {
            val px = (i - (n - 1) / 2f) * (r * 1.1f / n + 6f)
            canvas.drawLine(px, 0f, px, -r - 10f, p)
        }
        canvas.restore()
    }

    private fun drawSocketGrounded(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 20f; val p = stroke(paint)
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        canvas.drawLine(-9f, -r - 10f, 9f, -r - 10f, p)
        canvas.restore()
    }

    private fun drawSocket380(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 24f; val p = stroke(paint)
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, p)
        for (px in listOf(-12f, 0f, 12f)) canvas.drawLine(px, 0f, px, -r - 10f, p)
        canvas.drawText("380", 0f, 16f, text(paint, 12f))
        canvas.restore()
    }

    private fun drawSocketBlock(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint, n: Int) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val w = n * 24f + 16f; val p = stroke(paint, 3f)
        canvas.drawRoundRect(RectF(-w / 2, -18f, w / 2, 18f), 6f, 6f, p)
        for (i in 0 until n) {
            val cx = -w / 2 + 20f + i * 24f
            canvas.drawArc(RectF(cx - 8f, -4f, cx + 8f, 12f), 180f, 180f, false, p)
            canvas.drawLine(cx, 4f, cx, -8f, p)
        }
        canvas.restore()
    }

    private fun drawSwitch(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint, keys: Int) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 14f; val p = stroke(paint)
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(r * 0.7f, -r * 0.7f, r * 0.7f + 18f, -r * 0.7f - 18f, p)
        for (i in 0 until keys) {
            val t = 8f + i * 6f
            val px = r * 0.7f + t * 0.707f; val py = -r * 0.7f - t * 0.707f
            canvas.drawLine(px - 3f, py - 3f, px + 3f, py + 3f, p)
        }
        canvas.restore()
    }

    private fun drawSwitchPass(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 14f; val p = stroke(paint)
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(r * 0.7f, -r * 0.7f, r * 0.7f + 18f, -r * 0.7f - 18f, p)
        canvas.drawLine(r * 0.7f + 5f, -r * 0.7f - 1f, r * 0.7f + 23f, -r * 0.7f - 19f, p)
        canvas.restore()
    }

    private fun drawSwitchDimmer(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 14f; val p = stroke(paint)
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(r * 0.7f, -r * 0.7f, r * 0.7f + 18f, -r * 0.7f - 18f, p)
        canvas.drawText("~", 0f, 5f, text(paint, 14f))
        canvas.restore()
    }

    private fun drawSwitchMotion(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 14f; val p = stroke(paint)
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(r * 0.7f, -r * 0.7f, r * 0.7f + 18f, -r * 0.7f - 18f, p)
        canvas.drawArc(RectF(-r - 9f, -r - 9f, r + 9f, r + 9f), 120f, 70f, false, stroke(paint, 2f))
        canvas.restore()
    }

    private fun drawLampLinear(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint, w: Float) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint, 3f)
        canvas.drawRect(RectF(-w, -7f, w, 7f), p)
        canvas.drawLine(-w, 0f, w, 0f, p)
        canvas.restore()
    }

    private fun drawLampSpot(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawCircle(0f, 0f, 10f, fill(paint))
        val p = stroke(paint, 2f)
        for (a in listOf(45f, 135f, 225f, 315f)) {
            val dx = kotlin.math.cos(Math.toRadians(a.toDouble())).toFloat(); val dy = kotlin.math.sin(Math.toRadians(a.toDouble())).toFloat()
            canvas.drawLine(dx * 12f, dy * 12f, dx * 17f, dy * 17f, p)
        }
        canvas.restore()
    }

    private fun drawLampChandelier(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val r = 18f; val p = stroke(paint, 3f)
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(-r, 0f, r, 0f, p)
        canvas.drawLine(0f, -r, 0f, r, p)
        canvas.restore()
    }

    private fun drawLampBra(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint, 3f)
        canvas.drawLine(-16f, 0f, 16f, 0f, p)
        canvas.drawArc(RectF(-12f, -14f, 12f, 10f), 180f, 180f, false, p)
        canvas.restore()
    }

    private fun drawLampLedStrip(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawLine(-30f, 0f, 30f, 0f, stroke(paint, 6f))
        for (dx in listOf(-18f, 0f, 18f)) canvas.drawCircle(dx, -8f, 3f, fill(paint))
        canvas.restore()
    }

    private fun drawLampStreet(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint, 3f)
        canvas.drawCircle(0f, -6f, 12f, p)
        canvas.drawLine(0f, 6f, 0f, 24f, p)
        canvas.restore()
    }

    private fun drawLampEmergency(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawCircle(0f, 0f, 16f, stroke(paint, 3f))
        canvas.drawText("!", 0f, 6f, text(paint, 18f))
        canvas.restore()
    }

    private fun drawLampExit(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawRect(RectF(-20f, -10f, 20f, 10f), stroke(paint, 3f))
        canvas.drawText("ВЫХ", 0f, 4f, text(paint, 11f))
        canvas.restore()
    }

    private fun drawWeakCurrent(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint, label: String) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawRect(RectF(-19f, -13f, 19f, 13f), stroke(paint, 3f))
        canvas.drawText(label, 0f, 4f, text(paint, 12f))
        canvas.restore()
    }

    private fun drawCamera(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint, 3f)
        canvas.drawRect(RectF(-15f, -10f, 15f, 10f), p)
        canvas.drawCircle(23f, 0f, 8f, p)
        canvas.restore()
    }

    private fun drawSmokeDetector(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint, 3f)
        canvas.drawCircle(0f, 0f, 16f, p)
        canvas.drawCircle(0f, 0f, 8f, p)
        canvas.restore()
    }

    private fun drawSecuritySensor(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawCircle(0f, 0f, 14f, stroke(paint, 3f))
        canvas.drawArc(RectF(-24f, -24f, 24f, 24f), 120f, 120f, false, stroke(paint, 2f))
        canvas.restore()
    }

    private fun drawJunctionBox(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawCircle(0f, 0f, 14f, fill(paint))
        canvas.drawText("РК", 0f, 5f, text(paint, 12f).apply { color = android.graphics.Color.WHITE })
        canvas.restore()
    }

    private fun drawPanel(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint, label: String) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawRect(RectF(-25f, -20f, 25f, 20f), stroke(paint))
        canvas.drawText(label, 0f, 6f, text(paint, 15f))
        canvas.restore()
    }

    private fun drawInput(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint)
        canvas.drawRect(RectF(-25f, -20f, 25f, 20f), p)
        canvas.drawLine(-25f, 0f, 25f, 0f, p)
        canvas.drawText("220В", 0f, -6f, text(paint, 11f))
        canvas.restore()
    }

    private fun drawGround(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint)
        canvas.drawLine(0f, -20f, 0f, 0f, p)
        canvas.drawLine(-20f, 0f, 20f, 0f, p)
        canvas.drawLine(-12f, 8f, 12f, 8f, p)
        canvas.drawLine(-4f, 16f, 4f, 16f, p)
        canvas.restore()
    }

    private fun drawConditioner(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawRect(RectF(-24f, -16f, 24f, 16f), stroke(paint, 3f))
        val p = stroke(paint, 2f)
        for (wy in listOf(-8f, 0f, 8f)) {
            val path = Path()
            path.moveTo(-16f, wy)
            path.quadTo(-8f, wy - 4f, 0f, wy)
            path.quadTo(8f, wy + 4f, 16f, wy)
            canvas.drawPath(path, p)
        }
        canvas.restore()
    }

    private fun drawHood(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        val p = stroke(paint, 3f)
        canvas.drawCircle(0f, 0f, 16f, p)
        for (a in listOf(90f, 210f, 330f)) {
            val dx = kotlin.math.cos(Math.toRadians(a.toDouble())).toFloat(); val dy = -kotlin.math.sin(Math.toRadians(a.toDouble())).toFloat()
            canvas.drawLine(0f, 0f, dx * 12f, dy * 12f, p)
        }
        canvas.drawLine(0f, -16f, 0f, -30f, p)
        canvas.drawLine(-5f, -25f, 0f, -30f, p)
        canvas.drawLine(5f, -25f, 0f, -30f, p)
        canvas.restore()
    }

    private fun drawConsumer(canvas: Canvas, x: Float, y: Float, rot: Float, paint: Paint, label: String) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rot)
        canvas.drawRect(RectF(-24f, -14f, 24f, 14f), stroke(paint, 3f))
        canvas.drawText(label, 0f, 4f, text(paint, 11f))
        canvas.restore()
    }
}
