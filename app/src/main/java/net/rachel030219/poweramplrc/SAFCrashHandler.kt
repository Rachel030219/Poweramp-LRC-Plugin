package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import java.io.BufferedOutputStream
import java.io.File
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

class SAFCrashHandler: Thread.UncaughtExceptionHandler {
    var context: Context? = null
    var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init (context: Context) {
        this.context = context
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    private fun genTitle (content: String): String {
        return "\n\n************ $content ************\n\n"
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        val errorReport = StringBuilder()
        errorReport.append(genTitle("CAUSE OF ERROR"))
        errorReport.append(e.toString() + "\n")
        e.stackTrace.forEach { errorReport.append("$it\n") }
        errorReport.append(genTitle("PATHS"))
        context?.run {
            for ((key, value) in getSharedPreferences("paths", Context.MODE_PRIVATE).all) {
                errorReport.append("key: $key, value: $value\n")
            }
        }
        errorReport.append(genTitle("DEVICE INFORMATION"))
        errorReport.append("Brand: ")
        errorReport.append(Build.BRAND)
        errorReport.append("\n")
        errorReport.append("Device: ")
        errorReport.append(Build.DEVICE)
        errorReport.append("\n")
        errorReport.append("Model: ")
        errorReport.append(Build.MODEL)
        errorReport.append("\n")
        errorReport.append("Id: ")
        errorReport.append(Build.ID)
        errorReport.append("\n")
        errorReport.append("Product: ")
        errorReport.append(Build.PRODUCT)
        errorReport.append("\n")
        errorReport.append(genTitle("FIRMWARE"))
        errorReport.append("SDK Version: ")
        errorReport.append(Build.VERSION.SDK_INT)
        errorReport.append("\n")
        errorReport.append("Release: ")
        errorReport.append(Build.VERSION.RELEASE)
        errorReport.append("\n")

        try {
            saveLogFile(errorReport)
            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (e: Exception) {
            defaultHandler!!.uncaughtException(t, e)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun saveLogFile (log: StringBuilder) {
        val fileName = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date()) + ".log"
        val dirFile = context!!.getExternalFilesDir(null)!!
        //TODO: fix toast not appearing
        Toast.makeText(context, "Error occurred, log stored at " + dirFile.absolutePath + "/$fileName", Toast.LENGTH_LONG).show()
        BufferedOutputStream(File(dirFile, fileName).outputStream()).run {
            write(log.toString().toByteArray())
            close()
        }
    }
}