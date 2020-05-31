package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.Process
import android.widget.Toast
import java.io.BufferedOutputStream
import java.io.File
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

    override fun uncaughtException(t: Thread, e: Throwable) {
        val errorReport = LogGenerator(context!!).generateFromException(e)
        saveLogFile(errorReport)
        try {
            Thread.sleep(3000)
            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (e: InterruptedException) {
            defaultHandler!!.uncaughtException(t, e)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveLogFile (log: StringBuilder) {
        val fileName = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date()) + ".log"
        val dirFile = context!!.getExternalFilesDir(null)!!
        Thread {
            Looper.prepare()
            Toast.makeText(context, "Error occurred, log stored at " + dirFile.absolutePath + "/$fileName", Toast.LENGTH_LONG).show()
            Looper.loop()
        }.start()
        BufferedOutputStream(File(dirFile, fileName).outputStream()).run {
            write(log.toString().toByteArray())
            close()
        }
    }
}