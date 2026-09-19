package ru.gdesanek.core

object DefaultPrices {
    val prices = mapOf(
        "cable" to 120f,        // Кабель ВВГнг 3×2.5, ₽/м
        "socket" to 350f,       // Розетка 220В с/з
        "socket380" to 1200f,   // Розетка 380В силовая
        "switch" to 300f,       // Выключатель
        "lamp600" to 450f,      // Светильник 600×600
        "lamp1200" to 650f,     // Светильник 1200
        "lampspot" to 250f,     // Светильник точечный
        "lamplust" to 1500f,    // Люстра
        "lampbra" to 800f,      // Бра настенное
        "lampled" to 350f,      // LED-лента, ₽/м
        "lampstreet" to 2500f,  // Светильник уличный
        "lampao" to 1200f,      // Светильник аварийный
        "lampexit" to 800f,     // Табло «Выход»
        "rj45" to 250f,         // Розетка RJ45
        "rj45x2" to 400f,       // Розетка RJ45 двойная
        "tv" to 300f,           // Розетка ТВ
        "phone" to 250f,        // Розетка телефонная
        "intercom" to 3500f,    // Домофон
        "cam" to 4500f,         // Камера видеонаблюдения
        "smoke" to 1200f,       // Датчик дыма
        "sec" to 1800f,         // Датчик охранный
        "rk" to 150f,           // Коробка распределительная РК
        "shr" to 2500f,         // Щит распределительный ЩР
        "sks" to 3500f,         // Щит слаботочный СКС
        "input" to 1500f,       // Ввод 220В
        "ground" to 2000f,      // Заземление
        "vk" to 25000f,         // Кондиционер
        "boiler" to 8000f,      // Бойлер
        "stove" to 3500f,       // Плита электрическая
        "pump" to 12000f,       // Насос
        "work_point" to 450f,   // Монтаж точки
        "work_lamp" to 350f,    // Монтаж светильника
        "work_shtroba" to 150f, // Штроба, ₽/м
        "work_gofra" to 40f,    // Затяжка в гофру, ₽/м
        "work_open" to 80f,     // Открытая прокладка, ₽/м
        "work_panel" to 3500f   // Сборка щита
    )

    fun get(key: String): Float = prices[key] ?: 0f
}
