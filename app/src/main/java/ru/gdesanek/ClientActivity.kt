package ru.gdesanek

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ru.gdesanek.core.EstimateCalculator
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.db.WallRepository
import ru.gdesanek.model.PlanObject
import ru.gdesanek.theme.AppTheme
import ru.gdesanek.theme.Design
import ru.gdesanek.ui.PlanView

class ClientActivity : Activity() {
    private lateinit var planView: PlanView
    private lateinit var estimatePanel: LinearLayout
    private var projectId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val theme = ru.gdesanek.theme.ThemeManager.current(this)
        projectId = intent.getLongExtra("PROJECT_ID", 0L)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "План"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Design.BG)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(theme.toolbarBg); setPadding(8, 10, 8, 10)
        }
        val title = TextView(this).apply {
            text = projectName; textSize = 16f; setTextColor(theme.textPrimary)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = " ✕ "; textSize = 22f; setTextColor(theme.textPrimary)
            setPadding(16, 8, 16, 8); setOnClickListener { finish() }
        }
        topBar.addView(title); topBar.addView(closeBtn)

        val frame = android.widget.FrameLayout(this)
        planView = PlanView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            currentTool = PlanView.Tool.PAN
        }

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(theme.toolbarBg); setPadding(12, 10, 12, 10)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = android.view.Gravity.BOTTOM }
        }
        val estimateBtn = TextView(this).apply {
            text = "▲ Смета"; textSize = 15f; setTextColor(Color.WHITE)
            setBackgroundColor(Design.ACCENT); setPadding(24, 16, 24, 16)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener { showEstimate(theme) }
        }
        bottomBar.addView(estimateBtn)

        frame.addView(planView); frame.addView(bottomBar)

        estimatePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.panelBg)
            setPadding(16, 16, 16, 16)
            visibility = View.GONE
            layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = android.view.Gravity.BOTTOM }
        }
        frame.addView(estimatePanel)

        root.addView(topBar)
        root.addView(frame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        loadPlanData(theme)
    }

    private fun loadPlanData(theme: AppTheme) {
        val walls = WallRepository(this).getAll(projectId)
        val objects = ObjectRepository(this).getAll(projectId)
        val tracks = TrackRepository(this).getAll(projectId)
        planView.projectId = projectId; planView.repository = WallRepository(this); planView.objectRepository = ObjectRepository(this); planView.trackRepository = TrackRepository(this); planView.walls.addAll(walls); planView.objects.addAll(objects); planView.tracks.addAll(tracks); planView.invalidate()
        planView.post { planView.fit() }
    }

    private fun showEstimate(theme: AppTheme) {
        val objects = ObjectRepository(this).getAll(projectId)
        val tracks = TrackRepository(this).getAll(projectId)
        val rows = EstimateCalculator.rows(objects, tracks)
        val prefs = getSharedPreferences("estimate", MODE_PRIVATE)

        estimatePanel.removeAllViews()
        val header = TextView(this).apply {
            text = "Смета проекта"; textSize = 18f; setTextColor(theme.textPrimary)
            typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, 16)
        }
        estimatePanel.addView(header)

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var total = 0f
        for (r in rows) {
            val price = prefs.getFloat(r.key, 0f)
            val sum = r.qty * price
            total += sum
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8)
            }
            val name = TextView(this).apply {
                text = "${r.name} — ${r.qty} ${r.unit}"; textSize = 13f
                setTextColor(theme.textPrimary)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val priceView = TextView(this).apply {
                text = String.format("%.0f ₽", sum); textSize = 13f
                setTextColor(theme.textSecondary); typeface = Typeface.MONOSPACE
            }
            row.addView(name); row.addView(priceView)
            list.addView(row)
        }
        scroll.addView(list)
        estimatePanel.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val totalView = TextView(this).apply {
            text = String.format("ИТОГО: %.0f ₽", total); textSize = 18f
            setTextColor(Color.WHITE); setBackgroundColor(Design.ACCENT)
            setPadding(16, 16, 16, 16); typeface = Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        val hideBtn = TextView(this).apply {
            text = "Закрыть"; textSize = 14f; setTextColor(theme.textPrimary)
            setPadding(16, 12, 16, 12); gravity = android.view.Gravity.CENTER
            setBackgroundColor(theme.btnBg)
            setOnClickListener { estimatePanel.visibility = View.GONE }
        }
        estimatePanel.addView(totalView)
        estimatePanel.addView(hideBtn)
        estimatePanel.visibility = View.VISIBLE
    }
}
