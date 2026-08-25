package ru.gdesanek

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.theme.ThemeManager
import ru.gdesanek.ui.ProjectAdapter
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var repository: ProjectRepository
    private val projects = mutableListOf<ru.gdesanek.model.Project>()
    private lateinit var adapter: ProjectAdapter
    private lateinit var header: TextView
    private lateinit var hint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCrashLogIfAny()
        val theme = ThemeManager.current(this)
        try {
            val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.canvasBg) }

            header = TextView(this).apply {
                text = "Мои проекты"; textSize = 24f; setTextColor(theme.textPrimary)
                typeface = Typeface.DEFAULT_BOLD; setPadding(32, 40, 32, 20)
            }
            root.addView(header)

            hint = TextView(this).apply {
                text = "Нажмите + чтобы создать первый проект"; textSize = 16f
                setTextColor(theme.hintColor); setPadding(32, 20, 32, 20)
                visibility = android.view.View.GONE
            }
            root.addView(hint)

            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            recyclerView = RecyclerView(this).apply {
                setBackgroundColor(theme.canvasBg)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            frame.addView(recyclerView)

            fabAdd = FloatingActionButton(this).apply {
                imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(theme.accent)
                setImageResource(R.drawable.ic_add)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                    setMargins(48, 48, 48, 48)
                }
            }
            frame.addView(fabAdd)
            root.addView(frame)

            setContentView(root)

            repository = ProjectRepository(this)
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter = ProjectAdapter(projects, theme) { project ->
                val intent = Intent(this, PlanEditorActivity::class.java)
                intent.putExtra("PROJECT_ID", project.id)
                intent.putExtra("PROJECT_NAME", project.name)
                startActivity(intent)
            }
            recyclerView.adapter = adapter
            fabAdd.setOnClickListener { showAddDialog(theme) }
            loadProjects()
        } catch (e: Exception) {
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            val et = EditText(this).apply { setText(sw.toString()) }
            AlertDialog.Builder(this).setTitle("ОШИБКА — скопируй и пришли").setView(et).setPositiveButton("ОК", null).show()
        }
    }

    private fun showCrashLogIfAny() {
        val prefs = getSharedPreferences("crash", MODE_PRIVATE)
        val log = prefs.getString("log", null) ?: return
        prefs.edit().remove("log").apply()
        val et = EditText(this).apply { setText(log) }
        AlertDialog.Builder(this).setTitle("ЛОГ КРАША — скопируй и пришли").setView(et).setPositiveButton("ОК", null).show()
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) loadProjects()
    }

    private fun loadProjects() {
        projects.clear()
        projects.addAll(repository.getAll())
        adapter.notifyDataSetChanged()
        if (projects.isEmpty()) { header.text = "Нет проектов"; hint.visibility = android.view.View.VISIBLE }
        else { header.text = "Мои проекты"; hint.visibility = android.view.View.GONE }
    }

    private fun showAddDialog(theme: ru.gdesanek.theme.AppTheme) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60, 40, 60, 20); setBackgroundColor(theme.panelBg) }
        val nameInput = EditText(this).apply { hint = "Название проекта"; setTextColor(theme.textPrimary); setHintTextColor(theme.hintColor) }
        val addressInput = EditText(this).apply { hint = "Адрес (необязательно)"; setTextColor(theme.textPrimary); setHintTextColor(theme.hintColor) }
        layout.addView(nameInput); layout.addView(addressInput)
        AlertDialog.Builder(this).setTitle("Новый проект").setView(layout)
            .setPositiveButton("Создать") { _, _ ->
                val name = nameInput.text.toString().ifEmpty { "Проект" }
                repository.insert(name, addressInput.text.toString())
                loadProjects()
            }
            .setNegativeButton("Отмена", null).show()
    }
}
