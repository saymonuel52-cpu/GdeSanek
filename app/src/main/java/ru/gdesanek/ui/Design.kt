package ru.gdesanek.ui

object Design {
    val TOOLBAR_HEIGHT = 56
    val TOOL_HEIGHT = 64
    val RADIUS = 16f
    val ELEVATION = 4f
    
    object Colors {
        val WALL = 0xFF5D4037.toInt()
        val OBJECT = 0xFF1976D2.toInt()
        val TRACK = 0xFFD32F2F.toInt()
        val ELEC = 0xFFF57C00.toInt()
        val EDIT = 0xFF388E3C.toInt()
        
        val SURFACE_LIGHT = 0xFFFAFAFA.toInt()
        val SURFACE_DARK = 0xFF1E1E1E.toInt()
        val CANVAS_LIGHT = 0xFFFFFFFF.toInt()
        val CANVAS_DARK = 0xFF2D2D2D.toInt()
        
        val ACTIVE_ALPHA = 0xFF
        val INACTIVE_ALPHA = 0xCC
    }
    
    object Spacing {
        val SMALL = 8
        val MEDIUM = 16
        val LARGE = 24
    }
}
