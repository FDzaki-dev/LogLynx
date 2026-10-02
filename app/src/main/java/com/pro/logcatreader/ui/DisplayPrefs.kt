package com.pro.logcatreader.ui

import android.content.Context

/** Pilihan Opsi tampilan yang disimpan antar sesi (SharedPreferences privat, bukan secret). */
data class DisplaySettings(
    val compact: Boolean = false,
    val options: LogDisplayOptions = LogDisplayOptions()
)

/**
 * Baca/tulis Opsi tampilan. Panggil dari Dispatchers.IO (file kecil, tapi tetap I/O).
 * Pemakaian: dibaca sekali saat layar dibuka, ditulis tiap pilihan berubah.
 */
object DisplayPrefs {
    private const val FILE = "loglynx_display"
    private const val K_COMPACT = "compact"
    private const val K_TAG = "show_tag"
    private const val K_DATE = "show_date"
    private const val K_TIME = "show_time"
    private const val K_PID = "show_pid"
    private const val K_TID = "show_tid"

    fun read(context: Context): DisplaySettings {
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return DisplaySettings(
            compact = p.getBoolean(K_COMPACT, false),
            options = LogDisplayOptions(
                showTag = p.getBoolean(K_TAG, true),
                showDate = p.getBoolean(K_DATE, true),
                showTime = p.getBoolean(K_TIME, true),
                showPid = p.getBoolean(K_PID, true),
                showTid = p.getBoolean(K_TID, true)
            )
        )
    }

    fun write(context: Context, settings: DisplaySettings) {
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(K_COMPACT, settings.compact)
            .putBoolean(K_TAG, settings.options.showTag)
            .putBoolean(K_DATE, settings.options.showDate)
            .putBoolean(K_TIME, settings.options.showTime)
            .putBoolean(K_PID, settings.options.showPid)
            .putBoolean(K_TID, settings.options.showTid)
            .apply()
    }
}
