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

        // === СТЕНЫ (внешний контур 620×420 + перегородки) ===
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

        // === ЩИТ ===
        objects.insert(projectId, "panel_shr", 600f, 50f, 0f, "ЩР-168 (у входа)", 0f, 180)

        // === КОМНАТА (x=0..300, y=0..420) ===
        objects.insert(projectId, "furn_bed2", 90f, 110f, 0f, "Кровать 160×200 (голова к стене)", 0f, -1)
        objects.insert(projectId, "lamp_bra", 50f, 100f, 270f, "Бра над кроватью", 0f, 180)
        objects.insert(projectId, "socket_b1", 10f, 110f, 90f, "Розетка прикроватная", 0f, 30)
        objects.insert(projectId, "furn_ward", 250f, 210f, 90f, "Шкаф (у правой стены)", 0f, -1)
        objects.insert(projectId, "furn_sofa", 150f, 360f, 0f, "Диван (у окна)", 0f, -1)
        objects.insert(projectId, "furn_table", 150f, 260f, 0f, "Журнальный стол", 0f, -1)
        objects.insert(projectId, "sks_tv", 10f, 360f, 90f, "ТВ-розетка напротив дивана", 0f, 30)
        objects.insert(projectId, "socket_b2", 10f, 220f, 90f, "Розетка общая", 0f, 30)
        objects.insert(projectId, "lamp_lust", 150f, 210f, 0f, "Люстра комнаты", 0f, 270)
        objects.insert(projectId, "switch_1", 290f, 235f, 180f, "Выкл. комнаты (у двери)", 0f, 110)
        objects.insert(projectId, "arch_win1400", 150f, 420f, 180f, "Окно комнаты", 0f, -1)

        // === САНУЗЕЛ (x=120..300, y=90..420) ===
        objects.insert(projectId, "furn_wc", 145f, 180f, 90f, "Унитаз", 0f, -1)
        objects.insert(projectId, "furn_sink", 145f, 280f, 90f, "Раковина", 0f, -1)
        objects.insert(projectId, "furn_bath", 210f, 390f, 0f, "Ванна 170×75", 0f, -1)
        objects.insert(projectId, "furn_wash", 285f, 130f, 90f, "Стиралка", 0f, -1)
        objects.insert(projectId, "socket_b3", 290f, 220f, 90f, "Розетка санузел IP44", 0f, 110)
        objects.insert(projectId, "lamp_titan", 210f, 250f, 0f, "Светильник санузел", 0f, 270)
        objects.insert(projectId, "switch_1", 160f, 105f, 0f, "Выкл. санузел (у двери)", 0f, 110)

        // === КОРИДОР/КУХНЯ (x=300..620, y=0..420) ===
        objects.insert(projectId, "arch_door900", 310f, 0f, 0f, "Дверь входная", 0f, -1)
        objects.insert(projectId, "switch_move", 460f, 60f, 0f, "Датчик движения (коридор)", 0f, 220)
        objects.insert(projectId, "lamp_grig", 460f, 60f, 0f, "Свет коридор (точечный)", 0f, 270)
        objects.insert(projectId, "furn_kitchen", 580f, 200f, 90f, "Кухня-линия 240", 0f, -1)
        objects.insert(projectId, "cons_hood", 580f, 130f, 90f, "Вытяжка над плитой", 0f, 220)
        objects.insert(projectId, "socket_block2", 614f, 150f, 270f, "Розетки рабочая зона", 0f, 110)
        objects.insert(projectId, "socket_380", 614f, 250f, 270f, "Розетка плиты 380В", 0f, 30)
        objects.insert(projectId, "lamp_lust", 460f, 300f, 0f, "Светильник кухня (над столом)", 0f, 270)
        objects.insert(projectId, "furn_table", 460f, 300f, 0f, "Обеденный стол", 0f, -1)
        objects.insert(projectId, "switch_2", 490f, 50f, 90f, "Выкл. 2-кл кухня (у двери)", 0f, 110)
        objects.insert(projectId, "socket_b2", 614f, 380f, 270f, "Розетка ТВ-зоны", 0f, 30)
        objects.insert(projectId, "sks_tv", 614f, 395f, 270f, "ТВ-розетка", 0f, 30)
        objects.insert(projectId, "switch_1", 310f, 380f, 180f, "Выкл. гостиная (у двери)", 0f, 110)

        // === ТРАССЫ (4 группы, по стенам) ===
        tracks.insert(projectId, "power", listOf(
            TrackPoint(600f, 50f), TrackPoint(614f, 50f), TrackPoint(614f, 150f), TrackPoint(614f, 250f)
        ), "shtroba", -11747600, "3x2.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(600f, 50f), TrackPoint(600f, 10f), TrackPoint(10f, 10f), TrackPoint(10f, 110f), TrackPoint(10f, 220f), TrackPoint(10f, 360f)
        ), "shtroba", -11747600, "3x2.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(600f, 50f), TrackPoint(490f, 50f), TrackPoint(460f, 60f), TrackPoint(460f, 200f), TrackPoint(460f, 300f), TrackPoint(150f, 300f), TrackPoint(150f, 210f), TrackPoint(50f, 210f), TrackPoint(50f, 100f)
        ), "shtroba", -11747600, "3x1.5")
        tracks.insert(projectId, "power", listOf(
            TrackPoint(600f, 50f), TrackPoint(460f, 50f), TrackPoint(460f, 90f), TrackPoint(160f, 90f), TrackPoint(160f, 105f), TrackPoint(210f, 105f), TrackPoint(290f, 105f), TrackPoint(290f, 130f), TrackPoint(290f, 220f), TrackPoint(290f, 250f)
        ), "shtroba", -11747600, "3x2.5")

        return projectId
    }
}
