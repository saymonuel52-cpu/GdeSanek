package ru.gdesanek.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.gdesanek.R
import ru.gdesanek.model.Project
import ru.gdesanek.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectAdapter(
    private val projects: List<Project>,
    private val theme: AppTheme,
    private val onClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.projectName)
        val addressText: TextView = view.findViewById(R.id.projectAddress)
        val dateText: TextView = view.findViewById(R.id.projectDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        view.setBackgroundColor(theme.panelBg)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        holder.nameText.text = project.name
        holder.nameText.setTextColor(theme.textPrimary)
        holder.addressText.text = project.address
        holder.addressText.setTextColor(theme.textSecondary)
        holder.dateText.text = "📅 " + SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(project.createdAt))
        holder.dateText.setTextColor(theme.textSecondary)
        holder.itemView.setOnClickListener { onClick(project) }
    }

    override fun getItemCount() = projects.size
}
