package ru.gdesanek.db
import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.PlanObject
class ObjectRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    fun insert(projectId: Long, type: String, x: Float, y: Float, rotation: Float): Long {
        val v = ContentValues().apply { put("project_id", projectId); put("type", type); put("x", x); put("y", y); put("rotation", rotation) }
        return dbHelper.writableDatabase.insert("objects", null, v)
    }
    fun update(obj: PlanObject) {
        val v = ContentValues().apply { put("rotation", obj.rotation); put("x", obj.x); put("y", obj.y) }
        dbHelper.writableDatabase.update("objects", v, "id = ?", arrayOf(obj.id.toString()))
    }
    fun delete(id: Long) { dbHelper.writableDatabase.delete("objects", "id = ?", arrayOf(id.toString())) }
    fun getAll(projectId: Long): List<PlanObject> {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM objects WHERE project_id = ? ORDER BY id", arrayOf(projectId.toString()))
        val list = mutableListOf<PlanObject>()
        if (cursor.moveToFirst()) { do { list.add(PlanObject(cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getFloat(3), cursor.getFloat(4), cursor.getFloat(5))) } while (cursor.moveToNext()) }
        cursor.close(); return list
    }
}
