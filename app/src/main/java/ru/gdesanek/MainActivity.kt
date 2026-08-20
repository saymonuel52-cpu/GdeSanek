package ru.gdesanek

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.theme.ThemeManager
import ru.gdesanek.theme.Themes
import ru.gdesanek.ui.ProjectAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var repository: ProjectRepository
    private val projects = mutableListOf<ru.gdesanek.model.Project>()
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeManager.current(this)
        setContentView(R.layout.activity_main)
        window.decorView.setBackgroundColor(theme.canvasBg)

        repository = ProjectRepository(this)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setBackgroundColor(theme.canvasBg)
        fabAdd = findViewById(R.id.fabAdd)
        fabAdd.backgroundTintList = android.content.res.ColorStateList.valueOf(theme.accent)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(projects, theme) { project ->
            val intent = android.content.Intent(this, PlanEditorActivity::class.java)
            intent.putExtra("PROJECT_ID", project.id)
            intent.putExtra("PROJECT_NAME", project.name)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener { showAddDialog(theme) }
        loadProjects()
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun loadProjects() {
        projects.clear()
        projects.addAll(repository.getAll())
        adapter.notifyDataSetChanged()
    }

    private fun showAddDialog(theme: ru.gdesanek.theme.AppTheme) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
            setBackgroundColor(theme.panelBg)
        }
        val nameInput = EditText(this).apply { hint = "Название"; maxLines = 1; setTextColor(theme.textPrimary); setHintTextColor(theme.textSecondary) }
        val addressInput = EditText(this).apply { hint = "Адрес"; maxLines = 1; setTextColor(theme.textPrimary); setHintTextColor(theme.textSecondary) }

        layout.addView(nameInput)
        layout.addView(addressInput)

        AlertDialog.Builder(this)
            .setTitle("Новый проект")
            .setView(layout)
            .setPositiveButton("Создать") { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    repository.insert(name, addressInput.text.toString())
                    loadProjects()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
