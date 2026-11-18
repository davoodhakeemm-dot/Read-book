package com.example.ui.screens

import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.MediaHelper
import com.example.model.MediaKind
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FullScreenMediaPreviewDialog(
    source: String,
    kind: MediaKind,
    onDismiss: () -> Unit,
    onDownload: suspend (String, MediaKind) -> Result<String>,
    onMessageFeedback: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var playableVideoFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(source, kind) {
        if (kind == MediaKind.VIDEO) {
            playableVideoFile = MediaHelper.materializePlayableFile(context, source, "mp4")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (kind) {
                    MediaKind.IMAGE -> {
                        AsyncImage(
                            model = source,
                            contentDescription = "Full photo preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    MediaKind.VIDEO -> {
                        val vFile = playableVideoFile
                        if (vFile != null) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(vFile.absolutePath)
                                        val controller = MediaController(ctx)
                                        controller.setAnchorView(this)
                                        setMediaController(controller)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(Modifier.height(10.dp))
                                Text("Preparing video preview…", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    MediaKind.AUDIO -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                            CloudAudioPlayer(source = source, tint = Color.White)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = Color.White)
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !downloading) {
                            downloading = true
                            scope.launch {
                                val res = onDownload(source, kind)
                                downloading = false
                                onMessageFeedback(
                                    res.getOrElse { it.localizedMessage ?: "Download failed." },
                                )
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (downloading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Download, contentDescription = "Download media", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = if (downloading) "Saving…" else "Download",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloudAudioPlayer(
    source: String,
    tint: Color,
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var localFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(source) {
        localFile = MediaHelper.materializePlayableFile(context, source, "m4a")
    }

    DisposableEffect(source) {
        onDispose {
            try {
                player?.stop()
                player?.release()
            } catch (_: Exception) {
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .clickable {
                if (playing) {
                    player?.pause()
                    playing = false
                } else {
                    val file = localFile ?: return@clickable
                    try {
                        val mp = player ?: MediaPlayer().apply {
                            setDataSource(file.absolutePath)
                            prepare()
                            setOnCompletionListener { playing = false }
                        }
                        player = mp
                        mp.start()
                        playing = true
                    } catch (_: Exception) {
                        playing = false
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Pause voice note" else "Play voice note",
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = if (playing) "Playing…" else "Voice note",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}
