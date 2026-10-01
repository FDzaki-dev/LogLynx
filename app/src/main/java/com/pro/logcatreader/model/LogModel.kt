package com.pro.logcatreader.model

import androidx.compose.ui.graphics.Color

enum class LogLevel(val char: String, val color: Color, val priority: Int) {
    VERBOSE("V", Color(0xFFBBBBBB), 2),
    DEBUG("D", Color(0xFF29B6F6), 3),
    INFO("I", Color(0xFF66BB6A), 4),
    WARN("W", Color(0xFFFFA726), 5),
    ERROR("E", Color(0xFFEF5350), 6),
    FATAL("F", Color(0xFFD32F2F), 7);

    companion object {
        fun fromChar(char: String): LogLevel = entries.find { it.char == char } ?: INFO
    }
}

data class LogLine(
    val id: Long,
    val timestamp: String,
    val pid: String,
    val tid: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

object LogcatParser {
    // Pattern Regex untuk mem-parsing format -v threadtime
    private val logPattern = Regex("""^([0-9-]+\s+[0-9:.]+)\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.*?): (.*)$""")
    private var idCounter = 0L

    fun parse(line: String): LogLine? {
        val matchResult = logPattern.matchEntire(line.trim()) ?: return null
        val groups = matchResult.groupValues

        return LogLine(
            id = idCounter++,
            timestamp = groups[1],
            pid = groups[2],
            tid = groups[3],
            level = LogLevel.fromChar(groups[4]),
            tag = groups[5].trim(),
            message = groups[6]
        )
    }
}
