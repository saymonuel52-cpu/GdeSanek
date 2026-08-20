package ru.gdesanek.model

data class Wall(
    val id: Long = 0,
    val projectId: Long,
    val x1: Float, val y1: Float, val x2: Float, val y2: Float,
    val material: String = "beton",
    val thickness: Float = 100f
)

object WallMaterials {
    val list = listOf(
        "beton" to "Бетон / ж/б",
        "kirpich" to "Кирпич",
        "gazobeton" to "Газобетон",
        "gkl" to "Гипсокартон",
        "derevo" to "Дерево"
    )
    fun name(code: String) = list.firstOrNull { it.first == code }?.second ?: code
}

object WiringTypes {
    val list = listOf(
        "shtroba" to "Скрыто (штроба)",
        "otkryto" to "Открыто",
        "gofra" to "В гофре",
        "truba" to "В трубе",
        "kanal" to "Кабель-канал",
        "lotok" to "В лотке"
    )
    fun name(code: String) = list.firstOrNull { it.first == code }?.second ?: code
}
