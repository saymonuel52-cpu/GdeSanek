package ru.gdesanek

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ru.gdesanek.core.EstimateCalculator
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository

class EstimateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId = intent.getLongExtra("PROJECT_ID", 0)
        val prefs = getSharedPreferences("estimate", MODE_PRIVATE)

        val objects = ObjectRepository(this).getAll(projectId)
        val tracks = TrackRepository(this).getAll(projectId)
        val rows = EstimateCalculator.rows(objects, tracks)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#121212")); setPadding(24, 24, 24, 24) }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, 16) }
        val backBtn = TextView(this).apply { text = "  <-  "; textSize = 22f; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E1E1E")); setOnClickListener { finish() } }
        val title = TextView(this).apply { text = "   СМЕТА"; textSize = 20f; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        header.addView(backBtn); header.addView(title)
        root.addView(header)

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)

        val totalView = TextView(this).apply { textSize = 26f; setTextColor(Color.parseColor("#00C4DC")); setPadding(0, 24, 0, 0) }

        fun recalcTotal() {
            var total = 0f
            for (r in rows) total += r.qty * prefs.getFloat(r.key, 0f)
            totalView.text = String.format("ИТОГО: %.0f руб.", total)
        }

        for (row in rows) {
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 12) }
            val nameView = TextView(this).apply { text = row.name; textSize = 16f; setTextColor(Color.WHITE) }
            val line2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 0) }
            val qtyView = TextView(this).apply { text = String.format("%.1f %s", row.qty, row.unit); textSize = 16f; setTextColor(Color.parseColor("#B0B0B0")) }
            val sumView = TextView(this).apply { textSize = 16f; setTextColor(Color.parseColor("#4CAF50")); setPadding(24, 0, 0, 0) }
            val priceInput = EditText(this).apply {
                hint = "цена"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setTextColor(Color.WHITE)
                val saved = prefs.getFloat(row.key, 0f)
                if (saved > 0) setText(String.format("%.0f", saved))
            }

            priceInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val price = s.toString().toFloatOrNull() ?: 0f
                    prefs.edit().putFloat(row.key, price).apply()
                    sumView.text = String.format("= %.0f руб.", row.qty * price)
                    recalcTotal()
                }
            })

            sumView.text = String.format("= %.0f руб.", row.qty * prefs.getFloat(row.key, 0f))

            line2.addView(qtyView)
            line2.addView(priceInput)
            line2.addView(sumView)
            box.addView(nameView)
            box.addView(line2)
            list.addView(box)
        }

        recalcTotal()
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(totalView)
        setContentView(root)
    }
}
