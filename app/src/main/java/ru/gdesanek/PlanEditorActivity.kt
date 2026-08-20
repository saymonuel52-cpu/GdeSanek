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
import ru.gdesanek.ui.PlanView

class PlanEditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId = intent.getLongExtra("PROJECT_ID", 0)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "План"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }
        val backBtn = TextView(this).apply {
            text = "  <-  "; textSize = 24f; setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setOnClickListener { finish() }
        }
        val title = TextView(this).apply {
            text = "   $projectName"; textSize = 14f; setTextColor(Color.parseColor("#B0B0B0"))
        }
        topBar.addView(backBtn); topBar.addView(title)

        val planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(8, 16, 8, 16)
        }

        val paddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics).toInt()
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(8, 0, 8, 0) }

        val btnWall = TextView(this).apply {
            text = "🧱 СТЕНА"; setTextColor(Color.WHITE); textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FF9800"))
            layoutParams = params; setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        val btnPan = TextView(this).apply {
            text = "✋ РУКА"; setTextColor(Color.WHITE); textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = params; setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        val btnUndo = TextView(this).apply {
            text = "🗑 УБРАТЬ"; setTextColor(Color.WHITE); textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = params; setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        }

        btnWall.setOnClickListener {
            planView.currentTool = PlanView.Tool.DRAW_WALL
            btnWall.setBackgroundColor(Color.parseColor("#FF9800"))
            btnPan.setBackgroundColor(Color.parseColor("#333333"))
        }

        btnPan.setOnClickListener {
            planView.currentTool = PlanView.Tool.PAN
            btnPan.setBackgroundColor(Color.parseColor("#FF9800"))
            btnWall.setBackgroundColor(Color.parseColor("#333333"))
        }

        btnUndo.setOnClickListener { planView.undo() }

        bottomBar.addView(btnWall)
        bottomBar.addView(btnPan)
        bottomBar.addView(btnUndo)

        root.addView(topBar)
        root.addView(planView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar)
        setContentView(root)

        planView.loadWalls()
    }
}
