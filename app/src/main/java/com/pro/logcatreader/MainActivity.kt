package com.pro.logcatreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pro.logcatreader.model.LogLevel
import com.pro.logcatreader.model.LogLine
import com.pro.logcatreader.viewmodel.LogcatViewModel

class MainActivity : ComponentActivity() {
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
                    color = Color(0xFF1E1E1E)
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

    // Key = id baris terakhir (bukan size) agar tetap scroll saat buffer penuh (size konstan)
    LaunchedEffect(logs.lastOrNull()?.id) {
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
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Cari tag / pesan...") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconToggleButton(
                        checked = isRegex,
                        onCheckedChange = { viewModel.isRegexEnabled.value = it }
                    ) {
                        Text(
                            text = ".*",
                            fontWeight = FontWeight.Bold,
                            color = if (isRegex) Color.Cyan else Color.Gray,
                            fontSize = 18.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Min Level: ", fontSize = 12.sp)
                        LogLevel.entries.forEach { level ->
                            Button(
                                onClick = { viewModel.selectedMinLevel.value = level },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (minLevel == level) level.color else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.padding(horizontal = 2.dp).height(28.dp)
                            ) {
                                Text(level.char, fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { autoScrollLocked = !autoScrollLocked }) {
                            Text(
                                text = if (autoScrollLocked) "🔒 Scroll" else "🔓 Freeze",
                                fontSize = 10.sp,
                                color = if (autoScrollLocked) Color.Green else Color.Yellow
                            )
                        }
                        IconButton(onClick = { viewModel.clearAllLogs() }) {
                            Text("🗑️ Clear", fontSize = 10.sp, color = Color.Red)
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().weight(1f).background(Color(0xFF1E1E1E))
        ) {
            items(logs, key = { it.id }) { log ->
                LogItemRow(logLine = log)
            }
        }
    }
}

@Composable
fun LogItemRow(logLine: LogLine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${logLine.timestamp}  ${logLine.pid}-${logLine.tid}  ${logLine.level.char}/${logLine.tag}:",
                color = logLine.level.color,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = logLine.message,
            color = Color(0xFFE2E2E2),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp, top = 1.dp)
        )
        Spacer(
            modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF2D2D2D))
        )
    }
}
