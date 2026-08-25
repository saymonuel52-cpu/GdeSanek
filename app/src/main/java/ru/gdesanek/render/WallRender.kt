package ru.gdesanek.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import ru.gdesanek.model.Wall
import kotlin.math.sqrt

object WallRender {
    fun draw(canvas: Canvas, wall: Wall, paint: Paint) {
        val dx = wall.x2 - wall.x1; val dy = wall.y2 - wall.y1
        val len = sqrt(dx * dx + dy * dy); if (len < 1f) return
        val ux = dx / len; val uy = dy / len
        val px = -uy; val py = ux
        val t = wall.thickness / 10f / 2f

        val outline = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        canvas.drawLine(wall.x1 + px * t, wall.y1 + py * t, wall.x2 + px * t, wall.y2 + py * t, outline)
        canvas.drawLine(wall.x1 - px * t, wall.y1 - py * t, wall.x2 - px * t, wall.y2 - py * t, outline)
        canvas.drawLine(wall.x1 + px * t, wall.y1 + py * t, wall.x1 - px * t, wall.y1 - py * t, outline)
        canvas.drawLine(wall.x2 + px * t, wall.y2 + py * t, wall.x2 - px * t, wall.y2 - py * t, outline)

        val path = Path().apply {
            moveTo(wall.x1 + px * t, wall.y1 + py * t)
            lineTo(wall.x2 + px * t, wall.y2 + py * t)
            lineTo(wall.x2 - px * t, wall.y2 - py * t)
            lineTo(wall.x1 - px * t, wall.y1 - py * t)
            close()
        }
        canvas.save()
        canvas.clipPath(path)
        val hatch = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
        val dot = Paint(paint).apply { style = Paint.Style.FILL }

        when (wall.material) {
            "kirpich" -> {
                var s = -2 * t
                while (s < len + 2 * t) {
                    val bx = wall.x1 + ux * s; val by = wall.y1 + uy * s
                    val ex = wall.x1 + ux * (s + 2 * t); val ey = wall.y1 + uy * (s + 2 * t)
                    canvas.drawLine(bx + px * t, by + py * t, ex - px * t, ey - py * t, hatch)
                    s += 14f
                }
            }
            "gazobeton" -> {
                var s = 6f
                while (s < len) {
                    val bx = wall.x1 + ux * s; val by = wall.y1 + uy * s
                    canvas.drawLine(bx + px * t * 0.8f, by + py * t * 0.8f, bx - px * t * 0.8f, by - py * t * 0.8f, hatch)
                    s += 24f
                }
            }
            "gkl" -> {
                canvas.drawLine(wall.x1 + px * t * 0.5f, wall.y1 + py * t * 0.5f, wall.x2 + px * t * 0.5f, wall.y2 + py * t * 0.5f, hatch)
                canvas.drawLine(wall.x1 - px * t * 0.5f, wall.y1 - py * t * 0.5f, wall.x2 - px * t * 0.5f, wall.y2 - py * t * 0.5f, hatch)
            }
            "derevo" -> {
                var s = -2 * t
                while (s < len + 2 * t) {
                    val bx = wall.x1 + ux * s; val by = wall.y1 + uy * s
                    val ex = wall.x1 + ux * (s + 2 * t); val ey = wall.y1 + uy * (s + 2 * t)
                    canvas.drawLine(bx + px * t, by + py * t, ex - px * t, ey - py * t, hatch)
                    canvas.drawLine(bx - px * t, by - py * t, ex + px * t, ey + py * t, hatch)
                    s += 16f
                }
            }
            else -> {
                var s = 8f; var row = 0
                while (s < len) {
                    val off = if (row % 2 == 0) t * 0.4f else -t * 0.4f
                    val bx = wall.x1 + ux * s + px * off
                    val by = wall.y1 + uy * s + py * off
                    canvas.drawCircle(bx, by, 2.5f, dot)
                    s += 30f; row++
                }
                s = -2 * t
                while (s < len + 2 * t) {
                    val bx = wall.x1 + ux * s; val by = wall.y1 + uy * s
                    val ex = wall.x1 + ux * (s + 2 * t); val ey = wall.y1 + uy * (s + 2 * t)
                    canvas.drawLine(bx + px * t, by + py * t, ex - px * t, ey - py * t, hatch)
                    s += 60f
                }
            }
        }
        canvas.restore()
    }
}
