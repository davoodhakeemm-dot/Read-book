package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.SocialUiState
import com.example.SocialViewModel
import com.example.data.MediaEncodeResult
import com.example.model.BlockedUserDoc
import com.example.model.CloudCallSignal
import com.example.model.CloudChatMessage
import com.example.model.CloudConversation
import com.example.model.CloudUserProfile
import com.example.model.MediaKind
import com.example.model.cloudThreadId
import com.example.model.initialsOf
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.SecretPalette
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CLOUD_EMOJIS = listOf(
    "😀", "😂", "🥰", "😍", "🤔", "👍", "🙌", "🔥",
    "✨", "📚", "☕", "🌙", "❤️", "🎉", "😭", "👀",
)

private data class CloudPendingAttachment(
    val dataUri: String,
    val kind: MediaKind,
)

private data class PreviewTarget(
    val source: String,
    val kind: MediaKind,
)

@Composable
fun CloudChatTab(
    palette: SecretPalette,
    me: CloudUserProfile,
    directoryState: SocialUiState<List<CloudUserProfile>>,
    conversations: List<CloudConversation>,
    blockedUsers: List<BlockedUserDoc>,
    selectedPeerId: String?,
    threadMessages: List<CloudChatMessage>,
    threadCalls: List<CloudCallSignal>,
    widthSizeClass: WindowWidthSizeClass,
    socialViewModel: SocialViewModel,
    onShowMessage: (String) -> Unit,
    onConversationOpenChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isWide = widthSizeClass != WindowWidthSizeClass.Compact

    // Ask notification permission cleanly when user opens Chat on Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(selectedPeerId, isWide) {
        onConversationOpenChanged(!isWide && selectedPeerId != null)
    }

    if (!isWide && selectedPeerId != null) {
        BackHandler {
            socialViewModel.selectPeer(null)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    var pendingAttachment by remember { mutableStateOf<CloudPendingAttachment?>(null) }
    var previewTarget by remember { mutableStateOf<PreviewTarget?>(null) }

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
                when (val encoded = socialViewModel.encodePickedMedia(uri, kind)) {
                    is MediaEncodeResult.Ready -> pendingAttachment = CloudPendingAttachment(encoded.dataUri, encoded.kind)
                    is MediaEncodeResult.LocalOnlyWarning -> onShowMessage(encoded.reason)
                    is MediaEncodeResult.Failure -> onShowMessage(encoded.message)
                }
            }
        }
    }

    fun startRecording() {
        val dir = File(context.cacheDir, "voice_rec").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
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
            onShowMessage("Microphone unavailable. Check mic permissions.")
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
        val file = recordingFile
        recordingFile = null
        if (file != null) {
            scope.launch {
                when (val encoded = socialViewModel.encodeAudioRecording(file)) {
                    is MediaEncodeResult.Ready -> pendingAttachment = CloudPendingAttachment(encoded.dataUri, MediaKind.AUDIO)
                    is MediaEncodeResult.LocalOnlyWarning -> onShowMessage(encoded.reason)
                    is MediaEncodeResult.Failure -> onShowMessage(encoded.message)
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startRecording()
        else onShowMessage("Allow microphone permission to record a voice note.")
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
        if (granted) startRecording()
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val allReaders = remember(directoryState, me.accountId) {
        when (directoryState) {
            is SocialUiState.Success -> directoryState.data.filter { it.accountId != me.accountId }
            else -> emptyList()
        }
    }

    val filteredReaders = remember(allReaders, searchQuery, conversations) {
        val q = searchQuery.trim().lowercase()
        val lastAtByPeer = conversations.associate { conv ->
            val peerId = conv.memberAccountIds.firstOrNull { it != me.accountId } ?: ""
            peerId to (conv.lastMessageAt?.toDate()?.time ?: 0L)
        }
        allReaders
            .filter { q.isEmpty() || it.displayName.lowercase().contains(q) }
            .sortedByDescending { lastAtByPeer[it.accountId] ?: 0L }
    }

    val blockedIds = remember(blockedUsers) { blockedUsers.map { it.blockedAccountId }.toSet() }
    val selectedPeer = allReaders.find { it.accountId == selectedPeerId }

    LaunchedEffect(selectedPeerId, threadMessages.size) {
        if (selectedPeerId != null) {
            socialViewModel.markThreadSeen(selectedPeerId)
        }
    }

    val sendCurrentMessage: (CloudUserProfile) -> Unit = { peer ->
        val txt = draft.trim()
        val att = pendingAttachment
        if (txt.isNotEmpty() || att != null) {
            socialViewModel.sendMessage(
                peerProfile = peer,
                text = txt,
                mediaDataUri = att?.dataUri,
                mediaKind = att?.kind,
            ) { res ->
                res.onSuccess {
                    draft = ""
                    pendingAttachment = null
                    showEmoji = false
                }.onFailure { err ->
                    onShowMessage(err.localizedMessage ?: "Message failed.")
                }
            }
        }
    }

    if (isWide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .border(1.dp, palette.border, RoundedCornerShape(24.dp)),
            ) {
                ChatInboxPane(
                    palette = palette,
                    me = me,
                    readers = filteredReaders,
                    conversations = conversations,
                    blockedIds = blockedIds,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedPeerId = selectedPeerId,
                    onSelectPeer = { socialViewModel.selectPeer(it) },
                )
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, palette.border, RoundedCornerShape(24.dp)),
            ) {
                if (selectedPeer != null) {
                    CloudConversationPane(
                        palette = palette,
                        me = me,
                        peer = selectedPeer,
                        messages = threadMessages,
                        calls = threadCalls,
                        isBlockedByMe = selectedPeer.accountId in blockedIds,
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
                        onSend = { sendCurrentMessage(selectedPeer) },
                        onPreviewMedia = { src, kind -> previewTarget = PreviewTarget(src, kind) },
                        onDownloadMedia = { src, kind ->
                            scope.launch {
                                val res = socialViewModel.downloadMediaToDevice(src, kind)
                                onShowMessage(res.getOrElse { it.localizedMessage ?: "Download failed." })
                            }
                        },
                        onStartCall = { callType ->
                            socialViewModel.initiateCall(selectedPeer, callType) { res ->
                                res.onFailure { onShowMessage(it.localizedMessage ?: "Call blocked.") }
                            }
                        },
                        onUpdateCall = { callId, status ->
                            socialViewModel.updateCallStatus(cloudThreadId(me.accountId, selectedPeer.accountId), callId, status)
                        },
                        onToggleBlock = {
                            if (selectedPeer.accountId in blockedIds) {
                                socialViewModel.unblockUser(selectedPeer.accountId) { res ->
                                    res.onSuccess { onShowMessage("Unblocked ${selectedPeer.displayName}") }
                                }
                            } else {
                                socialViewModel.blockUser(selectedPeer.accountId, selectedPeer.displayName) { res ->
                                    res.onSuccess { onShowMessage("Blocked ${selectedPeer.displayName}. Messaging & calling disabled.") }
                                }
                            }
                        },
                        onBack = null,
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text(
                                text = "Direct Messages",
                                fontFamily = DisplayFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onSurface,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Select a reader from the list to send messages, photos, videos, or start a call.",
                                fontSize = 14.sp,
                                color = palette.onSurfaceMuted,
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
                .background(palette.background),
        ) {
            if (selectedPeer == null) {
                ChatInboxPane(
                    palette = palette,
                    me = me,
                    readers = filteredReaders,
                    conversations = conversations,
                    blockedIds = blockedIds,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedPeerId = selectedPeerId,
                    onSelectPeer = { socialViewModel.selectPeer(it) },
                )
            } else {
                CloudConversationPane(
                    palette = palette,
                    me = me,
                    peer = selectedPeer,
                    messages = threadMessages,
                    calls = threadCalls,
                    isBlockedByMe = selectedPeer.accountId in blockedIds,
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
                    onSend = { sendCurrentMessage(selectedPeer) },
                    onPreviewMedia = { src, kind -> previewTarget = PreviewTarget(src, kind) },
                    onDownloadMedia = { src, kind ->
                        scope.launch {
                            val res = socialViewModel.downloadMediaToDevice(src, kind)
                            onShowMessage(res.getOrElse { it.localizedMessage ?: "Download failed." })
                        }
                    },
                    onStartCall = { callType ->
                        socialViewModel.initiateCall(selectedPeer, callType) { res ->
                            res.onFailure { onShowMessage(it.localizedMessage ?: "Call blocked.") }
                        }
                    },
                    onUpdateCall = { callId, status ->
                        socialViewModel.updateCallStatus(cloudThreadId(me.accountId, selectedPeer.accountId), callId, status)
                    },
                    onToggleBlock = {
                        if (selectedPeer.accountId in blockedIds) {
                            socialViewModel.unblockUser(selectedPeer.accountId) { res ->
                                res.onSuccess { onShowMessage("Unblocked ${selectedPeer.displayName}") }
                            }
                        } else {
                            socialViewModel.blockUser(selectedPeer.accountId, selectedPeer.displayName) { res ->
                                res.onSuccess { onShowMessage("Blocked ${selectedPeer.displayName}") }
                            }
                        }
                    },
                    onBack = { socialViewModel.selectPeer(null) },
                )
            }
        }
    }

    previewTarget?.let { target ->
        FullScreenMediaPreviewDialog(
            source = target.source,
            kind = target.kind,
            onDismiss = { previewTarget = null },
            onDownload = { src, kind -> socialViewModel.downloadMediaToDevice(src, kind) },
            onMessageFeedback = onShowMessage,
        )
    }
}

@Composable
private fun ChatInboxPane(
    palette: SecretPalette,
    me: CloudUserProfile,
    readers: List<CloudUserProfile>,
    conversations: List<CloudConversation>,
    blockedIds: Set<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedPeerId: String?,
    onSelectPeer: (String) -> Unit,
) {
    val convByPeer = remember(conversations, me.accountId) {
        conversations.associateBy { conv ->
            conv.memberAccountIds.firstOrNull { it != me.accountId } ?: ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.headerBrush)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StoryRingAvatar(palette = palette, name = me.displayName, dpUrl = me.dpUrl, online = true, size = 46)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = me.displayName,
                        fontFamily = DisplayFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Direct Messages",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(16.dp))
                Box(Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search readers", fontSize = 13.sp, color = Color.White.copy(alpha = 0.68f))
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("chat_search_input"),
                    )
                }
            }
        }

        // NOTE: Intentionally ZERO member-count or public user-count badges per privacy spec!
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    text = "MESSAGES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            items(readers, key = { it.accountId }) { peer ->
                val conv = convByPeer[peer.accountId]
                val isBlocked = peer.accountId in blockedIds
                val isSelected = selectedPeerId == peer.accountId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) palette.surfaceVariant else Color.Transparent)
                        .clickable { onSelectPeer(peer.accountId) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("chat_peer_${peer.accountId}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StoryRingAvatar(
                        palette = palette,
                        name = peer.displayName,
                        dpUrl = peer.dpUrl,
                        online = peer.online && !isBlocked,
                        size = 46,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = peer.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (conv?.lastMessageAt != null) {
                                Text(
                                    text = formatCloudTime(conv.lastMessageAt.toDate()),
                                    fontSize = 11.sp,
                                    color = palette.onSurfaceMuted,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when {
                                isBlocked -> "Blocked"
                                conv != null -> {
                                    val prefix = if (conv.lastSenderAccountId == me.accountId) "You: " else ""
                                    "$prefix${conv.lastMessageText}"
                                }
                                !peer.bio.isNullOrBlank() -> peer.bio
                                else -> "Tap to start chatting"
                            },
                            fontSize = 12.sp,
                            color = if (isBlocked) palette.danger else palette.onSurfaceMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (readers.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching readers found." else "No conversations yet.",
                        color = palette.onSurfaceMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CloudConversationPane(
    palette: SecretPalette,
    me: CloudUserProfile,
    peer: CloudUserProfile,
    messages: List<CloudChatMessage>,
    calls: List<CloudCallSignal>,
    isBlockedByMe: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    showEmoji: Boolean,
    onToggleEmoji: () -> Unit,
    attachment: CloudPendingAttachment?,
    onRemoveAttachment: () -> Unit,
    recording: Boolean,
    onToggleRecording: () -> Unit,
    onPickMedia: () -> Unit,
    onSend: () -> Unit,
    onPreviewMedia: (String, MediaKind) -> Unit,
    onDownloadMedia: (String, MediaKind) -> Unit,
    onStartCall: (String) -> Unit,
    onUpdateCall: (String, String) -> Unit,
    onToggleBlock: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, imeBottom) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val activeCall = calls.firstOrNull { it.status == "RINGING" || it.status == "ACCEPTED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .imePadding(),
    ) {
        // Conversation Top Bar
        Surface(color = palette.surface) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.onSurface)
                        }
                    }
                    StoryRingAvatar(
                        palette = palette,
                        name = peer.displayName,
                        dpUrl = peer.dpUrl,
                        online = peer.online && !isBlockedByMe,
                        size = 40,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peer.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when {
                                isBlockedByMe -> "Blocked"
                                peer.online -> "Active now"
                                else -> "Offline"
                            },
                            fontSize = 11.sp,
                            color = if (isBlockedByMe) palette.danger else palette.onSurfaceMuted,
                        )
                    }

                    IconButton(
                        onClick = { onStartCall("AUDIO") },
                        enabled = !isBlockedByMe,
                        modifier = Modifier.testTag("call_audio_button"),
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Audio call",
                            tint = if (isBlockedByMe) palette.onSurfaceMuted.copy(alpha = 0.4f) else palette.primary,
                        )
                    }
                    IconButton(
                        onClick = { onStartCall("VIDEO") },
                        enabled = !isBlockedByMe,
                        modifier = Modifier.testTag("call_video_button"),
                    ) {
                        Icon(
                            Icons.Filled.Videocam,
                            contentDescription = "Video call",
                            tint = if (isBlockedByMe) palette.onSurfaceMuted.copy(alpha = 0.4f) else palette.primary,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("chat_peer_menu")) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Chat options", tint = palette.onSurface)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isBlockedByMe) "Unblock ${peer.displayName}" else "Block ${peer.displayName}",
                                        color = if (isBlockedByMe) palette.primary else palette.danger,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Block,
                                        contentDescription = null,
                                        tint = if (isBlockedByMe) palette.primary else palette.danger,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleBlock()
                                },
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.7.dp, color = palette.border)
            }
        }

        // Active Call Banner (Ringing / Accepted)
        if (activeCall != null) {
            val isOutgoing = activeCall.senderAccountId == me.accountId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.headerBrush)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeCall.callType == "VIDEO") "Video Call" else "Voice Call",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = when {
                            activeCall.status == "ACCEPTED" -> "Connected with ${peer.displayName}"
                            isOutgoing -> "Calling ${peer.displayName}…"
                            else -> "Incoming call from ${peer.displayName}"
                        },
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isOutgoing && activeCall.status == "RINGING") {
                        Surface(
                            shape = CircleShape,
                            color = OnlineEmerald,
                            modifier = Modifier.clickable { onUpdateCall(activeCall.id, "ACCEPTED") },
                        ) {
                            Text(
                                "Accept",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = { onUpdateCall(activeCall.id, if (activeCall.status == "RINGING" && !isOutgoing) "DECLINED" else "ENDED") },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626)),
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "End call", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { m ->
                val mine = m.senderAccountId == me.accountId
                val kind = m.kindOrNull()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                ) {
                    val bubbleShape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (mine) 20.dp else 6.dp,
                        bottomEnd = if (mine) 6.dp else 20.dp,
                    )
                    val contentColor = if (mine) palette.onPrimary else palette.receivedBubbleFg

                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .clip(bubbleShape)
                            .then(
                                if (mine) Modifier.background(palette.sentBubbleBrush)
                                else Modifier.background(palette.receivedBubbleBg),
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Column {
                            if (!m.mediaDataUri.isNullOrBlank() && kind == MediaKind.IMAGE) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onPreviewMedia(m.mediaDataUri, MediaKind.IMAGE) },
                                ) {
                                    AsyncImage(
                                        model = m.mediaDataUri,
                                        contentDescription = "Photo message",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp),
                                    )
                                    IconButton(
                                        onClick = { onDownloadMedia(m.mediaDataUri, MediaKind.IMAGE) },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.55f)),
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = "Download photo", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }

                            if (!m.mediaDataUri.isNullOrBlank() && kind == MediaKind.VIDEO) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .clickable { onPreviewMedia(m.mediaDataUri, MediaKind.VIDEO) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Play video", tint = contentColor, modifier = Modifier.size(32.dp))
                                    Text("Tap to play video", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { onDownloadMedia(m.mediaDataUri, MediaKind.VIDEO) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = "Download video", tint = contentColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }

                            if (!m.mediaDataUri.isNullOrBlank() && kind == MediaKind.AUDIO) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    CloudAudioPlayer(source = m.mediaDataUri, tint = contentColor)
                                    IconButton(
                                        onClick = { onDownloadMedia(m.mediaDataUri, MediaKind.AUDIO) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = "Download audio", tint = contentColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }

                            if (m.text.isNotBlank()) {
                                Text(text = m.text, fontSize = 14.sp, color = contentColor)
                                Spacer(Modifier.height(4.dp))
                            }

                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val timeStr = m.createdAt?.toDate()?.let(::formatCloudTime) ?: ""
                                if (timeStr.isNotEmpty()) {
                                    Text(text = timeStr, fontSize = 10.sp, color = contentColor.copy(alpha = 0.74f))
                                }
                                if (mine) {
                                    Icon(
                                        imageVector = if (m.seen) Icons.Filled.DoneAll else Icons.Filled.Check,
                                        contentDescription = if (m.seen) "Read" else "Sent",
                                        tint = if (m.seen) Color.White else contentColor.copy(alpha = 0.72f),
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isBlockedByMe) {
            Surface(color = palette.surface) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(thickness = 0.7.dp, color = palette.border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "You blocked ${peer.displayName}. Unblock to chat or call.",
                            fontSize = 13.sp,
                            color = palette.danger,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Unblock",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onToggleBlock)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        } else {
            if (showEmoji) {
                HorizontalDivider(thickness = 0.6.dp, color = palette.border)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CLOUD_EMOJIS.forEach { emoji ->
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
                HorizontalDivider(thickness = 0.6.dp, color = palette.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (attachment.kind) {
                        MediaKind.IMAGE -> AsyncImage(
                            model = attachment.dataUri,
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        MediaKind.VIDEO -> Icon(Icons.Filled.Videocam, contentDescription = null, tint = palette.primary)
                        MediaKind.AUDIO -> Icon(Icons.Filled.Mic, contentDescription = null, tint = palette.primary)
                    }
                    Text(
                        text = when (attachment.kind) {
                            MediaKind.IMAGE -> "Photo attached"
                            MediaKind.VIDEO -> "Video attached"
                            MediaKind.AUDIO -> "Voice note attached"
                        },
                        fontSize = 13.sp,
                        color = palette.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRemoveAttachment) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove attachment", tint = palette.onSurfaceMuted)
                    }
                }
            }

            // Keyboard-safe composer (imePadding already applied on outer Column; navigationBarsPadding below when IME closed)
            Surface(color = palette.surface) {
                Column(modifier = Modifier.then(if (imeBottom == 0) Modifier.navigationBarsPadding() else Modifier)) {
                    HorizontalDivider(thickness = 0.7.dp, color = palette.border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IconButton(
                            onClick = onToggleEmoji,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(palette.surfaceVariant),
                        ) {
                            Icon(Icons.Filled.EmojiEmotions, contentDescription = "Emoji", tint = palette.onSurface, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onPickMedia,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(palette.surfaceVariant),
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Attach photo or video", tint = palette.onSurface, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onToggleRecording,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (recording) palette.danger else palette.surfaceVariant),
                        ) {
                            Icon(
                                imageVector = if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (recording) "Stop recording" else "Record voice note",
                                tint = if (recording) Color.White else palette.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp, max = 110.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(22.dp))
                                .background(palette.background)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = "Message ${peer.displayName}…",
                                    fontSize = 14.sp,
                                    color = palette.onSurfaceMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = onDraftChange,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.onSurface),
                                cursorBrush = SolidColor(palette.primary),
                                modifier = Modifier.fillMaxWidth().testTag("chat_message_input"),
                            )
                        }

                        IconButton(
                            onClick = onSend,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(palette.fabBrush)
                                .testTag("chat_send_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryRingAvatar(
    palette: SecretPalette,
    name: String,
    dpUrl: String?,
    online: Boolean?,
    size: Int = 48,
) {
    Box(modifier = Modifier.size(size.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.2.dp, palette.storyRingBrush, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(palette.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!dpUrl.isNullOrBlank()) {
                AsyncImage(
                    model = dpUrl,
                    contentDescription = "$name photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = initialsOf(name),
                    fontFamily = DisplayFontFamily,
                    fontSize = (size * 0.34f).sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurface,
                )
            }
        }
        if (online == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.25f).dp.coerceAtLeast(10.dp))
                    .clip(CircleShape)
                    .background(OnlineEmerald)
                    .border(2.dp, palette.surface, CircleShape),
            )
        }
    }
}

private fun formatCloudTime(date: Date): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
