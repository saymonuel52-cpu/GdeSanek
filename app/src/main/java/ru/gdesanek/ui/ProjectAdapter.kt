package ru.gdesanek.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.gdesanek.R
import ru.gdesanek.model.Project
import ru.gdesanek.theme.AppTheme
import java.io.File

class ProjectAdapter(
    private val projects: List<Project>,
    private val theme: AppTheme,
    private val onLongClick: (Project) -> Unit,
    private val onClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.projectName)
        val address: TextView = view.findViewById(R.id.projectAddress)
        val date: TextView = view.findViewById(R.id.projectDate)
        val preview: ImageView = view.findViewById(R.id.projectPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = projects[position]
        holder.name.text = p.name
        holder.address.text = p.address.ifEmpty { "—" }
        holder.date.text = "ID: ${p.id}"
        
        // Загрузка превью
        val previewFile = File(holder.itemView.context.filesDir, "preview_${p.id}.png")
        if (previewFile.exists()) {
            val bmp = BitmapFactory.decodeFile(previewFile.absolutePath)
            holder.preview.setImageBitmap(bmp)
        } else {
            holder.preview.setImageResource(R.drawable.ic_plan)
        }

        holder.itemView.setOnClickListener { onClick(p) }
        holder.itemView.setOnLongClickListener { onLongClick(p); true }
    }

    override fun getItemCount() = projects.size
}
