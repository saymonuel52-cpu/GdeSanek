package ru.gdesanek

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val theme = ru.gdesanek.theme.ThemeManager.current(this)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.canvasBg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(theme.toolbarBg); setPadding(16, 12, 16, 12) }
        top.addView(TextView(this).apply { text = "←"; textSize = 22f; setTextColor(theme.textPrimary); setPadding(8, 4, 16, 4); setOnClickListener { finish() } })
        top.addView(TextView(this).apply { text = "Настройки"; textSize = 18f; setTextColor(theme.textPrimary) })
        root.addView(top)
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 12, 20, 20) }
        fun section(t: String) { wrap.addView(TextView(this).apply { text = t; textSize = 12f; setTextColor(theme.hintColor); setPadding(0, 20, 0, 4) }) }
        fun row(label: String, value: () -> String, onClick: () -> Unit): TextView {
            val v = TextView(this).apply { textSize = 14f; setTextColor(theme.accent) }
            wrap.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 16, 0, 16)
                setOnClickListener { onClick(); v.text = value() }
                addView(TextView(this@SettingsActivity).apply { text = label; textSize = 16f; setTextColor(theme.textPrimary); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
                addView(v)
            })
            v.text = value()
            return v
        }
        section("ОФОРМЛЕНИЕ")
        row("Тема", { ru.gdesanek.theme.ThemeManager.current(this).name }) {
            val names = ru.gdesanek.theme.Themes.all.map { it.name }.toTypedArray()
            android.app.AlertDialog.Builder(this).setTitle("Тема").setItems(names) { _, i ->
                ru.gdesanek.theme.ThemeManager.set(this, ru.gdesanek.theme.Themes.all[i].id); recreate()
            }.show()
        }
        section("РИСОВАНИЕ И РАСЧЁТ")
        row("Виброотклик", { if (prefs.getBoolean("haptics", true)) "вкл" else "выкл" }) {
            prefs.edit().putBoolean("haptics", !prefs.getBoolean("haptics", true)).apply()
        }
        row("Запас кабеля", { prefs.getInt("reserve", 10).toString() + "%" }) {
            val cur = prefs.getInt("reserve", 10)
            val next = if (cur == 5) 10 else if (cur == 10) 15 else 5
            prefs.edit().putInt("reserve", next).apply()
        }
        row("Толщина стены по умолч.", { prefs.getInt("defThickness", 100).toString() }) {
            val cur = prefs.getInt("defThickness", 100)
            val next = when (cur) { 100 -> 150; 150 -> 200; 200 -> 380; else -> 100 }
            prefs.edit().putInt("defThickness", next).apply()
        }
        section("РЕКВИЗИТЫ МАСТЕРА")
        row("Имя / компания", { prefs.getString("masterName", "").orEmpty().ifEmpty { "—" } }) { textDialog("Имя / компания", "masterName", android.text.InputType.TYPE_CLASS_TEXT) }
        row("Телефон", { prefs.getString("masterPhone", "").orEmpty().ifEmpty { "—" } }) { textDialog("Телефон", "masterPhone", android.text.InputType.TYPE_CLASS_PHONE) }
        row("ИНН", { prefs.getString("masterInn", "").orEmpty().ifEmpty { "—" } }) { textDialog("ИНН", "masterInn", android.text.InputType.TYPE_CLASS_NUMBER) }
        section("ДАННЫЕ")
        row("Проектов сохранено", { getSharedPreferences("projects_meta", MODE_PRIVATE).all.size.toString() }) { }
        wrap.addView(TextView(this).apply { text = "ГдеСанёк v1.1 · офлайн, без сбора данных"; textSize = 12f; setTextColor(theme.hintColor); setPadding(0, 28, 0, 0) })
        root.addView(ScrollView(this).apply { addView(wrap) })
        setContentView(root)
    }

    private fun textDialog(title: String, key: String, inputType: Int) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val input = EditText(this).apply {
            setText(prefs.getString(key, "").orEmpty())
            this.inputType = inputType
            setPadding(60, 40, 60, 40)
        }
        android.app.AlertDialog.Builder(this).setTitle(title).setView(input)
            .setPositiveButton("Сохранить", DialogInterface.OnClickListener { _, _ ->
                prefs.edit().putString(key, input.text.toString()).apply()
                recreate()
            })
            .setNegativeButton("Отмена", null).show()
    }
}
