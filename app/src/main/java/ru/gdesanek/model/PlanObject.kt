package ru.gdesanek.model
data class PlanObject(val id: Long = 0, val projectId: Long, val type: String, val x: Float, val y: Float, val rotation: Float = 0f, val name: String = "", val area: Float = 0f, val height: Int = -1)
