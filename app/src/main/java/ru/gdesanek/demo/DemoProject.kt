package ru.gdesanek.demo

import android.content.Context
import ru.gdesanek.db.ObjectRepository
import ru.gdesanek.db.ProjectRepository
import ru.gdesanek.db.TrackRepository
import ru.gdesanek.db.WallRepository
import ru.gdesanek.model.TrackPoint

object DemoProject {
    fun load(context: Context): Long {
        val projectId = ProjectRepository(context).insert("Демо: 2-к квартира 48 м²", "Коридор сверху, кухня слева, санузел справа-сверху, гостиная справа")
        val walls = WallRepository(context)
        val objects = ObjectRepository(context)
        val tracks = TrackRepository(context)

        // Стены: контур 620x420; коридор y=0..90; кухня x=0..300 y=90..420;
        // санузел x=480..620 y=0..90; гостиная x=300..620 y=90..420
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

        // КОРИДОР (у входа)
        objects.insert(projectId, "panel_shr", 360f, 45f, 0f, "ЩР-168 у входа", 0f, 180)
        objects.insert(projectId, "switch_2", 280f, 45f, 0f, "Выкл 2-кл у входа", 0f, 110)
        objects.insert(projectId, "switch_move", 100f, 45f, 0f, "Датчик движения", 0f, 220)
        objects.insert(projectId, "lamp_grig", 200f, 45f, 0f, "Свет коридора", 0f, 270)
        objects.insert(projectId, "arch_door900", 310f, 0f, 0f, "Дверь входная", 0f, -1)

        // САНУЗЕЛ x=480..620 y=0..90
        objects.insert(projectId, "switch_1", 460f, 45f, 0f, "Выкл санузел у двери", 0f, 110)
        objects.insert(projectId, "furn_wc", 520f, 45f, 90f, "Унитаз", 0f, -1)
        objects.insert(projectId, "lamp_titan", 550f, 45f, 0f, "Свет санузел", 0f, 270)
        objects.insert(projectId, "furn_wash", 590f, 45f, 90f, "Стиралка", 0f, -1)
        objects.insert(projectId, "socket_b3", 610f, 45f, 270f, "Розетка IP44 стиралка", 0f, 110)

        // КУХНЯ x=0..300 y=90..420
        objects.insert(projectId, "switch_1", 210f, 105f, 0f, "Выкл кухня у двери", 0f, 110)
        objects.insert(projectId, "furn_kitchen", 40f, 250f, 90f, "Кухня-линия 240 вдоль стены", 0f, -1)
        objects.insert(projectId, "cons_hood", 40f, 180f, 90f, "Вытяжка над плитой", 0f, 220)
        objects.insert(projectId, "socket_block2", 15f, 200f, 90f, "Блок розеток столешница", 0f, 110)
        objects.insert(projectId, "socket_block2", 15f, 300f, 90f, "Блок розеток столешница 2", 0f, 110)
        objects.insert(projectId, "socket_380", 15f, 330f, 90f, "Розетка плиты 380В", 0f, 30)
        objects.insert(projectId, "furn_table", 180f, 300f, 0f, "Обеденный стол", 0f, -1)
        objects.insert(projectId, "lamp_lust", 150f, 250f, 0f, "Люстра кухни", 0f, 270)
        objects.insert(projectId, "socket_b1", 295f, 330f, 90f, "Розетка обеденной зоны", 0f, 30)

        // ГОСТИНАЯ x=300..620 y=90..420
        objects.insert(projectId, "switch_1", 315f, 230f, 0f, "Выкл гостиная у двери", 0f, 110)
        objects.insert(projectId, "furn_sofa", 350f, 250f, 90f, "Диван спиной к перегородке", 0f, -1)
        objects.insert(projectId, "lamp_bra", 315f, 180f, 90f, "Бра над диваном", 0f, 180)
        objects.insert(projectId, "furn_table", 430f, 250f, 0f, "Журнальный стол", 0f, -1)
        objects.insert(projectId, "sks_tv", 610f, 240f, 270f, "ТВ-розетка напротив дивана", 0f, 30)
        objects.insert(projectId, "socket_b2", 610f, 265f, 270f, "Розетка ТВ-зоны", 0f, 30)
        objects.insert(projectId, "furn_ward", 500f, 380f, 0f, "Шкаф у нижней стены", 0f, -1)
        objects.insert(projectId, "lamp_lust", 480f, 300f, 0f, "Люстра гостиной", 0f, 270)
        objects.insert(projectId, "arch_win1400", 460f, 420f, 180f, "Окно гостиной", 0f, -1)

        // ТРАССЫ: только по стенам и проёмам
        tracks.insert(projectId, "power", listOf(
            TrackPoint(360f, 45f), TrackPoint(20f, 45f), TrackPoint(20f, 200f), TrackPoint(20f, 300f), TrackPoint(20f, 345f), TrackPoint(20f, 410f), TrackPoint(295f, 410f), TrackPoint(295f, 330f)
        ), "shtroba", -11747600, "3x2.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(360f, 45f), TrackPoint(160f, 45f), TrackPoint(160f, 90f), TrackPoint(300f, 90f), TrackPoint(300f, 240f), TrackPoint(610f, 240f), TrackPoint(610f, 265f)
        ), "shtroba", -11747600, "3x2.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(360f, 45f), TrackPoint(280f, 45f), TrackPoint(200f, 45f), TrackPoint(100f, 45f), TrackPoint(160f, 90f), TrackPoint(210f, 105f), TrackPoint(150f, 250f), TrackPoint(300f, 240f), TrackPoint(315f, 230f), TrackPoint(315f, 180f), TrackPoint(480f, 300f)
        ), "shtroba", -11747600, "3x1.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(360f, 45f), TrackPoint(460f, 45f), TrackPoint(520f, 45f), TrackPoint(550f, 45f), TrackPoint(590f, 45f), TrackPoint(610f, 45f)
        ), "shtroba", -11747600, "3x2.5")

        return projectId
    }
}
