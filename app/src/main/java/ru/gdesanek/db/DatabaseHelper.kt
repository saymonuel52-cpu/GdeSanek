package ru.gdesanek.db
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE projects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, address TEXT, created_at INTEGER)")
        db.execSQL("CREATE TABLE walls (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, x1 REAL, y1 REAL, x2 REAL, y2 REAL, material TEXT DEFAULT 'beton', thickness REAL DEFAULT 100)")
        db.execSQL("CREATE TABLE objects (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, type TEXT, x REAL, y REAL, rotation REAL)")
        db.execSQL("CREATE TABLE tracks (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, kind TEXT, points TEXT, wiring TEXT DEFAULT 'shtroba', color INTEGER DEFAULT -11747600)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS walls (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, x1 REAL, y1 REAL, x2 REAL, y2 REAL, material TEXT DEFAULT 'beton', thickness REAL DEFAULT 100)")
        if (oldVersion < 3) db.execSQL("CREATE TABLE IF NOT EXISTS objects (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, type TEXT, x REAL, y REAL, rotation REAL)")
        if (oldVersion < 4) db.execSQL("CREATE TABLE IF NOT EXISTS tracks (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, kind TEXT, points TEXT, wiring TEXT DEFAULT 'shtroba', color INTEGER DEFAULT -11747600)")
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE walls ADD COLUMN material TEXT DEFAULT 'beton'") } catch (e: Exception) {}
            try { db.execSQL("ALTER TABLE walls ADD COLUMN thickness REAL DEFAULT 100") } catch (e: Exception) {}
            try { db.execSQL("ALTER TABLE tracks ADD COLUMN wiring TEXT DEFAULT 'shtroba'") } catch (e: Exception) {}
        }
        if (oldVersion < 6) { try { db.execSQL("ALTER TABLE tracks ADD COLUMN color INTEGER DEFAULT -11747600") } catch (e: Exception) {} }
    }
    companion object { const val DATABASE_NAME = "gdesanek.db"; const val DATABASE_VERSION = 6 }
}
