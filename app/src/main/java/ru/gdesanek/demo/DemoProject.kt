package ru.gdesanek.demo

import android.content.Context
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.db.WallRepository
import ru.gdesanek.model.TrackPoint

object DemoProject {
    fun load(context: Context): Long {
        val projectId = ProjectRepository(context).insert("Демо: 2-к квартира 48 м²", "Пример готового проекта")
        val walls = WallRepository(context)
        val objects = ObjectRepository(context)
        val tracks = TrackRepository(context)

        walls.insert(projectId, 0f, 0f, 620f, 0f, "beton", 200f)
        walls.insert(projectId, 620f, 0f, 620f, 420f, "beton", 200f)
        walls.insert(projectId, 620f, 420f, 0f, 420f, "beton", 200f)
        walls.insert(projectId, 0f, 420f, 0f, 0f, "beton", 200f)
        walls.insert(projectId, 480f, 0f, 480f, 25f, "kirpich", 100f)
        walls.insert(projectId, 480f, 65f, 480f, 90f, "kirpich", 100f)
        walls.insert(projectId, 480f, 90f, 620f, 90f, "kirpich", 100f)
        walls.insert(projectId, 0f, 90f, 120f, 90f, "kirpich", 100f)
        walls.insert(projectId, 200f, 90f, 300f, 90f, "kirpich", 100f)
        walls.insert(projectId, 300f, 90f, 300f, 200f, "kirpich", 100f)
        walls.insert(projectId, 300f, 280f, 300f, 420f, "kirpich", 100f)

        objects.insert(projectId, "panel_shr", 40f, 45f, 0f, "Электрощит", 0f, 180)
        objects.insert(projectId, "switch_2", 90f, 75f, 0f, "Выключатель 2-кл коридор", 0f, 110)
        objects.insert(projectId, "switch_move", 160f, 75f, 0f, "Датчик движения", 0f, 220)
        objects.insert(projectId, "socket_block2", 6f, 200f, 90f, "Блок розеток рабочая зона", 0f, 110)
        objects.insert(projectId, "socket_380", 6f, 330f, 90f, "Розетка плиты 380В", 0f, 30)
        objects.insert(projectId, "cons_hood", 150f, 100f, 0f, "Вытяжка", 0f, 220)
        objects.insert(projectId, "lamp_grig", 150f, 250f, 0f, "Точечный свет кухня", 0f, 270)
        objects.insert(projectId, "switch_1", 215f, 110f, 0f, "Выключатель кухня", 0f, 110)
        objects.insert(projectId, "sks_tv", 460f, 414f, 180f, "ТВ-розетка", 0f, 30)
        objects.insert(projectId, "socket_b2", 614f, 220f, 270f, "Розетка диван", 0f, 30)
        objects.insert(projectId, "lamp_lust", 460f, 250f, 0f, "Люстра гостиная", 0f, 270)
        objects.insert(projectId, "lamp_bra", 614f, 150f, 270f, "Бра над диваном", 0f, 180)
        objects.insert(projectId, "switch_1", 320f, 230f, 0f, "Выключатель гостиная", 0f, 110)
        objects.insert(projectId, "socket_b3", 614f, 45f, 270f, "Розетка санузел IP44", 0f, 110)
        objects.insert(projectId, "lamp_titan", 550f, 45f, 0f, "Светильник санузел", 0f, 270)

        tracks.insert(projectId, "power", listOf(
            TrackPoint(40f, 45f), TrackPoint(40f, 200f), TrackPoint(6f, 200f), TrackPoint(6f, 330f)
        ), "shtroba", -11747600, "3x2.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(40f, 45f), TrackPoint(150f, 45f), TrackPoint(150f, 250f), TrackPoint(460f, 250f), TrackPoint(614f, 250f), TrackPoint(614f, 150f)
        ), "shtroba", -11747600, "3x1.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(40f, 45f), TrackPoint(614f, 45f)
        ), "shtroba", -11747600, "3x2.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(40f, 45f), TrackPoint(40f, 410f), TrackPoint(614f, 410f), TrackPoint(614f, 220f)
        ), "shtroba", -11747600, "3x2.5")

        return projectId
    }
}
