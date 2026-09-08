package ru.gdesanek.demo

import android.content.Context
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.db.WallRepository
import ru.gdesanek.model.TrackPoint

object DemoProject {
    fun load(context: Context): Long {
        val projectId = ProjectRepository(context).insert("Демо: Кухня-гостиная", "Пример планировки")
        val walls = WallRepository(context)
        val objects = ObjectRepository(context)
        val tracks = TrackRepository(context)

        // Стены (координаты в см, 600×400 см = 6×4 м)
        walls.insert(projectId, 0f, 0f, 600f, 0f, "beton", 200f)
        walls.insert(projectId, 600f, 0f, 600f, 400f, "beton", 200f)
        walls.insert(projectId, 600f, 400f, 0f, 400f, "beton", 200f)
        walls.insert(projectId, 0f, 400f, 0f, 0f, "beton", 200f)
        walls.insert(projectId, 300f, 80f, 300f, 400f, "kirpich", 120f)
        walls.insert(projectId, 0f, 80f, 300f, 80f, "kirpich", 120f)
        walls.insert(projectId, 450f, 0f, 450f, 80f, "kirpich", 120f)
        walls.insert(projectId, 450f, 80f, 600f, 80f, "kirpich", 120f)

        // Кухня
        objects.insert(projectId, "socket_block2", 100f, 200f, 0f, "Розетки рабочая зона", 0f, 110)
        objects.insert(projectId, "socket_380", 100f, 380f, 0f, "Плита", 0f, 30)
        objects.insert(projectId, "cons_hood", 150f, 50f, 0f, "Вытяжка", 0f, 220)
        objects.insert(projectId, "lamp_grig", 150f, 250f, 0f, "Точечный свет кухня", 0f, 270)
        objects.insert(projectId, "switch_1", 280f, 200f, 0f, "Выключатель кухня", 0f, 110)

        // Гостиная
        objects.insert(projectId, "sks_tv", 400f, 380f, 0f, "ТВ-розетка", 0f, 30)
        objects.insert(projectId, "socket_b2", 500f, 200f, 0f, "Розетка диван", 0f, 30)
        objects.insert(projectId, "lamp_lust", 450f, 250f, 0f, "Люстра гостиная", 0f, 270)
        objects.insert(projectId, "lamp_bra", 550f, 150f, 0f, "Бра над диваном", 0f, 180)

        // Санузел
        objects.insert(projectId, "socket_b3", 520f, 50f, 0f, "Розетка санузел IP44", 0f, 110)
        objects.insert(projectId, "lamp_titan", 525f, 40f, 0f, "Светильник санузел", 0f, 270)

        // Коридор
        objects.insert(projectId, "switch_2", 250f, 40f, 0f, "Выключатель 2-кл коридор", 0f, 110)
        objects.insert(projectId, "switch_move", 250f, 60f, 0f, "Датчик движения", 0f, 220)
        objects.insert(projectId, "panel_shr", 50f, 40f, 0f, "Электрощит", 0f, 180)

        // Трассы
        tracks.insert(projectId, "power", listOf(
            TrackPoint(50f, 40f), TrackPoint(50f, 200f), TrackPoint(100f, 200f), TrackPoint(100f, 380f)
        ), "shtroba", -11747600, "3x2.5")

        tracks.insert(projectId, "power", listOf(
            TrackPoint(50f, 40f), TrackPoint(50f, 100f), TrackPoint(150f, 100f), 
            TrackPoint(150f, 250f), TrackPoint(450f, 250f)
        ), "shtroba", -11747600, "3x1.5")

        tracks.insert(projectId, "power", listOf(
            TrackPoint(50f, 40f), TrackPoint(50f, 60f), TrackPoint(520f, 60f), TrackPoint(520f, 50f)
        ), "shtroba", -11747600, "3x2.5")

        return projectId
    }
}
