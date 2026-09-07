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
    private val undoManager = ru.gdesanek.core.UndoManager(50)
    private lateinit var contextPanel: LinearLayout
    private lateinit var catalogScroll: View
    private lateinit var btnWall: ru.gdesanek.ui.SkewButton
    private lateinit var btnElec: ru.gdesanek.ui.SkewButton
    private lateinit var btnTrack: ru.gdesanek.ui.SkewButton
    private lateinit var shareBtn: TextView
    private val stepViews = mutableListOf<TextView>()
    private lateinit var statusLine: TextView
    private var currentStep = 0
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
        shareBtn = TextView(this).apply { tooltipText = "Отправить PDF";
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_share, 0, 0); setBackgroundColor(theme.btnBg); setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8)
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); p.marginStart = 8; layoutParams = p
            setOnClickListener { exportPdf() }
        }
        val undoBtn = TextView(this).apply { text = "↶"; textSize = 20f; setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8); setOnClickListener { if (undoManager.undo()) planView.reloadAll() } }
        val redoBtn = TextView(this).apply { text = "↷"; textSize = 20f; setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8); setOnClickListener { if (undoManager.redo()) planView.reloadAll() } }
        val clientBtn = TextView(this).apply { text = "👁"; textSize = 18f; setTextColor(theme.textPrimary); setPadding(12, 8, 12, 8); setOnClickListener { startActivity(android.content.Intent(this@PlanEditorActivity, ClientActivity::class.java).putExtra("PROJECT_ID", projectId).putExtra("PROJECT_NAME", intent.getStringExtra("PROJECT_NAME") ?: "План")) } }
        topBar.addView(menuBtn); topBar.addView(backBtn); topBar.addView(undoBtn); topBar.addView(redoBtn); topBar.addView(title); topBar.addView(underlayBtn); topBar.addView(estimateBtn); topBar.addView(clientBtn); topBar.addView(shareBtn)

        planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)
        planView.undoManager = undoManager
        planView.objectRepository = ObjectRepository(this)
        planView.trackRepository = TrackRepository(this)
        planView.applyTheme(theme)

        val toolsBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(theme.panelBg); setPadding(8, 10, 8, 4) }
        val toolParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(2, 0, 2, 0) }
        fun makeTool(text: String, icon: Int = 0): SkewButton = SkewButton(this@PlanEditorActivity).apply {
            this.text = text; this.iconRes = icon; this.theme = this@PlanEditorActivity.theme; layoutParams = toolParams
        }
        btnWall = makeTool("СТЕНА", R.drawable.ic_wall)
        val btnPan = makeTool("РУКА", R.drawable.ic_pan)
        btnTrack = makeTool("ТРАССА", R.drawable.ic_track)
        val btnEdit = makeTool("РЕД", R.drawable.ic_edit)
        btnElec = makeTool("ЭЛЕКТ", R.drawable.ic_elec)
        toolsBar.addView(btnWall); toolsBar.addView(btnPan); toolsBar.addView(btnTrack); toolsBar.addView(btnElec); toolsBar.addView(btnEdit)
        toolButtons.addAll(listOf(btnWall, btnPan, btnTrack, btnElec, btnEdit))

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


        contextPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(theme.panelBg)
            setPadding(8, 6, 8, 6)
            visibility = View.GONE
        }

        catalogScroll = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.panelBg); setPadding(8, 4, 8, 10); visibility = View.GONE }
        val catalogPanel = catalogScroll as LinearLayout
        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val searchBox = EditText(this).apply { hint = "Поиск символа…"; textSize = 14f; setTextColor(theme.textPrimary); setHintTextColor(theme.hintColor); setBackgroundColor(theme.btnBg); setPadding(16, 10, 16, 10); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        searchRow.addView(searchBox)
        searchRow.addView(TextView(this).apply { text = " ✕ "; textSize = 20f; setTextColor(theme.textPrimary); setPadding(20, 8, 20, 8); setOnClickListener { catalogScroll.visibility = View.GONE } })
        catalogPanel.addView(searchRow)
        val chipsScroll = HorizontalScrollView(this)
        val chipsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 6, 0, 6) }
        chipsScroll.addView(chipsRow); catalogPanel.addView(chipsScroll)
        val recentScroll = HorizontalScrollView(this)
        val recentRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, 6) }
        recentScroll.addView(recentRow); catalogPanel.addView(recentScroll)
        val itemsScroll = HorizontalScrollView(this)
        val itemsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        itemsScroll.addView(itemsRow); catalogPanel.addView(itemsScroll)

        fun catalogBtn(label: String, type: String): TextView = TextView(this).apply {
            text = label; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
            setBackgroundColor(theme.btnBg); setPadding(18, 12, 18, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
            setOnClickListener {
                planView.currentTool = PlanView.Tool.PLACE; planView.placeType = type
                catalogScroll.visibility = View.GONE
                val prefs = getSharedPreferences("recent", MODE_PRIVATE)
                val old = prefs.getString("list", "").orEmpty().split("|").filter { it.isNotEmpty() }.toMutableList()
                old.remove(type); old.add(0, type)
                prefs.edit().putString("list", old.take(6).joinToString("|")).apply()
                // rebuildRecent()
            }
        }
        var currentGroup = ""
        fun rebuildCatalog() {
            val q = searchBox.text.toString().trim().lowercase()
            itemsRow.removeAllViews()
            for (item in ru.gdesanek.model.Catalog.items) {
                if (currentGroup.isNotEmpty() && item.group != currentGroup) continue
                if (q.isNotEmpty() && !item.label.lowercase().contains(q) && !item.type.contains(q)) continue
                itemsRow.addView(catalogBtn(item.label, item.type))
            }
        }
        fun rebuildRecent() {
            recentRow.removeAllViews()
            val prefs = getSharedPreferences("recent", MODE_PRIVATE)
            for (t in prefs.getString("list", "").orEmpty().split("|").filter { it.isNotEmpty() }) {
                val item = ru.gdesanek.model.Catalog.items.firstOrNull { it.type == t } ?: continue
                recentRow.addView(catalogBtn("★ " + item.label, item.type))
            }
        }
        fun chip(label: String, group: String): TextView = TextView(this).apply {
            text = label; textSize = 12f; setTextColor(theme.textPrimary); setBackgroundColor(theme.btnActiveBg); setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
            setOnClickListener { currentGroup = group; rebuildCatalog() }
        }
        chipsRow.addView(chip("Все", ""))
        for (g in ru.gdesanek.model.Catalog.groups) chipsRow.addView(chip(g, g))
        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { rebuildCatalog() }
        })
        rebuildCatalog(); rebuildRecent()

        root.addView(topBar)
        root.addView(View(this).apply { setBackgroundColor(theme.accent); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4) })
        val stepper = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(theme.toolbarBg); setPadding(8, 6, 8, 6) }
        val stepNames = arrayOf("1 План", "2 Электрика", "3 Трассы", "4 Смета", "5 PDF")
        for (i in 0 until 5) {
            val s = TextView(this).apply {
                text = stepNames[i]; textSize = 12f; setTextColor(theme.textPrimary)
                setBackgroundColor(theme.btnBg); setPadding(10, 10, 10, 10)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 }
                gravity = android.view.Gravity.CENTER
                setOnClickListener { onStepClick(i) }
            }
            stepViews.add(s); stepper.addView(s)
        }
        statusLine = TextView(this).apply { textSize = 12f; setTextColor(ru.gdesanek.theme.Design.DIM); setBackgroundColor(theme.toolbarBg); setPadding(16, 4, 16, 6); typeface = android.graphics.Typeface.MONOSPACE }
        val frame = android.widget.FrameLayout(this)
        planView.layoutParams = android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
        val zoomPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, android.widget.FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL; marginEnd = 8 }
        }
        fun zoomBtn(t: String, f: Float): TextView = TextView(this).apply {
            text = t; textSize = 20f; setTextColor(theme.textPrimary); setBackgroundColor(theme.panelBg)
            setPadding(22, 14, 22, 14); setOnClickListener { planView.zoomBy(f) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 6 }
        }
        zoomPanel.addView(zoomBtn("+", 1.25f))
        zoomPanel.addView(zoomBtn("−", 0.8f))
        zoomPanel.addView(zoomBtn("⤢", 0f).apply { setOnClickListener { planView.fit() } })
        frame.addView(planView); frame.addView(zoomPanel)
        root.addView(stepper)
        root.addView(statusLine)
        root.addView(frame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(toolsBar)
        root.addView(contextPanel)
        root.addView(catalogScroll)
        setContentView(root)

        btnWall.performClick()
        planView.loadWalls()
        planView.loadObjects()
        planView.loadTracks()
        loadUnderlay()
        updateStepper(); updateStatus()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(object : Runnable { override fun run() { updateStatus(); updateStepper(); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 3000) } }, 3000)

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
        updateStepper(); updateStatus()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(object : Runnable { override fun run() { updateStatus(); updateStepper(); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 3000) } }, 3000)

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
                showShareDialog(uri, file)
            }
        }.start()
    }

    private fun showShareDialog(uri: android.net.Uri, file: java.io.File) {
        val names = arrayOf("WhatsApp", "Telegram", "Сохранить", "Другое…")
        AlertDialog.Builder(this)
            .setTitle("Отправить PDF")
            .setItems(names) { _, which ->
                when (which) {
                    0 -> sendTo("com.whatsapp", uri)
                    1 -> sendTo("org.telegram.messenger", uri)
                    2 -> saveToFiles(file)
                    3 -> {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(android.content.Intent.createChooser(send, "Отправить"))
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun sendTo(pkg: String, uri: android.net.Uri) {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(pkg)
        }
        try { startActivity(send) } catch (e: Exception) { Toast.makeText(this, "Приложение не установлено", Toast.LENGTH_SHORT).show() }
    }

    private fun saveToFiles(file: java.io.File) {
        val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val dest = java.io.File(dir, file.name)
        file.copyTo(dest, overwrite = true)
        Toast.makeText(this, "Сохранено в Downloads/${file.name}", Toast.LENGTH_LONG).show()
    }


    private fun renderPreview() {
        try {
            val walls = planView.walls
            if (walls.isEmpty()) return
            val minX = walls.minOf { minOf(it.x1, it.x2) } - 50f
            val maxX = walls.maxOf { maxOf(it.x1, it.x2) } + 50f
            val minY = walls.minOf { minOf(it.y1, it.y2) } - 50f
            val maxY = walls.maxOf { maxOf(it.y1, it.y2) } + 50f
            val size = 288
            val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(bmp)
            val sc = minOf(size / (maxX - minX), size / (maxY - minY))
            val p = android.graphics.Paint().apply { color = ru.gdesanek.theme.Design.ACCENT; strokeWidth = 4f }
            for (w in walls) c.drawLine((w.x1 - minX) * sc, (w.y1 - minY) * sc, (w.x2 - minX) * sc, (w.y2 - minY) * sc, p)
            java.io.FileOutputStream(java.io.File(filesDir, "preview_$projectId.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
        } catch (e: Exception) { }
    }


    private fun onStepClick(i: Int) {
        currentStep = i
        when (i) {
            0 -> btnWall.performClick()
            1 -> btnElec.performClick()
            2 -> btnTrack.performClick()
            3 -> startActivity(android.content.Intent(this, EstimateActivity::class.java).putExtra("PROJECT_ID", projectId).putExtra("PROJECT_NAME", intent.getStringExtra("PROJECT_NAME") ?: ""))
            4 -> shareBtn.performClick()
        }
        updateStepper(); updateStatus()
    }

    private fun updateStepper() {
        val names = arrayOf("1 План", "2 Электрика", "3 Трассы", "4 Смета", "5 PDF")
        for (i in stepViews.indices) {
            val done = when (i) { 0 -> planView.walls.isNotEmpty(); 1 -> planView.objects.isNotEmpty(); 2 -> planView.tracks.isNotEmpty(); else -> false }
            stepViews[i].text = names[i] + (if (done) " ✓" else "")
            stepViews[i].setBackgroundColor(if (i == currentStep) theme.accent else theme.btnBg)
            stepViews[i].setTextColor(if (i == currentStep) android.graphics.Color.WHITE else theme.textPrimary)
        }
    }

    private fun updateStatus() {
        val hint = when (currentStep) {
            0 -> "тап — начало стены, тап — конец; оранжевый кружок = привязка"
            1 -> "выбери символ в каталоге и тапай по плану"
            2 -> "тапай точки трассы по порядку, финиш в первой точке"
            3 -> "введи цены — итог снизу, PDF сметы там же"
            else -> "проверь лист и отправь заказчику"
        }
        val live = planView.liveTrackMeters(); statusLine.text = if (live > 0f) String.format("Кабель: %.1f m (запас 10%%)", live) else String.format("Шаг %d · Стен:%d Точек:%d Трасс:%d · %s", currentStep + 1, planView.walls.size, planView.objects.size, planView.tracks.size, hint)
    }
    override fun onDestroy() {
        super.onDestroy()
        planView.commitPending(); renderPreview()
    }

    private fun showCatalogPanel() {
        if (catalogScroll.visibility == View.VISIBLE) return
        catalogScroll.visibility = View.VISIBLE
        catalogScroll.post {
            catalogScroll.translationY = catalogScroll.height.toFloat()
            catalogScroll.animate().translationY(0f).setDuration(220).start()
        }
    }
}
