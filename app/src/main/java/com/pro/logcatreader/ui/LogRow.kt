package com.pro.logcatreader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pro.logcatreader.model.LogLevel
import com.pro.logcatreader.model.LogLine
import com.pro.logcatreader.ui.theme.RobotoMonoFontFamily
import com.pro.logcatreader.ui.theme.badgeColor
import com.pro.logcatreader.ui.theme.logSecondaryColor
import com.pro.logcatreader.ui.theme.searchHitColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val MAX_HIGHLIGHTS_PER_FIELD = 20

/** Kolom yang tampil di baris log (Opsi tampilan). Semua aktif = tampilan LogcatReader penuh. */
@Immutable
data class LogDisplayOptions(
    val showTag: Boolean = true,
    val showDate: Boolean = true,
    val showTime: Boolean = true,
    val showPid: Boolean = true,
    val showTid: Boolean = true
)

private val AllColumns = LogDisplayOptions()

// Gaya teks baris log (Roboto Mono, ukuran identik LogcatReader)
private val BadgeStyle = TextStyle(
    fontFamily = RobotoMonoFontFamily,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = Color.White,
    textAlign = TextAlign.Center
)
private val TagStyle = TextStyle(
    fontFamily = RobotoMonoFontFamily,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium
)
private val CompactTagStyle = TextStyle(
    fontFamily = RobotoMonoFontFamily,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium
)
private val MessageStyle = TextStyle(
    fontFamily = RobotoMonoFontFamily,
    fontSize = 12.sp
)
private val MetaStyle = TextStyle(
    fontFamily = RobotoMonoFontFamily,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold
)

/**
 * Satu entri daftar: pemisah tipis + baris log.
 */
@Composable
fun LogListEntry(
    logLine: LogLine,
    showDivider: Boolean,
    compact: Boolean,
    options: LogDisplayOptions,
    highlight: Regex?,
    onLongClick: () -> Unit
) {
    Column {
        if (showDivider) HorizontalDivider()
        LogItemRow(
            logLine = logLine,
            compact = compact,
            options = options,
            highlight = highlight,
            onLongClick = onLongClick
        )
    }
}

/**
 * Baris log gaya LogcatReader: strip badge prioritas di kiri, lalu tag / pesan / tanggal-jam-PID-TID
 * (Roboto Mono; kolom mengikuti Opsi tampilan).
 * Mode ringkas: satu baris (tag 20% | pesan 80%), tap = buka (animasi) menampilkan semua kolom.
 * Tekan lama = menu aksi. Tint tipis untuk W/E/F dipertahankan dari LogLynx.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItemRow(
    logLine: LogLine,
    compact: Boolean,
    options: LogDisplayOptions,
    highlight: Regex?,
    onLongClick: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val showFull = !compact || expanded
    // Ringkas + dibuka = semua kolom tampil (seperti LogcatReader); non-ringkas mengikuti Opsi tampilan
    val shown = if (compact) AllColumns else options
    val hitColor = searchHitColor()
    val secondary = logSecondaryColor()
    val priorityColor = logLine.level.badgeColor()
    val metaStyle = remember(secondary) { MetaStyle.copy(color = secondary) }

    val tagText = remember(logLine.id, highlight, hitColor) {
        highlighted(logLine.tag, highlight, hitColor)
    }
    val messageText = remember(logLine.id, highlight, hitColor) {
        highlighted(logLine.message, highlight, hitColor)
    }

    val tint = when (logLine.level) {
        LogLevel.ERROR, LogLevel.FATAL -> priorityColor.copy(alpha = 0.12f)
        LogLevel.WARN -> priorityColor.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            .height(IntrinsicSize.Max)
            .background(tint)
            .combinedClickable(
                onClick = { if (compact) expanded = !expanded },
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(priorityColor)
                .padding(if (showFull) 5.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = logLine.level.char, style = BadgeStyle)
        }

        if (showFull) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (shown.showTag) {
                    Text(
                        text = tagText,
                        modifier = Modifier.fillMaxWidth(),
                        style = TagStyle
                    )
                }
                Text(
                    text = messageText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MessageStyle
                )
                if (shown.showDate || shown.showTime || shown.showPid || shown.showTid) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (shown.showDate) {
                            Text(text = logLine.timestamp.substringBefore(' '), style = metaStyle)
                        }
                        if (shown.showTime) {
                            Text(text = logLine.timestamp.substringAfter(' '), style = metaStyle)
                        }
                        if (shown.showPid) Text(text = logLine.pid, style = metaStyle)
                        if (shown.showTid) Text(text = logLine.tid, style = metaStyle)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                if (options.showTag) {
                    Text(
                        text = tagText,
                        modifier = Modifier.weight(0.2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = CompactTagStyle
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = messageText,
                    modifier = Modifier.weight(if (options.showTag) 0.8f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MessageStyle
                )
            }
        }
    }
}

/** Sorot kecocokan query (maks 20 per kolom agar baris panjang tetap ringan). */
private fun highlighted(text: String, regex: Regex?, hitColor: Color): AnnotatedString {
    if (regex == null || text.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        var count = 0
        for (match in regex.findAll(text)) {
            if (match.range.isEmpty()) continue
            addStyle(SpanStyle(background = hitColor), match.range.first, match.range.last + 1)
            count++
            if (count >= MAX_HIGHLIGHTS_PER_FIELD) break
        }
    }
}

/**
 * Scrollbar tipis: muncul saat list digulir, memudar ~1,5 dtk setelah berhenti (seperti LogcatReader).
 * Posisi dihitung dari layoutInfo (perkiraan; tinggi baris bervariasi).
 */
@Composable
fun Modifier.logScrollbar(state: LazyListState): Modifier {
    val alpha = remember { Animatable(0f) }
    val color = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { scrolling ->
                if (scrolling) {
                    alpha.animateTo(0.5f)
                } else {
                    delay(1500)
                    alpha.animateTo(0f)
                }
            }
    }

    return this.drawWithContent {
        drawContent()
        val a = alpha.value
        val info = state.layoutInfo
        val total = info.totalItemsCount
        val visible = info.visibleItemsInfo
        if (a > 0f && visible.isNotEmpty() && total > visible.size) {
            val first = visible.first()
            val fraction = if (first.size > 0) {
                state.firstVisibleItemScrollOffset.toFloat() / first.size
            } else {
                0f
            }
            val denom = (total - visible.size).coerceAtLeast(1)
            val progress = ((state.firstVisibleItemIndex + fraction) / denom).coerceIn(0f, 1f)
            val barWidth = 4.dp.toPx()
            val barHeight = (size.height * visible.size / total)
                .coerceAtLeast(36.dp.toPx())
                .coerceAtMost(size.height)
            drawRoundRect(
                color = color.copy(alpha = a),
                topLeft = Offset(
                    x = size.width - barWidth - 2.dp.toPx(),
                    y = progress * (size.height - barHeight)
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth)
            )
        }
    }
}
