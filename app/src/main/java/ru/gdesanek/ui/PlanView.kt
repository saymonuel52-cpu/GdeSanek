package ru.gdesanek.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import ru.gdesanek.model.Wall
import kotlin.math.pow
import kotlin.math.sqrt

class PlanView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val walls = mutableListOf<Wall>()
    private var currentWall: Wall? = null
    
    private val wallPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    private val tempWallPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        alpha = 150
    }

    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    
    private var touchMode = 0 // 0=none, 1=draw, 3=zoom/pan
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var lastSpan = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        
        canvas.save()
        canvas.concat(matrix)
        
        for (wall in walls) {
            canvas.drawLine(wall.x1, wall.y1, wall.x2, wall.y2, wallPaint)
        }
        
        currentWall?.let {
            canvas.drawLine(it.x1, it.y1, it.x2, it.y2, tempWallPaint)
        }
        
        canvas.restore()
    }

    private fun screenToCanvas(x: Float, y: Float): PointF {
        matrix.invert(inverseMatrix)
        val pts = floatArrayOf(x, y)
        inverseMatrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    val pt = screenToCanvas(event.x, event.y)
                    currentWall = Wall(projectId = 0, x1 = pt.x, y1 = pt.y, x2 = pt.x, y2 = pt.y)
                    touchMode = 1 // DRAW
                    invalidate()
                }
                lastFocusX = event.x
                lastFocusY = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchMode == 1) {
                    currentWall = null
                    touchMode = 3 // ZOOM/PAN
                    invalidate()
                }
                lastFocusX = (event.getX(0) + event.getX(1)) / 2
                lastFocusY = (event.getY(0) + event.getY(1)) / 2
                lastSpan = sqrt((event.getX(1) - event.getX(0)).toDouble().pow(2) + (event.getY(1) - event.getY(0)).toDouble().pow(2)).toFloat()
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode == 1 && currentWall != null) {
                    val pt = screenToCanvas(event.x, event.y)
                    currentWall = currentWall!!.copy(x2 = pt.x, y2 = pt.y)
                    invalidate()
                } else if (event.pointerCount >= 2) {
                    val focusX = (event.getX(0) + event.getX(1)) / 2
                    val focusY = (event.getY(0) + event.getY(1)) / 2
                    val span = sqrt((event.getX(1) - event.getX(0)).toDouble().pow(2) + (event.getY(1) - event.getY(0)).toDouble().pow(2)).toFloat()

                    if (lastSpan > 0) {
                        val scale = span / lastSpan
                        matrix.postScale(scale, scale, focusX, focusY)
                    }
                    matrix.postTranslate(focusX - lastFocusX, focusY - lastFocusY)

                    lastFocusX = focusX
                    lastFocusY = focusY
                    lastSpan = span
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (touchMode == 1 && currentWall != null) {
                    val dx = currentWall!!.x2 - currentWall!!.x1
                    val dy = currentWall!!.y2 - currentWall!!.y1
                    if (dx * dx + dy * dy > 100) {
                        walls.add(currentWall!!)
                    }
                    currentWall = null
                    invalidate()
                }
                if (event.pointerCount <= 1) touchMode = 0
            }
        }
        return true
    }
}
