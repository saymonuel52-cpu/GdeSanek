package ru.gdesanek.ui

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.gdesanek.model.Project
import ru.gdesanek.theme.AppTheme
import ru.gdesanek.theme.Design
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectAdapter(
    private val projects: List<Project>,
    private val theme: AppTheme,
    private val onClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

    class ViewHolder(view: View, val title: TextView, val address: TextView, val date: TextView, val preview: ImageView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ctx = parent.context
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(theme.panelBg)
            layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT).apply { setMargins(Design.dp(10), Design.dp(6), Design.dp(10), Design.dp(6)) }
        }
        val stripe = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(Design.dp(4), LinearLayout.LayoutParams.MATCH_PARENT); setBackgroundColor(theme.accent) }
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Design.dp(12), Design.dp(10), Design.dp(8), Design.dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val title = TextView(ctx).apply { textSize = Design.sp(Design.TS_L); setTextColor(theme.textPrimary); typeface = Typeface.DEFAULT_BOLD }
        val address = TextView(ctx).apply { textSize = Design.sp(Design.TS_S); setTextColor(theme.textSecondary); setPadding(0, Design.dp(3), 0, 0) }
        val date = TextView(ctx).apply { textSize = Design.sp(Design.TS_S); setTextColor(theme.hintColor); setPadding(0, Design.dp(3), 0, 0) }
        content.addView(title); content.addView(address); content.addView(date)
        val preview = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(Design.dp(76), Design.dp(76))
            setPadding(Design.dp(8), Design.dp(8), Design.dp(8), Design.dp(8))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        card.addView(stripe); card.addView(content); card.addView(preview)
        return ViewHolder(card, title, address, date, preview)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = projects[position]
        holder.title.text = p.name
        holder.address.text = if (p.address.isNotEmpty()) p.address else "Адрес не указан"
        holder.date.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(p.createdAt))
        val f = File(holder.itemView.context.filesDir, "preview_${p.id}.png")
        if (f.exists()) { holder.preview.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath)); holder.preview.visibility = View.VISIBLE }
        else holder.preview.visibility = View.GONE
        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = projects.size
}
