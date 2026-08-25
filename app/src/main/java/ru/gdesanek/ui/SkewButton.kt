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
    var isActive: Boolean = false
        set(v) { field = v; invalidate() }
    var theme: AppTheme? = null
        set(v) { field = v; invalidate() }
    var iconRes: Int = 0
        set(v) { field = v; invalidate() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val path = Path()

    init {
        try { textPaint.typeface = ResourcesCompat.getFont(context, R.font.russoone) } catch (e: Exception) {}
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 150)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = theme ?: return
        strokePaint.color = t.accent
        fillPaint.color = if (isActive) t.btnActiveBg else t.btnBg
        textPaint.color = if (isActive) Color.WHITE else t.textPrimary
        val skew = height * 0.22f
        path.reset()
        path.moveTo(skew, 0f)
        path.lineTo(width.toFloat(), 0f)
        path.lineTo(width - skew, height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
        val ts = height * 0.20f
        textPaint.textSize = ts
        if (iconRes != 0) { val d = androidx.core.content.ContextCompat.getDrawable(context, iconRes); if (d != null) { d.setTint(if (isActive) Color.WHITE else t.textPrimary); val sz = (height * 0.36f).toInt(); d.setBounds((width / 2 - sz / 2).toInt(), (height * 0.12f).toInt(), (width / 2 + sz / 2).toInt(), (height * 0.12f).toInt() + sz); d.draw(canvas) } }
        canvas.drawText(text, width / 2f, height * 0.80f, textPaint)
    }
}
