package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.LibraryState
import com.example.model.LineSpacing
import com.example.model.ReaderFont
import com.example.model.ReaderTheme
import com.example.model.ReaderWidth
import com.example.model.progressPercent
import com.example.ui.components.EmptyState
import com.example.ui.components.PillChip
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalReadingColors
import com.example.ui.theme.ReaderDarkBg
import com.example.ui.theme.ReaderDarkFg
import com.example.ui.theme.ReaderLightBg
import com.example.ui.theme.ReaderLightFg
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaFg
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book?,
    initialPage: Int?,
    libraryState: LibraryState,
    onBack: () -> Unit,
    onExploreClick: () -> Unit,
    onFinishToLibrary: () -> Unit,
    onSaveProgress: (String, Int, Int) -> Unit,
    onMarkCompleted: (String) -> Unit,
    onToggleBookmark: (String, Int) -> Unit,
    onSetTheme: (ReaderTheme) -> Unit,
    onSetFont: (ReaderFont) -> Unit,
    onSetFontSize: (Int) -> Unit,
    onSetLineSpacing: (LineSpacing) -> Unit,
    onSetWidth: (ReaderWidth) -> Unit,
    onSetSpeechRate: (Float) -> Unit,
) {
    if (book == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Book not found",
                description = "This book isn't in the library. Try exploring our collection instead.",
                actionLabel = "Explore books",
                onAction = onExploreClick,
            )
        }
        return
    }

    val context = LocalContext.current
    val pages = book.content
    val total = pages.size
    val prefs = libraryState.reader
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var restored by remember(book.id) { mutableStateOf(false) }
    var page by remember(book.id) {
        val start = when {
            initialPage != null && total > 0 -> initialPage.coerceIn(0, total - 1)
            else -> {
                val saved = libraryState.progress[book.id]?.page ?: 0
                if (total > 0) saved.coerceIn(0, total - 1) else 0
            }
        }
        mutableIntStateOf(start)
    }

    LaunchedEffect(book.id, initialPage) {
        if (!restored && total > 0) {
            val target = when {
                initialPage != null -> initialPage.coerceIn(0, total - 1)
                else -> (libraryState.progress[book.id]?.page ?: 0).coerceIn(0, total - 1)
            }
            page = target
            restored = true
        }
    }

    LaunchedEffect(book.id, page, total, restored) {
        if (restored && total > 0) {
            onSaveProgress(book.id, page, total)
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                instance?.language = Locale.getDefault()
            }
        }
        instance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                speaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speaking = false
            }
        })
        instance
    }

    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun stopSpeech() {
        tts.stop()
        speaking = false
    }

    fun toggleSpeech() {
        if (!ttsReady || pages.isEmpty()) return
        if (speaking) {
            stopSpeech()
            return
        }
        tts.setSpeechRate(prefs.speechRate)
        tts.speak(pages[page], TextToSpeech.QUEUE_FLUSH, null, "page_$page")
        speaking = true
    }

    fun go(delta: Int) {
        if (total == 0) return
        stopSpeech()
        val next = (page + delta).coerceIn(0, total - 1)
        page = next
        scope.launch { scrollState.scrollTo(0) }
    }

    val (bgColor, fgColor) = when (prefs.theme) {
        ReaderTheme.LIGHT -> ReaderLightBg to ReaderLightFg
        ReaderTheme.SEPIA -> ReaderSepiaBg to ReaderSepiaFg
        ReaderTheme.DARK -> ReaderDarkBg to ReaderDarkFg
    }

    val percent = progressPercent(page, total)
    val bookmarked = libraryState.bookmarks.any { it.bookId == book.id && it.page == page }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // Sticky Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.92f))
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 768.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(
                    onClick = {
                        stopSpeech()
                        onBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(fgColor.copy(alpha = 0.1f))
                        .testTag("reader_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to book",
                        tint = fgColor,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = book.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = fgColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Page ${page + 1} of $total · $percent%",
                        fontSize = 11.sp,
                        color = fgColor.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onToggleBookmark(book.id, page) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(fgColor.copy(alpha = 0.1f))
                            .testTag("reader_bookmark_button"),
                    ) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark this page",
                            tint = fgColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = ::toggleSpeech,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(fgColor.copy(alpha = 0.1f))
                            .testTag("reader_tts_button"),
                    ) {
                        Icon(
                            imageVector = if (speaking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (speaking) "Stop reading aloud" else "Read aloud",
                            tint = fgColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(fgColor.copy(alpha = 0.1f))
                            .testTag("reader_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Reader settings",
                            tint = fgColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(fgColor.copy(alpha = 0.1f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((percent.coerceIn(0, 100) / 100f))
                        .background(fgColor.copy(alpha = 0.55f)),
                )
            }
        }

        // Main Reading Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = prefs.width.maxWidthDp.dp)
                    .fillMaxWidth(),
            ) {
                val pageText = pages.getOrElse(page) { "" }
                Text(
                    text = pageText,
                    color = fgColor,
                    fontFamily = if (prefs.font == ReaderFont.SERIF) DisplayFontFamily else BodyFontFamily,
                    fontSize = prefs.fontSize.sp,
                    lineHeight = (prefs.fontSize * prefs.lineSpacing.multiplier).sp,
                )

                Spacer(modifier = Modifier.height(36.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val prevEnabled = page > 0
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = prevEnabled) { go(-1) }
                            .testTag("reader_previous_button"),
                        shape = CircleShape,
                        color = fgColor.copy(alpha = if (prevEnabled) 0.12f else 0.05f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = fgColor.copy(alpha = if (prevEnabled) 1f else 0.4f),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Previous",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = fgColor.copy(alpha = if (prevEnabled) 1f else 0.4f),
                            )
                        }
                    }

                    if (page >= total - 1) {
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    stopSpeech()
                                    onMarkCompleted(book.id)
                                    onFinishToLibrary()
                                }
                                .testTag("reader_finish_button"),
                            shape = CircleShape,
                            color = fgColor.copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = "Finish",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = fgColor,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { go(1) }
                                .testTag("reader_next_button"),
                            shape = CircleShape,
                            color = fgColor.copy(alpha = 0.18f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Next",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fgColor,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = fgColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        val readingColors = LocalReadingColors.current
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Reading settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    IconButton(
                        onClick = { showSettings = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close settings",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                SettingField(label = "Theme") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderTheme.entries.forEach { t ->
                            PillChip(
                                label = t.label,
                                selected = prefs.theme == t,
                                onClick = { onSetTheme(t) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                SettingField(label = "Font") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderFont.entries.forEach { f ->
                            PillChip(
                                label = f.label,
                                selected = prefs.font == f,
                                onClick = { onSetFont(f) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                SettingField(label = "Font size — ${prefs.fontSize}sp") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconButton(
                            onClick = { onSetFontSize((prefs.fontSize - 1).coerceAtLeast(14)) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Decrease font size",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        ) {
                            val ratio = ((prefs.fontSize - 14).toFloat() / 14f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio)
                                    .clip(CircleShape)
                                    .background(readingColors.brandBrush),
                            )
                        }
                        IconButton(
                            onClick = { onSetFontSize((prefs.fontSize + 1).coerceAtMost(28)) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Increase font size",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                SettingField(label = "Line spacing") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LineSpacing.entries.forEach { s ->
                            PillChip(
                                label = s.label,
                                selected = prefs.lineSpacing == s,
                                onClick = { onSetLineSpacing(s) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                SettingField(label = "Page width") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderWidth.entries.forEach { w ->
                            PillChip(
                                label = w.label,
                                selected = prefs.width == w,
                                onClick = { onSetWidth(w) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                SettingField(label = "Read-aloud speed — ${String.format(Locale.US, "%.1fx", prefs.speechRate)}") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.8f to "0.8x", 1f to "1x", 1.3f to "1.3x").forEach { (rate, label) ->
                            PillChip(
                                label = label,
                                selected = prefs.speechRate == rate,
                                onClick = { onSetSpeechRate(rate) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
