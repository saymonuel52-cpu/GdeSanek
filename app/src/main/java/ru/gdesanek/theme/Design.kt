package ru.gdesanek.theme

import android.content.res.Resources

object Design {
    const val BG = 0xFF121212.toInt()
    const val SURFACE = 0xFF1B2026.toInt()
    const val SURFACE2 = 0xFF262D35.toInt()
    const val LINE = 0xFF303842.toInt()
    const val ACCENT = 0xFF00BFA5.toInt()
    const val WARN = 0xFFFFA000.toInt()
    const val DANGER = 0xFFFF5252.toInt()
    const val TEXT1 = 0xFFECEFF1.toInt()
    const val TEXT2 = 0xFF8FA3AD.toInt()
    const val DIM = 0xFF4DD0E1.toInt()

    const val TS_S = 12f; const val TS_M = 14f; const val TS_L = 16f; const val TS_XL = 20f
    const val GRID = 8
    const val R_CARD = 12f; const val R_BTN = 8f

    private val d = Resources.getSystem().displayMetrics
    fun dp(v: Int): Int = (v * d.density).toInt()
    fun dp(v: Float): Int = (v * d.density).toInt()
    fun sp(v: Float): Float = v * d.scaledDensity
}
