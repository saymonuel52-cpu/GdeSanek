package ru.gdesanek.model
data class TrackPoint(val x: Float, val y: Float)
data class CableTrack(val id: Long = 0, val projectId: Long, val kind: String, val points: List<TrackPoint>, val wiring: String = "shtroba", val color: Int = -11747600, val cable: String = "3x2.5")
