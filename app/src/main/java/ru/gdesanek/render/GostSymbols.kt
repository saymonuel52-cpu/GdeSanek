package ru.gdesanek.render
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object GostSymbols {
    fun draw(canvas: Canvas, type: String, x: Float, y: Float, rotation: Float, paint: Paint) {
        when(type) {
            "socket_b1" -> drawSocket(canvas, x, y, rotation, paint)
            "socket_b3" -> drawSocketIp44(canvas, x, y, rotation, paint)
            "switch_o" -> drawSwitch(canvas, x, y, rotation, paint)
        }
    }
    private fun drawSocket(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val rect = RectF(-r, -r, r, r)
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawArc(rect, 180f, 180f, false, p) // Полукруг
        canvas.drawLine(0f, 0f, 0f, -r - 10f, p)   // Заземление
        canvas.restore()
    }
    private fun drawSocketIp44(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 20f; val rect = RectF(-r, -r, r, r)
        val pFill = Paint(paint).apply { style = Paint.Style.FILL }
        canvas.drawArc(rect, 180f, 180f, true, pFill) // Закрашенный полукруг
        val pStroke = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawLine(0f, 0f, 0f, -r - 10f, pStroke)
        canvas.restore()
    }
    private fun drawSwitch(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Float, paintObj: Paint) {
        canvas.save(); canvas.translate(x, y); canvas.rotate(rotation)
        val r = 16f
        val p = Paint(paintObj).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
        canvas.drawCircle(0f, 0f, r, p)
        canvas.drawLine(-r/2, r/2, r/2, -r/2, p) // Черта под 45 градусов
        canvas.restore()
    }
    // Обертка чтобы не менять сигнатуру
    private fun drawSwitch(canvas: Canvas, x: Float, y: Float, rotation: Float, paint: Paint) {
        drawSwitch(canvas, x, y, rotation, 0f, paint)
    }
}
