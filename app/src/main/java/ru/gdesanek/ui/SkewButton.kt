package ru.gdesanek.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import ru.gdesanek.R
import ru.gdesanek.theme.AppTheme

class SkewButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var text: String = ""
        set(v) { field = v; invalidate() }
    var selected: Boolean = false
        set(v) { field = v; invalidate() }
    var theme: AppTheme? = null
        set(v) { field = v; invalidate() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val path = Path()

    init {
        try { textPaint.typeface = ResourcesCompat.getFont(context, R.font.russoone) } catch (e: Exception) {}
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = theme ?: return
        strokePaint.color = t.accent
        fillPaint.color = if (selected) t.btnActiveBg else t.btnBg
        textPaint.color = if (selected) Color.WHITE else t.textPrimary
        val skew = height * 0.35f
        path.reset()
        path.moveTo(skew, 0f)
        path.lineTo(width.toFloat(), 0f)
        path.lineTo(width - skew, height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
        val ts = height * 0.40f
        textPaint.textSize = ts
        canvas.drawText(text, width / 2f, height / 2f + ts * 0.36f, textPaint)
    }
}
