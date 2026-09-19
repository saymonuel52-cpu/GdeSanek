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
import ru.gdesanek.ui.Design
import ru.gdesanek.theme.ThemeManager
import ru.gdesanek.theme.Themes
import ru.gdesanek.ui.PlanView
import java.io.File

class PlanEditorActivity : AppCompatActivity() {
    private lateinit var planView: PlanView
    private val undoManager = ru.gdesanek.core.UndoManager(50)
    private lateinit var contextPanel: LinearLayout
    private lateinit var catalogScroll: View
    private lateinit var btnWall: TextView
    private lateinit var btnElec: TextView
    private lateinit var btnTrack: TextView
    private lateinit var shareBtn: TextView
    private val stepViews = mutableListOf<TextView>()
    private lateinit var statusLine: TextView
    private var currentStep = 0
    private lateinit var theme: AppTheme
    private var projectId = 0L
    private var projectName = "План"
    private var currentCatalogGroup = "Розетки"
    private val catalogButtons = mutableListOf<TextView>()
    private val toolButtons = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getLongExtra("PROJECT_ID", 0)
        projectName = intent.getStringExtra("PROJECT_NAME") ?: "План"
        theme = ThemeManager.current(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.canvasBg) }

        // Новый топбар: ☰ | имя | ↺ | ⋮
        val topBar = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(theme.toolbarBg); setPadding(Design.Spacing.MEDIUM, 8, Design.Spacing.MEDIUM, 8)
            elevation = Design.ELEVATION
        }
        val menuBtn = TextView(this).apply { 
            text = "☰"; textSize = 24f; setTextColor(theme.textPrimary)
            setPadding(Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL)
            setOnClickListener { showThemeDialog() }
        }
        val title = TextView(this).apply { 
            text = projectName; textSize = 18f; setTextColor(theme.textPrimary)
            try { typeface = androidx.core.content.res.ResourcesCompat.getFont(this@PlanEditorActivity, R.font.russoone) } catch (e: Exception) {}
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { startActivity(Intent(this@PlanEditorActivity, MainActivity::class.java)) }
        }
        val undoBtn = TextView(this).apply { 
            text = "↶"; textSize = 22f; setTextColor(theme.textPrimary)
            setPadding(Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL)
            setOnClickListener { if (undoManager.undo()) planView.reloadAll() }
        }
        val redoBtn = TextView(this).apply { 
            text = "↷"; textSize = 22f; setTextColor(theme.textPrimary)
            setPadding(Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL)
            setOnClickListener { if (undoManager.redo()) planView.reloadAll() }
        }
        val moreBtn = TextView(this).apply { 
            text = "⋮"; textSize = 24f; setTextColor(theme.textPrimary)
            setPadding(Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL)
            setOnClickListener { 
                android.widget.PopupMenu(this@PlanEditorActivity, this).apply {
                    menu.add("Подложка").setOnMenuItemClickListener { 
                        if (planView.underlay == null) pickUnderlay()
                        else android.app.AlertDialog.Builder(this@PlanEditorActivity).setTitle("Подложка").setItems(arrayOf("Калибровать", "Прозрачность", "Заменить", "Убрать")) { _, i ->
                            when (i) { 0 -> planView.startCalibration(); 1 -> showUnderlayDialog(); 2 -> pickUnderlay(); 3 -> removeUnderlay() }
                        }.show()
                        true
                    }
                    menu.add("Смета").setOnMenuItemClickListener { startActivity(Intent(this@PlanEditorActivity, EstimateActivity::class.java).putExtra("PROJECT_ID", projectId)); true }
                    menu.add("Экспорт PDF").setOnMenuItemClickListener { exportPdf(); true }
                    menu.add("Заказчик").setOnMenuItemClickListener { startActivity(Intent(this@PlanEditorActivity, ClientActivity::class.java).putExtra("PROJECT_ID", projectId)); true }
                    show()
                }
            }
        }
        topBar.addView(menuBtn); topBar.addView(title); topBar.addView(undoBtn); topBar.addView(redoBtn); topBar.addView(moreBtn)
        
        // shareBtn (используется в stepper, но не виден в UI)
        shareBtn = TextView(this).apply { visibility = View.GONE }

        planView = PlanView(this)
        planView.projectId = projectId
        planView.repository = WallRepository(this)
        planView.undoManager = undoManager
        planView.post { planView.fitToContent() }
        planView.objectRepository = ObjectRepository(this)
        planView.trackRepository = TrackRepository(this)
        planView.applyTheme(theme)

        // Карточка "Начнём?" на пустом холсте (П2)
        val startCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0x80000000.toInt()); setPadding(Design.Spacing.LARGE, Design.Spacing.LARGE, Design.Spacing.LARGE, Design.Spacing.LARGE)
            visibility = if (planView.walls.isEmpty() && planView.objects.isEmpty() && planView.tracks.isEmpty()) View.VISIBLE else View.GONE
        }
        val cardTitle = TextView(this).apply { 
            text = "Начнём?"; textSize = 24f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
            setPadding(0, 0, 0, Design.Spacing.MEDIUM)
        }
        val btnStartWall = TextView(this).apply { 
            text = "🧱 Нарисовать стены"; textSize = 16f; setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(Design.Spacing.LARGE, Design.Spacing.MEDIUM, Design.Spacing.LARGE, Design.Spacing.MEDIUM)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Design.Spacing.SMALL }
            setOnClickListener { btnWall.performClick(); startCard.visibility = View.GONE }
        }
        val btnStartDemo = TextView(this).apply { 
            text = "📋 Примеры: список проектов"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(Design.Spacing.LARGE, Design.Spacing.MEDIUM, Design.Spacing.LARGE, Design.Spacing.MEDIUM)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = Design.Spacing.SMALL }
            setOnClickListener { finish() }
        }
        val btnStartPhoto = TextView(this).apply { 
            text = "🖼 План с фото"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(Design.Spacing.LARGE, Design.Spacing.MEDIUM, Design.Spacing.LARGE, Design.Spacing.MEDIUM)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { pickUnderlay(); startCard.visibility = View.GONE }
        }
        startCard.addView(cardTitle); startCard.addView(btnStartWall); startCard.addView(btnStartDemo); startCard.addView(btnStartPhoto)

        // Новый тулбар: плоские кнопки с подписью, 64dp
        val toolsBar = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(theme.panelBg)
            setPadding(Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL)
            elevation = Design.ELEVATION
        }
        val toolParams = LinearLayout.LayoutParams(0, Design.TOOL_HEIGHT.dpToPx(), 1f).apply { setMargins(2, 0, 2, 0) }
        fun highlightTool(sel: TextView?) { toolButtons.forEach { b -> b.setBackgroundColor(if (b == sel) (b.tag as? Int ?: 0xFF333333.toInt()) else 0x00000000) } }
        fun makeTool(text: String, icon: Int, color: Int): TextView = TextView(this@PlanEditorActivity).apply {
            setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0)
            compoundDrawablePadding = 4.dpToPx()
            this.text = text
            tag = color
            textSize = 11f
            setTextColor(theme.textPrimary)
            gravity = Gravity.CENTER
            setBackgroundColor(0x00000000)
            layoutParams = LinearLayout.LayoutParams(0, Design.TOOL_HEIGHT.dpToPx(), 1f).apply { setMargins(2, 0, 2, 0) }
            setOnClickListener {
                planView.currentTool = when (text) {
                    "Выбор" -> PlanView.Tool.PAN
                    "Стена" -> PlanView.Tool.DRAW_WALL
                    "Трасса" -> PlanView.Tool.DRAW_TRACK
                    "Элект" -> PlanView.Tool.PLACE
                    "Правка" -> PlanView.Tool.EDIT
                    else -> PlanView.Tool.PAN
                }
                if (text == "Элект") planView.placeType = "socket_b1" else planView.placeType = null
                highlightTool(this)
                when (text) {
                    "Стена" -> showWallContext()
                    "Трасса" -> showTrackContext()
                    "Элект" -> showCatalog()
                    else -> hideContext()
                }
                showHint(hintFor(text))
            }
        }
        
        btnWall = makeTool("Стена", R.drawable.ic_wall, Design.Colors.WALL)
        val btnPan = makeTool("Выбор", R.drawable.ic_pan, Design.Colors.OBJECT)
        btnTrack = makeTool("Трасса", R.drawable.ic_track, Design.Colors.TRACK)
        val btnEdit = makeTool("Правка", R.drawable.ic_edit, Design.Colors.EDIT)
        btnElec = makeTool("Элект", R.drawable.ic_elec, Design.Colors.ELEC)
        
        toolsBar.addView(btnPan); toolsBar.addView(btnWall); toolsBar.addView(btnElec); toolsBar.addView(btnTrack); toolsBar.addView(btnEdit)
        toolButtons.addAll(listOf(btnWall, btnPan, btnTrack, btnElec, btnEdit))
        
        // П1: вход всегда в Выбор
        planView.currentTool = PlanView.Tool.PAN
        highlightTool(btnPan)

        contextPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(theme.panelBg)
            setPadding(Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL)
            visibility = View.GONE
        }

        catalogScroll = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(theme.panelBg); setPadding(Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.SMALL, Design.Spacing.MEDIUM); visibility = View.GONE }
        val catalogPanel = catalogScroll as LinearLayout
        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val searchBox = EditText(this).apply { hint = "Поиск символа…"; textSize = 14f; setTextColor(theme.textPrimary); setHintTextColor(theme.hintColor); setBackgroundColor(theme.btnBg); setPadding(Design.Spacing.MEDIUM, 10, Design.Spacing.MEDIUM, 10); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
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

        fun catalogButton(label: String, type: String): TextView = TextView(this).apply {
            text = label; setTextColor(theme.textPrimary); textSize = 12f; gravity = Gravity.CENTER
            setBackgroundColor(theme.btnBg); setPadding(18, 12, 18, 12)
            val bmp = android.graphics.Bitmap.createBitmap(44, 44, android.graphics.Bitmap.Config.ARGB_8888); val bcv = android.graphics.Canvas(bmp); bcv.scale(0.7f, 0.7f, 22f, 22f); val pp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = ru.gdesanek.render.CategoryPalette.color(type); style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }; ru.gdesanek.render.GostSymbols.draw(bcv, type, 22f, 24f, 0f, pp); compoundDrawablePadding = 6; setCompoundDrawablesWithIntrinsicBounds(null, android.graphics.drawable.BitmapDrawable(resources, bmp), null, null)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
            setOnClickListener {
                planView.currentTool = PlanView.Tool.PLACE; planView.placeType = type
                catalogScroll.visibility = View.GONE
                val prefs = getSharedPreferences("recent", MODE_PRIVATE)
                val old = prefs.getString("list", "").orEmpty().split("|").filter { it.isNotEmpty() }.toMutableList()
                old.remove(type); old.add(0, type)
                prefs.edit().putString("list", old.take(6).joinToString("|")).apply()
            }
        }
        var currentGroup = ""
        fun rebuildCatalog() {
            val q = searchBox.text.toString().trim().lowercase()
            itemsRow.removeAllViews()
            for (item in ru.gdesanek.model.Catalog.items) {
                if (currentGroup.isNotEmpty() && item.group != currentGroup) continue
                if (q.isNotEmpty() && !item.label.lowercase().contains(q) && !item.type.contains(q)) continue
                itemsRow.addView(catalogButton(item.label, item.type))
            }
        }
        fun rebuildRecent() {
            recentRow.removeAllViews()
            val prefs = getSharedPreferences("recent", MODE_PRIVATE)
            for (t in prefs.getString("list", "").orEmpty().split("|").filter { it.isNotEmpty() }) {
                val item = ru.gdesanek.model.Catalog.items.firstOrNull { it.type == t } ?: continue
                recentRow.addView(catalogButton("★ " + item.label, item.type))
            }
        }
        fun chip(label: String, group: String): TextView = TextView(this).apply {
            text = label; textSize = 12f; setTextColor(theme.textPrimary); setBackgroundColor(theme.btnActiveBg); setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 }
            setOnClickListener { currentGroup = group; rebuildCatalog() }
        }
        chipsRow.addView(chip("Все", ""))
        for (g in ru.gdesanek.model.Catalog.items.map { it.group }.distinct()) chipsRow.addView(chip(g, g))
        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { rebuildCatalog() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        rebuildRecent(); rebuildCatalog()
        // FrameLayout с planView и карточкой "Начнём?"
        val frame = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        frame.addView(planView, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        frame.addView(startCard, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        
        // П5: пилюля-подсказка поверх холста
        statusLine = TextView(this).apply {
            textSize = 13f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
            setPadding(24, 10, 24, 10); elevation = 6f
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16f * resources.displayMetrics.density
                setColor(0xCC1A1A1A.toInt())
            }
            visibility = View.GONE
        }
        frame.addView(statusLine, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; topMargin = 12.dpToPx(); leftMargin = 24.dpToPx(); rightMargin = 24.dpToPx() })
        
        root.addView(frame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(toolsBar)
        root.addView(contextPanel)
        root.addView(catalogScroll)
        root.addView(topBar, 0)
        setContentView(root)

        // П1: вход всегда в Выбор
        btnPan.performClick()
        planView.loadWalls()
        planView.loadObjects()
        planView.loadTracks()
        startCard.visibility = if (planView.walls.isEmpty() && planView.objects.isEmpty() && planView.tracks.isEmpty()) View.VISIBLE else View.GONE
        loadUnderlay()
        updateStepper(); updateStatus()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(object : Runnable { override fun run() { updateStatus(); updateStepper(); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 3000) } }, 3000)

    }

    private val hintHideRunnable = Runnable { statusLine.visibility = View.GONE }
    private fun showHint(text: String) {
        statusLine.text = text
        statusLine.visibility = View.VISIBLE
        statusLine.removeCallbacks(hintHideRunnable)
        statusLine.postDelayed(hintHideRunnable, 4000)
    }
    private fun hintFor(mode: String) = when (mode) {
        "Выбор" -> "Выбор: тап по объекту — свойства, перетаскивание — перемещение"
        "Стена" -> "Стена: тап — начало, тап — конец; оранжевый кружок = привязка"
        "Элект" -> "Элект: выбери символ в каталоге и тапай по плану"
        "Трасса" -> "Трасса: тапай точки по порядку, финиш в первой точке"
        "Правка" -> "Правка: тап по объекту или стене → ручки; долгий тап → удалить"
        else -> ""
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
                val bmp = android.graphics.Bitmap.createBitmap(44, 44, android.graphics.Bitmap.Config.ARGB_8888); val bcv = android.graphics.Canvas(bmp); bcv.scale(0.7f, 0.7f, 22f, 22f); val pp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = ru.gdesanek.render.CategoryPalette.color(item.type); style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }; ru.gdesanek.render.GostSymbols.draw(bcv, item.type, 22f, 24f, 0f, pp); compoundDrawablePadding = 6; setCompoundDrawablesWithIntrinsicBounds(null, android.graphics.drawable.BitmapDrawable(resources, bmp), null, null)
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
        val live = planView.liveTrackMeters()
        if (live > 0f) showHint(String.format("Кабель: %.1f м (запас 10%%)", live))
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


fun Int.dpToPx(): Int = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
