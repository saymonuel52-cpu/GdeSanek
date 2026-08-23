package ru.gdesanek.render
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object GostSymbols {
    fun draw(canvas: Canvas, type: String, x: Float, y: Float, rotation: Float, paint: Paint) {
        when(type) {
            "socket_b1" -> drawSocketProtected(canvas, x, y, rotation, paint)
            "socket_b2" -> drawSocketDouble(canvas, x, y, rotation, paint)
            "socket_b3" -> drawSocketIp44(canvas, x, y, rotation, paint)
            "socket_b4" -> drawSocketTriple(canvas, x, y, rotation, paint)
            "socket_k" -> drawSocketComputer(canvas, x, y, rotation, paint)
            "socket_double" -> drawSocketDouble(canvas, x, y, rotation, paint)
            "socket_380" -> drawSocket380(canvas, x, y, rotation, paint)
            "switch_o", "switch_1" -> drawSwitch(canvas, x, y, rotation, paint, 1)
            "switch_2" -> drawSwitch(canvas, x, y, rotation, paint, 2)
            "switch_3" -> drawSwitch(canvas, x, y, rotation, paint, 3)
            "switch_pass" -> drawSwitchPass(canvas, x, y, rotation, paint)
            "switch_dim" -> drawSwitchDimmer(canvas, x, y, rotation, paint)
            "switch_move" -> drawSwitchMotion(canvas, x, y, rotation, paint)
            "lamp_titan" -> drawLampFluorescent(canvas, x, y, rotation, paint, 600f)
            "lamp_flame" -> drawLampFluorescent(canvas, x, y, rotation, paint, 1200f)
            "lamp_grig" -> drawLampSpot(canvas, x, y, rotation, paint)
            "lamp_lust" -> drawLampChandelier(canvas, x, y, rotation, paint)
            "lamp_bra" -> drawLampBra(canvas, x, y, rotation, paint)
            "lamp_led" -> drawLampLedStrip(canvas, x, y, rotation, paint)
            "lamp_street" -> drawLampStreet(canvas, x, y, rotation, paint)
            "lamp_ao" -> drawLampEmergency(canvas, x, y, rotation, paint)
            "lamp_exit" -> drawLampExit(canvas, x, y, rotation, paint)
            "rj45" -> drawWeakCurrent(canvas, x, y, rotation, paint, "RJ45")
            "rj45x2" -> drawWeakCurrent(canvas, x, y, rotation, paint, "2RJ45")
            "sks_tv" -> drawWeakCurrent(canvas, x, y, rotation, paint, "ТВ")
            "sks_phone" -> drawWeakCurrent(canvas, x, y, rotation, paint, "ТЕЛ")
            "sks_intercom" -> drawWeakCurrent(canvas, x, y, rotation, paint, "ДОМ")
            "sks_cam" -> drawCamera(canvas, x, y, rotation, paint)
            "sks_smoke" -> drawSmokeDetector(canvas, x, y, rotation, paint)
            "sks_sec" -> drawSecuritySensor(canvas, x, y, rotation, paint)
            "box_rk" -> drawJunctionBox(canvas, x, y, rotation, paint)
            "panel_shr" -> drawPanel(canvas, x, y, rotation, paint, "ЩР")
            "panel_sks" -> drawPanel(canvas, x, y, rotation, paint, "СКС")
            "input_220" -> drawInput(canvas, x, y, rotation, paint)
            "ground" -> drawGround(canvas, x, y, rotation, paint)
            "cond_vk" -> drawConsumer(canvas, x, y, rotation, paint, "ВК")
            "cons_boiler" -> drawConsumer(canvas, x, y, rotation, paint, "БОЙЛ")
            "cons_stove" -> drawConsumer(canvas, x, y, rotation, paint, "ПЛИТА")
            "cons_pump" -> drawConsumer(canvas, x, y, rotation, paint, "НАСОС")
        }
    }

    private fun drawSocket(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val rect = RectF(-r, -r, r, r)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(rect, 180f, 180f, false, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        canvas.restore()
    }

    private fun drawSocketIp44(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val rect = RectF(-r, -r, r, r)
        val pFill = Paint(paint).apply { style = Paint.Style.FILL }
        canvas.drawArc(rect, 180f, 180f, true, pFill)
        val pStroke = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawLine(0f, 0f, 0f, -r - 10f, pStroke)
        canvas.restore()
    }

    private fun drawSocketDouble(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, p)
        canvas.drawArc(RectF(-r + 25f, -r, r + 25f, r), 180f, 180f, false, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        canvas.drawLine(25f, 0f, 25f, -r - 10f, p)
        canvas.restore()
    }

    private fun drawSocketIp44Double(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f
        val pFill = Paint(paint).apply { style = Paint.Style.FILL }
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, true, pFill)
        canvas.drawArc(RectF(-r + 25f, -r, r + 25f, r), 180f, 180f, true, pFill)
        val pStroke = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawLine(0f, 0f, 0f, -r - 10f, pStroke)
        canvas.drawLine(25f, 0f, 25f, -r - 10f, pStroke)
        canvas.restore()
    }

    private fun drawSocketComputer(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val rect = RectF(-r, -r, r, r)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(rect, 180f, 180f, false, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        val pText = Paint(paint).apply { textSize = 16f; textAlign = Paint.Align.CENTER }
        canvas.drawText("К", 0f, 5f, pText)
        canvas.restore()
    }

    private fun drawSocket380(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val rect = RectF(-r, -r, r, r)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(rect, 180f, 180f, false, p)
        canvas.drawLine(-10f, 0f, -10f, -r - 10f, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        canvas.drawLine(10f, 0f, 10f, -r - 10f, p)
        canvas.restore()
    }

    private fun drawSwitch(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint, keys: Int) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 16f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawCircle(0f, 0f, r, p)
        for (i in 0 until keys) {
            val offset = (i - (keys - 1) / 2f) * 8f
            canvas.drawLine(-r/2 + offset, r/2, r/2 + offset, -r/2, p)
        }
        canvas.restore()
    }

    private fun drawSwitchPass(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 16f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(-r/2, r/2, r/2, -r/2, p)
        canvas.drawLine(-r/2 + 5f, r/2, r/2 + 5f, -r/2, p)
        canvas.restore()
    }

    private fun drawSwitchDimmer(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 16f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(-r/2, r/2, r/2, -r/2, p)
        val pText = Paint(paint).apply { textSize = 14f; textAlign = Paint.Align.CENTER }
        canvas.drawText("~", 0f, 5f, pText)
        canvas.restore()
    }

    private fun drawSwitchMotion(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 16f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(-r/2, r/2, r/2, -r/2, p)
        val pArc = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawArc(RectF(-r - 10f, -r - 10f, r + 10f, r + 10f), 135f, 90f, false, pArc)
        canvas.restore()
    }

    private fun drawLampFluorescent(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint, length: Float) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val w = length / 30f; val h = 15f
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawRect(RectF(-w/2, -h/2, w/2, h/2), p)
        canvas.drawLine(-w/2, 0f, w/2, 0f, p)
        canvas.restore()
    }

    private fun drawLampSpot(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.FILL }
        canvas.drawCircle(0f, 0f, 12f, p)
        canvas.restore()
    }

    private fun drawLampChandelier(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 18f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(-r, 0f, r, 0f, p)
        canvas.drawLine(0f, -r, 0f, r, p)
        canvas.restore()
    }

    private fun drawLampBra(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 14f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, p)
        canvas.drawLine(0f, 0f, 0f, -r - 8f, p)
        canvas.restore()
    }

    private fun drawLampLedStrip(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 6f }
        canvas.drawLine(-30f, 0f, 30f, 0f, p)
        canvas.restore()
    }

    private fun drawLampStreet(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 18f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(0f, r, 0f, r + 20f, p)
        canvas.restore()
    }

    private fun drawLampEmergency(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 16f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawCircle(0f, 0f, r, p)
        val pText = Paint(paint).apply { textSize = 18f; textAlign = Paint.Align.CENTER }
        canvas.drawText("!", 0f, 6f, pText)
        canvas.restore()
    }

    private fun drawLampExit(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawRect(RectF(-20f, -10f, 20f, 10f), p)
        val pText = Paint(paint).apply { textSize = 12f; textAlign = Paint.Align.CENTER }
        canvas.drawText("ВЫХОД", 0f, 4f, pText)
        canvas.restore()
    }

    private fun drawWeakCurrent(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint, label: String) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val rect = RectF(-r, -r, r, r)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(rect, 180f, 180f, false, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        val pText = Paint(paint).apply { textSize = 11f; textAlign = Paint.Align.CENTER }
        canvas.drawText(label, 0f, 8f, pText)
        canvas.restore()
    }

    private fun drawCamera(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawRect(RectF(-15f, -10f, 15f, 10f), p)
        canvas.drawCircle(23f, 0f, 8f, p)
        canvas.restore()
    }

    private fun drawSmokeDetector(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawCircle(0f, 0f, 18f, p)
        canvas.drawCircle(0f, 0f, 9f, p)
        canvas.restore()
    }

    private fun drawSecuritySensor(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawCircle(0f, 0f, 16f, p)
        val pArc = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawArc(RectF(-24f, -24f, 24f, 24f), 120f, 120f, false, pArc)
        canvas.restore()
    }

    private fun drawJunctionBox(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.FILL }
        canvas.drawOval(RectF(-20f, -20f, 20f, 20f), p)
        val pText = Paint(paint).apply { textSize = 14f; textAlign = Paint.Align.CENTER; color = android.graphics.Color.WHITE }
        canvas.drawText("РК", 0f, 5f, pText)
        canvas.restore()
    }

    private fun drawPanel(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint, label: String) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawRect(RectF(-25f, -20f, 25f, 20f), p)
        val pText = Paint(paint).apply { textSize = 16f; textAlign = Paint.Align.CENTER }
        canvas.drawText(label, 0f, 6f, pText)
        canvas.restore()
    }

    private fun drawInput(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawRect(RectF(-25f, -20f, 25f, 20f), p)
        canvas.drawLine(-25f, 0f, 25f, 0f, p)
        val pText = Paint(paint).apply { textSize = 12f; textAlign = Paint.Align.CENTER }
        canvas.drawText("ВВОД 220В", 0f, -6f, pText)
        canvas.restore()
    }

    private fun drawGround(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawLine(0f, -20f, 0f, 0f, p)
        canvas.drawLine(-20f, 0f, 20f, 0f, p)
        canvas.drawLine(-12f, 8f, 12f, 8f, p)
        canvas.drawLine(-4f, 16f, 4f, 16f, p)
        canvas.restore()
    }

    private fun drawConsumer(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint, label: String) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawCircle(0f, 0f, 22f, p)
        val pText = Paint(paint).apply { textSize = 12f; textAlign = Paint.Align.CENTER }
        canvas.drawText(label, 0f, 4f, pText)
        canvas.restore()
    }
    private fun drawSocketProtected(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        canvas.drawLine(-r - 8f, -8f, -r, -8f, p)
        canvas.restore()
    }

        val r = 20f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(RectF(-r - 25f, -r, r - 25f, r), 180f, 180f, false, p)
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, p)
        canvas.drawArc(RectF(-r + 25f, -r, r + 25f, r), 180f, 180f, false, p)
        canvas.drawLine(-25f, 0f, -25f, -r - 10f, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        canvas.drawLine(25f, 0f, 25f, -r - 10f, p)
        canvas.restore()
    }
    private fun drawSocketTriple(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(RectF(-r - 25f, -r, r - 25f, r), 180f, 180f, false, p)
        canvas.drawArc(RectF(-r, -r, r, r), 180f, 180f, false, p)
        canvas.drawArc(RectF(-r + 25f, -r, r + 25f, r), 180f, 180f, false, p)
        canvas.drawLine(-25f, 0f, -25f, -r - 10f, p)
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)
        canvas.drawLine(25f, 0f, 25f, -r - 10f, p)
        canvas.restore()
    }
}
