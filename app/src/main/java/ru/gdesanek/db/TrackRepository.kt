package ru.gdesanek.db
import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.TrackPoint
class TrackRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("project_id", projectId); put("kind", kind)
            put("points", points.joinToString(";") { "${it.x},${it.y}" })
        }
        return db.insert("tracks", null, values)
    }
    fun delete(id: Long) { dbHelper.writableDatabase.delete("tracks", "id = ?", arrayOf(id.toString())) }
    fun getAll(projectId: Long): List<CableTrack> {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM tracks WHERE project_id = ? ORDER BY id", arrayOf(projectId.toString()))
        val list = mutableListOf<CableTrack>()
        if (cursor.moveToFirst()) {
            do {
                val pts = (cursor.getString(3) ?: "").split(";").filter { it.contains(",") }.map { val p = it.split(","); TrackPoint(p[0].toFloat(), p[1].toFloat()) }
                list.add(CableTrack(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), pts))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }
}
