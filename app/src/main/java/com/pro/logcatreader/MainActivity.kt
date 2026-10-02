@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pro.logcatreader.model.LogLevel
import com.pro.logcatreader.model.LogLine
import com.pro.logcatreader.ui.LogDisplayOptions
import com.pro.logcatreader.ui.LogListEntry
import com.pro.logcatreader.ui.logScrollbar
import com.pro.logcatreader.ui.theme.LogLynxTheme
import com.pro.logcatreader.ui.theme.RobotoMonoFontFamily
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
    var showDisplaySheet by rememberSaveable { mutableStateOf(false) }
    var showTag by rememberSaveable { mutableStateOf(true) }
    var showDate by rememberSaveable { mutableStateOf(true) }
    var showTime by rememberSaveable { mutableStateOf(true) }
    var showPid by rememberSaveable { mutableStateOf(true) }
    var showTid by rememberSaveable { mutableStateOf(true) }
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
    val displayOptions = remember(showTag, showDate, showTime, showPid, showTid) {
        LogDisplayOptions(showTag, showDate, showTime, showPid, showTid)
    }
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
                    matchCount = logs.size,
                    onQueryChange = { viewModel.searchQuery.value = it },
                    onToggleRegex = { viewModel.isRegexEnabled.value = !isRegex },
                    onClose = { closeSearch() }
                )
            } else {
                LogTopBar(
                    count = logs.size,
                    filtered = filtered,
                    scrollPaused = !autoScrollLocked,
                    onSearch = { searchActive = true },
                    onTogglePause = { autoScrollLocked = !autoScrollLocked },
                    onDisplayOptions = { showDisplaySheet = true },
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
                    StatusMessage(
                        message = if (filtered) "Tidak ada log yang cocok dengan filter" else "Menunggu log…",
                        loading = !filtered,
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
                            options = displayOptions,
                            highlight = highlight,
                            onLongClick = { selectedId = log.id }
                        )
                    }
                }

                // FAB gulir atas/bawah (muncul saat auto-scroll dijeda)
                ScrollFabs(
                    visible = !autoScrollLocked && logs.isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    onScrollTop = { scope.launch { listState.scrollToItem(0) } },
                    onScrollBottom = { autoScrollLocked = true }
                )
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

    if (showDisplaySheet) {
        DisplayOptionsSheet(
            options = displayOptions,
            compactView = compactView,
            onOptionsChange = {
                showTag = it.showTag
                showDate = it.showDate
                showTime = it.showTime
                showPid = it.showPid
                showTid = it.showTid
            },
            onCompactChange = { compactView = it },
            onDismiss = { showDisplaySheet = false }
        )
    }
}

// Top-level (bukan di dalam Column/Box): menghindari bentrok DslMarker antara
// ColumnScope.AnimatedVisibility dan BoxScope pada pemanggilan implicit receiver.
@Composable
private fun ScrollFabs(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FloatingActionButton(
                onClick = onScrollTop,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Ke atas")
            }
            FloatingActionButton(
                onClick = onScrollBottom,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ke bawah")
            }
        }
    }
}

@Composable
private fun LogTopBar(
    count: Int,
    filtered: Boolean,
    scrollPaused: Boolean,
    onSearch: () -> Unit,
    onTogglePause: () -> Unit,
    onDisplayOptions: () -> Unit,
    onClear: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val subtitle = buildString {
        append("%,d baris".format(count))
        if (scrollPaused) append(" · scroll dijeda")
    }

    TopAppBar(
        title = {
            Column {
                Text(text = "LogLynx", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ikon filter di depan jumlah baris (seperti LogcatReader)
                    if (filtered) {
                        Icon(
                            imageVector = FilterListIcon,
                            contentDescription = "Filter aktif",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                        text = { Text("Opsi tampilan") },
                        onClick = {
                            menuOpen = false
                            onDisplayOptions()
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

/**
 * Search bar menggantikan app bar (seperti LogcatReader): kembali, kolom cari (18sp),
 * jumlah baris cocok, tombol ".*" (Regex). Pencarian = filter daftar (bukan sorot-lompat).
 */
@Composable
private fun SearchTopBar(
    query: String,
    isRegex: Boolean,
    regexError: Boolean,
    matchCount: Int,
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
                textStyle = TextStyle(color = textColor, fontSize = 18.sp),
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
                                fontSize = 18.sp,
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
            if (query.isNotEmpty() && !regexError) {
                Text(
                    text = "%,d".format(matchCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus teks")
                }
            }
            TextButton(
                onClick = onToggleRegex,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(36.dp)
                    .semantics {
                        contentDescription = if (isRegex) "Regex aktif" else "Regex nonaktif"
                    },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isRegex) content else content.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = ".*",
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
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
                        fontFamily = RobotoMonoFontFamily,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "${logLine.level.char}/${logLine.tag}",
                color = logLine.level.badgeColor(),
                fontFamily = RobotoMonoFontFamily,
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

/** Status kosong (seperti LogcatReader): indikator memuat + pesan di tengah. */
@Composable
private fun StatusMessage(
    message: String,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Opsi tampilan (seperti LogcatReader): chip kolom + saklar tampilan ringkas.
 * Berlaku langsung (tanpa tombol Simpan); di mode ringkas hanya chip Tag yang aktif.
 */
@Composable
private fun DisplayOptionsSheet(
    options: LogDisplayOptions,
    compactView: Boolean,
    onOptionsChange: (LogDisplayOptions) -> Unit,
    onCompactChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = "Opsi tampilan",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OptionChip("Tag", options.showTag, true) {
                    onOptionsChange(options.copy(showTag = !options.showTag))
                }
                OptionChip("Tanggal", options.showDate, !compactView) {
                    onOptionsChange(options.copy(showDate = !options.showDate))
                }
                OptionChip("Jam", options.showTime, !compactView) {
                    onOptionsChange(options.copy(showTime = !options.showTime))
                }
                OptionChip("PID", options.showPid, !compactView) {
                    onOptionsChange(options.copy(showPid = !options.showPid))
                }
                OptionChip("TID", options.showTid, !compactView) {
                    onOptionsChange(options.copy(showTid = !options.showTid))
                }
            }
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCompactChange(!compactView) },
                headlineContent = { Text("Tampilan ringkas") },
                trailingContent = {
                    Switch(checked = compactView, onCheckedChange = null)
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        enabled = enabled,
        label = { Text(label) }
    )
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// Ikon Filter (material-icons-extended tidak dipakai agar APK tetap kecil)
private val FilterListIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(10f, 18f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(-2f)
        horizontalLineToRelative(-4f)
        verticalLineToRelative(2f)
        close()
        moveTo(3f, 6f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(18f)
        verticalLineTo(6f)
        horizontalLineTo(3f)
        close()
        moveTo(6f, 13f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(-2f)
        horizontalLineTo(6f)
        verticalLineToRelative(2f)
        close()
    }.build()
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
