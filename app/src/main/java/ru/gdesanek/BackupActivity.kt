package ru.gdesanek

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import ru.gdesanek.core.BackupManager
import ru.gdesanek.db.ProjectRepository

class BackupActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val theme = ru.gdesanek.theme.ThemeManager.current(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.canvasBg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(theme.toolbarBg); setPadding(16, 12, 16, 12) }
        top.addView(TextView(this).apply { text = "←"; textSize = 22f; setTextColor(theme.textPrimary); setPadding(8, 4, 16, 4); setOnClickListener { finish() } })
        top.addView(TextView(this).apply { text = "Резервная копия"; textSize = 18f; setTextColor(theme.textPrimary) })
        root.addView(top)
        
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        
        val info = TextView(this).apply {
            text = "Экспорт сохраняет ВСЕ проекты, стены, объекты и трассы в один файл .gsanek"
            textSize = 14f; setTextColor(theme.textSecondary); setPadding(0, 0, 0, 24)
        }
        wrap.addView(info)
        
        val exportBtn = TextView(this).apply {
            text = "📤 Экспорт всех проектов"
            textSize = 16f; setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(theme.accent); setPadding(32, 20, 32, 20)
            gravity = Gravity.CENTER
            setOnClickListener {
                val file = BackupManager.export(this@BackupActivity)
                val uri = FileProvider.getUriForFile(this@BackupActivity, "$packageName.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Отправить резервную копию"))
            }
        }
        wrap.addView(exportBtn)
        
        val importBtn = TextView(this).apply {
            text = "📥 Восстановить из файла"
            textSize = 16f; setTextColor(theme.textPrimary)
            setBackgroundColor(theme.panelBg); setPadding(32, 20, 32, 20)
            gravity = Gravity.CENTER
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(intent, 42)
            }
        }
        wrap.addView(importBtn)
        
        val count = ProjectRepository(this).getAll().size
        val stats = TextView(this).apply {
            text = "В базе: $count проектов"
            textSize = 12f; setTextColor(theme.hintColor); setPadding(0, 24, 0, 0)
        }
        wrap.addView(stats)
        
        root.addView(wrap)
        setContentView(root)
    }
    
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 42 && res == RESULT_OK && data?.data != null) {
            try {
                val uri = data.data!!
                val tempFile = java.io.File(cacheDir, "import_temp.gsanek")
                contentResolver.openInputStream(uri)?.use { inp ->
                    tempFile.outputStream().use { out -> inp.copyTo(out) }
                }
                
                android.app.AlertDialog.Builder(this)
                    .setTitle("Восстановить?")
                    .setMessage("Текущие проекты будут заменены данными из файла. Продолжить?")
                    .setPositiveButton("Восстановить") { _, _ ->
                        BackupManager.import(this, tempFile, overwrite = true)
                        Toast.makeText(this, "Восстановлено успешно", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
