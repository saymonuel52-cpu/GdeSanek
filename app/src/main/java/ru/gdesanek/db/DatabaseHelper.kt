package ru.gdesanek.db
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE projects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, address TEXT, created_at INTEGER)")
        db.execSQL("CREATE TABLE walls (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, x1 REAL, y1 REAL, x2 REAL, y2 REAL)")
        db.execSQL("CREATE TABLE objects (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, type TEXT, x REAL, y REAL, rotation REAL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS walls (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, x1 REAL, y1 REAL, x2 REAL, y2 REAL)")
        if (oldVersion < 3) db.execSQL("CREATE TABLE IF NOT EXISTS objects (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, type TEXT, x REAL, y REAL, rotation REAL)")
    }
    companion object { const val DATABASE_NAME = "gdesanek.db"; const val DATABASE_VERSION = 3 }
}
