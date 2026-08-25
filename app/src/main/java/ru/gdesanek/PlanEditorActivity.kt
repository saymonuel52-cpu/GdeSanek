package ru.gdesanek
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import ru.gdesanek.R
import ru.gdesanek.db.WallRepository
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.export.PdfExporter
import ru.gdesanek.model.Catalog
import ru.gdesanek.model.WallMaterials
import ru.gdesanek.model.WiringTypes
import ru.gdesanek.theme.AppTheme
import ru.gdesanek.theme.ThemeManager
import ru.gdesanek.theme.Themes
import ru.gdesanek.ui.PlanView
import ru.gdesanek.ui.SkewButton
import java.io.File

class PlanEditorActivity : AppCompatActivity() {
    private lateinit var planView: PlanView
    private lateinit var contextPanel: LinearLayout
    private lateinit var catalogScroll: HorizontalScrollView
    private lateinit var theme: AppTheme
    private var projectId = 0L
    private var projectName = "План"
    private var currentCatalogGroup = "Розетки"
    private val catalogButtons = mutableListOf<TextView>()
    private val toolButtons = mutableListOf<SkewButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getLongExtra("PROJECT_ID", 0)
        projectName = intent.getStringExtra("PROJECT_NAME") ?: "План"
        theme = ThemeManager.current(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.canvasBg) }

        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(theme.toolbarBg); setPadding(12, 12, 12, 12) }
        val menuBtn = TextView(this).apply { setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu, 0, 0, 0); setTextColor(theme.textPrimary); setPadding(16, 4, 16, 4); setOnClickListener { showThemeDialog() }; tooltipText = "Меню и темы" }
        val backBtn = TextView(this).apply { setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0); setTextColor(theme.textPrimary); setPadding(16, 4, 16, 4); setOnClickListener { finish() }; tooltipText = "Назад" }
        val title = TextView(this).apply { text = projectName; textSize = 17f; setTextColor(theme.textPrimary); try { typeface = androidx.core.content.res.ResourcesCompat.getFont(this@PlanEditorActivity, R.font.russoone) } catch (e: Exception) {}; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val underlayBtn = TextView(this).apply {
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_underlay, 0, 0)
            setPadding(12, 8, 12, 8); setBackgroundColor(theme.btnBg)
            tooltipText = "Подложка: фото помещения"
            setOnClickListener {
                if (planView.underlay == null) pickUnderlay()
                else AlertDialog.Builder(this@PlanEditorActivity).setTitle("Подложка").setItems(arrayOf("Калибровать масштаб", "Прозрачность", "Заменить фото", "Убрать")) { _, i ->
                    when (i) { 0 -> planView.startCalibration(); 1 -> showUnderlayDialog(); 2 -> pickUnderlay(); 3 -> removeUnderlay() }
                }.show()
            }
        }
        val calibBtn = TextView(this).apply {
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_calib, 0, 0); setBackgroundColor(theme.btnBg); setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { planView.startCalibration() }
        }
        val dimBtn = TextView(this).apply {
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_dim, 0, 0); setBackgroundColor(theme.btnBg); setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { showUnderlayDialog() }
        }
        val estimateBtn = TextView(this).apply { tooltipText = "Смета";
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_estimate, 0, 0); setBackgroundColor(theme.btnBg); setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { startActivity(Intent(this@PlanEditorActivity, EstimateActivity::class.java).putExtra("PROJECT_ID", projectId)) }
        }
        val shareBtn = TextView(this).apply { tooltipText = "Отправить PDF";
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_share, 0, 0); setBackgroundColor(theme.btnBg); setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { exportPdf() }
        }
        topBar.addView(menuBtn); topBar.addView(backBtn); topBar.addView(title); topBar.addView(underlayBtn); topBar.addView(estimateBtn); topBar.addView(shareBtn)

        planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)
        planView.objectRepository = ObjectRepository(this)
        planView.trackRepository = TrackRepository(this)
        planView.applyTheme(theme)

        val toolsBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(theme.panelBg); setPadding(8, 10, 8, 4) }
        val toolParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(2, 0, 2, 0) }
        fun makeTool(text: String, icon: Int = 0): SkewButton = SkewButton(this@PlanEditorActivity).apply {
            this.text = text; this.iconRes = icon; this.theme = this@PlanEditorActivity.theme; layoutParams = toolParams
        }
        val btnWall = makeTool("СТЕНА", R.drawable.ic_wall)
        val btnPan = makeTool("РУКА", R.drawable.ic_pan)
        val btnTrack = makeTool("ТРАССА", R.drawable.ic_track)
        val btnEdit = makeTool("РЕД", R.drawable.ic_edit)
        val btnElec = makeTool("ЭЛЕКТ", R.drawable.ic_elec)
        val btnUndo = makeTool("УБРАТЬ", R.drawable.ic_undo)
        toolButtons.addAll(listOf(btnWall, btnPan, btnTrack, btnElec, btnEdit, btnUndo))

        fun highlightTool(sel: SkewButton?) { toolButtons.forEach { it.isActive = it == sel } }
        fun highlightCatalog(sel: TextView?) { catalogButtons.forEach { it.setBackgroundColor(if (it == sel) theme.btnActiveBg else theme.btnBg) } }

        btnWall.setOnClickListener { planView.currentTool = PlanView.Tool.DRAW_WALL; planView.placeType = null; highlightTool(btnWall); highlightCatalog(null); showWallContext() }
        btnPan.setOnClickListener { planView.currentTool = PlanView.Tool.PAN; planView.placeType = null; highlightTool(btnPan); highlightCatalog(null); hideContext() }
        btnTrack.setOnClickListener { planView.currentTool = PlanView.Tool.DRAW_TRACK; planView.placeType = null; highlightTool(btnTrack); highlightCatalog(null); showTrackContext() }
        btnElec.setOnClickListener { planView.currentTool = PlanView.Tool.PLACE; planView.placeType = "socket_b1"; highlightTool(btnElec); highlightCatalog(null); showCatalog() }
        btnEdit.setOnClickListener {
            planView.currentTool = PlanView.Tool.EDIT; planView.placeType = null
            planView.selectedWallId = null; planView.selectedObjectId = null; planView.selectedTrackId = null
            highlightTool(btnEdit); highlightCatalog(null); hideContext()
            planView.invalidate()
        }
        btnUndo.setOnClickListener {
            val t = when {
                planView.tracks.isNotEmpty() -> "трасса"
                planView.objects.isNotEmpty() -> "объект"
                planView.walls.isNotEmpty() -> "стена"
                else -> null
            }
            if (t == null) Toast.makeText(this, "Нечего убирать", Toast.LENGTH_SHORT).show()
            else AlertDialog.Builder(this).setTitle("Убрать: $t?").setPositiveButton("Убрать") { _, _ ->
                planView.undo()
                com.google.android.material.snackbar.Snackbar.make(root, "Убрано: $t", 5000).setAction("Вернуть") { planView.restoreLast() }.show()
            }.setNegativeButton("Отмена", null).show()
        }

        toolsBar.addView(btnWall); toolsBar.addView(btnPan); toolsBar.addView(btnTrack); toolsBar.addView(btnElec); toolsBar.addView(btnEdit); toolsBar.addView(btnUndo)

        contextPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(theme.panelBg)
            setPadding(8, 6, 8, 6)
            visibility = View.GONE
        }

        catalogScroll = HorizontalScrollView(this).apply { setBackgroundColor(theme.panelBg); setPadding(8, 4, 8, 10); visibility = View.GONE }
        val catalogRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (item in Catalog.items) {
            val b = TextView(this).apply {
                text = item.label; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundColor(theme.btnBg); setPadding(18, 12, 18, 12)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
                setOnClickListener {
                    planView.currentTool = PlanView.Tool.PLACE; planView.placeType = item.type
                    highlightCatalog(this); highlightTool(btnElec)
                    hideContext()
                    catalogScroll.visibility = View.VISIBLE
                }
            }
            catalogButtons.add(b); catalogRow.addView(b)
        }
        catalogScroll.addView(catalogRow)

        root.addView(topBar)
        root.addView(View(this).apply { setBackgroundColor(theme.accent); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4) })
        root.addView(planView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(toolsBar)
        root.addView(contextPanel)
        root.addView(catalogScroll)
        setContentView(root)

        btnWall.performClick()
        planView.loadWalls()
        planView.loadObjects()
        planView.loadTracks()
        loadUnderlay()

    }

    private fun showWallContext() {
        contextPanel.removeAllViews()
        contextPanel.visibility = View.VISIBLE
        catalogScroll.visibility = View.GONE

        val scroll = HorizontalScrollView(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((code, name) in WallMaterials.list) {
            val b = TextView(this).apply {
                text = name; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundColor(if (code == planView.currentMaterial) theme.btnActiveBg else theme.btnBg)
                setPadding(16, 10, 16, 10)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
                setOnClickListener { planView.currentMaterial = code; showWallContext() }
            }
            row.addView(b)
        }
        scroll.addView(row)
        contextPanel.addView(scroll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val thickEdit = EditText(this).apply {
            setText(planView.currentThickness.toInt().toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(theme.textPrimary)
            setPadding(16, 10, 16, 10)
            setBackgroundColor(theme.btnBg)
            layoutParams = LinearLayout.LayoutParams(150, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = 8 }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { planView.currentThickness = s.toString().toFloatOrNull() ?: 100f }
            })
        }
        contextPanel.addView(thickEdit)

        val orthoBtn = TextView(this).apply {
            text = if (planView.orthoMode) "90° ✓" else "90°"
            setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
            setBackgroundColor(if (planView.orthoMode) theme.btnActiveBg else theme.btnBg)
            setPadding(16, 10, 16, 10)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = 8 }
            setOnClickListener { planView.orthoMode = !planView.orthoMode; showWallContext() }
        }
        contextPanel.addView(orthoBtn)

        val snapBtn = TextView(this).apply {
            text = if (planView.snapEnd) "⚓ ✓" else "⚓"
            setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
            setBackgroundColor(if (planView.snapEnd) theme.btnActiveBg else theme.btnBg)
            setPadding(16, 10, 16, 10)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = 8 }
            setOnClickListener { planView.snapEnd = !planView.snapEnd; showWallContext() }
        }
        contextPanel.addView(snapBtn)
    }

    private fun showTrackContext() {
        contextPanel.removeAllViews()
        contextPanel.visibility = View.VISIBLE
        catalogScroll.visibility = View.GONE
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        val scroll = HorizontalScrollView(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((code, name) in WiringTypes.list) {
            val b = TextView(this).apply {
                text = name; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundColor(if (code == planView.currentWiring) theme.btnActiveBg else theme.btnBg)
                setPadding(16, 10, 16, 10)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
                setOnClickListener { planView.currentWiring = code; showTrackContext() }
            }
            row.addView(b)
        }
        scroll.addView(row); wrap.addView(scroll)
        val cableScroll = HorizontalScrollView(this)
        val cableRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 6, 0, 0) }
        for (cbl in listOf("3x1.5", "3x2.5", "3x4", "3x6")) {
            val b = TextView(this).apply {
                text = cbl; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundColor(if (cbl == planView.currentCable) theme.btnActiveBg else theme.btnBg)
                setPadding(18, 10, 18, 10)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
                setOnClickListener { planView.currentCable = cbl; showTrackContext() }
            }
            cableRow.addView(b)
        }
        cableScroll.addView(cableRow); wrap.addView(cableScroll)
        val colorScroll = HorizontalScrollView(this)
        val colorRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 6, 0, 0) }
        val palette = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#FF5252"), Color.parseColor("#2196F3"), Color.parseColor("#FF9800"), Color.parseColor("#FFEB3B"), Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"), Color.parseColor("#FFFFFF"))
        for (c in palette) {
            val b = TextView(this).apply {
                text = if (planView.currentTrackColor == c) "✓" else ""
                textSize = 14f; gravity = Gravity.CENTER
                setTextColor(if (c == -1) Color.BLACK else Color.WHITE)
                setBackgroundColor(c)
                layoutParams = LinearLayout.LayoutParams(70, 50).apply { marginEnd = 8 }
                setOnClickListener { planView.currentTrackColor = c; showTrackContext() }
            }
            colorRow.addView(b)
        }
        colorScroll.addView(colorRow); wrap.addView(colorScroll)
        contextPanel.addView(wrap)
    }

    private fun showCatalog() {
        contextPanel.removeAllViews()
        contextPanel.visibility = View.VISIBLE
        catalogScroll.visibility = View.GONE

        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }

        val groupScroll = HorizontalScrollView(this)
        val groupRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (g in Catalog.groups) {
            val b = TextView(this).apply {
                text = g; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundColor(if (g == currentCatalogGroup) theme.btnActiveBg else theme.btnBg)
                setPadding(16, 10, 16, 10)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
                setOnClickListener { currentCatalogGroup = g; showCatalog() }
            }
            groupRow.addView(b)
        }
        groupScroll.addView(groupRow)
        wrap.addView(groupScroll)

        val itemScroll = HorizontalScrollView(this)
        val itemRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 6, 0, 0) }
        for (item in Catalog.byGroup(currentCatalogGroup)) {
            val b = TextView(this).apply {
                text = item.label; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundColor(if (item.type == planView.placeType) theme.btnActiveBg else theme.btnBg)
                setPadding(18, 12, 18, 12)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
                setOnClickListener { planView.currentTool = PlanView.Tool.PLACE; planView.placeType = item.type; showCatalog() }
            }
            itemRow.addView(b)
        }
        itemScroll.addView(itemRow)
        wrap.addView(itemScroll)
        contextPanel.addView(wrap)
    }

    private fun hideContext() {
        contextPanel.removeAllViews()
        contextPanel.visibility = View.GONE
        catalogScroll.visibility = View.GONE
    }

    private fun showThemeDialog() {
        val names = Themes.all.map { it.name }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Выбор темы").setItems(names) { _, i ->
            ThemeManager.set(this, Themes.all[i].id); recreate()
            recreate()
        }.setNegativeButton("Отмена", null).show()
    }

    private fun pickUnderlay() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(Intent.createChooser(i, "Подложка"), 42)
    }

    private fun removeUnderlay() {
        val f = File(filesDir, "underlay_$projectId.jpg")
        if (f.exists()) f.delete()
        planView.underlay = null
        getSharedPreferences("underlay", MODE_PRIVATE).edit().remove("us_$projectId").remove("ux_$projectId").remove("uy_$projectId").remove("ua_$projectId").apply()
        Toast.makeText(this, "Подложка удалена", Toast.LENGTH_SHORT).show()
    }

    private fun showUnderlayDialog() {
        if (planView.underlay == null) { Toast.makeText(this, "Сначала загрузи подложку 🖼", Toast.LENGTH_SHORT).show(); return }
        val prefs = getSharedPreferences("underlay", MODE_PRIVATE)
        val seek = SeekBar(this).apply { max = 255; progress = planView.underlayAlpha; setPadding(60, 30, 60, 30) }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { planView.underlayAlpha = p; planView.invalidate() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) { prefs.edit().putInt("ua_$projectId", planView.underlayAlpha).apply() }
        })
        AlertDialog.Builder(this).setTitle("Прозрачность подложки").setView(seek)
            .setPositiveButton("Готово") { _, _ -> prefs.edit().putInt("ua_$projectId", planView.underlayAlpha).apply() }
            .setNegativeButton("Отмена", null).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK && data?.data != null) {
            try {
                val f = File(filesDir, "underlay_$projectId.jpg")
                contentResolver.openInputStream(data.data!!)?.use { inp -> f.outputStream().use { out -> inp.copyTo(out) } }
                loadUnderlay()

                Toast.makeText(this, "Подложка загружена", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось загрузить подложку", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUnderlay() {
        val f = File(filesDir, "underlay_$projectId.jpg")
        if (!f.exists()) return
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, opts)
        var sample = 1
        while (opts.outWidth / sample > 2000 || opts.outHeight / sample > 2000) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
        planView.underlay = bmp
        val prefs = getSharedPreferences("underlay", MODE_PRIVATE)
        planView.underlayScale = prefs.getFloat("us_$projectId", 1f)
        planView.underlayX = prefs.getFloat("ux_$projectId", 0f)
        planView.underlayY = prefs.getFloat("uy_$projectId", 0f)
        planView.underlayAlpha = prefs.getInt("ua_$projectId", 128)
        planView.onUnderlayChanged = {
            prefs.edit().putFloat("us_$projectId", planView.underlayScale).putFloat("ux_$projectId", planView.underlayX).putFloat("uy_$projectId", planView.underlayY).apply()
        }
    }

    private fun exportPdf() {
        Toast.makeText(this, "Формируем PDF...", Toast.LENGTH_SHORT).show()
        Thread {
            val walls = WallRepository(this).getAll(projectId)
            val objects = ObjectRepository(this).getAll(projectId)
            val tracks = TrackRepository(this).getAll(projectId)
            val file = PdfExporter.export(this, projectName, projectId, walls, objects, tracks)
            runOnUiThread {
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND)
                send.type = "application/pdf"
                send.putExtra(Intent.EXTRA_STREAM, uri)
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(send, "Отправить PDF"))
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        planView.commitPending()
    }
}
