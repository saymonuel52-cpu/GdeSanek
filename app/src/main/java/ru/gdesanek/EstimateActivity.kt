package ru.gdesanek

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import ru.gdesanek.core.EstimateCalculator
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.theme.ThemeManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstimateActivity : AppCompatActivity() {
    private lateinit var rows: List<ru.gdesanek.core.EstimateRow>
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var theme: ru.gdesanek.theme.AppTheme
    private lateinit var projectId: Long
    private lateinit var projectName: String
    private lateinit var totalView: TextView
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getLongExtra("PROJECT_ID", 0)
        projectName = intent.getStringExtra("PROJECT_NAME") ?: "Проект"
        prefs = getSharedPreferences("estimate", MODE_PRIVATE)
        theme = ThemeManager.current(this)

        val objects = ObjectRepository(this).getAll(projectId)
        val tracks = TrackRepository(this).getAll(projectId)
        rows = EstimateCalculator.rows(objects, tracks)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.canvasBg)
        }

        // Заголовок
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(theme.toolbarBg)
            setPadding(32, 40, 32, 24)
        }
        val backBtn = TextView(this).apply {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0)
            textSize = 24f
            setTextColor(theme.textPrimary)
            setOnClickListener { finish() }
        }
        val title = TextView(this).apply {
            text = projectName
            textSize = 22f
            setTextColor(theme.textPrimary)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(24, 0, 0, 0)
            }
        }
        header.addView(backBtn)
        header.addView(title)
        root.addView(header)

        // Заголовок таблицы
        val tableHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(theme.panelBg)
            setPadding(24, 16, 24, 16)
        }
        val hName = TextView(this).apply {
            text = "Наименование"; textSize = 14f; setTextColor(theme.textSecondary)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
        }
        val hQty = TextView(this).apply {
            text = "Кол-во"; textSize = 14f; setTextColor(theme.textSecondary)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
        }
        val hPrice = TextView(this).apply {
            text = "Цена"; textSize = 14f; setTextColor(theme.textSecondary)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
        }
        val hSum = TextView(this).apply {
            text = "Сумма"; textSize = 14f; setTextColor(theme.textSecondary)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            gravity = android.view.Gravity.END
        }
        tableHeader.addView(hName)
        tableHeader.addView(hQty)
        tableHeader.addView(hPrice)
        tableHeader.addView(hSum)
        root.addView(tableHeader)

        // Скролл
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.canvasBg)
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Итого + экспорт
        val totalBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.accent)
            setPadding(24, 24, 24, 24)
        }
        totalView = TextView(this).apply {
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        val exportBtn = TextView(this).apply {
            text = "Экспорт сметы в PDF"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 0)
            setOnClickListener { exportEstimatePdf() }
        }
        totalBox.addView(totalView)
        totalBox.addView(exportBtn)
        root.addView(totalBox)

        setContentView(root)

        // Строки таблицы
        for ((index, row) in rows.withIndex()) {
            val rowBox = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(if (index % 2 == 0) theme.canvasBg else theme.panelBg)
                setPadding(24, 16, 24, 16)
            }
            val nameView = TextView(this).apply {
                text = row.name; textSize = 15f; setTextColor(theme.textPrimary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
            }
            val qtyView = TextView(this).apply {
                text = String.format("%.1f %s", row.qty, row.unit)
                textSize = 15f; setTextColor(theme.textSecondary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
            }
            val priceInput = EditText(this).apply {
                hint = "₽"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setTextColor(theme.textPrimary); textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                val saved = prefs.getFloat(row.key, 0f)
                if (saved > 0) setText(String.format("%.0f", saved))
                setHintTextColor(theme.hintColor)
            }
            val sumView = TextView(this).apply {
                textSize = 15f; setTextColor(theme.accent)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                gravity = android.view.Gravity.END
            }

            priceInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val price = s.toString().toFloatOrNull() ?: 0f
                    prefs.edit().putFloat(row.key, price).apply()
                    sumView.text = String.format("%.0f ₽", row.qty * price)
                    recalcTotal()
                }
            })

            sumView.text = String.format("%.0f ₽", row.qty * prefs.getFloat(row.key, 0f))

            rowBox.addView(nameView)
            rowBox.addView(qtyView)
            rowBox.addView(priceInput)
            rowBox.addView(sumView)
            list.addView(rowBox)
        }

        recalcTotal()
    }

    private fun recalcTotal() {
        var total = 0f
        for (r in rows) total += r.qty * prefs.getFloat(r.key, 0f)
        totalView.text = String.format("ИТОГО: %.0f ₽", total)
    }

    private fun exportEstimatePdf() {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)
        val paint = android.graphics.Paint()
        paint.color = Color.BLACK

        // Заголовок
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("СМЕТА", 40f, 40f, paint)
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Проект: $projectName", 40f, 60f, paint)
        canvas.drawText("Дата: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())}", 40f, 75f, paint)

        // Заголовок таблицы
        var y = 110f
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Наименование", 40f, y, paint)
        canvas.drawText("Кол-во", 340f, y, paint)
        canvas.drawText("Цена", 420f, y, paint)
        canvas.drawText("Сумма", 500f, y, paint)

        y += 5f
        paint.color = Color.GRAY
        canvas.drawLine(40f, y, 555f, y, paint)
        paint.color = Color.BLACK

        // Строки
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        y = 130f
        var total = 0f
        for (row in rows) {
            val price = prefs.getFloat(row.key, 0f)
            val sum = row.qty * price
            total += sum

            canvas.drawText(row.name, 40f, y, paint)
            canvas.drawText(String.format("%.1f %s", row.qty, row.unit), 340f, y, paint)
            canvas.drawText(String.format("%.0f ₽", price), 420f, y, paint)
            canvas.drawText(String.format("%.0f ₽", sum), 500f, y, paint)

            y += 20f
            if (y > 780f) {
                doc.finishPage(page)
                val nextPage = doc.startPage(pageInfo)
                y = 40f
            }
        }

        // Итого
        y += 10f
        paint.color = Color.GRAY
        canvas.drawLine(40f, y - 10f, 555f, y - 10f, paint)
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(String.format("ИТОГО: %.0f ₽", total), 500f, y, paint)

        doc.finishPage(page)

        try {
            val file = File(cacheDir, "Estimate_$projectId.pdf")
            doc.writeTo(java.io.FileOutputStream(file))
            doc.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Отправить смету"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
