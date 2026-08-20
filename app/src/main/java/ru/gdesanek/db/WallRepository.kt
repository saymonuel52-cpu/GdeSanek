package ru.gdesanek.db
import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.Wall
class WallRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    fun insert(projectId: Long, x1: Float, y1: Float, x2: Float, y2: Float): Long = insert(projectId, x1, y1, x2, y2, "beton", 100f)
    fun insert(projectId: Long, x1: Float, y1: Float, x2: Float, y2: Float, material: String, thickness: Float): Long {
        val values = ContentValues().apply { put("project_id", projectId); put("x1", x1); put("y1", y1); put("x2", x2); put("y2", y2); put("material", material); put("thickness", thickness) }
        return dbHelper.writableDatabase.insert("walls", null, values)
    }
    fun update(wall: Wall) {
        val values = ContentValues().apply { put("material", wall.material); put("thickness", wall.thickness) }
        dbHelper.writableDatabase.update("walls", values, "id = ?", arrayOf(wall.id.toString()))
    }
    fun delete(id: Long) { dbHelper.writableDatabase.delete("walls", "id = ?", arrayOf(id.toString())) }
    fun getAll(projectId: Long): List<Wall> {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM walls WHERE project_id = ? ORDER BY id", arrayOf(projectId.toString()))
        val list = mutableListOf<Wall>()
        if (cursor.moveToFirst()) { do { list.add(Wall(cursor.getLong(0), cursor.getLong(1), cursor.getFloat(2), cursor.getFloat(3), cursor.getFloat(4), cursor.getFloat(5), cursor.getString(6) ?: "beton", cursor.getFloat(7))) } while (cursor.moveToNext()) }
        cursor.close(); return list
    }
}
