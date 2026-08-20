package ru.gdesanek
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
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
import ru.gdesanek.ui.PlanView

class PlanEditorActivity : AppCompatActivity() {
    private lateinit var planView: PlanView
    private var projectId = 0L
    private var projectName = "План"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getLongExtra("PROJECT_ID", 0)
        projectName = intent.getStringExtra("PROJECT_NAME") ?: "План"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#101418")) }

        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#008C9E")); setPadding(12, 12, 12, 12) }
        val backBtn = TextView(this).apply { text = "←"; textSize = 26f; setTextColor(Color.WHITE); setPadding(16, 4, 16, 4); setOnClickListener { finish() } }
        val title = TextView(this).apply { text = projectName; textSize = 17f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val estimateBtn = TextView(this).apply { text = " 📊 "; textSize = 18f; setBackgroundResource(R.drawable.btn_dark); setPadding(12, 8, 12, 8); setOnClickListener { startActivity(Intent(this@PlanEditorActivity, EstimateActivity::class.java).putExtra("PROJECT_ID", projectId)) } }
        val shareBtn = TextView(this).apply { text = " 📤 "; textSize = 18f; setBackgroundResource(R.drawable.btn_dark); setPadding(12, 8, 12, 8); val p = layoutParams as LinearLayout.LayoutParams; p.marginStart = 8; layoutParams = p; setOnClickListener { exportPdf() } }
        topBar.addView(backBtn); topBar.addView(title); topBar.addView(estimateBtn); topBar.addView(shareBtn)

        planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)
        planView.objectRepository = ObjectRepository(this)
        planView.trackRepository = TrackRepository(this)

        val bottomBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#14181B")); setPadding(8, 14, 8, 14) }
        val paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) }

        fun makeButton(text: String): TextView = TextView(this).apply {
            this.text = text; setTextColor(Color.WHITE); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.btn_gray); layoutParams = params; setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        val btnWall = makeButton("🧱 СТЕНА")
        val btnPan = makeButton("✋ РУКА")
        val btnSocket = makeButton("🔌 РОЗЕТКА")
        val btnSwitch = makeButton("💡 ВЫКЛ")
        val btnTrack = makeButton("📏 ТРАССА")
        val btnUndo = makeButton("🗑 УБРАТЬ")

        val buttons = listOf(btnWall, btnPan, btnSocket, btnSwitch, btnTrack, btnUndo)
        fun highlight(selected: TextView) { buttons.forEach { it.setBackgroundResource(if (it == selected) R.drawable.btn_active else R.drawable.btn_gray) } }

        btnWall.setOnClickListener { planView.currentTool = PlanView.Tool.DRAW_WALL; planView.placeType = null; highlight(btnWall) }
        btnPan.setOnClickListener { planView.currentTool = PlanView.Tool.PAN; planView.placeType = null; highlight(btnPan) }
        btnSocket.setOnClickListener { planView.currentTool = PlanView.Tool.PLACE; planView.placeType = "socket_b1"; highlight(btnSocket) }
        btnSwitch.setOnClickListener { planView.currentTool = PlanView.Tool.PLACE; planView.placeType = "switch_o"; highlight(btnSwitch) }
        btnTrack.setOnClickListener { planView.currentTool = PlanView.Tool.DRAW_TRACK; planView.placeType = null; highlight(btnTrack) }
        btnUndo.setOnClickListener { planView.undo() }

        buttons.forEach { bottomBar.addView(it) }
        root.addView(topBar)
        root.addView(planView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar)
        setContentView(root)

        btnWall.performClick()
        planView.loadWalls()
        planView.loadObjects()
        planView.loadTracks()
    }

    private fun exportPdf() {
        Toast.makeText(this, "Формируем PDF...", Toast.LENGTH_SHORT).show()
        Thread {
            val walls = WallRepository(this).getWalls(projectId)
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
