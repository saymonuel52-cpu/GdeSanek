package ru.gdesanek.ui
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.DashPathEffect
import android.graphics.RectF
import android.text.InputType
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import ru.gdesanek.db.WallRepository
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.model.Wall
import ru.gdesanek.model.PlanObject
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.TrackPoint
import ru.gdesanek.model.WallMaterials
import ru.gdesanek.model.WiringTypes
import ru.gdesanek.render.GostSymbols
import ru.gdesanek.render.WallRender
import ru.gdesanek.theme.AppTheme
import ru.gdesanek.theme.SymbolPalette
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.round
import kotlin.math.atan2

class PlanView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    enum class Tool { DRAW_WALL, PAN, PLACE, DRAW_TRACK, EDIT }
    var currentTool = Tool.DRAW_WALL
    var placeType: String? = null
    var projectId: Long = 0
    var repository: WallRepository? = null
    var objectRepository: ObjectRepository? = null
    var trackRepository: TrackRepository? = null

    var underlay: Bitmap? = null
    var underlayScale = 1f
    var underlayX = 0f
    var underlayY = 0f
    var underlayAlpha = 128
    var onUnderlayChanged: (() -> Unit)? = null
    private var calibrating = false
    private val calibPoints = mutableListOf<TrackPoint>()

    var selectedWallId: Long? = null
    var selectedObjectId: Long? = null
    var selectedTrackId: Long? = null
    private var dragObject: PlanObject? = null
    private var dragWall: Wall? = null
    private var isDragging = false
    var currentMaterial = "beton"
    var currentThickness = 100f
    var currentWiring = "shtroba"
    var currentTrackColor = Color.parseColor("#4CAF50")
    var currentCable = "3x2.5"
    var onObjectTap: ((PlanObject) -> Unit)? = null
    var orthoMode = true
    var snapEnd = true
    private var dragWallEnd = 0
    private val handlePaint = Paint().apply { color = Color.parseColor("#FFD700"); style = Paint.Style.FILL; alpha = 110 }

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
    private val calibPaint = Paint().apply { color = Color.parseColor("#FF5252"); strokeWidth = 6f; style = Paint.Style.STROKE }
    private val selectionPaint = Paint().apply { color = Color.parseColor("#00BFFF"); strokeWidth = 4f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f) }
    private val underlayPaint = Paint().apply { alpha = 128 }
    private val symPaint = Paint().apply { color = Color.WHITE; strokeWidth = 8f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val labelPaint = Paint().apply { color = Color.parseColor("#9E9E9E"); textSize = 20f }
    private val trackLabelPaint = Paint().apply { textSize = 20f }

    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private var touchMode = 0
    private var lastFocusX = 0f; private var lastFocusY = 0f; private var lastSpan = 0f
    private var lastTouchX = 0f; private var lastTouchY = 0f
    private var downX = 0f; private var downY = 0f
    private val gridSize = 50f

    private fun snap(value: Float): Float = round(value / gridSize) * gridSize

    fun loadWalls() { walls.clear(); repository?.let { walls.addAll(it.getAll(projectId)) }; invalidate() }
    fun loadObjects() { objects.clear(); objectRepository?.let { objects.addAll(it.getAll(projectId)) }; invalidate() }
    fun loadTracks() { tracks.clear(); trackRepository?.let { tracks.addAll(it.getAll(projectId)) }; invalidate() }

    fun applyTheme(t: AppTheme) {
        wallPaint.color = t.wallColor
        gridPaint.color = t.gridColor
        hintPaint.color = t.hintColor
        labelPaint.color = t.hintColor
        trackPaint.color = t.trackColor
        setBackgroundColor(t.canvasBg)
        invalidate()
    }

    fun startCalibration() { calibrating = true; calibPoints.clear(); invalidate() }

    fun totalTrackMeters(): Float = tracks.map { trackLength(it.points) }.sum() / 100f * 1.1f

    private fun trackLength(pts: List<TrackPoint>): Float {
        var s = 0f
        for (i in 0 until pts.size - 1) s += sqrt((pts[i+1].x - pts[i].x).pow(2) + (pts[i+1].y - pts[i].y).pow(2))
        return s
    }

    private var pendingWall: Wall? = null
    private var pendingObject: PlanObject? = null
    private var pendingTrack: CableTrack? = null

    fun commitPending() {
        pendingWall?.let { repository?.delete(it.id) }
        pendingObject?.let { objectRepository?.delete(it.id) }
        pendingTrack?.let { trackRepository?.delete(it.id) }
        pendingWall = null; pendingObject = null; pendingTrack = null
    }

    fun restoreLast() {
        pendingWall?.let { walls.add(it) }
        pendingObject?.let { objects.add(it) }
        pendingTrack?.let { tracks.add(it) }
        pendingWall = null; pendingObject = null; pendingTrack = null
        invalidate()
    }

    fun undo() {
        if (currentTrackPoints.isNotEmpty()) { currentTrackPoints.removeAt(currentTrackPoints.size - 1); invalidate(); return }
        commitPending()
        when {
            tracks.isNotEmpty() -> pendingTrack = tracks.removeAt(tracks.size - 1)
            objects.isNotEmpty() -> pendingObject = objects.removeAt(objects.size - 1)
            walls.isNotEmpty() -> pendingWall = walls.removeAt(walls.size - 1)
        }
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save(); canvas.concat(matrix)
        underlay?.let { b -> underlayPaint.alpha = underlayAlpha; canvas.drawBitmap(b, null, RectF(underlayX, underlayY, underlayX + b.width * underlayScale, underlayY + b.height * underlayScale), underlayPaint) }
        var x = -5000f; while (x <= 5000f) { canvas.drawLine(x, -5000f, x, 5000f, gridPaint); x += gridSize }
        var y = -5000f; while (y <= 5000f) { canvas.drawLine(-5000f, y, 5000f, y, gridPaint); y += gridSize }
        for (t in tracks) { trackPaint.color = t.color; trackPaint.pathEffect = when (t.wiring) { "shtroba" -> DashPathEffect(floatArrayOf(20f, 15f), 0f); "gofra" -> DashPathEffect(floatArrayOf(20f, 10f, 5f, 10f), 0f); "truba" -> DashPathEffect(floatArrayOf(5f, 10f), 0f); "lotok" -> DashPathEffect(floatArrayOf(30f, 10f), 0f); else -> null }
            for (i in 0 until t.points.size - 1) canvas.drawLine(t.points[i].x, t.points[i].y, t.points[i+1].x, t.points[i+1].y, trackPaint)
            if (t.points.isNotEmpty()) { val p0 = t.points[0]; trackLabelPaint.color = t.color; canvas.drawText("Гр." + (tracks.indexOf(t) + 1), p0.x + 20f, p0.y - 20f, trackLabelPaint); canvas.drawText("ВВГнг-LS " + t.cable, p0.x + 20f, p0.y + 30f, trackLabelPaint) }
            if (t.id == selectedTrackId) {
                for (i in 0 until t.points.size - 1) {
                    val dx = t.points[i+1].x - t.points[i].x; val dy = t.points[i+1].y - t.points[i].y
                    val len = sqrt(dx * dx + dy * dy); if (len < 1f) continue
                    val ux = dx / len; val uy = dy / len; val px = -uy; val py = ux; val pad = 15f
                    canvas.drawLine(t.points[i].x + px * pad, t.points[i].y + py * pad, t.points[i+1].x + px * pad, t.points[i+1].y + py * pad, selectionPaint)
                    canvas.drawLine(t.points[i].x - px * pad, t.points[i].y - py * pad, t.points[i+1].x - px * pad, t.points[i+1].y - py * pad, selectionPaint)
                }
            }
        }
        for (wall in walls) {
            WallRender.draw(canvas, wall, wallPaint)
            if (wall.id == selectedWallId) {
                val dx = wall.x2 - wall.x1; val dy = wall.y2 - wall.y1
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0f) {
                    val ux = dx / len; val uy = dy / len; val px = -uy; val py = ux
                    val t = wall.thickness / 10f / 2f + 10f
                    canvas.drawLine(wall.x1 + px * t, wall.y1 + py * t, wall.x2 + px * t, wall.y2 + py * t, selectionPaint)
                    canvas.drawLine(wall.x1 - px * t, wall.y1 - py * t, wall.x2 - px * t, wall.y2 - py * t, selectionPaint)
                    canvas.drawRect(wall.x1 - 12f, wall.y1 - 12f, wall.x1 + 12f, wall.y1 + 12f, handlePaint)
                    canvas.drawRect(wall.x2 - 12f, wall.y2 - 12f, wall.x2 + 12f, wall.y2 + 12f, handlePaint)
                    canvas.drawLine(wall.x1 + px * t, wall.y1 + py * t, wall.x1 - px * t, wall.y1 - py * t, selectionPaint)
                    canvas.drawLine(wall.x2 + px * t, wall.y2 + py * t, wall.x2 - px * t, wall.y2 - py * t, selectionPaint)
                }
            }
        }
        currentWall?.let { canvas.drawLine(it.x1, it.y1, it.x2, it.y2, tempWallPaint) }
        currentWall?.let { w ->
            val lenM = sqrt((w.x2 - w.x1).pow(2) + (w.y2 - w.y1).pow(2)) / 100f
            val sizePaint = Paint(hintPaint).apply { color = Color.parseColor("#FFD700"); textSize = 32f }
            canvas.drawText(String.format("%.2f м", lenM), (w.x1 + w.x2) / 2f + 20f, (w.y1 + w.y2) / 2f - 20f, sizePaint)
        }
        for (i in 0 until currentTrackPoints.size - 1) canvas.drawLine(currentTrackPoints[i].x, currentTrackPoints[i].y, currentTrackPoints[i+1].x, currentTrackPoints[i+1].y, tempTrackPaint)
        if (currentTrackPoints.isNotEmpty() && fingerOn) { val l = currentTrackPoints.last(); canvas.drawLine(l.x, l.y, fingerX, fingerY, tempTrackPaint) }
        for (obj in objects) {
            symPaint.color = SymbolPalette.color(obj.type); GostSymbols.draw(canvas, obj.type, obj.x, obj.y, obj.rotation, symPaint)
            SymbolPalette.height(obj.type)?.let { h -> canvas.drawText("H=" + h, obj.x + 28f, obj.y - 28f, labelPaint) }
            SymbolPalette.power(obj.type)?.let { w -> canvas.drawText(w.toString() + " Вт", obj.x + 28f, obj.y + 60f, labelPaint) }
            if (obj.id == selectedObjectId) canvas.drawCircle(obj.x, obj.y, 35f, selectionPaint)
        }
        for (p in calibPoints) { canvas.drawLine(p.x - 20f, p.y, p.x + 20f, p.y, calibPaint); canvas.drawLine(p.x, p.y - 20f, p.x, p.y + 20f, calibPaint) }
        canvas.restore()
        if (calibrating) canvas.drawText("Калибровка: отметь 2 точки", width / 2f, height / 2f, hintPaint)
        else if (currentTool == Tool.EDIT) canvas.drawText("РЕДАКТ: тапни или перетащи", width / 2f, height / 2f, hintPaint)
        else if (currentTool == Tool.DRAW_TRACK && currentTrackPoints.isEmpty()) canvas.drawText("Трасса: тапай точки", width / 2f, height / 2f, hintPaint)
        else if (walls.isEmpty() && objects.isEmpty() && currentWall == null && underlay == null) canvas.drawText("Выбери инструмент снизу", width / 2f, height / 2f, hintPaint)
    }

    private fun screenToCanvas(x: Float, y: Float): PointF {
        matrix.invert(inverseMatrix); val pts = floatArrayOf(x, y); inverseMatrix.mapPoints(pts); return PointF(pts[0], pts[1])
    }

    private fun applyOrtho(sx: Float, sy: Float, ex: Float, ey: Float): TrackPoint {
        if (!orthoMode) return TrackPoint(ex, ey)
        val dx = ex - sx; val dy = ey - sy
        return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) TrackPoint(ex, sy) else TrackPoint(sx, ey)
    }

    private fun snapWallPoint(x: Float, y: Float): TrackPoint {
        if (snapEnd) {
            var bestD = 35f; var bx = x; var by = y; var found = false
            for (w in walls) {
                for (p in listOf(TrackPoint(w.x1, w.y1), TrackPoint(w.x2, w.y2))) {
                    val d = sqrt((x - p.x).pow(2) + (y - p.y).pow(2))
                    if (d < bestD) { bestD = d; bx = p.x; by = p.y; found = true }
                }
            }
            if (found) return TrackPoint(bx, by)
        }
        return TrackPoint(snap(x), snap(y))
    }
    private fun snapPoint(x: Float, y: Float): TrackPoint {
        var bestD = 40f; var bx = x; var by = y; var found = false
        for (o in objects) { val d = sqrt((x - o.x).pow(2) + (y - o.y).pow(2)); if (d < bestD) { bestD = d; bx = o.x; by = o.y; found = true } }
        if (found) return TrackPoint(bx, by)
        return TrackPoint(snap(x), snap(y))
    }

    private fun showCalibDialog(p1: TrackPoint, p2: TrackPoint) {
        val dWorld = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        if (dWorld < 1f) return
        val edit = EditText(context).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; hint = "Расстояние, м" }
        AlertDialog.Builder(context).setTitle("Калибровка масштаба").setView(edit)
            .setPositiveButton("ОК") { _, _ ->
                val meters = edit.text.toString().toFloatOrNull() ?: return@setPositiveButton
                if (meters <= 0f) return@setPositiveButton
                val us = underlayScale
                val i1x = (p1.x - underlayX) / us; val i1y = (p1.y - underlayY) / us
                val i2x = (p2.x - underlayX) / us; val i2y = (p2.y - underlayY) / us
                val lPix = sqrt((i2x - i1x).pow(2) + (i2y - i1y).pow(2))
                if (lPix < 1f) return@setPositiveButton
                val newUs = meters * 100f / lPix
                underlayX = p1.x - i1x * newUs
                underlayY = p1.y - i1y * newUs
                underlayScale = newUs
                onUnderlayChanged?.invoke()
                invalidate()
            }
            .setNegativeButton("Отмена", null).show()
    }

    private fun showWallDialog(x1: Float, y1: Float, x2: Float, y2: Float) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 10) }
        val thick = EditText(context).apply { setText("100"); inputType = InputType.TYPE_CLASS_NUMBER; hint = "Толщина, мм" }
        val scroll = HorizontalScrollView(context)
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(row); box.addView(thick); box.addView(scroll)
        val dlg = AlertDialog.Builder(context).setTitle("Материал стены").setView(box).setNegativeButton("Отмена", null).create()
        for ((code, name) in WallMaterials.list) {
            val b = TextView(context).apply {
                text = name; setTextColor(Color.WHITE); textSize = 14f
                setBackgroundColor(Color.parseColor("#1E1E1E")); setPadding(20, 16, 20, 16)
                setOnClickListener {
                    val t = thick.text.toString().toFloatOrNull() ?: 100f
                    val id = repository?.insert(projectId, x1, y1, x2, y2, code, t) ?: 0L
                    walls.add(Wall(id, projectId, x1, y1, x2, y2, code, t))
                    invalidate(); dlg.dismiss()
                }
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 8 }
            row.addView(b, lp)
        }
        dlg.show()
    }

    private fun showWiringDialog(pts: List<TrackPoint>) {
        val names = WiringTypes.list.map { it.second }.toTypedArray()
        AlertDialog.Builder(context).setTitle("Способ прокладки").setItems(names) { _, i ->
            val code = WiringTypes.list[i].first
            val id = trackRepository?.insert(projectId, "power", pts, code) ?: 0L
            tracks.add(CableTrack(id, projectId, "power", pts, code))
            invalidate()
        }.setNegativeButton("Отмена", null).show()
    }

    private fun showObjectDialog(obj: PlanObject) {
        val curH = if (obj.height >= 0) obj.height else (ru.gdesanek.theme.SymbolPalette.height(obj.type) ?: 0)
        AlertDialog.Builder(context).setTitle("Объект: ${obj.type}").setItems(arrayOf(
            "Высота (сейчас H=$curH см)",
            "Повернуть на 45°",
            "Дублировать",
            "Удалить"
        )) { _, i ->
            when (i) {
                0 -> {
                    val et = EditText(context).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(curH.toString()) }
                    AlertDialog.Builder(context).setTitle("Высота установки, см").setView(et).setPositiveButton("ОК") { _, _ ->
                        val h = et.text.toString().toIntOrNull() ?: -1
                        val upd = obj.copy(height = h)
                        objectRepository?.update(upd)
                        val idx = objects.indexOfFirst { it.id == obj.id }; if (idx >= 0) objects[idx] = upd
                        invalidate()
                    }.setNegativeButton("Отмена", null).show()
                }
                1 -> {
                    val upd = obj.copy(rotation = obj.rotation + 45f); objectRepository?.update(upd)
                    val idx = objects.indexOfFirst { it.id == obj.id }; if (idx >= 0) objects[idx] = upd
                }
                2 -> {
                    val newId = objectRepository?.insert(obj.projectId, obj.type, obj.x + 60f, obj.y + 60f, obj.rotation, obj.name, obj.area) ?: 0L
                    objects.add(obj.copy(id = newId, x = obj.x + 60f, y = obj.y + 60f))
                }
                3 -> { objectRepository?.delete(obj.id); objects.removeAll { it.id == obj.id }; selectedObjectId = null }
            }
            invalidate()
        }.show()
    }

    private fun showWallEditDialog(wall: Wall) {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 10) }
        val thick = EditText(context).apply { setText(wall.thickness.toString()); inputType = InputType.TYPE_CLASS_NUMBER; hint = "Толщина, мм" }
        val scroll = HorizontalScrollView(context)
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(row); box.addView(thick); box.addView(scroll)
        val dlg = AlertDialog.Builder(context).setTitle("Редактировать стену").setView(box)
            .setPositiveButton("Сохранить") { _, _ ->
                val t = thick.text.toString().toFloatOrNull() ?: wall.thickness
                val upd = wall.copy(thickness = t)
                repository?.update(upd)
                val idx = walls.indexOfFirst { it.id == wall.id }
                if (idx >= 0) walls[idx] = upd
                selectedWallId = null
                invalidate()
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Удалить") { _, _ ->
                repository?.delete(wall.id)
                walls.removeAll { it.id == wall.id }
                selectedWallId = null
                invalidate()
            }
            .create()
        for ((code, name) in WallMaterials.list) {
            val b = TextView(context).apply {
                text = name; setTextColor(if (code == wall.material) Color.parseColor("#00BFFF") else Color.WHITE); textSize = 14f
                setBackgroundColor(Color.parseColor("#1E1E1E")); setPadding(20, 16, 20, 16)
                setOnClickListener {
                    val t = thick.text.toString().toFloatOrNull() ?: wall.thickness
                    val upd = wall.copy(material = code, thickness = t)
                    repository?.update(upd)
                    val idx = walls.indexOfFirst { it.id == wall.id }
                    if (idx >= 0) walls[idx] = upd
                    invalidate(); dlg.dismiss()
                }
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 8 }
            row.addView(b, lp)
        }
        dlg.show()
    }

    private fun hitObject(wx: Float, wy: Float): PlanObject? =
        objects.lastOrNull { (it.x - wx) * (it.x - wx) + (it.y - wy) * (it.y - wy) < 45f * 45f }

    private fun hitWall(wx: Float, wy: Float): Wall? {
        for (wall in walls.reversed()) {
            val dx = wall.x2 - wall.x1; val dy = wall.y2 - wall.y1; val lenSq = dx * dx + dy * dy
            if (lenSq == 0f) continue
            var t = ((wx - wall.x1) * dx + (wy - wall.y1) * dy) / lenSq; t = t.coerceIn(0f, 1f)
            val projX = wall.x1 + t * dx; val projY = wall.y1 + t * dy
            val dist = sqrt((wx - projX) * (wx - projX) + (wy - projY) * (wy - projY))
            val threshold = wall.thickness / 10f / 2f + 20f
            if (dist < threshold) return wall
        }
        return null
    }

    private fun hitTrack(wx: Float, wy: Float): CableTrack? {
        for (track in tracks.reversed()) {
            for (i in 0 until track.points.size - 1) {
                val p1 = track.points[i]; val p2 = track.points[i+1]
                val dx = p2.x - p1.x; val dy = p2.y - p1.y; val lenSq = dx * dx + dy * dy
                if (lenSq == 0f) continue
                var t = ((wx - p1.x) * dx + (wy - p1.y) * dy) / lenSq; t = t.coerceIn(0f, 1f)
                val projX = p1.x + t * dx; val projY = p1.y + t * dy
                val dist = sqrt((wx - projX) * (wx - projX) + (wy - projY) * (wy - projY))
                if (dist < 25f) return track
            }
        }
        return null
    }

    private fun finishTrack() {
        if (currentTrackPoints.size >= 2) {
            val meters = trackLength(currentTrackPoints) / 100f
            val pts = currentTrackPoints.toList()
            Toast.makeText(context, String.format("Трасса: %.1f м (запас x1.1 = %.1f м)", meters, meters * 1.1f), Toast.LENGTH_LONG).show()
            val id = trackRepository?.insert(projectId, "power", pts, currentWiring) ?: 0L; tracks.add(CableTrack(id, projectId, "power", pts, currentWiring, currentTrackColor, currentCable))
        }
        currentTrackPoints.clear(); fingerOn = false
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (calibrating && event.actionMasked == MotionEvent.ACTION_DOWN && event.pointerCount == 1) {
            val pt = screenToCanvas(event.x, event.y)
            calibPoints.add(TrackPoint(pt.x, pt.y))
            if (calibPoints.size >= 2) {
                calibrating = false
                showCalibDialog(calibPoints[0], calibPoints[1])
                calibPoints.clear()
            }
            invalidate()
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x; lastTouchY = event.y; downX = event.x; downY = event.y
                if (event.pointerCount == 1) {
                    when (currentTool) {
                        Tool.EDIT -> {
                            val pt = screenToCanvas(event.x, event.y)
                            val hitObj = hitObject(pt.x, pt.y)
                            val hitW = hitWall(pt.x, pt.y)
                            val hitT = hitTrack(pt.x, pt.y)
                            selectedObjectId = hitObj?.id
                            selectedWallId = hitW?.id
                            selectedTrackId = hitT?.id
                            dragObject = hitObj
                            dragWall = hitW
                            val selW = walls.firstOrNull { it.id == selectedWallId }
                            dragWallEnd = 0
                            if (selW != null) {
                                if (sqrt((pt.x - selW.x1).pow(2) + (pt.y - selW.y1).pow(2)) < 40f) dragWallEnd = 1
                                else if (sqrt((pt.x - selW.x2).pow(2) + (pt.y - selW.y2).pow(2)) < 40f) dragWallEnd = 2
                            }
                            isDragging = false
                            invalidate()
                        }
                        Tool.DRAW_WALL -> {
                            val pt = screenToCanvas(event.x, event.y); val sp = snapWallPoint(pt.x, pt.y); val sx = sp.x; val sy = sp.y
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
                        Tool.EDIT -> {
                            if (dragWallEnd != 0) {
                                val pt = screenToCanvas(event.x, event.y)
                                val idx = walls.indexOfFirst { it.id == selectedWallId }
                                if (idx >= 0) {
                                    val w = walls[idx]
                                    walls[idx] = if (dragWallEnd == 1) w.copy(x1 = pt.x, y1 = pt.y) else w.copy(x2 = pt.x, y2 = pt.y)
                                    isDragging = true
                                    invalidate()
                                }
                            } else
                            if (dragWallEnd != 0) {
                                val pt = screenToCanvas(event.x, event.y)
                                val idx = walls.indexOfFirst { it.id == selectedWallId }
                                if (idx >= 0) {
                                    val w = walls[idx]
                                    walls[idx] = if (dragWallEnd == 1) w.copy(x1 = pt.x, y1 = pt.y) else w.copy(x2 = pt.x, y2 = pt.y)
                                    isDragging = true
                                    invalidate()
                                }
                            } else
                            if (dragObject != null) {
                                val pt = screenToCanvas(event.x, event.y)
                                val idx = objects.indexOfFirst { it.id == dragObject!!.id }
                                if (idx >= 0) {
                                    objects[idx] = dragObject!!.copy(x = pt.x, y = pt.y)
                                    isDragging = true
                                    invalidate()
                                }
                            } else if (dragWall != null) {
                                val pt = screenToCanvas(event.x, event.y)
                                val idx = walls.indexOfFirst { it.id == dragWall!!.id }
                                if (idx >= 0) {
                                    val dx = dragWall!!.x2 - dragWall!!.x1
                                    val dy = dragWall!!.y2 - dragWall!!.y1
                                    walls[idx] = dragWall!!.copy(x1 = pt.x - dx/2, y1 = pt.y - dy/2, x2 = pt.x + dx/2, y2 = pt.y + dy/2)
                                    isDragging = true
                                    invalidate()
                                }
                            }
                        }
                        Tool.DRAW_WALL -> { if (currentWall != null) { val pt = screenToCanvas(event.x, event.y); val sp = snapWallPoint(pt.x, pt.y); val op = applyOrtho(currentWall!!.x1, currentWall!!.y1, sp.x, sp.y); currentWall = currentWall!!.copy(x2 = op.x, y2 = op.y); invalidate() } }
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
                if (currentTool == Tool.EDIT) {
                    if (dragWallEnd != 0 && isDragging) {
                        val idx = walls.indexOfFirst { it.id == selectedWallId }
                        if (idx >= 0) repository?.update(walls[idx])
                        dragWallEnd = 0
                        isDragging = false
                        invalidate()
                    } else
                    if (isDragging && dragObject != null) {
                        objectRepository?.update(dragObject!!)
                    } else if (isDragging && dragWall != null) {
                        repository?.update(dragWall!!)
                    } else if (!isDragging) {
                        val pt = screenToCanvas(event.x, event.y)
                        val hitObj = hitObject(pt.x, pt.y)
                        val hitW = hitWall(pt.x, pt.y)
                        if (hitObj != null) showObjectDialog(hitObj)
                        else if (hitW != null) showWallEditDialog(hitW)
                    }
                    dragObject = null; dragWall = null; isDragging = false; dragWallEnd = 0
                    invalidate()
                } else if (touchMode == 1 && currentTool == Tool.DRAW_WALL && currentWall != null) {
                    val w = currentWall!!; val dx = w.x2 - w.x1; val dy = w.y2 - w.y1
                    currentWall = null
                    if (dx * dx + dy * dy > 100) { val id = repository?.insert(projectId, w.x1, w.y1, w.x2, w.y2, currentMaterial, currentThickness) ?: 0L; walls.add(Wall(id, projectId, w.x1, w.y1, w.x2, w.y2, currentMaterial, currentThickness)) }
                    invalidate()
                } else if (touchMode == 1 && currentTool == Tool.PLACE && placeType != null) {
                    val dxS = event.x - downX; val dyS = event.y - downY
                    if (dxS * dxS + dyS * dyS < 400) {
                        val pt = screenToCanvas(event.x, event.y)
                        val hit = hitObject(pt.x, pt.y)
                        if (hit != null) showObjectDialog(hit)
                        else {
                            val s = snapPointForPlace(pt.x, pt.y)
                            val savedId = objectRepository?.insert(projectId, placeType!!, s.x, s.y, s.rot) ?: 0L
                            objects.add(PlanObject(savedId, projectId, placeType!!, s.x, s.y, s.rot)); invalidate()
                        }
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
