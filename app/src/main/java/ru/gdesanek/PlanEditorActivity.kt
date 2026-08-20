package ru.gdesanek

import android.graphics.Color
import android.os.Bundle
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
            text = "  <-  "
            textSize = 24f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setOnClickListener { finish() }
        }

        val title = TextView(this).apply {
            text = "   $projectName"
            textSize = 14f
            setTextColor(Color.parseColor("#B0B0B0"))
        }

        topBar.addView(backBtn)
        topBar.addView(title)

        val planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)

        root.addView(topBar)
        root.addView(planView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        planView.loadWalls()
    }
}
