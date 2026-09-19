package ru.gdesanek

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ru.gdesanek.core.Checks
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.pdf.PanelPage

class IssuesActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val projectId = intent.getLongExtra("PROJECT_ID", 0)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "Проект"
        val theme = ru.gdesanek.theme.ThemeManager.current(this)
        
        val objects = ObjectRepository(this).getAll(projectId)
        val tracks = TrackRepository(this).getAll(projectId)
        val issues = Checks.run(objects, tracks)
        
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.canvasBg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(theme.toolbarBg); setPadding(16, 12, 16, 12) }
        top.addView(TextView(this).apply { text = "←"; textSize = 22f; setTextColor(theme.textPrimary); setPadding(8, 4, 16, 4); setOnClickListener { finish() } })
        top.addView(TextView(this).apply { text = "Замечания: $projectName"; textSize = 18f; setTextColor(theme.textPrimary) })
        root.addView(top)
        
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 12, 16, 20) }
        if (issues.isEmpty()) {
            wrap.addView(TextView(this).apply { text = "✅ Замечаний нет"; textSize = 18f; setTextColor(theme.accent); setPadding(0, 40, 0, 0) })
        } else {
            val groups = PanelPage.groupByTrack(tracks, objects)
            issues.forEachIndexed { idx, issue ->
                val card = TextView(this).apply {
                    text = "⚠ $issue"
                    textSize = 15f; setTextColor(theme.textPrimary)
                    setBackgroundColor(theme.panelBg); setPadding(20, 16, 20, 16)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 }
                    setOnClickListener {
                        val coords = findCoords(issue, groups)
                        if (coords != null) {
                            val intent = Intent(this@IssuesActivity, PlanEditorActivity::class.java).apply {
                                putExtra("PROJECT_ID", projectId)
                                putExtra("PROJECT_NAME", projectName)
                                putExtra("FOCUS_X", coords.first)
                                putExtra("FOCUS_Y", coords.second)
                            }
                            startActivity(intent)
                        }
                    }
                }
                wrap.addView(card)
            }
        }
        root.addView(ScrollView(this).apply { addView(wrap) })
        setContentView(root)
    }
    
    private fun findCoords(issue: String, groups: List<PanelPage.Group>): Pair<Float, Float>? {
        val m = Regex("Гр\\.(\\d+)").find(issue)
        if (m != null) {
            val idx = m.groupValues[1].toIntOrNull()?.minus(1) ?: return null
            if (idx in groups.indices) {
                val g = groups[idx]
                val track = g.track
                if (track != null && track.points.isNotEmpty()) {
                    val p = track.points[0]
                    return Pair(p.x, p.y)
                } else if (g.objects.isNotEmpty()) {
                    val o = g.objects[0]
                    return Pair(o.x, o.y)
                }
            }
        }
        return null
    }
}
