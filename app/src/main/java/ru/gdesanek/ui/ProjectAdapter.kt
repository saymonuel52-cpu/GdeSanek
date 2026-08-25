package ru.gdesanek.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val card = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(theme.panelBg)
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
        }

        // Teal-ребро слева
        val stripe = View(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(12, LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(theme.accent)
        }
        card.addView(stripe)

        // Контент
        val content = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val title = TextView(parent.context).apply {
            textSize = 18f
            setTextColor(theme.textPrimary)
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(title)

        val address = TextView(parent.context).apply {
            textSize = 14f
            setTextColor(theme.textSecondary)
            setPadding(0, 4, 0, 0)
        }
        content.addView(address)

        val date = TextView(parent.context).apply {
            textSize = 12f
            setTextColor(theme.hintColor)
            setPadding(0, 4, 0, 0)
        }
        content.addView(date)

        card.addView(content)
        card.tag = mapOf("title" to title, "address" to address, "date" to date)
        return ViewHolder(card)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        val views = holder.itemView.tag as Map<String, TextView>
        
        views["title"]?.text = project.name
        views["address"]?.text = if (project.address.isNotEmpty()) project.address else "Адрес не указан"
        views["date"]?.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(project.createdAt))
        
        holder.itemView.setOnClickListener { onClick(project) }
    }

    override fun getItemCount() = projects.size
}
