package net.rachel030219.poweramplrc

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FoldersDatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_PHOTOS_TABLE =
            "CREATE TABLE $TABLE_FOLDERS($FOLDER_NAME_KEY TEXT,$FOLDER_PATH_KEY TEXT)"
        db.execSQL(CREATE_PHOTOS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FOLDERS")
            onCreate(db)
        }
    }

    fun addFolder (folder: Folder) {
        writableDatabase.insert(TABLE_FOLDERS, null, ContentValues().apply {
            put(FOLDER_NAME_KEY, folder.name)
            put(FOLDER_PATH_KEY, folder.path)
        })
    }

    fun removeFolder (folder: Folder) {
        writableDatabase.delete(TABLE_FOLDERS, "$FOLDER_NAME_KEY = ? AND $FOLDER_PATH_KEY = ?", arrayOf(folder.name, folder.path))
    }
    
    fun fetchFolders (): MutableList<Folder> {
        return mutableListOf<Folder>().apply {
            readableDatabase.query(TABLE_FOLDERS, arrayOf(FOLDER_NAME_KEY, FOLDER_PATH_KEY), null,
                null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    add(Folder(cursor.getString(FOLDER_NAME), cursor.getString(FOLDER_PATH)))
                }
            }
        }
    }

    companion object {
        // Database Info
        private const val DATABASE_NAME = "folderDatabase"
        private const val DATABASE_VERSION = 1

        // Table Names
        private const val TABLE_FOLDERS = "folders"

        // Columns
        private const val FOLDER_NAME = 0
        private const val FOLDER_NAME_KEY = "name"
        private const val FOLDER_PATH = 1
        private const val FOLDER_PATH_KEY = "path"
        
        class Folder(val name: String, val path: String)
    }
}
