@file:OptIn(ExperimentalMaterial3Api::class)

package com.pro.logcatreader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pro.logcatreader.model.LogLevel
import com.pro.logcatreader.model.LogLine
import com.pro.logcatreader.ui.LogListEntry
import com.pro.logcatreader.ui.logScrollbar
import com.pro.logcatreader.ui.theme.LogLynxTheme
import com.pro.logcatreader.ui.theme.badgeColor
import com.pro.logcatreader.viewmodel.LogcatViewModel
import kotlinx.coroutines.launch

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
        // App bar selalu teal gelap -> ikon status bar terang di kedua tema; nav bar mengikuti tema sistem
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            LogLynxTheme {
                LogcatScreen()
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
    val scope = rememberCoroutineScope()
    var autoScrollLocked by rememberSaveable { mutableStateOf(true) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var compactView by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Regex sorotan dikompilasi sekali per perubahan query (bukan per baris)
    val highlight: Regex? = remember(searchQuery, isRegex) {
        if (searchQuery.isEmpty()) {
            null
        } else {
            runCatching {
                Regex(if (isRegex) searchQuery else Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
            }.getOrNull()
        }
    }
    val regexError = isRegex && searchQuery.isNotEmpty() && highlight == null
    val filtered = searchQuery.isNotEmpty() || minLevel != LogLevel.VERBOSE
    val selected: LogLine? = selectedId?.let { id -> logs.lastOrNull { it.id == id } }

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

    fun closeSearch() {
        searchActive = false
        viewModel.searchQuery.value = ""
    }

    BackHandler(enabled = searchActive) { closeSearch() }

    // safeDrawing = systemBars + ime + cutout: konten tidak terpotong status/nav bar maupun keyboard
    Scaffold(
        topBar = {
            if (searchActive) {
                SearchTopBar(
                    query = searchQuery,
                    isRegex = isRegex,
                    regexError = regexError,
                    onQueryChange = { viewModel.searchQuery.value = it },
                    onToggleRegex = { viewModel.isRegexEnabled.value = !isRegex },
                    onClose = { closeSearch() }
                )
            } else {
                LogTopBar(
                    count = logs.size,
                    filtered = filtered,
                    scrollPaused = !autoScrollLocked,
                    compactView = compactView,
                    onSearch = { searchActive = true },
                    onTogglePause = { autoScrollLocked = !autoScrollLocked },
                    onToggleCompact = { compactView = !compactView },
                    onClear = { viewModel.clearAllLogs() }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LevelFilterRow(
                minLevel = minLevel,
                onSelect = { viewModel.selectedMinLevel.value = it }
            )
            HorizontalDivider()

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (logs.isEmpty()) {
                    Text(
                        text = if (filtered) "Tidak ada log yang cocok dengan filter" else "Menunggu log…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .logScrollbar(listState)
                ) {
                    itemsIndexed(logs, key = { _, log -> log.id }) { index, log ->
                        LogListEntry(
                            logLine = log,
                            showDivider = index > 0,
                            compact = compactView,
                            highlight = highlight,
                            onLongClick = { selectedId = log.id }
                        )
                    }
                }

                // FAB gulir atas/bawah (muncul saat auto-scroll dijeda)
                AnimatedVisibility(
                    visible = !autoScrollLocked && logs.isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FloatingActionButton(
                            onClick = { scope.launch { listState.scrollToItem(0) } },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Ke atas")
                        }
                        FloatingActionButton(
                            onClick = { autoScrollLocked = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ke bawah")
                        }
                    }
                }
            }
        }
    }

    if (selected != null) {
        LogActionsSheet(
            logLine = selected,
            onDismiss = { selectedId = null },
            onFilterTag = { tag ->
                viewModel.isRegexEnabled.value = false
                viewModel.searchQuery.value = tag
                selectedId = null
            }
        )
    }
}

@Composable
private fun LogTopBar(
    count: Int,
    filtered: Boolean,
    scrollPaused: Boolean,
    compactView: Boolean,
    onSearch: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleCompact: () -> Unit,
    onClear: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val subtitle = buildString {
        append("%,d baris".format(count))
        if (filtered) append(" · filter aktif")
        if (scrollPaused) append(" · scroll dijeda")
    }

    TopAppBar(
        title = {
            Column {
                Text(text = "LogLynx", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Cari")
            }
            IconButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (scrollPaused) Icons.Default.PlayArrow else PauseIcon,
                    contentDescription = if (scrollPaused) "Lanjutkan auto-scroll" else "Jeda auto-scroll"
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opsi lainnya")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (compactView) "Tampilan lengkap" else "Tampilan ringkas") },
                        onClick = {
                            menuOpen = false
                            onToggleCompact()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus log") },
                        onClick = {
                            menuOpen = false
                            onClear()
                        }
                    )
                }
            }
        }
    )
}

/** Search bar menggantikan app bar (seperti LogcatReader): kembali, kolom cari, tombol Regex. */
@Composable
private fun SearchTopBar(
    query: String,
    isRegex: Boolean,
    regexError: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleRegex: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        runCatching { focusRequester.requestFocus() }
    }

    val content = MaterialTheme.colorScheme.onPrimaryContainer
    val textColor = if (regexError) Color(0xFFFFB4AB) else content

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tutup pencarian")
            }
        },
        title = {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                cursorBrush = SolidColor(content),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Cari tag / pesan…",
                                color = content.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = content,
            navigationIconContentColor = content,
            actionIconContentColor = content
        ),
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus teks")
                }
            }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRegex) content else Color.Transparent)
                    .clickable(onClick = onToggleRegex)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Regex",
                    color = if (isRegex) MaterialTheme.colorScheme.primaryContainer else content,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    )
}

/** Level minimum: satu baris chip yang bisa digulir horizontal (tidak terpotong di layar sempit). */
@Composable
private fun LevelFilterRow(minLevel: LogLevel, onSelect: (LogLevel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Min",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LogLevel.entries.forEach { level ->
            FilterChip(
                selected = minLevel == level,
                onClick = { onSelect(level) },
                label = {
                    Text(
                        text = level.char,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = level.badgeColor(),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

/** Bottom sheet tekan-lama (seperti LogcatReader): salin baris / salin pesan / filter tag ini. */
@Composable
private fun LogActionsSheet(
    logLine: LogLine,
    onDismiss: () -> Unit,
    onFilterTag: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "${logLine.level.char}/${logLine.tag}",
                color = logLine.level.badgeColor(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = logLine.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp)
            )
            HorizontalDivider()
            SheetAction("Salin baris lengkap") {
                clipboard.setText(
                    AnnotatedString(
                        "${logLine.timestamp} ${logLine.pid}-${logLine.tid} " +
                            "${logLine.level.char}/${logLine.tag}: ${logLine.message}"
                    )
                )
                Toast.makeText(context, "Baris log disalin", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SheetAction("Salin pesan") {
                clipboard.setText(AnnotatedString(logLine.message))
                Toast.makeText(context, "Pesan disalin", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SheetAction("Filter tag ini") { onFilterTag(logLine.tag) }
        }
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// Ikon Pause (material-icons-extended tidak dipakai agar APK tetap kecil)
private val PauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 19f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineTo(6f)
        verticalLineToRelative(14f)
        close()
        moveTo(14f, 5f)
        verticalLineToRelative(14f)
        horizontalLineToRelative(4f)
        verticalLineTo(5f)
        horizontalLineToRelative(-4f)
        close()
    }.build()
}
