package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.conversationFor
import com.example.data.lastMessageFor
import com.example.data.unreadCount
import com.example.model.ChatAccount
import com.example.model.ChatMessage
import com.example.model.ChatState
import com.example.model.MediaKind
import com.example.model.formatRelative
import com.example.model.formatTime
import com.example.model.handleOf
import com.example.model.initialsOf
import com.example.model.isAccountOnline
import com.example.model.presenceLabel
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalReadingColors
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.OnlineEmeraldText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val EMOJIS = listOf(
    "😀", "😂", "🥰", "😍", "🤔", "👍", "🙌", "🔥",
    "✨", "📚", "☕", "🌙", "❤️", "🎉", "😭", "👀",
)

private data class PendingAttachment(
    val path: String,
    val kind: MediaKind,
)

@Composable
fun ChatScreen(
    chatState: ChatState,
    widthSizeClass: WindowWidthSizeClass,
    onRequireAuth: () -> Unit,
    onBackToHub: () -> Unit,
    onOpenChatSettings: () -> Unit,
    onTouchPresence: () -> Unit,
    onMarkSeen: (String) -> Unit,
    onSendMessage: (String, String, String?, MediaKind?) -> Unit,
    onImportMedia: suspend (Uri, MediaKind) -> String?,
    onNewAudioFile: () -> File,
) {
    val context = LocalContext.current
    val readingColors = LocalReadingColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val session = chatState.session
    val me = chatState.accounts.find { it.id == session }

    LaunchedEffect(session, me) {
        if (session == null || me == null) onRequireAuth()
    }

    var presenceTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(session) {
        if (session != null) {
            onTouchPresence()
            while (true) {
                delay(30_000L)
                onTouchPresence()
                presenceTick++
            }
        }
    }

    if (session == null || me == null) return

    val isWide = widthSizeClass != WindowWidthSizeClass.Compact
    var selectedPeerId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showDirectory by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {
            }
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val mime = context.contentResolver.getType(uri) ?: ""
                val kind = when {
                    mime.startsWith("video") -> MediaKind.VIDEO
                    mime.startsWith("audio") -> MediaKind.AUDIO
                    else -> MediaKind.IMAGE
                }
                val path = onImportMedia(uri, kind)
                if (path != null) {
                    pendingAttachment = PendingAttachment(path, kind)
                }
            }
        }
    }

    fun startRecording() {
        val file = onNewAudioFile()
        try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            recordingFile = file
            recording = true
        } catch (_: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone unavailable. Check mic permissions.")
            }
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        recording = false
        recordingFile?.let { file ->
            if (file.exists() && file.length() > 0L) {
                pendingAttachment = PendingAttachment(file.absolutePath, MediaKind.AUDIO)
            }
        }
        recordingFile = null
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Allow microphone permission to record a voice note.")
            }
        }
    }

    fun toggleRecording() {
        if (recording) {
            stopRecording()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startRecording()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val directoryList = remember(chatState.accounts, searchQuery, session, presenceTick) {
        val q = searchQuery.trim().lowercase()
        chatState.accounts
            .filter { a ->
                q.isEmpty() ||
                    a.username.lowercase().contains(q) ||
                    handleOf(a).contains(q)
            }
            .sortedWith(
                compareByDescending<ChatAccount> { isAccountOnline(it, session) }
                    .thenBy { it.username.lowercase() },
            )
    }

    val peers = remember(chatState.accounts, searchQuery, chatState.messages, session, presenceTick) {
        val q = searchQuery.trim().lowercase()
        chatState.accounts
            .filter { a -> a.id != session && (q.isEmpty() || a.username.lowercase().contains(q)) }
            .sortedByDescending { a -> lastMessageFor(chatState.messages, session, a.id)?.at ?: "" }
    }

    val peer = chatState.accounts.find { it.id == selectedPeerId && it.id != session }
    val thread = remember(chatState.messages, session, selectedPeerId) {
        if (selectedPeerId != null) conversationFor(chatState.messages, session, selectedPeerId!!)
        else emptyList()
    }

    LaunchedEffect(selectedPeerId, chatState.messages.size) {
        selectedPeerId?.let { onMarkSeen(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = readingColors.card,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                ) {
                    ChatSidebarContent(
                        me = me,
                        session = session,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        showDirectory = showDirectory,
                        onToggleDirectory = { showDirectory = !showDirectory },
                        directoryList = directoryList,
                        peers = peers,
                        messages = chatState.messages,
                        selectedPeerId = selectedPeerId,
                        onSelectPeer = { id ->
                            selectedPeerId = id
                            showDirectory = false
                        },
                        onBackToHub = onBackToHub,
                        onOpenSettings = onOpenChatSettings,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = readingColors.card,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    if (peer != null) {
                        ChatConversationPane(
                            peer = peer,
                            session = session,
                            thread = thread,
                            draft = draft,
                            onDraftChange = { draft = it },
                            showEmoji = showEmoji,
                            onToggleEmoji = { showEmoji = !showEmoji },
                            attachment = pendingAttachment,
                            onRemoveAttachment = { pendingAttachment = null },
                            recording = recording,
                            onToggleRecording = ::toggleRecording,
                            onPickMedia = {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                )
                            },
                            onSend = {
                                val text = draft.trim()
                                val att = pendingAttachment
                                if (text.isNotEmpty() || att != null) {
                                    val label = text.ifEmpty {
                                        when (att?.kind) {
                                            MediaKind.VIDEO -> "Sent a video"
                                            MediaKind.AUDIO -> "Voice note"
                                            else -> "Sent a photo"
                                        }
                                    }
                                    onSendMessage(peer.id, label, att?.path, att?.kind)
                                    draft = ""
                                    pendingAttachment = null
                                    showEmoji = false
                                }
                            },
                            onBackToPeers = null,
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Pick an account to start chatting",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Your conversations stay on this device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (peer == null) {
                    ChatSidebarContent(
                        me = me,
                        session = session,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        showDirectory = showDirectory,
                        onToggleDirectory = { showDirectory = !showDirectory },
                        directoryList = directoryList,
                        peers = peers,
                        messages = chatState.messages,
                        selectedPeerId = selectedPeerId,
                        onSelectPeer = { id ->
                            selectedPeerId = id
                            showDirectory = false
                        },
                        onBackToHub = onBackToHub,
                        onOpenSettings = onOpenChatSettings,
                    )
                } else {
                    ChatConversationPane(
                        peer = peer,
                        session = session,
                        thread = thread,
                        draft = draft,
                        onDraftChange = { draft = it },
                        showEmoji = showEmoji,
                        onToggleEmoji = { showEmoji = !showEmoji },
                        attachment = pendingAttachment,
                        onRemoveAttachment = { pendingAttachment = null },
                        recording = recording,
                        onToggleRecording = ::toggleRecording,
                        onPickMedia = {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                            )
                        },
                        onSend = {
                            val text = draft.trim()
                            val att = pendingAttachment
                            if (text.isNotEmpty() || att != null) {
                                val label = text.ifEmpty {
                                    when (att?.kind) {
                                        MediaKind.VIDEO -> "Sent a video"
                                        MediaKind.AUDIO -> "Voice note"
                                        else -> "Sent a photo"
                                    }
                                }
                                onSendMessage(peer.id, label, att?.path, att?.kind)
                                draft = ""
                                pendingAttachment = null
                                showEmoji = false
                            }
                        },
                        onBackToPeers = { selectedPeerId = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(
    name: String,
    dpUri: String?,
    online: Boolean?,
    large: Boolean = false,
) {
    val readingColors = LocalReadingColors.current
    val sizeDp = if (large) 48.dp else 42.dp
    Box(modifier = Modifier.size(sizeDp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(readingColors.brandBrush),
            contentAlignment = Alignment.Center,
        ) {
            if (dpUri != null) {
                AsyncImage(
                    model = File(dpUri),
                    contentDescription = "$name profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = initialsOf(name),
                    fontFamily = DisplayFontFamily,
                    fontSize = if (large) 16.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        if (online != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (online) OnlineEmerald else Color(0xFF9CA3AF))
                    .border(2.dp, readingColors.card, CircleShape),
            )
        }
    }
}

@Composable
private fun ChatSidebarContent(
    me: ChatAccount,
    session: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showDirectory: Boolean,
    onToggleDirectory: () -> Unit,
    directoryList: List<ChatAccount>,
    peers: List<ChatAccount>,
    messages: List<ChatMessage>,
    selectedPeerId: String?,
    onSelectPeer: (String) -> Unit,
    onBackToHub: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val readingColors = LocalReadingColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(readingColors.nightBrush)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(
                    onClick = onBackToHub,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }

                ChatAvatar(name = me.username, dpUri = me.dpUri, online = true, large = true)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = me.username,
                        fontFamily = DisplayFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${handleOf(me)} · Online",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onToggleDirectory,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (showDirectory) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.15f),
                        )
                        .testTag("chat_directory_toggle"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = "View all accounts",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Chat settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search accounts",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (showDirectory) "READERS" else "CHATS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (showDirectory) {
                items(directoryList, key = { it.id }) { a ->
                    val online = isAccountOnline(a, session)
                    val isMe = a.id == session
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isMe) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                else Color.Transparent,
                            )
                            .clickable(enabled = !isMe) { onSelectPeer(a.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ChatAvatar(name = a.username, dpUri = a.dpUri, online = online)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = a.username,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isMe) {
                                    Text(
                                        text = "You",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                text = handleOf(a),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = presenceLabel(a, session),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (online) OnlineEmeraldText else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(peers, key = { it.id }) { p ->
                    val last = lastMessageFor(messages, session, p.id)
                    val count = unreadCount(messages, session, p.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selectedPeerId == p.id) MaterialTheme.colorScheme.secondary
                                else Color.Transparent,
                            )
                            .clickable { onSelectPeer(p.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ChatAvatar(name = p.username, dpUri = p.dpUri, online = isAccountOnline(p, session))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = p.username,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (last != null) {
                                    Text(
                                        text = formatRelative(last.at),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (last != null) {
                                        "${if (last.from == session) "You: " else ""}${last.text}"
                                    } else {
                                        handleOf(p)
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(readingColors.brandBrush),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val empty = if (showDirectory) directoryList.isEmpty() else peers.isEmpty()
            if (empty) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No accounts match “$searchQuery”." else "No other accounts yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatConversationPane(
    peer: ChatAccount,
    session: String,
    thread: List<ChatMessage>,
    draft: String,
    onDraftChange: (String) -> Unit,
    showEmoji: Boolean,
    onToggleEmoji: () -> Unit,
    attachment: PendingAttachment?,
    onRemoveAttachment: () -> Unit,
    recording: Boolean,
    onToggleRecording: () -> Unit,
    onPickMedia: () -> Unit,
    onSend: () -> Unit,
    onBackToPeers: (() -> Unit)?,
) {
    val readingColors = LocalReadingColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(thread.size) {
        if (thread.isNotEmpty()) {
            listState.animateScrollToItem(thread.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Conversation Header
        Surface(color = readingColors.card) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onBackToPeers != null) {
                        IconButton(
                            onClick = onBackToPeers,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to accounts",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    ChatAvatar(
                        name = peer.username,
                        dpUri = peer.dpUri,
                        online = isAccountOnline(peer, session),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peer.username,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${handleOf(peer)} · ${presenceLabel(peer, session)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = readingColors.border)
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(thread, key = { it.id }) { m ->
                val mine = m.from == session
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (mine) 20.dp else 6.dp,
                            bottomEnd = if (mine) 6.dp else 20.dp,
                        ),
                        color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        shadowElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 290.dp),
                    ) {
                        val contentColor = if (mine) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSecondary
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            if (m.mediaUri != null && m.mediaKind == MediaKind.IMAGE) {
                                AsyncImage(
                                    model = File(m.mediaUri),
                                    contentDescription = "Shared photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (m.mediaUri != null && m.mediaKind == MediaKind.VIDEO) {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            setVideoPath(m.mediaUri)
                                            val controller = MediaController(ctx)
                                            controller.setAnchorView(this)
                                            setMediaController(controller)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (m.mediaUri != null && m.mediaKind == MediaKind.AUDIO) {
                                AudioMessagePlayer(
                                    filePath = m.mediaUri,
                                    tint = contentColor,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = m.text,
                                fontSize = 14.sp,
                                color = contentColor,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = formatTime(m.at),
                                    fontSize = 10.sp,
                                    color = contentColor.copy(alpha = 0.72f),
                                )
                                if (mine) {
                                    Icon(
                                        imageVector = if (m.seen) Icons.Filled.DoneAll else Icons.Filled.Check,
                                        contentDescription = if (m.seen) "Seen" else "Sent",
                                        tint = contentColor.copy(alpha = 0.72f),
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEmoji) {
            HorizontalDivider(thickness = 0.5.dp, color = readingColors.border)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(readingColors.card)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EMOJIS.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDraftChange(draft + emoji) }
                            .padding(6.dp),
                    )
                }
            }
        }

        if (attachment != null) {
            HorizontalDivider(thickness = 0.5.dp, color = readingColors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(readingColors.card)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (attachment.kind) {
                    MediaKind.IMAGE -> {
                        AsyncImage(
                            model = File(attachment.path),
                            contentDescription = "Attachment preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    MediaKind.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = null)
                        }
                    }
                    MediaKind.AUDIO -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null)
                        }
                    }
                }

                Text(
                    text = when (attachment.kind) {
                        MediaKind.AUDIO -> "Voice note ready to send"
                        MediaKind.VIDEO -> "Video ready to send"
                        MediaKind.IMAGE -> "Photo ready to send"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = onRemoveAttachment,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove attachment",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // Composer bar
        Surface(color = readingColors.card) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                HorizontalDivider(thickness = 0.5.dp, color = readingColors.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(
                        onClick = onToggleEmoji,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEmotions,
                            contentDescription = "Insert emoji",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onPickMedia,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Attach photo or video",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onToggleRecording,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (recording) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.secondary,
                            ),
                    ) {
                        Icon(
                            imageVector = if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (recording) "Stop recording" else "Record a voice note",
                            tint = if (recording) MaterialTheme.colorScheme.onError
                            else MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, readingColors.border, RoundedCornerShape(20.dp))
                            .background(readingColors.card)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Message ${peer.username}…",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(readingColors.brandBrush),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioMessagePlayer(
    filePath: String,
    tint: Color,
) {
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(filePath) {
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
                    try {
                        val mp = player ?: MediaPlayer().apply {
                            setDataSource(filePath)
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
