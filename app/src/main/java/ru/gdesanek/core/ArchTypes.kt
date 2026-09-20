package ru.gdesanek.core

object ArchTypes {
    fun isArch(type: String): Boolean = type.startsWith("arch_")
    
    fun size(type: String): Float = when (type) {
        "arch_door800", "arch_open900" -> 80f
        "arch_door900" -> 90f
        "arch_win1200" -> 120f
        "arch_win1400" -> 140f
        "arch_win1800" -> 180f
        else -> 0f
    }
}
