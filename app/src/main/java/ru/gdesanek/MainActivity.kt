package ru.gdesanek

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ru.gdesanek.databinding.ActivityMainBinding
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.model.Project
import ru.gdesanek.ui.ProjectAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ProjectRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ProjectRepository(this)
        setupRecyclerView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun setupRecyclerView() {
        binding.projectsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFab() {
        binding.fabAddProject.setOnClickListener {
            showAddProjectDialog()
        }
    }

    private fun loadProjects() {
        val projects = repository.getAllProjects()
        binding.projectsRecyclerView.adapter = ProjectAdapter(
            projects,
            onItemClick = { project -> openProject(project) },
            onItemLongClick = { project -> showDeleteDialog(project) }
        )
    }

    private fun showAddProjectDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val nameInput = EditText(this).apply {
            hint = "Название проекта"
        }
        val addressInput = EditText(this).apply {
            hint = "Адрес / комментарий"
        }

        layout.addView(nameInput)
        layout.addView(addressInput)

        AlertDialog.Builder(this)
            .setTitle("Новый проект")
            .setView(layout)
            .setPositiveButton("Создать") { _, _ ->
                val name = nameInput.text.toString().trim()
                val address = addressInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val project = Project(name = name, address = address)
                    repository.insertProject(project)
                    loadProjects()
                    Toast.makeText(this, "Проект создан", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteDialog(project: Project) {
        AlertDialog.Builder(this)
            .setTitle("Удалить проект?")
            .setMessage(project.name)
            .setPositiveButton("Удалить") { _, _ ->
                repository.deleteProject(project.id)
                loadProjects()
                Toast.makeText(this, "Проект удалён", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openProject(project: Project) {
        Toast.makeText(this, "Открываем: ${project.name}", Toast.LENGTH_SHORT).show()
        // TODO: Переход на экран редактора (следующий блок)
    }
}
