package ru.gdesanek

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.ui.ProjectAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var repository: ProjectRepository
    private val projects = mutableListOf<ru.gdesanek.model.Project>()
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        repository = ProjectRepository(this)
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(projects) { project ->
            val intent = android.content.Intent(this, PlanEditorActivity::class.java)
            intent.putExtra("PROJECT_ID", project.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        
        fabAdd.setOnClickListener { showAddDialog() }
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

    private fun showAddDialog() {
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20) 
        }
        val nameInput = EditText(this).apply { hint = "Название"; maxLines = 1 }
        val addressInput = EditText(this).apply { hint = "Адрес"; maxLines = 1 }
        
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
