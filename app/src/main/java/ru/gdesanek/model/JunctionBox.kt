package ru.gdesanek.model

data class JunctionBox(
    val id: Long = 0,
    val projectId: Long,
    val x: Float,
    val y: Float,
    val height: Float = 250f,
    val name: String = "",
    val manual: Boolean = false
)
