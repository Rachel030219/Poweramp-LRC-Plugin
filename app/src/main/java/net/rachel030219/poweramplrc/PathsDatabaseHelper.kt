package net.rachel030219.poweramplrc

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PathsDatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_PATHS($PATH_ORIG_KEY TEXT,$PATH_FILE_KEY TEXT,$PATH_EMBEDDED_KEY INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PATHS")
            onCreate(db)
        }
    }

    fun addPath (path: Path) {
        writableDatabase.insert(TABLE_PATHS, null, ContentValues().apply {
            put(PATH_ORIG_KEY, path.origPath)
            put(PATH_FILE_KEY, path.filePath)
            put(PATH_EMBEDDED_KEY, if (path.embedded) 1 else 0)
        })
    }

    fun removePath (filePath: String) {
        writableDatabase.delete(TABLE_PATHS, "$PATH_FILE_KEY = ?", arrayOf(filePath))
    }

    fun queryPath (origPath: String, embedded: Boolean): MutableList<String> {
        return mutableListOf<String>().apply {
            readableDatabase.query(TABLE_PATHS, arrayOf(PATH_ORIG_KEY, PATH_FILE_KEY, PATH_EMBEDDED_KEY), "$PATH_ORIG_KEY = ? AND $PATH_EMBEDDED_KEY = ?",
                arrayOf(origPath, if (embedded) "1" else "0"), null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    add(cursor.getString(PATH_FILE))
                }
            }
        }
    }

    fun isInstrumental (origPath: String): Boolean {
        return readableDatabase.query(TABLE_PATHS, arrayOf(PATH_ORIG_KEY, PATH_FILE_KEY), "$PATH_ORIG_KEY = ?",
            arrayOf(origPath), null, null, null).use { cursor ->
                var instrumental = false
                while (cursor.moveToNext() && !instrumental){
                    if (cursor.getString(PATH_FILE) == "INSTRUMENTAL")
                        instrumental = true
                }
                instrumental
        }
    }

    fun setInstrumental (origPath: String) {
        writableDatabase.insert(TABLE_PATHS, null, ContentValues().apply {
            put(PATH_ORIG_KEY, origPath)
            put(PATH_FILE_KEY, "INSTRUMENTAL")
            put(PATH_EMBEDDED_KEY, "0")
        })
    }

    companion object {
        private const val DATABASE_NAME = "pathDatabase.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_PATHS = "paths"
        private const val PATH_ORIG_KEY = "origPath"
        private const val PATH_FILE = 1
        private const val PATH_FILE_KEY = "filePath"
        private const val PATH_EMBEDDED_KEY = "embedded"

        class Path(val origPath: String, val filePath: String, val embedded: Boolean)
    }
}
