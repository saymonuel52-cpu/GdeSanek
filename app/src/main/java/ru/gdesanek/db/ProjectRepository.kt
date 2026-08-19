package ru.gdesanek.db

import android.content.ContentValues
import android.content.Context
import ru.gdesanek.model.Project

class ProjectRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insert(name: String, address: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("address", address)
            put("created_at", System.currentTimeMillis())
        }
        return db.insert("projects", null, values)
    }

    fun getAll(): List<Project> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM projects ORDER BY created_at DESC", null)
        val projects = mutableListOf<Project>()
        if (cursor.moveToFirst()) {
            do {
                projects.add(Project(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    address = cursor.getString(2) ?: "",
                    createdAt = cursor.getLong(3)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return projects
    }
}
