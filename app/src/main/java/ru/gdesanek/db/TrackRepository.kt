package ru.gdesanek.db

import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.CableTrack
import ru.gdesanek.model.TrackPoint

class TrackRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    private fun ensureTrackColumns() {
        try {
            val c = dbHelper.readableDatabase.rawQuery("PRAGMA table_info(tracks)", null)
            val cols = mutableSetOf<String>()
            if (c.moveToFirst()) { do { cols.add(c.getString(1)) } while (c.moveToNext()) }
            c.close()
            val db = dbHelper.writableDatabase
            if (!cols.contains("wiring")) db.execSQL("ALTER TABLE tracks ADD COLUMN wiring TEXT DEFAULT 'shtroba'")
            if (!cols.contains("color")) db.execSQL("ALTER TABLE tracks ADD COLUMN color INTEGER DEFAULT -11747600")
            if (!cols.contains("cable")) db.execSQL("ALTER TABLE tracks ADD COLUMN cable TEXT DEFAULT '3x2.5'")
        } catch (e: Exception) { /* ignore */ }
    }

    fun insert(projectId: Long, kind: String, points: List<TrackPoint>): Long = insert(projectId, kind, points, "shtroba", -11747600, "3x2.5")
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>, wiring: String): Long = insert(projectId, kind, points, wiring, -11747600, "3x2.5")
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>, wiring: String, color: Int): Long = insert(projectId, kind, points, wiring, color, "3x2.5")
    fun insert(projectId: Long, kind: String, points: List<TrackPoint>, wiring: String, color: Int, cable: String): Long {
        ensureTrackColumns()
        val v = ContentValues().apply { put("project_id", projectId); put("kind", kind); put("points", points.joinToString(";") { "${it.x},${it.y}" }); put("wiring", wiring); put("color", color); put("cable", cable) }
        return dbHelper.writableDatabase.insert("tracks", null, v)
    }

    fun update(track: CableTrack) {
        ensureTrackColumns()
        val v = ContentValues().apply { put("points", track.points.joinToString(";") { "${it.x},${it.y}" }); put("wiring", track.wiring); put("color", track.color); put("cable", track.cable) }
        dbHelper.writableDatabase.update("tracks", v, "id = ?", arrayOf(track.id.toString()))
    }

    fun delete(id: Long) { dbHelper.writableDatabase.delete("tracks", "id = ?", arrayOf(id.toString())) }

    fun getAll(projectId: Long): List<CableTrack> {
        ensureTrackColumns()
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM tracks WHERE project_id = ? ORDER BY id", arrayOf(projectId.toString()))
        val list = mutableListOf<CableTrack>()
        if (cursor.moveToFirst()) {
            do {
                val pts = (cursor.getString(3) ?: "").split(";").filter { it.contains(",") }.map { val p = it.split(","); TrackPoint(p[0].toFloat(), p[1].toFloat()) }
                val wiring = if (cursor.columnCount > 4) (cursor.getString(4) ?: "shtroba") else "shtroba"
                val color = if (cursor.columnCount > 5) cursor.getInt(5) else -11747600
                val cable = if (cursor.columnCount > 6) (cursor.getString(6) ?: "3x2.5") else "3x2.5"
                list.add(CableTrack(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), pts, wiring, color, cable))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
