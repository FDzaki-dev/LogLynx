package com.pro.logcatreader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pro.logcatreader.model.LogLevel
import com.pro.logcatreader.model.LogLine
import com.pro.logcatreader.viewmodel.LogcatViewModel

private val BgColor = Color(0xFF1E1E1E)
private val ToolbarColor = Color(0xFF2B2B2B)
private val ChipColor = Color(0xFF3A3A3A)
private val MessageColor = Color(0xFFE2E2E2)
private val DimColor = Color(0xFF8A8A8A)

class MainActivity : ComponentActivity() {
    // Instance sama dengan viewModel() di LogcatScreen (owner = Activity)
    private val logcatViewModel: LogcatViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        logcatViewModel.onUiStart()
    }

    override fun onStop() {
        logcatViewModel.onUiStop()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgColor,
                    contentColor = MessageColor
                ) {
                    LogcatScreen()
                }
            }
        }
    }
}

@Composable
fun LogcatScreen(viewModel: LogcatViewModel = viewModel()) {
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val minLevel by viewModel.selectedMinLevel.collectAsStateWithLifecycle()
    val isRegex by viewModel.isRegexEnabled.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    var autoScrollLocked by rememberSaveable { mutableStateOf(true) }

    // Auto-Scroll Interruption Lock: user menyentuh/menggeser list -> auto-scroll berhenti
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) autoScrollLocked = false
        }
    }

    // Key = id baris terakhir (bukan size) agar tetap scroll saat buffer penuh (size konstan)
    LaunchedEffect(logs.lastOrNull()?.id, autoScrollLocked) {
        if (autoScrollLocked && logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        // ===== TOOLBAR (tiap baris muat di layar sempit: bobot rata, tanpa tinggi fixed) =====
        // Tinggi dibatasi maks 50% area tersedia + scrollable: tidak terpotong di landscape/keyboard terbuka
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val limited = if (constraints.hasBoundedHeight) {
                        constraints.copy(maxHeight = maxOf(constraints.minHeight, constraints.maxHeight / 2))
                    } else {
                        constraints
                    }
                    val placeable = measurable.measure(limited)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .background(ToolbarColor)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    singleLine = true,
                    textStyle = TextStyle(color = MessageColor, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.Cyan),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ChipColor)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Cari tag / pesan...", color = DimColor, fontSize = 14.sp, maxLines = 1)
                                }
                                innerTextField()
                            }
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "✕",
                                    color = DimColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable { viewModel.searchQuery.value = "" }
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }
                )
                ToolbarChip(
                    label = "Regex",
                    onClick = { viewModel.isRegexEnabled.value = !isRegex },
                    containerColor = if (isRegex) Color.Cyan else ChipColor,
                    contentColor = if (isRegex) Color.Black else MessageColor,
                    bold = isRegex
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Min", color = DimColor, fontSize = 12.sp, modifier = Modifier.padding(end = 2.dp))
                LogLevel.entries.forEach { level ->
                    val selected = minLevel == level
                    ToolbarChip(
                        label = level.char,
                        onClick = { viewModel.selectedMinLevel.value = level },
                        modifier = Modifier.weight(1f),
                        containerColor = if (selected) level.color else ChipColor,
                        contentColor = if (selected) Color.Black else level.color,
                        bold = true
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToolbarChip(
                    label = if (autoScrollLocked) "🔒 Auto-scroll" else "⏸ Dibekukan",
                    onClick = { autoScrollLocked = !autoScrollLocked },
                    contentColor = if (autoScrollLocked) Color(0xFF66BB6A) else Color(0xFFFFEB3B)
                )
                ToolbarChip(
                    label = "🗑 Clear",
                    onClick = { viewModel.clearAllLogs() },
                    contentColor = Color(0xFFEF5350)
                )
                Text(
                    text = "%,d baris".format(logs.size),
                    color = DimColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ===== DAFTAR LOG =====
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (logs.isEmpty()) {
                val filtered = searchQuery.isNotEmpty() || minLevel != LogLevel.VERBOSE
                Text(
                    text = if (filtered) "Tidak ada log yang cocok dengan filter" else "Menunggu log…",
                    color = DimColor,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItemRow(logLine = log)
                }
            }

            if (!autoScrollLocked && logs.isNotEmpty()) {
                ToolbarChip(
                    label = "⬇ Ke bawah",
                    onClick = { autoScrollLocked = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    containerColor = Color(0xFF66BB6A),
                    contentColor = Color.Black,
                    bold = true
                )
            }
        }
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ChipColor,
    contentColor: Color = MessageColor,
    bold: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Satu baris log ringkas: jam | level | tag: pesan.
 * Tap = tampilkan detail (tanggal, PID, TID). Tekan lama = salin baris.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItemRow(logLine: LogLine) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    val rowBackground = when (logLine.level) {
        LogLevel.ERROR, LogLevel.FATAL -> Color(0x33EF5350)
        LogLevel.WARN -> Color(0x22FFA726)
        else -> Color.Transparent
    }

    val styled = remember(logLine, expanded) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = DimColor)) {
                append(logLine.timestamp.substringAfterLast(' '))
                append(' ')
            }
            withStyle(
                SpanStyle(color = Color.Black, background = logLine.level.color, fontWeight = FontWeight.Bold)
            ) {
                append(" ${logLine.level.char} ")
            }
            withStyle(SpanStyle(color = logLine.level.color, fontWeight = FontWeight.Bold)) {
                append(" ${logLine.tag}")
            }
            withStyle(SpanStyle(color = DimColor)) { append(": ") }
            withStyle(SpanStyle(color = MessageColor)) { append(logLine.message) }
            if (expanded) {
                withStyle(SpanStyle(color = DimColor)) {
                    append("\n${logLine.timestamp}  PID ${logLine.pid}  TID ${logLine.tid}")
                }
            }
        }
    }

    Text(
        text = styled,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    clipboard.setText(
                        AnnotatedString(
                            "${logLine.timestamp} ${logLine.pid}-${logLine.tid} " +
                                "${logLine.level.char}/${logLine.tag}: ${logLine.message}"
                        )
                    )
                    Toast.makeText(context, "Baris log disalin", Toast.LENGTH_SHORT).show()
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
