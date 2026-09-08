package ru.gdesanek.render

object CategoryPalette {
    fun color(type: String): Int = when {
        type.startsWith("socket") -> 0xFFFF6B6B.toInt()
        type.startsWith("lamp") -> 0xFFFFC53D.toInt()
        type.startsWith("switch") -> 0xFF4D9FFF.toInt()
        type == "box_rk" || type.startsWith("panel") || type == "input_220" || type == "ground" -> 0xFFFF9F43.toInt()
        type.startsWith("cond") || type.startsWith("cons") -> 0xFFB57BFF.toInt()
        type.startsWith("rj45") || type.startsWith("sks") -> 0xFF3DDC84.toInt()
        else -> 0xFFCCCCCC.toInt()
    }
}
