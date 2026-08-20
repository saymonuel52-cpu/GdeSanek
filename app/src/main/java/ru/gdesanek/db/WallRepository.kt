package ru.gdesanek.db

import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.Wall

class WallRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insert(projectId: Long, x1: Float, y1: Float, x2: Float, y2: Float): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("project_id", projectId)
            put("x1", x1); put("y1", y1); put("x2", x2); put("y2", y2)
        }
        return db.insert("walls", null, values)
    }

    fun delete(wallId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("walls", "id = ?", arrayOf(wallId.toString()))
    }

    fun getWalls(projectId: Long): List<Wall> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM walls WHERE project_id = ? ORDER BY id", arrayOf(projectId.toString()))
        val list = mutableListOf<Wall>()
        if (cursor.moveToFirst()) {
            do {
                list.add(Wall(
                    id = cursor.getLong(0), projectId = cursor.getLong(1),
                    x1 = cursor.getFloat(2), y1 = cursor.getFloat(3),
                    x2 = cursor.getFloat(4), y2 = cursor.getFloat(5)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
