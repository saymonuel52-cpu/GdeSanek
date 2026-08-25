package ru.gdesanek.theme

import android.content.Context
import android.graphics.Color

data class AppTheme(
    val id: String,
    val name: String,
    val canvasBg: Int,
    val toolbarBg: Int,
    val panelBg: Int,
    val btnBg: Int,
    val btnActiveBg: Int,
    val accent: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val gridColor: Int,
    val wallColor: Int,
    val trackColor: Int,
    val hintColor: Int
)

object SymbolPalette {
    fun color(type: String): Int = when {
        type.startsWith("socket") -> Color.parseColor("#FF5252")
        type.startsWith("switch") -> Color.parseColor("#FF7043")
        type.startsWith("lamp") -> Color.parseColor("#FFCA28")
        type.startsWith("sks") || type.startsWith("rj45") -> Color.parseColor("#40C4FF")
        type.startsWith("panel") || type.startsWith("box") || type.startsWith("ground") || type.startsWith("input") -> Color.parseColor("#26A69A")
        type.startsWith("cons") || type.startsWith("cond") -> Color.parseColor("#AB47BC")
        else -> Color.WHITE
    }

    fun height(type: String): Int? = when {
        type.startsWith("socket_b") -> 30
        type.startsWith("socket_k") -> 110
        type.startsWith("socket_380") -> 100
        type.startsWith("switch") -> 90
        type.startsWith("lamp") -> 240
        type.startsWith("box") -> 150
        type.startsWith("panel") -> 180
        type.startsWith("cond_vk") -> 200
        type.startsWith("cons_boiler") -> 100
        else -> null
    }

    fun power(type: String): Int? = when {
        type.startsWith("lamp_grig") -> 50
        type.startsWith("lamp_lust") -> 180
        type.startsWith("lamp_titan") -> 36
        type.startsWith("lamp_flame") -> 72
        type.startsWith("lamp_bra") -> 60
        type.startsWith("lamp_led") -> 12
        type.startsWith("lamp_street") -> 100
        type.startsWith("lamp_ao") -> 5
        type.startsWith("lamp_exit") -> 5
        else -> null
    }
}

object Themes {
    val classic = AppTheme(
        "classic", "Классика AutoCAD",
        Color.parseColor("#121212"), Color.parseColor("#1A1A1A"), Color.parseColor("#14181B"),
        Color.parseColor("#1E1E1E"), Color.parseColor("#008C9E"), Color.parseColor("#008C9E"),
        Color.WHITE, Color.parseColor("#B0B0B0"), Color.parseColor("#222222"),
        Color.WHITE, Color.parseColor("#4CAF50"), Color.parseColor("#777777")
    )
    val paper = AppTheme(
        "paper", "ГОСТ Бумага",
        Color.parseColor("#F2F0EB"), Color.parseColor("#1A1A1A"), Color.parseColor("#14181B"),
        Color.parseColor("#1E1E1E"), Color.parseColor("#008C9E"), Color.parseColor("#008C9E"),
        Color.WHITE, Color.parseColor("#B0B0B0"), Color.parseColor("#D8D5CE"),
        Color.parseColor("#222222"), Color.parseColor("#222222"), Color.parseColor("#9E9E9E")
    )
    val blueprint = AppTheme(
        "blueprint", "Blueprint (чертёж)",
        Color.parseColor("#0A2540"), Color.parseColor("#0D2F52"), Color.parseColor("#0A2540"),
        Color.parseColor("#0D2F52"), Color.parseColor("#FFD54F"), Color.parseColor("#FFD54F"),
        Color.WHITE, Color.parseColor("#B3D4FC"), Color.parseColor("#1E4870"),
        Color.WHITE, Color.parseColor("#FFD54F"), Color.parseColor("#6B8FB8")
    )
    val terminal = AppTheme(
        "terminal", "Ретро-терминал",
        Color.BLACK, Color.BLACK, Color.BLACK,
        Color.parseColor("#0A0A0A"), Color.parseColor("#00FF9D"), Color.parseColor("#00FF9D"),
        Color.parseColor("#00FF9D"), Color.parseColor("#00AA66"), Color.parseColor("#003322"),
        Color.parseColor("#00FF9D"), Color.parseColor("#FF6B00"), Color.parseColor("#006644")
    )
    val glossy = AppTheme(
        "glossy", "Глянец",
        Color.parseColor("#0F1829"), Color.parseColor("#162238"), Color.parseColor("#0F1829"),
        Color.parseColor("#1E2B47"), Color.parseColor("#4FC3F7"), Color.parseColor("#4FC3F7"),
        Color.WHITE, Color.parseColor("#B3D4FC"), Color.parseColor("#1A2A4A"),
        Color.WHITE, Color.parseColor("#81C784"), Color.parseColor("#6B7FA0")
    )
    val hatching = AppTheme(
        "hatching", "ГОСТ-штриховка",
        Color.parseColor("#1C1C1C"), Color.parseColor("#252525"), Color.parseColor("#1C1C1C"),
        Color.parseColor("#2A2A2A"), Color.parseColor("#008C9E"), Color.parseColor("#008C9E"),
        Color.WHITE, Color.parseColor("#C0C0C0"), Color.parseColor("#333333"),
        Color.WHITE, Color.parseColor("#66BB6A"), Color.parseColor("#888888")
    )

    val all = listOf(classic)
    fun byId(id: String) = all.firstOrNull { it.id == id } ?: classic
}

object ThemeManager {
    private const val PREF = "theme_prefs"
    private const val KEY = "current_theme"
    fun current(ctx: Context): AppTheme {
        val id = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "classic") ?: "classic"
        return Themes.byId(id)
    }
    fun set(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, id).apply()
    }
}
