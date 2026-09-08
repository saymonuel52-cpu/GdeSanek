package ru.gdesanek.model

data class CatalogItem(val type: String, val label: String, val group: String)

object Catalog {
    val groups = listOf("Розетки", "Выключатели", "Освещение", "Слаботочка", "Щиты", "Климат и нагрузка")

    val items = listOf(
        CatalogItem("socket_b1", "Розетка 1", "Розетки"),
        CatalogItem("socket_b2", "Розетка 2", "Розетки"),
        CatalogItem("socket_b4", "Розетка 3", "Розетки"),
        CatalogItem("socket_block2", "Блок 2 розетки", "Розетки"),
        CatalogItem("socket_block3", "Блок 3 розетки", "Розетки"),
        CatalogItem("socket_block4", "Блок 4 розетки", "Розетки"),
        CatalogItem("socket_k", "С заземлением", "Розетки"),
        CatalogItem("socket_b3", "Влагостойкая IP44", "Розетки"),
        CatalogItem("socket_380", "Силовая 380В", "Розетки"),
        CatalogItem("switch_1", "Выключатель 1-кл", "Выключатели"),
        CatalogItem("switch_2", "Выключатель 2-кл", "Выключатели"),
        CatalogItem("switch_3", "Выключатель 3-кл", "Выключатели"),
        CatalogItem("switch_pass", "Проходной", "Выключатели"),
        CatalogItem("switch_dim", "Диммер", "Выключатели"),
        CatalogItem("switch_move", "Датчик движения", "Выключатели"),
        CatalogItem("lamp_grig", "Точечный свет", "Освещение"),
        CatalogItem("lamp_lust", "Люстра", "Освещение"),
        CatalogItem("lamp_titan", "Лин. светильник 600", "Освещение"),
        CatalogItem("lamp_flame", "Лин. светильник 1200", "Освещение"),
        CatalogItem("lamp_bra", "Бра настенное", "Освещение"),
        CatalogItem("lamp_led", "LED-лента", "Освещение"),
        CatalogItem("lamp_street", "Уличный фонарь", "Освещение"),
        CatalogItem("lamp_ao", "Аварийный свет", "Освещение"),
        CatalogItem("lamp_exit", "Табло ВЫХОД", "Освещение"),
        CatalogItem("rj45", "Интернет RJ45", "Слаботочка"),
        CatalogItem("rj45x2", "Интернет 2xRJ45", "Слаботочка"),
        CatalogItem("sks_tv", "ТВ-розетка", "Слаботочка"),
        CatalogItem("sks_phone", "Телефон", "Слаботочка"),
        CatalogItem("sks_intercom", "Домофон", "Слаботочка"),
        CatalogItem("sks_cam", "Камера", "Слаботочка"),
        CatalogItem("sks_smoke", "Дымовой датчик", "Слаботочка"),
        CatalogItem("sks_sec", "Датчик охраны", "Слаботочка"),
        CatalogItem("panel_shr", "Электрощит", "Щиты"),
        CatalogItem("box_rk", "Распаячная коробка", "Щиты"),
        CatalogItem("panel_sks", "Щит слаботочки", "Щиты"),
        CatalogItem("input_220", "Ввод 220В", "Щиты"),
        CatalogItem("ground", "Заземление", "Щиты"),
        CatalogItem("cond_vk", "Кондиционер", "Климат и нагрузка"),
        CatalogItem("cons_hood", "Вытяжка", "Климат и нагрузка"),
        CatalogItem("cons_boiler", "Бойлер", "Климат и нагрузка"),
        CatalogItem("cons_stove", "Плита", "Климат и нагрузка"),
        CatalogItem("cons_pump", "Насос", "Климат и нагрузка")
    )

    fun byGroup(g: String) = items.filter { it.group == g }
}
