package com.pro.logcatreader

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menangkap uncaught exception dan menyimpannya ke Documents/LogLynx via MediaStore (API 29+).
 * API < 29: fallback ke folder Documents milik aplikasi (tanpa izin storage).
 */
object CrashLogger {

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(appContext, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "LogLynx_crash_$stamp.txt"
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val content = buildString {
            appendLine("LogLynx crash @ $stamp")
            appendLine("Thread : ${thread.name}")
            appendLine("Device : ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            append(trace)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/LogLynx")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            }
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            File(dir, fileName).writeText(content)
        }
    }
}
