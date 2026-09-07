package ru.gdesanek

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import ru.gdesanek.theme.Design

class OnboardingActivity : Activity() {
    private var page = 0
    private lateinit var title: TextView
    private lateinit var text: TextView
    private lateinit var illu: IllustrationView
    private lateinit var next: TextView

    private val titles = arrayOf("1. Нарисуй стены", "2. Расставь точки", "3. Отдай PDF заказчику")
    private val texts = arrayOf(
        "Тапай по плану: каждая пара тапов — стена. Оранжевый кружок — привязка к углу, длины считаются сами.",
        "Выбери символ в каталоге (розетки, выключатели, светильники) и тапай по плану. Высоты и мощности подписываются сами.",
        "Смета соберётся автоматически, а лист Э1 с рамкой и штампом уйдёт в WhatsApp или Telegram одним тапом."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Design.BG); setPadding(Design.dp(24), Design.dp(32), Design.dp(24), Design.dp(24)) }
        illu = IllustrationView(this)
        root.addView(illu, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        title = TextView(this).apply { textSize = Design.TS_XL; setTextColor(Design.TEXT1); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, Design.dp(16), 0, 0) }
        text = TextView(this).apply { textSize = Design.TS_M; setTextColor(Design.TEXT2); gravity = Gravity.CENTER; setPadding(0, Design.dp(8), 0, Design.dp(24)) }
        root.addView(title); root.addView(text)
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val skip = TextView(this).apply { text = "Пропустить"; textSize = Design.TS_M; setTextColor(Design.TEXT2); setPadding(Design.dp(16), Design.dp(12), Design.dp(16), Design.dp(12)); setOnClickListener { finishOnboard() } }
        next = TextView(this).apply { textSize = Design.TS_M; setTextColor(Color.WHITE); setBackgroundColor(Design.ACCENT); setPadding(Design.dp(24), Design.dp(12), Design.dp(24), Design.dp(12)); gravity = Gravity.CENTER; setOnClickListener { if (page < 2) { page++; refresh() } else finishOnboard() } }
        btnRow.addView(skip); btnRow.addView(next, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(btnRow)
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        title.text = titles[page]
        text.text = texts[page]
        illu.page = page
        illu.invalidate()
        next.text = if (page < 2) "Далее →" else "Готово ✓"
    }

    private fun finishOnboard() {
        getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
        finish()
    }

    inner class IllustrationView(context: Context) : View(context) {
        var page = 0
        private val acc = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = Design.ACCENT }
        private val dim = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Design.LINE }
        private val dot = Paint().apply { style = Paint.Style.FILL; color = Design.ACCENT }
        private val sheet = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Design.TEXT1 }
        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val w = width.toFloat(); val h = height.toFloat()
            val l = w * 0.2f; val t = h * 0.25f; val r = w * 0.8f; val b = h * 0.75f
            c.drawRect(l, t, r, b, if (page == 0) acc else dim)
            if (page >= 1) {
                c.drawCircle(l + (r - l) * 0.25f, t + (b - t) * 0.5f, 12f, dot)
                c.drawCircle(l + (r - l) * 0.75f, t + (b - t) * 0.5f, 12f, dot)
            }
            if (page == 2) {
                c.drawRect(w * 0.32f, h * 0.35f, w * 0.68f, h * 0.65f, sheet)
                c.drawLine(w * 0.37f, h * 0.45f, w * 0.63f, h * 0.45f, sheet)
                c.drawLine(w * 0.37f, h * 0.55f, w * 0.63f, h * 0.55f, sheet)
            }
        }
    }
}
