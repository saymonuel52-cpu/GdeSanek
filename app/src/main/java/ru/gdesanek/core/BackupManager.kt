package ru.gdesanek.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {
    fun export(context: Context): File {
        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
            context.getDatabasePath("gdesanek.db").absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )
        
        val json = JSONObject()
        json.put("version", "1.1.0")
        json.put("timestamp", System.currentTimeMillis())
        
        // projects
        val projects = JSONArray()
        db.rawQuery("SELECT * FROM projects", null).use { c ->
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", c.getLong(0))
                obj.put("name", c.getString(1))
                obj.put("address", c.getString(2))
                obj.put("created_at", c.getLong(3))
                projects.put(obj)
            }
        }
        json.put("projects", projects)
        
        // walls
        val walls = JSONArray()
        db.rawQuery("SELECT * FROM walls", null).use { c ->
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", c.getLong(0))
                obj.put("project_id", c.getLong(1))
                obj.put("x1", c.getDouble(2))
                obj.put("y1", c.getDouble(3))
                obj.put("x2", c.getDouble(4))
                obj.put("y2", c.getDouble(5))
                obj.put("material", c.getString(6))
                obj.put("thickness", c.getDouble(7))
                walls.put(obj)
            }
        }
        json.put("walls", walls)
        
        // objects
        val objects = JSONArray()
        db.rawQuery("SELECT * FROM objects", null).use { c ->
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", c.getLong(0))
                obj.put("project_id", c.getLong(1))
                obj.put("type", c.getString(2))
                obj.put("x", c.getDouble(3))
                obj.put("y", c.getDouble(4))
                obj.put("rotation", c.getDouble(5))
                objects.put(obj)
            }
        }
        json.put("objects", objects)
        
        // tracks
        val tracks = JSONArray()
        db.rawQuery("SELECT * FROM tracks", null).use { c ->
            while (c.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", c.getLong(0))
                obj.put("project_id", c.getLong(1))
                obj.put("kind", c.getString(2))
                obj.put("points", c.getString(3))
                obj.put("wiring", c.getString(4))
                obj.put("color", c.getInt(5))
                obj.put("cable", c.getString(6))
                tracks.put(obj)
            }
        }
        json.put("tracks", tracks)
        
        db.close()
        
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val fileName = "GdeSanek_backup_$date.gsanek"
        val file = File(context.filesDir, fileName)
        file.writeText(json.toString(2), Charsets.UTF_8)
        return file
    }
    
    fun import(context: Context, file: File, overwrite: Boolean = true) {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
            context.getDatabasePath("gdesanek.db").absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )
        
        db.beginTransaction()
        try {
            if (overwrite) {
                db.execSQL("DELETE FROM tracks")
                db.execSQL("DELETE FROM objects")
                db.execSQL("DELETE FROM walls")
                db.execSQL("DELETE FROM projects")
            }
            
            // projects
            val projects = json.getJSONArray("projects")
            for (i in 0 until projects.length()) {
                val p = projects.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO projects (id, name, address, created_at) VALUES (?, ?, ?, ?)",
                    arrayOf(p.getLong("id"), p.getString("name"), p.getString("address"), p.getLong("created_at"))
                )
            }
            
            // walls
            val walls = json.getJSONArray("walls")
            for (i in 0 until walls.length()) {
                val w = walls.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO walls (id, project_id, x1, y1, x2, y2, material, thickness) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(w.getLong("id"), w.getLong("project_id"), w.getDouble("x1"), w.getDouble("y1"), w.getDouble("x2"), w.getDouble("y2"), w.getString("material"), w.getDouble("thickness"))
                )
            }
            
            // objects
            val objects = json.getJSONArray("objects")
            for (i in 0 until objects.length()) {
                val o = objects.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO objects (id, project_id, type, x, y, rotation) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getLong("project_id"), o.getString("type"), o.getDouble("x"), o.getDouble("y"), o.getDouble("rotation"))
                )
            }
            
            // tracks
            val tracks = json.getJSONArray("tracks")
            for (i in 0 until tracks.length()) {
                val t = tracks.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO tracks (id, project_id, kind, points, wiring, color, cable) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(t.getLong("id"), t.getLong("project_id"), t.getString("kind"), t.getString("points"), t.getString("wiring"), t.getInt("color"), t.getString("cable"))
                )
            }
            
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }
}
