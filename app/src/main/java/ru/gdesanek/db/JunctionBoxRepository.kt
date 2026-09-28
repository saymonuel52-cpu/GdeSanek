package ru.gdesanek.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import ru.gdesanek.model.JunctionBox

object JunctionBoxRepository {

    fun ensureTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS junction_boxes (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "project_id INTEGER NOT NULL, " +
            "x REAL NOT NULL, " +
            "y REAL NOT NULL, " +
            "height REAL NOT NULL DEFAULT 250, " +
            "name TEXT NOT NULL DEFAULT '', " +
            "manual INTEGER NOT NULL DEFAULT 0)")
    }

    fun insert(db: SQLiteDatabase, b: JunctionBox): Long {
        ensureTable(db)
        val cv = ContentValues().apply {
            put("project_id", b.projectId); put("x", b.x); put("y", b.y)
            put("height", b.height); put("name", b.name); put("manual", if (b.manual) 1 else 0)
        }
        return db.insert("junction_boxes", null, cv)
    }

    fun update(db: SQLiteDatabase, b: JunctionBox) {
        ensureTable(db)
        val cv = ContentValues().apply {
            put("x", b.x); put("y", b.y); put("height", b.height)
            put("name", b.name); put("manual", if (b.manual) 1 else 0)
        }
        db.update("junction_boxes", cv, "id = ?", arrayOf(b.id.toString()))
    }

    fun delete(db: SQLiteDatabase, id: Long) {
        ensureTable(db)
        db.delete("junction_boxes", "id = ?", arrayOf(id.toString()))
    }

    fun deleteProject(db: SQLiteDatabase, projectId: Long) {
        ensureTable(db)
        db.delete("junction_boxes", "project_id = ?", arrayOf(projectId.toString()))
    }

    fun getAll(db: SQLiteDatabase, projectId: Long): List<JunctionBox> {
        ensureTable(db)
        val out = mutableListOf<JunctionBox>()
        val c = db.query("junction_boxes", null, "project_id = ?", arrayOf(projectId.toString()), null, null, "id")
        while (c.moveToNext()) {
            out.add(JunctionBox(
                id = c.getLong(c.getColumnIndexOrThrow("id")),
                projectId = projectId,
                x = c.getFloat(c.getColumnIndexOrThrow("x")),
                y = c.getFloat(c.getColumnIndexOrThrow("y")),
                height = c.getFloat(c.getColumnIndexOrThrow("height")),
                name = c.getString(c.getColumnIndexOrThrow("name")) ?: "",
                manual = c.getInt(c.getColumnIndexOrThrow("manual")) == 1
            ))
        }
        c.close()
        return out
    }

    fun count(db: SQLiteDatabase, projectId: Long): Int {
        ensureTable(db)
        val c = db.rawQuery("SELECT COUNT(*) FROM junction_boxes WHERE project_id = ?", arrayOf(projectId.toString()))
        val n = if (c.moveToFirst()) c.getInt(0) else 0
        c.close()
        return n
    }

    fun copyProject(db: SQLiteDatabase, fromId: Long, toId: Long) {
        ensureTable(db)
        for (b in getAll(db, fromId)) insert(db, b.copy(id = 0, projectId = toId))
    }
}
