package ru.gdesanek.db
import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.TrackPoint
class TrackRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>): Long = insert(projectId, kind, points, "shtroba", -11747600, "3x2.5")
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>, wiring: String): Long = insert(projectId, kind, points, wiring, -11747600, "3x2.5")
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>, wiring: String, color: Int): Long = insert(projectId, kind, points, wiring, color, "3x2.5")
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>, wiring: String, color: Int, cable: String): Long {
        val values = ContentValues().apply { put("project_id", projectId); put("kind", kind); put("points", points.joinToString(";") { "${it.x},${it.y}" }); put("wiring", wiring); put("color", color); put("cable", cable) }
        return dbHelper.writableDatabase.insert("tracks", null, values)
    }
    fun delete(id: Long) { dbHelper.writableDatabase.delete("tracks", "id = ?", arrayOf(id.toString())) }
    fun getAll(projectId: Long): List<CableTrack> {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM tracks WHERE project_id = ? ORDER BY id", arrayOf(projectId.toString()))
        val list = mutableListOf<CableTrack>()
        if (cursor.moveToFirst()) {
            do {
                val pts = (cursor.getString(3) ?: "").split(";").filter { it.contains(",") }.map { val p = it.split(","); TrackPoint(p[0].toFloat(), p[1].toFloat()) }
                list.add(CableTrack(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), pts, cursor.getString(4) ?: "shtroba", cursor.getInt(5), cursor.getString(6) ?: "3x2.5"))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }
}
