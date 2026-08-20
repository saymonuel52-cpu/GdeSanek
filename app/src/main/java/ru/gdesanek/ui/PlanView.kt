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
import ru.gdesanek.db.WallRepository
import ru.gdesanek.model.Wall
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.round

class PlanView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Tool { DRAW_WALL, PAN }
    var currentTool = Tool.DRAW_WALL

    var projectId: Long = 0
    var repository: WallRepository? = null
    val walls = mutableListOf<Wall>()
    private var currentWall: Wall? = null

    private val wallPaint = Paint().apply { color = Color.WHITE; strokeWidth = 8f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val tempWallPaint = Paint().apply { color = Color.YELLOW; strokeWidth = 8f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; alpha = 150 }
    private val gridPaint = Paint().apply { color = Color.parseColor("#222222"); strokeWidth = 2f }
    private val hintPaint = Paint().apply { color = Color.parseColor("#777777"); textSize = 44f; textAlign = Paint.Align.CENTER }

    private val matrix = Matrix()
    private val inverseMatrix = Matrix()

    private var touchMode = 0 // 0=none, 1=action, 3=zoom/pan
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var lastSpan = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val gridSize = 50f // шаг сетки (условно 0.5 метра)

    private fun snap(value: Float): Float = round(value / gridSize) * gridSize

    fun loadWalls() {
        walls.clear()
        repository?.let { walls.addAll(it.getWalls(projectId)) }
        invalidate()
    }

    fun undo() {
        if (walls.isNotEmpty()) {
            val last = walls.removeAt(walls.size - 1)
            repository?.delete(last.id)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        canvas.save()
        canvas.concat(matrix)

        var x = -5000f; while (x <= 5000f) { canvas.drawLine(x, -5000f, x, 5000f, gridPaint); x += gridSize }
        var y = -5000f; while (y <= 5000f) { canvas.drawLine(-5000f, y, 5000f, y, gridPaint); y += gridSize }

        for (wall in walls) canvas.drawLine(wall.x1, wall.y1, wall.x2, wall.y2, wallPaint)
        currentWall?.let { canvas.drawLine(it.x1, it.y1, it.x2, it.y2, tempWallPaint) }
        canvas.restore()

        if (walls.isEmpty() && currentWall == null) {
            canvas.drawText("Выбери инструмент снизу и черти", width / 2f, height / 2f, hintPaint)
        }
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
                lastTouchX = event.x; lastTouchY = event.y
                if (event.pointerCount == 1) {
                    if (currentTool == Tool.DRAW_WALL) {
                        val pt = screenToCanvas(event.x, event.y)
                        val sx = snap(pt.x); val sy = snap(pt.y)
                        currentWall = Wall(projectId = projectId, x1 = sx, y1 = sy, x2 = sx, y2 = sy)
                        touchMode = 1
                        invalidate()
                    } else {
                        touchMode = 1 // PAN mode
                    }
                }
                lastFocusX = event.x; lastFocusY = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchMode == 1 && currentTool == Tool.DRAW_WALL) { currentWall = null; invalidate() }
                touchMode = 3
                lastFocusX = (event.getX(0) + event.getX(1)) / 2
                lastFocusY = (event.getY(0) + event.getY(1)) / 2
                lastSpan = sqrt((event.getX(1) - event.getX(0)).toDouble().pow(2) + (event.getY(1) - event.getY(0)).toDouble().pow(2)).toFloat()
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode == 1 && event.pointerCount == 1) {
                    if (currentTool == Tool.DRAW_WALL && currentWall != null) {
                        val pt = screenToCanvas(event.x, event.y)
                        currentWall = currentWall!!.copy(x2 = snap(pt.x), y2 = snap(pt.y))
                        invalidate()
                    } else if (currentTool == Tool.PAN) {
                        matrix.postTranslate(event.x - lastTouchX, event.y - lastTouchY)
                        lastTouchX = event.x; lastTouchY = event.y
                        invalidate()
                    }
                } else if (event.pointerCount >= 2) {
                    val focusX = (event.getX(0) + event.getX(1)) / 2
                    val focusY = (event.getY(0) + event.getY(1)) / 2
                    val span = sqrt((event.getX(1) - event.getX(0)).toDouble().pow(2) + (event.getY(1) - event.getY(0)).toDouble().pow(2)).toFloat()
                    if (lastSpan > 0) matrix.postScale(span / lastSpan, span / lastSpan, focusX, focusY)
                    matrix.postTranslate(focusX - lastFocusX, focusY - lastFocusY)
                    lastFocusX = focusX; lastFocusY = focusY; lastSpan = span
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (touchMode == 1 && currentTool == Tool.DRAW_WALL && currentWall != null) {
                    val w = currentWall!!
                    val dx = w.x2 - w.x1; val dy = w.y2 - w.y1
                    if (dx * dx + dy * dy > 100) {
                        val savedId = repository?.insert(projectId, w.x1, w.y1, w.x2, w.y2) ?: 0L
                        walls.add(w.copy(id = savedId))
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
