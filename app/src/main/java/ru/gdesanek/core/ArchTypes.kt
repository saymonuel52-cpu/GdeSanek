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

    fun isFurn(type: String): Boolean = type.startsWith("furn_")

    fun furnDims(type: String): Pair<Float, Float> = when (type) {
        "furn_bed2" -> 160f to 200f
        "furn_bed1" -> 90f to 200f
        "furn_sofa" -> 200f to 90f
        "furn_table" -> 120f to 80f
        "furn_kitchen" -> 240f to 60f
        "furn_bath" -> 170f to 75f
        "furn_wc" -> 40f to 65f
        "furn_sink" -> 50f to 40f
        "furn_ward" -> 200f to 60f
        "furn_wash" -> 60f to 60f
        "furn_stove" -> 60f to 60f
        else -> 60f to 60f
    }
}
