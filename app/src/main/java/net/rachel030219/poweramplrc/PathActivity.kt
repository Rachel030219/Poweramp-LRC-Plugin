package net.rachel030219.poweramplrc

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile

class PathActivity: Activity() {
    companion object {
        const val REQUEST_FOLDER = 11
    }
    var path = "null"
    var embedded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("request")) {
            try {
                when (intent.getIntExtra("request", 0)) {
                    REQUEST_FOLDER ->
                        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_FOLDER)
                    LrcWindow.REQUEST_SELECT -> {
                        NotificationManagerCompat.from(this).cancel(210)
                        if (intent.hasExtra("path"))
                            path = intent.getStringExtra("path") ?: "null"
                        if (intent.hasExtra("embedded"))
                            embedded = intent.getBooleanExtra("embedded", false)
                        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }, LrcWindow.REQUEST_SELECT)
                    }
                }
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Your device does not have DocumentsUI and is unsupported so far!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val treeUri = data.data
            if (treeUri != null) {
                val takeFlags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, takeFlags)
                when (requestCode) {
                    REQUEST_FOLDER -> {
                        DocumentFile.fromTreeUri(this, treeUri)?.let { file ->
                            if (file.isDirectory && file.name != null) {
                                FoldersDatabaseHelper(this).addFolder(FoldersDatabaseHelper.Companion.Folder(file.name!!, treeUri.toString()))
                            }
                        }
                    }
                    LrcWindow.REQUEST_SELECT -> {
                        contentResolver.openFileDescriptor(treeUri, "r")?.use { parcelFileDescriptor ->
                            if (parcelFileDescriptor.fileDescriptor.valid() && path != "null") {
                                PathsDatabaseHelper(this).addPath(PathsDatabaseHelper.Companion.Path(path, treeUri.toString(), false))
                                LrcWindow.reloadLyrics(false, this)
                            }
                            path = "null"
                        }
                    }
                }
            }
        }
        finish()
    }
}