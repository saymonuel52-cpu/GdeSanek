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

    fun deleteProject(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete("walls", "project_id = ?", arrayOf(id.toString()))
        db.delete("objects", "project_id = ?", arrayOf(id.toString()))
        db.delete("tracks", "project_id = ?", arrayOf(id.toString()))
        db.delete("projects", "id = ?", arrayOf(id.toString()))
    }

    fun updateName(id: Long, newName: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("name", newName) }
        db.update("projects", values, "id = ?", arrayOf(id.toString()))
    }

    fun duplicateProject(id: Long): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Скопировать проект
            val projCursor = db.rawQuery("SELECT name, address FROM projects WHERE id = ?", arrayOf(id.toString()))
            if (!projCursor.moveToFirst()) { db.endTransaction(); return -1 }
            val oldName = projCursor.getString(0)
            val address = projCursor.getString(1)
            projCursor.close()

            val newId = insert("$oldName (копия)", address)

            // Скопировать стены
            val wallsCursor = db.rawQuery("SELECT x1, y1, x2, y2, thickness, material FROM walls WHERE project_id = ?", arrayOf(id.toString()))
            while (wallsCursor.moveToNext()) {
                val v = ContentValues().apply {
                    put("project_id", newId)
                    put("x1", wallsCursor.getFloat(0))
                    put("y1", wallsCursor.getFloat(1))
                    put("x2", wallsCursor.getFloat(2))
                    put("y2", wallsCursor.getFloat(3))
                    put("thickness", wallsCursor.getFloat(4))
                    put("material", wallsCursor.getString(5))
                }
                db.insert("walls", null, v)
            }
            wallsCursor.close()

            // Скопировать объекты
            val objCursor = db.rawQuery("SELECT type, x, y, rotation FROM objects WHERE project_id = ?", arrayOf(id.toString()))
            while (objCursor.moveToNext()) {
                val v = ContentValues().apply {
                    put("project_id", newId)
                    put("type", objCursor.getString(0))
                    put("x", objCursor.getFloat(1))
                    put("y", objCursor.getFloat(2))
                    put("rotation", objCursor.getFloat(3))
                }
                db.insert("objects", null, v)
            }
            objCursor.close()

            // Скопировать трассы
            val trackCursor = db.rawQuery("SELECT wiring, cable, color, points FROM tracks WHERE project_id = ?", arrayOf(id.toString()))
            while (trackCursor.moveToNext()) {
                val v = ContentValues().apply {
                    put("project_id", newId)
                    put("wiring", trackCursor.getString(0))
                    put("cable", trackCursor.getString(1))
                    put("color", trackCursor.getInt(2))
                    put("points", trackCursor.getString(3))
                }
                db.insert("tracks", null, v)
            }
            trackCursor.close()

            db.setTransactionSuccessful()
            return newId
        } finally {
            db.endTransaction()
        }
    }

    fun getCounts(id: Long): Triple<Int, Int, Int> {
        val db = dbHelper.readableDatabase
        val w = db.rawQuery("SELECT COUNT(*) FROM walls WHERE project_id = ?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val o = db.rawQuery("SELECT COUNT(*) FROM objects WHERE project_id = ?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val t = db.rawQuery("SELECT COUNT(*) FROM tracks WHERE project_id = ?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        return Triple(w, o, t)
    }

    fun ensurePassportColumns() {
        try {
            val c = dbHelper.readableDatabase.rawQuery("PRAGMA table_info(projects)", null)
            val cols = mutableSetOf<String>()
            if (c.moveToFirst()) { do { cols.add(c.getString(1)) } while (c.moveToNext()) }
            c.close()
            val db = dbHelper.writableDatabase
            if (!cols.contains("doc_num")) db.execSQL("ALTER TABLE projects ADD COLUMN doc_num TEXT DEFAULT ''")
            if (!cols.contains("org")) db.execSQL("ALTER TABLE projects ADD COLUMN org TEXT DEFAULT ''")
            if (!cols.contains("author")) db.execSQL("ALTER TABLE projects ADD COLUMN author TEXT DEFAULT ''")
        } catch (e: Exception) { /* ignore */ }
    }

    fun getPassport(id: Long): List<String> {
        ensurePassportColumns()
        return try {
            dbHelper.readableDatabase.rawQuery("SELECT doc_num, org, author, address FROM projects WHERE id = ?", arrayOf(id.toString())).use {
                if (it.moveToFirst()) listOf(it.getString(0) ?: "", it.getString(1) ?: "", it.getString(2) ?: "", it.getString(3) ?: "") else listOf("", "", "", "")
            }
        } catch (e: Exception) { listOf("", "", "", "") }
    }

    fun updatePassport(id: Long, doc: String, org: String, author: String, address: String) {
        ensurePassportColumns()
        val v = ContentValues().apply { put("doc_num", doc); put("org", org); put("author", author); put("address", address) }
        dbHelper.writableDatabase.update("projects", v, "id = ?", arrayOf(id.toString()))
    }
}
