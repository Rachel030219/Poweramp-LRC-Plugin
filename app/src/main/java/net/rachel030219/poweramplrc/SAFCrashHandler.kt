package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Process
import android.widget.Toast
import java.lang.Character.LINE_SEPARATOR
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


class SAFCrashHandler: Thread.UncaughtExceptionHandler {
    var context: Context? = null

    override fun uncaughtException(t: Thread, e: Throwable) {
        val errorReport = StringBuilder()
        errorReport.append("************ CAUSE OF ERROR ************\n\n")
        e.stackTrace.forEach { errorReport.append("$it\n") }
        errorReport.append("************ PATHS ************\n\n")
        context?.let {
            it.getSharedPreferences("paths", Context.MODE_PRIVATE).all.forEach { mapEntry ->
                errorReport.append("key: " + mapEntry.key + ", value:" + mapEntry.value + "\n")
            }
        }
        errorReport.append("\n************ DEVICE INFORMATION ***********\n")
        errorReport.append("Brand: ")
        errorReport.append(Build.BRAND)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Device: ")
        errorReport.append(Build.DEVICE)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Model: ")
        errorReport.append(Build.MODEL)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Id: ")
        errorReport.append(Build.ID)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Product: ")
        errorReport.append(Build.PRODUCT)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("\n************ FIRMWARE ************\n")
        errorReport.append("SDK Version: ")
        errorReport.append(Build.VERSION.SDK_INT)
        errorReport.append(LINE_SEPARATOR)
        errorReport.append("Release: ")
        errorReport.append(Build.VERSION.RELEASE)
        errorReport.append(LINE_SEPARATOR)

        saveLogFile(errorReport)

        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {} finally {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun saveLogFile (log: StringBuilder) {
        val fileName = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date()) + ".log"
        context!!.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(log.toString().toByteArray())
        }
        Toast.makeText(context, "Error occurred, log stored at " + context!!.filesDir.absolutePath + "/$fileName", Toast.LENGTH_LONG).show()
    }

    //TODO: add this to application
}