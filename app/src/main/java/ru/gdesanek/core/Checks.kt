package ru.gdesanek.core

import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.PlanObject
import ru.gdesanek.pdf.PanelPage

object Checks {
    fun run(objects: List<PlanObject>, tracks: List<CableTrack>): List<String> {
        val out = mutableListOf<String>()
        val groups = PanelPage.groupByTrack(tracks, objects)
        var totalA = 0.0
        groups.forEachIndexed { i, g ->
            val n = i + 1
            val power = g.objects.sumOf { PanelPage.defaultPower(it.type) }
            val cur = power * 1000.0 / 220.0
            totalA += cur
            val cable = g.track?.cable ?: ""
            val br = PanelPage.breakerBySection(PanelPage.parseSection(cable))
            if (g.track != null && cur > br) out.add("Гр.$n: ток ${String.format("%.0f", cur)} А выше автомата C$br")
            val len = if (g.track != null) PanelPage.trackLength(g.track.points) / 100f * 1.1f else 0f
            if (len > 40f) out.add("Гр.$n: ${String.format("%.0f", len)} м — проверьте падение напряжения")
            val cores = Regex("^(\\d+)x").find(cable)?.groupValues?.get(1)?.toIntOrNull() ?: 3
            if (g.track != null && cores < 3) out.add("Гр.$n: кабель $cores жилы — нет PE-заземления")
            val heavy = g.objects.filter { PanelPage.defaultPower(it.type) >= 2.0 }
            if (heavy.size > 1 || (heavy.isNotEmpty() && g.objects.size > heavy.size + 1))
                out.add("Гр.$n: мощный (${heavy.joinToString { it.name.ifEmpty { it.type } }}) — рекомендую отдельную линию")
        }
        val orphans = groups.filter { it.track == null }.flatMap { it.objects }
        if (orphans.isNotEmpty()) out.add("Без группы: ${orphans.size} об. (" + orphans.take(3).joinToString { it.name.ifEmpty { it.type } } + ")")
        if (totalA > 63) out.add("Суммарно ${String.format("%.0f", totalA)} А > вводного автомата 63 А")
        if (objects.none { it.type == "box_rk" } && objects.count { it.type.startsWith("socket") } > 4) out.add("Добавьте распаячные коробки (РК) для групп розеток")
        return out
    }
}
