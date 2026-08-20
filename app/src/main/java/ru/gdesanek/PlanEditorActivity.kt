package ru.gdesanek
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ru.gdesanek.db.WallRepository
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.ui.PlanView

class PlanEditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId = intent.getLongExtra("PROJECT_ID", 0)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "План"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#121212")) }
        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16, 16, 16, 16) }
        val backBtn = TextView(this).apply { text = "  <-  "; textSize = 24f; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E1E1E")); setOnClickListener { finish() } }
        val title = TextView(this).apply { text = "   $projectName"; textSize = 14f; setTextColor(Color.parseColor("#B0B0B0")) }
        topBar.addView(backBtn); topBar.addView(title)

        val planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)
        planView.objectRepository = ObjectRepository(this)
        planView.trackRepository = TrackRepository(this)

        val bottomBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#1E1E1E")); setPadding(8, 16, 8, 16) }
        val paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 0, 4, 0) }

        fun makeButton(text: String): TextView = TextView(this).apply {
            this.text = text; setTextColor(Color.WHITE); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#333333")); layoutParams = params; setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        val btnWall = makeButton("🧱 СТЕНА")
        val btnPan = makeButton("✋ РУКА")
        val btnSocket = makeButton("🔌 РОЗЕТКА")
        val btnSwitch = makeButton("💡 ВЫКЛ")
        val btnTrack = makeButton("📏 ТРАССА")
        val btnUndo = makeButton("🗑 УБРАТЬ")

        val buttons = listOf(btnWall, btnPan, btnSocket, btnSwitch, btnTrack, btnUndo)
        fun highlight(selected: TextView) { buttons.forEach { it.setBackgroundColor(if (it == selected) Color.parseColor("#FF9800") else Color.parseColor("#333333")) } }

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
}
