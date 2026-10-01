package com.pro.logcatreader.engine

import android.content.pm.PackageManager
import android.util.Log
import com.pro.logcatreader.model.LogLine
import com.pro.logcatreader.model.LogcatParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object LogcatEngine {
    private const val TAG = "LogcatEngine"
    private val logcatCommand = arrayOf("logcat", "-v", "threadtime")

    @Volatile
    private var logJob: Job? = null
    val logFlow = MutableSharedFlow<List<LogLine>>(replay = 1)

    // Circular Buffer menggunakan ConcurrentLinkedDeque (Thread-safe)
    private val maxBufferLimit = 50000
    private val internalBuffer = ConcurrentLinkedDeque<LogLine>()
    // size ConcurrentLinkedDeque = O(n); pakai counter terpisah agar tiap baris O(1)
    private val bufferSize = AtomicInteger(0)
    // Emit ke UI hanya jika ada data baru
    private val dirty = AtomicBoolean(false)
    // UI terlihat (Activity STARTED)? Saat false, sinkron ke UI ditangguhkan total (tanpa wake-up & salinan buffer)
    private val uiActive = MutableStateFlow(false)

    fun setUiActive(active: Boolean) {
        uiActive.value = active
    }

    fun startStreaming(scope: CoroutineScope) {
        val previous = logJob
        logJob = scope.launch(Dispatchers.IO) {
            previous?.cancelAndJoin()
            // logcat memutar ulang buffer sistem tiap start -> bersihkan agar tidak duplikat
            clearBuffer()

            val process: Process = try {
                openProcess()
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membuka proses logcat", e)
                return@launch
            }

            // Saat job dibatalkan, proses logcat wajib dimatikan (readLine() tidak cancellable)
            val killer = launch {
                try {
                    awaitCancellation()
                } finally {
                    process.destroy()
                }
            }

            // Buffer UI secara berkala (200ms) agar aplikasi tidak lag.
            // Saat UI di background loop suspend (0 wake-up); begitu UI aktif, data tertunda langsung di-emit.
            val uiSyncJob = launch {
                while (isActive) {
                    uiActive.first { it }
                    if (dirty.getAndSet(false)) {
                        logFlow.emit(internalBuffer.toList())
                    }
                    delay(200)
                }
            }

            try {
                val reader = BufferedReader(InputStreamReader(process.inputStream), 8192)
                while (isActive) {
                    val line = reader.readLine() ?: break
                    val parsed = LogcatParser.parse(line) ?: continue

                    internalBuffer.addLast(parsed)
                    dirty.set(true)

                    // Jika melampaui batas max buffer, tendang baris paling tua
                    if (bufferSize.incrementAndGet() > maxBufferLimit) {
                        if (internalBuffer.pollFirst() != null) bufferSize.decrementAndGet()
                    }
                }
            } catch (e: Exception) {
                if (isActive) Log.e(TAG, "Stream logcat terputus", e)
            } finally {
                uiSyncJob.cancel()
                killer.cancel()
            }
        }
    }

    fun stopStreaming() {
        logJob?.cancel()
    }

    fun clearBuffer() {
        internalBuffer.clear()
        bufferSize.set(0)
        dirty.set(false)
    }

    private fun openProcess(): Process {
        val viaShizuku: Process? = runCatching {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                // Eksekusi via Shizuku API (Membaca log seluruh sistem tanpa root fisik)
                newShizukuProcess()
            } else {
                null
            }
        }.onFailure { Log.e(TAG, "Shizuku newProcess gagal, fallback ke proses lokal", it) }
            .getOrNull()

        // Fallback ke proses lokal (Hanya membaca log internal aplikasi sendiri)
        return viaShizuku ?: Runtime.getRuntime().exec(logcatCommand)
    }

    // Shizuku#newProcess bersifat deprecated/private di API 13.1.x -> dipanggil via reflection
    private fun newShizukuProcess(): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(null, logcatCommand, null, null) as Process
    }
}
