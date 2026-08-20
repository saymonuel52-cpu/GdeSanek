package ru.gdesanek.ui
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.DashPathEffect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import ru.gdesanek.db.WallRepository
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.model.Wall
import ru.gdesanek.model.PlanObject
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.TrackPoint
import ru.gdesanek.render.GostSymbols
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.round
import kotlin.math.atan2

class PlanView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    enum class Tool { DRAW_WALL, PAN, PLACE, DRAW_TRACK }
    var currentTool = Tool.DRAW_WALL
    var placeType: String? = null

    var projectId: Long = 0
    var repository: WallRepository? = null
    var objectRepository: ObjectRepository? = null
    var trackRepository: TrackRepository? = null

    val walls = mutableListOf<Wall>()
    val objects = mutableListOf<PlanObject>()
    val tracks = mutableListOf<CableTrack>()
    private var currentWall: Wall? = null
    private val currentTrackPoints = mutableListOf<TrackPoint>()
    private var fingerX = 0f; private var fingerY = 0f; private var fingerOn = false

    private val wallPaint = Paint().apply { color = Color.WHITE; strokeWidth = 8f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val tempWallPaint = Paint().apply { color = Color.YELLOW; strokeWidth = 8f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; alpha = 150 }
    private val gridPaint = Paint().apply { color = Color.parseColor("#222222"); strokeWidth = 2f }
    private val hintPaint = Paint().apply { color = Color.parseColor("#777777"); textSize = 40f; textAlign = Paint.Align.CENTER }
    private val trackPaint = Paint().apply { color = Color.parseColor("#4CAF50"); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val tempTrackPaint = Paint().apply { color = Color.YELLOW; strokeWidth = 5f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(20f, 15f), 0f) }

    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private var touchMode = 0
    private var lastFocusX = 0f; private var lastFocusY = 0f; private var lastSpan = 0f
    private var lastTouchX = 0f; private var lastTouchY = 0f
    private var downX = 0f; private var downY = 0f
    private val gridSize = 50f

    private fun snap(value: Float): Float = round(value / gridSize) * gridSize

    fun loadWalls() { walls.clear(); repository?.let { walls.addAll(it.getWalls(projectId)) }; invalidate() }
    fun loadObjects() { objects.clear(); objectRepository?.let { objects.addAll(it.getAll(projectId)) }; invalidate() }
    fun loadTracks() { tracks.clear(); trackRepository?.let { tracks.addAll(it.getAll(projectId)) }; invalidate() }

    fun totalTrackMeters(): Float = tracks.map { trackLength(it.points) }.sum() / 100f * 1.1f

    private fun trackLength(pts: List<TrackPoint>): Float {
        var s = 0f
        for (i in 0 until pts.size - 1) s += sqrt((pts[i+1].x - pts[i].x).pow(2) + (pts[i+1].y - pts[i].y).pow(2))
        return s
    }

    fun undo() {
        if (currentTrackPoints.isNotEmpty()) currentTrackPoints.removeAt(currentTrackPoints.size - 1)
        else if (tracks.isNotEmpty()) { val last = tracks.removeAt(tracks.size - 1); trackRepository?.delete(last.id) }
        else if (objects.isNotEmpty()) { val last = objects.removeAt(objects.size - 1); objectRepository?.delete(last.id) }
        else if (walls.isNotEmpty()) { val last = walls.removeAt(walls.size - 1); repository?.delete(last.id) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); canvas.drawColor(Color.parseColor("#121212"))
        canvas.save(); canvas.concat(matrix)
        var x = -5000f; while (x <= 5000f) { canvas.drawLine(x, -5000f, x, 5000f, gridPaint); x += gridSize }
        var y = -5000f; while (y <= 5000f) { canvas.drawLine(-5000f, y, 5000f, y, gridPaint); y += gridSize }
        for (t in tracks) for (i in 0 until t.points.size - 1) canvas.drawLine(t.points[i].x, t.points[i].y, t.points[i+1].x, t.points[i+1].y, trackPaint)
        for (wall in walls) canvas.drawLine(wall.x1, wall.y1, wall.x2, wall.y2, wallPaint)
        currentWall?.let { canvas.drawLine(it.x1, it.y1, it.x2, it.y2, tempWallPaint) }
        for (i in 0 until currentTrackPoints.size - 1) canvas.drawLine(currentTrackPoints[i].x, currentTrackPoints[i].y, currentTrackPoints[i+1].x, currentTrackPoints[i+1].y, tempTrackPaint)
        if (currentTrackPoints.isNotEmpty() && fingerOn) { val l = currentTrackPoints.last(); canvas.drawLine(l.x, l.y, fingerX, fingerY, tempTrackPaint) }
        for (obj in objects) GostSymbols.draw(canvas, obj.type, obj.x, obj.y, obj.rotation, wallPaint)
        canvas.restore()
        if (currentTool == Tool.DRAW_TRACK && currentTrackPoints.isEmpty()) canvas.drawText("Трасса: тапай точки, тап по последней — готово", width / 2f, height / 2f, hintPaint)
        else if (walls.isEmpty() && objects.isEmpty() && currentWall == null) canvas.drawText("Выбери инструмент снизу", width / 2f, height / 2f, hintPaint)
    }

    private fun screenToCanvas(x: Float, y: Float): PointF {
        matrix.invert(inverseMatrix); val pts = floatArrayOf(x, y); inverseMatrix.mapPoints(pts); return PointF(pts[0], pts[1])
    }

    private fun snapPoint(x: Float, y: Float): TrackPoint {
        var bestD = 40f; var bx = x; var by = y; var found = false
        for (o in objects) { val d = sqrt((x - o.x).pow(2) + (y - o.y).pow(2)); if (d < bestD) { bestD = d; bx = o.x; by = o.y; found = true } }
        if (found) return TrackPoint(bx, by)
        return TrackPoint(snap(x), snap(y))
    }

    private fun finishTrack() {
        if (currentTrackPoints.size >= 2) {
            val meters = trackLength(currentTrackPoints) / 100f
            val savedId = trackRepository?.insert(projectId, "power", currentTrackPoints.toList()) ?: 0L
            tracks.add(CableTrack(savedId, projectId, "power", currentTrackPoints.toList()))
            Toast.makeText(context, String.format("Трасса: %.1f м (с запасом x1.1 = %.1f м)", meters, meters * 1.1f), Toast.LENGTH_LONG).show()
        }
        currentTrackPoints.clear(); fingerOn = false
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x; lastTouchY = event.y; downX = event.x; downY = event.y
                if (event.pointerCount == 1) {
                    when (currentTool) {
                        Tool.DRAW_WALL -> {
                            val pt = screenToCanvas(event.x, event.y); val sx = snap(pt.x); val sy = snap(pt.y)
                            currentWall = Wall(projectId = projectId, x1 = sx, y1 = sy, x2 = sx, y2 = sy); invalidate()
                        }
                        Tool.DRAW_TRACK -> {
                            val pt = screenToCanvas(event.x, event.y)
                            if (currentTrackPoints.isNotEmpty()) {
                                val last = currentTrackPoints.last()
                                if (sqrt((pt.x - last.x).pow(2) + (pt.y - last.y).pow(2)) < 30f) { touchMode = 0; finishTrack(); return true }
                            }
                            currentTrackPoints.add(snapPoint(pt.x, pt.y))
                            fingerX = pt.x; fingerY = pt.y; fingerOn = true
                            invalidate()
                        }
                        else -> {}
                    }
                    touchMode = 1
                }
                lastFocusX = event.x; lastFocusY = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchMode == 1 && currentTool == Tool.DRAW_WALL) { currentWall = null; invalidate() }
                touchMode = 3; lastFocusX = (event.getX(0) + event.getX(1)) / 2; lastFocusY = (event.getY(0) + event.getY(1)) / 2
                lastSpan = sqrt((event.getX(1) - event.getX(0)).toDouble().pow(2) + (event.getY(1) - event.getY(0)).toDouble().pow(2)).toFloat()
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode == 1 && event.pointerCount == 1) {
                    when (currentTool) {
                        Tool.DRAW_WALL -> { if (currentWall != null) { val pt = screenToCanvas(event.x, event.y); currentWall = currentWall!!.copy(x2 = snap(pt.x), y2 = snap(pt.y)); invalidate() } }
                        Tool.PAN -> { matrix.postTranslate(event.x - lastTouchX, event.y - lastTouchY); lastTouchX = event.x; lastTouchY = event.y; invalidate() }
                        Tool.DRAW_TRACK -> { val pt = screenToCanvas(event.x, event.y); fingerX = pt.x; fingerY = pt.y; fingerOn = true; invalidate() }
                        else -> {}
                    }
                } else if (event.pointerCount >= 2) {
                    val focusX = (event.getX(0) + event.getX(1)) / 2; val focusY = (event.getY(0) + event.getY(1)) / 2
                    val span = sqrt((event.getX(1) - event.getX(0)).toDouble().pow(2) + (event.getY(1) - event.getY(0)).toDouble().pow(2)).toFloat()
                    if (lastSpan > 0) matrix.postScale(span / lastSpan, span / lastSpan, focusX, focusY)
                    matrix.postTranslate(focusX - lastFocusX, focusY - lastFocusY); lastFocusX = focusX; lastFocusY = focusY; lastSpan = span; invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (touchMode == 1 && currentTool == Tool.DRAW_WALL && currentWall != null) {
                    val w = currentWall!!; val dx = w.x2 - w.x1; val dy = w.y2 - w.y1
                    if (dx * dx + dy * dy > 100) { val savedId = repository?.insert(projectId, w.x1, w.y1, w.x2, w.y2) ?: 0L; walls.add(w.copy(id = savedId)) }
                    currentWall = null; invalidate()
                } else if (touchMode == 1 && currentTool == Tool.PLACE && placeType != null) {
                    val dxS = event.x - downX; val dyS = event.y - downY
                    if (dxS * dxS + dyS * dyS < 400) {
                        val pt = screenToCanvas(event.x, event.y); val s = snapPointForPlace(pt.x, pt.y)
                        val savedId = objectRepository?.insert(projectId, placeType!!, s.x, s.y, s.rot) ?: 0L
                        objects.add(PlanObject(savedId, projectId, placeType!!, s.x, s.y, s.rot)); invalidate()
                    }
                } else if (currentTool == Tool.DRAW_TRACK) { fingerOn = false; invalidate() }
                if (event.pointerCount <= 1) touchMode = 0
            }
        }
        return true
    }

    private data class PlaceSnap(val x: Float, val y: Float, val rot: Float)
    private fun snapPointForPlace(x: Float, y: Float): PlaceSnap {
        var minDist = 40f; var bestX = x; var bestY = y; var bestRot = 0f
        for (wall in walls) {
            val dx = wall.x2 - wall.x1; val dy = wall.y2 - wall.y1; val lenSq = dx * dx + dy * dy
            if (lenSq == 0f) continue
            var t = ((x - wall.x1) * dx + (y - wall.y1) * dy) / lenSq; t = t.coerceIn(0f, 1f)
            val projX = wall.x1 + t * dx; val projY = wall.y1 + t * dy
            val dist = sqrt((x - projX) * (x - projX) + (y - projY) * (y - projY))
            if (dist < minDist) { minDist = dist; bestX = projX; bestY = projY; bestRot = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() }
        }
        return PlaceSnap(bestX, bestY, bestRot)
    }
}
