package ru.gdesanek.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.gdesanek.R
import ru.gdesanek.model.Project

class ProjectAdapter(private val projects: List<Project>) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.projectName)
        val addressText: TextView = view.findViewById(R.id.projectAddress)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        holder.nameText.text = project.name
        holder.addressText.text = project.address
    }
    override fun getItemCount() = projects.size
}
