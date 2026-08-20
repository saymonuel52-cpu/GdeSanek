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
        val step: Float; val cross: Boolean; val double: Boolean
        when (wall.material) {
            "kirpich" -> { step = 14f; cross = false; double = false }
            "gazobeton" -> { step = 22f; cross = false; double = false }
            "gkl" -> { step = 18f; cross = false; double = true }
            "derevo" -> { step = 16f; cross = true; double = false }
            else -> { step = 12f; cross = false; double = false }
        }
        var s = -2 * t
        while (s < len + 2 * t) {
            val bx = wall.x1 + ux * s; val by = wall.y1 + uy * s
            val ex = wall.x1 + ux * (s + 2 * t); val ey = wall.y1 + uy * (s + 2 * t)
            canvas.drawLine(bx + px * t, by + py * t, ex - px * t, ey - py * t, hatch)
            if (cross) canvas.drawLine(bx - px * t, by - py * t, ex + px * t, ey + py * t, hatch)
            if (double) canvas.drawLine(bx + px * t * 0.5f, by + py * t * 0.5f, ex - px * t * 0.5f, ey - py * t * 0.5f, hatch)
            s += step
        }
        canvas.restore()
    }
}
