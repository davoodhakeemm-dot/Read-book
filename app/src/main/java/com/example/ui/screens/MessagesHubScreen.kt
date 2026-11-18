package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatState
import com.example.model.initialsOf
import com.example.ui.components.TripleTapExitContainer
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalReadingColors
import java.io.File

@Composable
fun MessagesHubScreen(
    chatState: ChatState,
    onRequireAuth: () -> Unit,
    onBackHome: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenChatSettings: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val me = chatState.accounts.find { it.id == chatState.session }

    LaunchedEffect(chatState.session, me) {
        if (chatState.session == null || me == null) {
            onRequireAuth()
        }
    }

    if (me == null) return

    TripleTapExitContainer(
        onTripleTapExit = onBackHome,
        modifier = Modifier.background(readingColors.nightBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onBackHome)
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Back to Reading Corner",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(readingColors.brandBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (me.dpUri != null) {
                            AsyncImage(
                                model = File(me.dpUri),
                                contentDescription = "${me.username} profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = initialsOf(me.username),
                                fontFamily = DisplayFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = me.username,
                            fontFamily = DisplayFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "You're online",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenChats)
                            .testTag("messages_hub_private_chats"),
                        shape = RoundedCornerShape(24.dp),
                        color = readingColors.card,
                        shadowElevation = 6.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(readingColors.brandBrush),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Message,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                            Column {
                                Text(
                                    text = "Private chats",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "All accounts, live messages, photos, videos and voice notes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenChatSettings)
                            .testTag("messages_hub_settings"),
                        shape = RoundedCornerShape(24.dp),
                        color = readingColors.card,
                        shadowElevation = 6.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                )
                            }
                            Column {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Change your profile photo, username and bio.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Tap the screen 3 times to jump back to reading.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
