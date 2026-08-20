package ru.gdesanek.model

data class CatalogItem(val type: String, val label: String)

object Catalog {
    val items = listOf(
        CatalogItem("socket_b1", "🔌 Б1"), CatalogItem("socket_b2", "🔌 Б2"),
        CatalogItem("socket_b3", "💧 Б3"), CatalogItem("socket_b4", "🔋 Б4"),
        CatalogItem("socket_k", "🖥 К"), CatalogItem("socket_double", "🔌 2х"),
        CatalogItem("socket_380", "⚡ 380"),
        CatalogItem("switch_1", "🎚 1кл"), CatalogItem("switch_2", "🎚 2кл"),
        CatalogItem("switch_3", "🎚 3кл"), CatalogItem("switch_pass", "🔁 Прох"),
        CatalogItem("switch_dim", "🌀 Дим"), CatalogItem("switch_move", "🚶 Датч"),
        CatalogItem("lamp_titan", "💡 600"), CatalogItem("lamp_flame", "💡 1200"),
        CatalogItem("lamp_grig", "💡 Точк"), CatalogItem("lamp_lust", "💡 Люстр"),
        CatalogItem("lamp_bra", "💡 Бра"), CatalogItem("lamp_led", "💡 Лента"),
        CatalogItem("lamp_street", "🔦 Улич"), CatalogItem("lamp_ao", "🚨 АО"),
        CatalogItem("lamp_exit", "🏃 Вых"),
        CatalogItem("rj45", "🌐 RJ45"), CatalogItem("rj45x2", "🌐 2RJ45"),
        CatalogItem("sks_tv", "📺 ТВ"), CatalogItem("sks_phone", "☎ Тел"),
        CatalogItem("sks_intercom", "🚪 Домоф"), CatalogItem("sks_cam", "📷 Кам"),
        CatalogItem("sks_smoke", "💨 Дым"), CatalogItem("sks_sec", "🛡 Охр"),
        CatalogItem("box_rk", "📦 РК"), CatalogItem("panel_shr", "⚡ ЩР"),
        CatalogItem("panel_sks", "🗄 СКС"), CatalogItem("input_220", "🔋 Ввод"),
        CatalogItem("ground", "🟢 Зазем"),
        CatalogItem("cond_vk", "❄ ВК"), CatalogItem("cons_boiler", "♨ Бойл"),
        CatalogItem("cons_stove", "🍳 Плита"), CatalogItem("cons_pump", "⚙ Насос")
    )
}
