package ru.gdesanek.core

import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import kotlin.math.pow
import kotlin.math.sqrt

data class EstimateRow(val key: String, val name: String, val qty: Float, val unit: String)

object EstimateCalculator {
    fun trackMeters(tracks: List<CableTrack>): Float {
        var s = 0f
        for (t in tracks) for (i in 0 until t.points.size - 1) {
            s += sqrt((t.points[i+1].x - t.points[i].x).pow(2) + (t.points[i+1].y - t.points[i].y).pow(2))
        }
        return s / 100f * 1.1f
    }

    fun rows(objects: List<PlanObject>, tracks: List<CableTrack>): List<EstimateRow> = listOf(
        EstimateRow("cable", "Кабель ППГнг(А)-HF 3x2,5 (запас x1.1)", trackMeters(tracks), "м"),
        EstimateRow("socket", "Розетка 220В с заземлением (Б1/Б3)", objects.count { it.type == "socket_b1" || it.type == "socket_b3" }.toFloat(), "шт"),
        EstimateRow("switch", "Выключатель одноклавишный (О)", objects.count { it.type == "switch_o" }.toFloat(), "шт")
    )
}
