package ru.gdesanek.model

data class CatalogItem(val type: String, val label: String, val group: String)

object Catalog {
    val groups = listOf("Розетки", "Выкл", "Свет", "Слаботочка", "Щиты", "Нагрузка")

    val items = listOf(
        CatalogItem("socket_b1", "Б1", "Розетки"),
        CatalogItem("socket_b2", "Б2", "Розетки"),
        CatalogItem("socket_b3", "Б3 IP44", "Розетки"),
        CatalogItem("socket_b4", "Б4", "Розетки"),
        CatalogItem("socket_k", "К", "Розетки"),
        CatalogItem("socket_double", "2х", "Розетки"),
        CatalogItem("socket_380", "380В", "Розетки"),
        CatalogItem("switch_1", "1кл", "Выкл"),
        CatalogItem("switch_2", "2кл", "Выкл"),
        CatalogItem("switch_3", "3кл", "Выкл"),
        CatalogItem("switch_pass", "Прох", "Выкл"),
        CatalogItem("switch_dim", "Дим", "Выкл"),
        CatalogItem("switch_move", "Датч", "Выкл"),
        CatalogItem("lamp_titan", "600", "Свет"),
        CatalogItem("lamp_flame", "1200", "Свет"),
        CatalogItem("lamp_grig", "Точк", "Свет"),
        CatalogItem("lamp_lust", "Люстр", "Свет"),
        CatalogItem("lamp_bra", "Бра", "Свет"),
        CatalogItem("lamp_led", "Лента", "Свет"),
        CatalogItem("lamp_street", "Улич", "Свет"),
        CatalogItem("lamp_ao", "АО", "Свет"),
        CatalogItem("lamp_exit", "Выход", "Свет"),
        CatalogItem("rj45", "RJ45", "Слаботочка"),
        CatalogItem("rj45x2", "2RJ45", "Слаботочка"),
        CatalogItem("sks_tv", "ТВ", "Слаботочка"),
        CatalogItem("sks_phone", "Тел", "Слаботочка"),
        CatalogItem("sks_intercom", "Домоф", "Слаботочка"),
        CatalogItem("sks_cam", "Кам", "Слаботочка"),
        CatalogItem("sks_smoke", "Дым", "Слаботочка"),
        CatalogItem("sks_sec", "Охр", "Слаботочка"),
        CatalogItem("box_rk", "РК", "Щиты"),
        CatalogItem("panel_shr", "ЩР", "Щиты"),
        CatalogItem("panel_sks", "СКС", "Щиты"),
        CatalogItem("input_220", "Ввод", "Щиты"),
        CatalogItem("ground", "Зазем", "Щиты"),
        CatalogItem("cond_vk", "ВК", "Нагрузка"),
        CatalogItem("cons_boiler", "Бойл", "Нагрузка"),
        CatalogItem("cons_stove", "Плита", "Нагрузка"),
        CatalogItem("cons_pump", "Насос", "Нагрузка")
    )

    fun byGroup(g: String) = items.filter { it.group == g }
}
