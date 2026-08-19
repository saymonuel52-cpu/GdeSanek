package ru.gdesanek.model

data class Wall(
    val id: Long = 0,
    val projectId: Long,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)
