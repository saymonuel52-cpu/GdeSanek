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

    fun rows(objects: List<PlanObject>, tracks: List<CableTrack>): List<EstimateRow> {
        val r = mutableListOf<EstimateRow>()
        fun add(key: String, name: String, qty: Float, unit: String) {
            if (qty > 0f) r.add(EstimateRow(key, name, qty, unit))
        }
        fun count(vararg types: String) = objects.count { it.type in types }.toFloat()

        add("cable", "Кабель ВВГнг(А)-LS 3х2,5 (запас 10%)", trackMeters(tracks), "м")
        add("socket", "Розетка 220В с/з (Б1/Б2/Б3/Б4/К/2х)", count("socket_b1","socket_b2","socket_b3","socket_b4","socket_k","socket_double"), "шт")
        add("socket380", "Розетка 380В силовая", count("socket_380"), "шт")
        add("switch", "Выключатель (1/2/3кл, проходн., диммер, датчик)", count("switch_1","switch_2","switch_3","switch_pass","switch_dim","switch_move"), "шт")
        add("lamp600", "Светильник 600х600", count("lamp_titan"), "шт")
        add("lamp1200", "Светильник 1200", count("lamp_flame"), "шт")
        add("lampspot", "Светильник точечный", count("lamp_grig"), "шт")
        add("lamplust", "Люстра", count("lamp_lust"), "шт")
        add("lampbra", "Бра настенное", count("lamp_bra"), "шт")
        add("lampled", "LED-лента", count("lamp_led"), "м")
        add("lampstreet", "Светильник уличный", count("lamp_street"), "шт")
        add("lampao", "Светильник аварийный", count("lamp_ao"), "шт")
        add("lampexit", "Табло «Выход»", count("lamp_exit"), "шт")
        add("rj45", "Розетка RJ45", count("rj45"), "шт")
        add("rj45x2", "Розетка RJ45 двойная", count("rj45x2"), "шт")
        add("tv", "Розетка ТВ", count("sks_tv"), "шт")
        add("phone", "Розетка телефонная", count("sks_phone"), "шт")
        add("intercom", "Домофон", count("sks_intercom"), "шт")
        add("cam", "Камера видеонаблюдения", count("sks_cam"), "шт")
        add("smoke", "Датчик дыма", count("sks_smoke"), "шт")
        add("sec", "Датчик охранный", count("sks_sec"), "шт")
        add("rk", "Коробка распределительная РК", count("box_rk"), "шт")
        add("shr", "Щит распределительный ЩР", count("panel_shr"), "шт")
        add("sks", "Щит слаботочный СКС", count("panel_sks"), "шт")
        add("input", "Ввод 220В", count("input_220"), "шт")
        add("ground", "Заземление", count("ground"), "шт")
        add("vk", "Кондиционер", count("cond_vk"), "шт")
        add("boiler", "Бойлер", count("cons_boiler"), "шт")
        add("stove", "Плита электрическая", count("cons_stove"), "шт")
        add("pump", "Насос", count("cons_pump"), "шт")
        return r
    }
}
