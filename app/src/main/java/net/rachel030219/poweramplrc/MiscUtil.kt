package net.rachel030219.poweramplrc

import android.content.Context
import android.net.Uri
import android.util.TypedValue
import androidx.documentfile.provider.DocumentFile

object MiscUtil {
    // metrics converting
    fun spToPx (sp: Float, context: Context): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
    }
    fun dpToPx (dp: Float, context: Context): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    fun extractAndReplaceExt (oldString: String): String {
        return StringBuilder(oldString).substring(0, oldString.lastIndexOf('.')) + ".lrc"
    }
}