package com.soniq.player

import android.content.Intent
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private val Bg = Color(0xFF09090D)
private val Panel = Color(0xFF171720)
private val Accent = Color(0xFFB18CFF)

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var eq: Equalizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        setContent {
            SoniqScreen(
                onPlayUri = { uri -> play(uri) },
                onToggle = {
                    player?.let { if (it.isPlaying) it.pause() else it.play() }
                },
                isPlaying = player?.isPlaying == true,
                onEqualizer = { enabled ->
                    try {
                        if (enabled && eq == null) {
                            eq = Equalizer(0, player?.audioSessionId ?: 0).apply { enabled = true }
                        } else if (!enabled) {
                            eq?.release()
                            eq = null
                        }
                    } catch (_: Exception) { }
                }
            )
        }
    }

    private fun play(uri: Uri) {
        player?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
    }

    override fun onDestroy() {
        eq?.release()
        player?.release()
        player = null
        super.onDestroy()
    }
}

@Composable
fun SoniqScreen(
    onPlayUri: (Uri) -> Unit,
    onToggle: () -> Unit,
    isPlaying: Boolean,
    onEqualizer: (Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var eqOn by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready to play") }
    var timer by remember { mutableStateOf("Off") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPlayUri(uri)
            status = "Playing selected audio"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg, surface = Panel, primary = Accent,
            onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF191326), Bg, Bg)))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SONIQ", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("FEEL EVERY BEAT", color = Accent, fontSize = 10.sp, letterSpacing = 3.sp)
                }
                Icon(Icons.Default.Headphones, null, tint = Accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.fillMaxWidth().height(210.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF4A2B70), Color(0xFF171720))),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Album, null, tint = Accent, modifier = Modifier.size(78.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("YOUR SOUND. YOUR WORLD.", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(status, color = Color.LightGray, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Now Playing", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("OFFLINE + ONLINE", color = Accent, fontSize = 10.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Choose song")
                }
                FilledTonalButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPlaying) "Pause" else "Play")
                }
            }
            Spacer(Modifier.height(20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Online stream", fontWeight = FontWeight.Bold)
                    Text("Paste a direct audio stream URL you have permission to use.", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true, placeholder = { Text("https://example.com/audio.mp3") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val parsed = runCatching { Uri.parse(url.trim()) }.getOrNull()
                            if (parsed != null && (parsed.scheme == "https" || parsed.scheme == "http")) {
                                onPlayUri(parsed); status = "Streaming online audio"
                            } else status = "Enter a valid audio URL"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Stream audio") }
                }
            }
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Equalizer", fontWeight = FontWeight.Bold)
                            Text("Use your device audio effects", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Switch(checked = eqOn, onCheckedChange = {
                            eqOn = it; onEqualizer(it)
                        })
                    }
                    Text("Bass / Treble / 3D presets can be expanded in the next build.", color = Accent, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FeatureChip("♫  Lyrics")
                FeatureChip("◉  Vocal tools")
                FeatureChip("◷  Sleep timer")
            }
            Spacer(Modifier.weight(1f))
            Text("SONIQ  •  Prototype 0.1", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun FeatureChip(label: String) {
    Surface(color = Color(0xFF211B2D), shape = RoundedCornerShape(50)) {
        Text(label, color = Accent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), fontSize = 10.sp)
    }
}
