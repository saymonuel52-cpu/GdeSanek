package ru.gdesanek.db

import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.PlanObject

class ObjectRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insert(projectId: Long, type: String, x: Float, y: Float, rotation: Float): Long =
        insert(projectId, type, x, y, rotation, "", 0f)

    fun insert(projectId: Long, type: String, x: Float, y: Float, rotation: Float, name: String, area: Float): Long =
        insert(projectId, type, x, y, rotation, name, area, -1)

    fun insert(projectId: Long, type: String, x: Float, y: Float, rotation: Float, name: String, area: Float, height: Int): Long {
        val v = ContentValues().apply {
            put("project_id", projectId); put("type", type); put("x", x); put("y", y)
            put("rotation", rotation); put("name", name); put("area", area); put("height", height)
        }
        return dbHelper.writableDatabase.insert("objects", null, v)
    }

    fun update(obj: PlanObject) {
        val v = ContentValues().apply {
            put("rotation", obj.rotation); put("x", obj.x); put("y", obj.y)
            put("name", obj.name); put("area", obj.area); put("height", obj.height)
        }
        dbHelper.writableDatabase.update("objects", v, "id = ?", arrayOf(obj.id.toString()))
    }

    fun delete(id: Long) { dbHelper.writableDatabase.delete("objects", "id = ?", arrayOf(id.toString())) }

    fun getAll(projectId: Long): List<PlanObject> {
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM objects WHERE project_id = ? ORDER BY id", arrayOf(projectId.toString()))
        val list = mutableListOf<PlanObject>()
        if (cursor.moveToFirst()) {
            do {
                list.add(PlanObject(
                    id = cursor.getLong(0),
                    projectId = cursor.getLong(1),
                    type = cursor.getString(2),
                    x = cursor.getFloat(3),
                    y = cursor.getFloat(4),
                    rotation = cursor.getFloat(5),
                    name = cursor.getString(6) ?: "",
                    area = cursor.getFloat(7),
                    height = cursor.getInt(8)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
