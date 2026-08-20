package ru.gdesanek
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import ru.gdesanek.R
import ru.gdesanek.db.WallRepository
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.export.PdfExporter
import ru.gdesanek.model.Catalog
import ru.gdesanek.ui.PlanView
import java.io.File

class PlanEditorActivity : AppCompatActivity() {
    private lateinit var planView: PlanView
    private var projectId = 0L
    private var projectName = "План"
    private val catalogButtons = mutableListOf<TextView>()
    private val toolButtons = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getLongExtra("PROJECT_ID", 0)
        projectName = intent.getStringExtra("PROJECT_NAME") ?: "План"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#101418")) }

        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#008C9E")); setPadding(12, 12, 12, 12) }
        val backBtn = TextView(this).apply { text = "←"; textSize = 26f; setTextColor(Color.WHITE); setPadding(16, 4, 16, 4); setOnClickListener { finish() } }
        val title = TextView(this).apply { text = projectName; textSize = 17f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val underlayBtn = TextView(this).apply {
            text = " 🖼 "; textSize = 18f; setBackgroundResource(R.drawable.btn_dark); setPadding(12, 8, 12, 8)
            setOnClickListener { pickUnderlay() }
            setOnLongClickListener { removeUnderlay(); true }
        }
        val calibBtn = TextView(this).apply {
            text = " 📐 "; textSize = 18f; setBackgroundResource(R.drawable.btn_dark); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { planView.startCalibration() }
        }
        val estimateBtn = TextView(this).apply {
            text = " 📊 "; textSize = 18f; setBackgroundResource(R.drawable.btn_dark); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { startActivity(Intent(this@PlanEditorActivity, EstimateActivity::class.java).putExtra("PROJECT_ID", projectId)) }
        }
        val shareBtn = TextView(this).apply {
            text = " 📤 "; textSize = 18f; setBackgroundResource(R.drawable.btn_dark); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { exportPdf() }
        }
        topBar.addView(backBtn); topBar.addView(title); topBar.addView(underlayBtn); topBar.addView(calibBtn); topBar.addView(estimateBtn); topBar.addView(shareBtn)

        planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)
        planView.objectRepository = ObjectRepository(this)
        planView.trackRepository = TrackRepository(this)

        val toolsBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#14181B")); setPadding(8, 10, 8, 4) }
        val toolParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) }
        fun makeTool(text: String): TextView = TextView(this@PlanEditorActivity).apply {
            this.text = text; setTextColor(Color.WHITE); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.btn_gray); layoutParams = toolParams; setPadding(10, 14, 10, 14)
        }
        val btnWall = makeTool("🧱 СТЕНА")
        val btnPan = makeTool("✋ РУКА")
        val btnTrack = makeTool("📏 ТРАССА")
        val btnUndo = makeTool("🗑 УБРАТЬ")
        toolButtons.addAll(listOf(btnWall, btnPan, btnTrack, btnUndo))

        fun highlightTool(sel: TextView?) { toolButtons.forEach { it.setBackgroundResource(if (it == sel) R.drawable.btn_active else R.drawable.btn_gray) } }
        fun highlightCatalog(sel: TextView?) { catalogButtons.forEach { it.setBackgroundResource(if (it == sel) R.drawable.btn_active else R.drawable.btn_dark) } }

        btnWall.setOnClickListener { planView.currentTool = PlanView.Tool.DRAW_WALL; planView.placeType = null; highlightTool(btnWall); highlightCatalog(null) }
        btnPan.setOnClickListener { planView.currentTool = PlanView.Tool.PAN; planView.placeType = null; highlightTool(btnPan); highlightCatalog(null) }
        btnTrack.setOnClickListener { planView.currentTool = PlanView.Tool.DRAW_TRACK; planView.placeType = null; highlightTool(btnTrack); highlightCatalog(null) }
        btnUndo.setOnClickListener { planView.undo() }

        toolsBar.addView(btnWall); toolsBar.addView(btnPan); toolsBar.addView(btnTrack); toolsBar.addView(btnUndo)

        val catalogScroll = HorizontalScrollView(this).apply { setBackgroundColor(Color.parseColor("#14181B")); setPadding(8, 4, 8, 10) }
        val catalogRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (item in Catalog.items) {
            val b = TextView(this).apply {
                text = item.label; setTextColor(Color.WHITE); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.btn_dark); setPadding(18, 12, 18, 12)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
                setOnClickListener {
                    planView.currentTool = PlanView.Tool.PLACE; planView.placeType = item.type
                    highlightCatalog(this); highlightTool(null)
                }
            }
            catalogButtons.add(b); catalogRow.addView(b)
        }
        catalogScroll.addView(catalogRow)

        root.addView(topBar)
        root.addView(planView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(toolsBar)
        root.addView(catalogScroll)
        setContentView(root)

        btnWall.performClick()
        planView.loadWalls()
        planView.loadObjects()
        planView.loadTracks()
        loadUnderlay()
    }

    private fun pickUnderlay() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(Intent.createChooser(i, "Подложка"), 42)
    }

    private fun removeUnderlay() {
        val f = File(filesDir, "underlay_$projectId.jpg")
        if (f.exists()) f.delete()
        planView.underlay = null
        getSharedPreferences("underlay", MODE_PRIVATE).edit().remove("us_$projectId").remove("ux_$projectId").remove("uy_$projectId").apply()
        Toast.makeText(this, "Подложка удалена", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK && data?.data != null) {
            try {
                val f = File(filesDir, "underlay_$projectId.jpg")
                contentResolver.openInputStream(data.data!!)?.use { inp -> f.outputStream().use { out -> inp.copyTo(out) } }
                loadUnderlay()
                Toast.makeText(this, "Подложка загружена. Нажми 📐 и отметь 2 точки известного размера", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось загрузить подложку", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUnderlay() {
        val f = File(filesDir, "underlay_$projectId.jpg")
        if (!f.exists()) return
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, opts)
        var sample = 1
        while (opts.outWidth / sample > 2000 || opts.outHeight / sample > 2000) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
        planView.underlay = bmp
        val prefs = getSharedPreferences("underlay", MODE_PRIVATE)
        planView.underlayScale = prefs.getFloat("us_$projectId", 1f)
        planView.underlayX = prefs.getFloat("ux_$projectId", 0f)
        planView.underlayY = prefs.getFloat("uy_$projectId", 0f)
        planView.onUnderlayChanged = {
            prefs.edit().putFloat("us_$projectId", planView.underlayScale).putFloat("ux_$projectId", planView.underlayX).putFloat("uy_$projectId", planView.underlayY).apply()
        }
    }

    private fun exportPdf() {
        Toast.makeText(this, "Формируем PDF...", Toast.LENGTH_SHORT).show()
        Thread {
            val walls = WallRepository(this).getAll(projectId)
            val objects = ObjectRepository(this).getAll(projectId)
            val tracks = TrackRepository(this).getAll(projectId)
            val file = PdfExporter.export(this, projectName, projectId, walls, objects, tracks)
            runOnUiThread {
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND)
                send.type = "application/pdf"
                send.putExtra(Intent.EXTRA_STREAM, uri)
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(send, "Отправить PDF"))
            }
        }.start()
    }
}
