package net.rachel030219.poweramplrc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle


class PathActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("path_request") && intent.getIntExtra("path_request", 0) == LrcService.REQUEST_PATH) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), LrcService.REQUEST_PATH)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LrcService.REQUEST_PATH && resultCode == RESULT_OK && data != null) {
            val treeUri = data.data
            if (treeUri != null) {
                android.util.Log.d("DEBUG-URI", treeUri.toString())
                val takeFlags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, takeFlags)
                getSharedPreferences("paths", Context.MODE_PRIVATE).edit().putString(intent.getStringExtra("key"), "$treeUri/document/").apply()
                finish()
            }
        }
    }
}