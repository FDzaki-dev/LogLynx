package com.pro.logcatreader.viewmodel

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pro.logcatreader.engine.LogcatEngine
import com.pro.logcatreader.model.LogLevel
import com.pro.logcatreader.model.LogLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

private const val SHIZUKU_REQUEST_CODE = 1001

class LogcatViewModel : ViewModel() {

    private val _rawLogs = MutableStateFlow<List<LogLine>>(emptyList())

    // State Filter UI
    val searchQuery = MutableStateFlow("")
    val selectedMinLevel = MutableStateFlow(LogLevel.VERBOSE)
    val isRegexEnabled = MutableStateFlow(false)

    // Shizuku: binder baru siap -> restart stream jika sudah diizinkan, selain itu minta izin
    private val binderListener = Shizuku.OnBinderReceivedListener {
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (granted) restartStreaming() else requestShizukuPermissionIfNeeded()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) restartStreaming()
    }

    init {
        viewModelScope.launch {
            LogcatEngine.logFlow.collect { updatedList ->
                _rawLogs.value = updatedList
            }
        }
        Shizuku.addBinderReceivedListener(binderListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        LogcatEngine.startStreaming(viewModelScope)
        requestShizukuPermissionIfNeeded()
    }

    // Pipeline Pemrosesan Filter Cepat
    val filteredLogs: StateFlow<List<LogLine>> = combine(
        _rawLogs, searchQuery, selectedMinLevel, isRegexEnabled
    ) { logs, query, minLevel, isRegex ->
        if (logs.isEmpty()) return@combine emptyList<LogLine>()

        // Regex dikompilasi sekali per emisi (bukan per baris log)
        val regex: Regex? = if (isRegex && query.isNotEmpty()) {
            try {
                Regex(query, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        logs.filter { log ->
            // 1. Filter Log Level Prioritas
            if (log.level.priority < minLevel.priority) return@filter false

            // 2. Filter Teks / Regex Search
            if (query.isEmpty()) return@filter true

            if (isRegex) {
                regex != null && (regex.containsMatchIn(log.tag) || regex.containsMatchIn(log.message))
            } else {
                log.tag.contains(query, ignoreCase = true) || log.message.contains(query, ignoreCase = true)
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllLogs() {
        LogcatEngine.clearBuffer()
        _rawLogs.value = emptyList()
    }

    private fun restartStreaming() {
        _rawLogs.value = emptyList()
        LogcatEngine.startStreaming(viewModelScope)
    }

    private fun requestShizukuPermissionIfNeeded() {
        runCatching {
            if (Shizuku.pingBinder() &&
                !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED &&
                !Shizuku.shouldShowRequestPermissionRationale()
            ) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        LogcatEngine.stopStreaming()
    }
}
