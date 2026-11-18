package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.SocialViewModel
import com.example.data.NfcCardHelper
import com.example.data.NfcHardwareStatus
import com.example.model.BlockedUserDoc
import com.example.model.CloudChatMessage
import com.example.model.CloudUserPrivateAccount
import com.example.model.CloudUserProfile
import com.example.model.CryptoUtils
import com.example.model.MediaKind
import com.example.model.SecretThemeMode
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.SecretPalette
import kotlinx.coroutines.launch

private enum class ProfileSubView { OVERVIEW, SETTINGS }

private data class GalleryPreviewItem(
    val source: String,
    val kind: MediaKind,
)

@Composable
fun CloudProfileTab(
    palette: SecretPalette,
    me: CloudUserProfile,
    privateAccount: CloudUserPrivateAccount?,
    blockedUsers: List<BlockedUserDoc>,
    galleryMessages: List<CloudChatMessage>,
    socialViewModel: SocialViewModel,
    onShowMessage: (String) -> Unit,
    onLogOut: () -> Unit,
    onBackToBooks: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var subView by remember { mutableStateOf(ProfileSubView.OVERVIEW) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showNfcWriteDialog by remember { mutableStateOf(false) }
    var confirmRevokeLost by remember { mutableStateOf(false) }
    var confirmRegenerate by remember { mutableStateOf(false) }
    var galleryPreview by remember { mutableStateOf<GalleryPreviewItem?>(null) }
    var revealLink by remember { mutableStateOf(false) }

    val personalToken = privateAccount?.personalLinkToken.orEmpty()
    val personalLinkUrl = remember(personalToken) {
        if (personalToken.isNotBlank()) CryptoUtils.buildPersonalCardUrl(personalToken) else ""
    }

    val mediaItems = remember(galleryMessages) {
        galleryMessages.filter {
            !it.mediaDataUri.isNullOrBlank() &&
                (it.kindOrNull() == MediaKind.IMAGE || it.kindOrNull() == MediaKind.VIDEO)
        }.reversed()
    }

    AnimatedContent(targetState = subView, label = "profile_subview") { view ->
        if (view == ProfileSubView.OVERVIEW) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.headerBrush)
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Profile",
                                fontFamily = DisplayFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onBackToBooks,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f)),
                                ) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = "Back to Reading Corner", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { subView = ProfileSubView.SETTINGS },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .testTag("profile_open_settings"),
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Profile settings", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            StoryRingAvatar(
                                palette = palette,
                                name = me.displayName,
                                dpUrl = me.dpUrl,
                                online = true,
                                size = 76,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = me.displayName,
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (privateAccount?.phoneVerified == true) {
                                        Icon(Icons.Filled.Verified, contentDescription = "Verified phone", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (!me.bio.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = me.bio,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!privateAccount?.phoneE164.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = privateAccount!!.phoneE164,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.72f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showEditDialog = true },
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit Profile", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { subView = ProfileSubView.SETTINGS }
                                    .testTag("profile_nfc_settings_btn"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("NFC & Settings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Shared Media Gallery Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Media Gallery",
                                fontFamily = DisplayFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Photos and videos from your active conversation appear here.",
                            fontSize = 12.sp,
                            color = palette.onSurfaceMuted,
                        )
                    }
                }

                if (mediaItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.surface)
                                .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No photos or videos yet. Open a conversation in Chat to view shared media here.",
                                color = palette.onSurfaceMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    val rows = mediaItems.chunked(3)
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            rowItems.forEach { msg ->
                                val kind = msg.kindOrNull() ?: MediaKind.IMAGE
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(palette.surfaceVariant)
                                        .clickable {
                                            galleryPreview = GalleryPreviewItem(msg.mediaDataUri!!, kind)
                                        },
                                ) {
                                    if (kind == MediaKind.IMAGE) {
                                        AsyncImage(
                                            model = msg.mediaDataUri,
                                            contentDescription = "Gallery photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Gallery video", tint = palette.primary, modifier = Modifier.size(36.dp))
                                        }
                                    }
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            // SETTINGS VIEW (Theme Selection + NFC Link Management + Blocked Users + Account Actions)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { subView = ProfileSubView.OVERVIEW }
                                .padding(6.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to profile", tint = palette.onBackground)
                            Text(
                                text = "Profile Settings",
                                fontFamily = DisplayFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onBackground,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // 1. Secret Page Themes (Instagram, Emerald, Dark)
                    SettingsCard(palette = palette, title = "Secret Page Theme", icon = Icons.Filled.Palette) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecretThemeMode.entries.forEach { mode ->
                                val selected = me.themeMode() == mode
                                val swatchBrush = when (mode) {
                                    SecretThemeMode.INSTAGRAM -> Brush.horizontalGradient(
                                        listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF)),
                                    )
                                    SecretThemeMode.EMERALD -> Brush.horizontalGradient(
                                        listOf(Color(0xFF059669), Color(0xFFFFFFFF), Color(0xFFF97316)),
                                    )
                                    SecretThemeMode.DARK -> Brush.horizontalGradient(
                                        listOf(Color(0xFF09090B), Color(0xFF3F3F46), Color(0xFFE11D48)),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) palette.navIndicatorBg else palette.surfaceVariant)
                                        .border(
                                            width = if (selected) 1.5.dp else 0.dp,
                                            color = if (selected) palette.primary else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                        .clickable {
                                            socialViewModel.setSecretTheme(mode)
                                            onShowMessage("${mode.title} theme applied.")
                                        }
                                        .padding(12.dp)
                                        .testTag("theme_option_${mode.name.lowercase()}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(swatchBrush)
                                            .border(1.dp, palette.border, CircleShape),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mode.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.onSurface)
                                        Text(mode.subtitle, fontSize = 11.sp, color = palette.onSurfaceMuted)
                                    }
                                    if (selected) {
                                        Icon(Icons.Filled.Check, contentDescription = "Selected theme", tint = palette.primary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2. Personal NFC Card & Secret Link
                    SettingsCard(palette = palette, title = "Personal NFC Card & Link", icon = Icons.Filled.Nfc) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // My Personal Link
                            Text("My Personal Link", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.onSurface)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = when {
                                        personalLinkUrl.isEmpty() -> "Revoked — tap Regenerate Link to create a new card link"
                                        revealLink -> personalLinkUrl
                                        else -> "readingcorner://card/••••••••••••${personalToken.takeLast(4)}"
                                    },
                                    fontSize = 12.sp,
                                    color = if (personalLinkUrl.isEmpty()) palette.danger else palette.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (personalLinkUrl.isNotEmpty()) {
                                    IconButton(onClick = { revealLink = !revealLink }, modifier = Modifier.size(34.dp)) {
                                        Icon(
                                            imageVector = if (revealLink) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (revealLink) "Hide link" else "Reveal link",
                                            tint = palette.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ActionChipButton(
                                    palette = palette,
                                    label = "Copy Link",
                                    icon = Icons.Filled.ContentCopy,
                                    enabled = personalLinkUrl.isNotEmpty(),
                                    modifier = Modifier.weight(1f).testTag("nfc_copy_link"),
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Reading Corner Link", personalLinkUrl))
                                        onShowMessage("Personal link copied.")
                                    },
                                )
                                ActionChipButton(
                                    palette = palette,
                                    label = "Share Link",
                                    icon = Icons.Filled.Share,
                                    enabled = personalLinkUrl.isNotEmpty(),
                                    modifier = Modifier.weight(1f).testTag("nfc_share_link"),
                                    onClick = {
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Open my Reading Corner link: $personalLinkUrl")
                                        }
                                        context.startActivity(Intent.createChooser(send, "Share Personal Link"))
                                    },
                                )
                            }

                            ActionChipButton(
                                palette = palette,
                                label = "NFC Card Link",
                                icon = Icons.Filled.Nfc,
                                enabled = personalLinkUrl.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().testTag("nfc_write_card"),
                                onClick = { showNfcWriteDialog = true },
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ActionChipButton(
                                    palette = palette,
                                    label = "Regenerate Link",
                                    icon = Icons.Filled.Refresh,
                                    enabled = true,
                                    modifier = Modifier.weight(1f).testTag("nfc_regenerate_link"),
                                    onClick = { confirmRegenerate = true },
                                )
                                ActionChipButton(
                                    palette = palette,
                                    label = "Revoke Lost Card",
                                    icon = Icons.Filled.CreditCardOff,
                                    enabled = personalLinkUrl.isNotEmpty(),
                                    isDanger = true,
                                    modifier = Modifier.weight(1f).testTag("nfc_revoke_card"),
                                    onClick = { confirmRevokeLost = true },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 3. Blocked Users List
                    SettingsCard(palette = palette, title = "Blocked Users", icon = Icons.Filled.Block) {
                        if (blockedUsers.isEmpty()) {
                            Text(
                                text = "You haven't blocked anyone.",
                                fontSize = 13.sp,
                                color = palette.onSurfaceMuted,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                blockedUsers.forEach { blocked ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(palette.surfaceVariant)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = blocked.blockedDisplayName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = palette.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = "Unblock",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = palette.primary,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    socialViewModel.unblockUser(blocked.blockedAccountId) { res ->
                                                        res.onSuccess { onShowMessage("Unblocked ${blocked.blockedDisplayName}") }
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Log Out & Return
                    Surface(
                        shape = CircleShape,
                        color = palette.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onLogOut)
                            .testTag("profile_logout_button"),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = palette.danger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Log Out", color = palette.danger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            palette = palette,
            me = me,
            socialViewModel = socialViewModel,
            onDismiss = { showEditDialog = false },
            onSaved = {
                showEditDialog = false
                onShowMessage("Profile saved.")
            },
            onError = onShowMessage,
        )
    }

    if (showNfcWriteDialog && personalLinkUrl.isNotEmpty()) {
        NfcWriteDialog(
            palette = palette,
            personalLinkUrl = personalLinkUrl,
            onDismiss = { showNfcWriteDialog = false },
            onStatusMessage = onShowMessage,
        )
    }

    if (confirmRegenerate) {
        AlertDialog(
            onDismissRequest = { confirmRegenerate = false },
            title = { Text("Regenerate Personal Link?") },
            text = { Text("Your previous NFC card or shared link will immediately stop working and a fresh secret link will be generated.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRegenerate = false
                    socialViewModel.regenerateNfcLink { res ->
                        res.onSuccess { onShowMessage("New personal NFC card link generated.") }
                            .onFailure { onShowMessage(it.localizedMessage ?: "Failed to regenerate link.") }
                    }
                }) { Text("Regenerate") }
            },
            dismissButton = { TextButton(onClick = { confirmRegenerate = false }) { Text("Cancel") } },
        )
    }

    if (confirmRevokeLost) {
        AlertDialog(
            onDismissRequest = { confirmRevokeLost = false },
            title = { Text("Revoke Lost NFC Card?") },
            text = { Text("This immediately deactivates your current personal link so a lost NFC card cannot open your account.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRevokeLost = false
                    socialViewModel.revokeLostCard { res ->
                        res.onSuccess { onShowMessage("Lost card revoked immediately.") }
                            .onFailure { onShowMessage(it.localizedMessage ?: "Failed to revoke card.") }
                    }
                }) { Text("Revoke Card", color = palette.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmRevokeLost = false }) { Text("Cancel") } },
        )
    }

    galleryPreview?.let { item ->
        FullScreenMediaPreviewDialog(
            source = item.source,
            kind = item.kind,
            onDismiss = { galleryPreview = null },
            onDownload = { src, k -> socialViewModel.downloadMediaToDevice(src, k) },
            onMessageFeedback = onShowMessage,
        )
    }
}

@Composable
private fun SettingsCard(
    palette: SecretPalette,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(22.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    fontFamily = DisplayFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.6.dp, color = palette.border)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ActionChipButton(
    palette: SecretPalette,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fg = when {
        !enabled -> palette.onSurfaceMuted.copy(alpha = 0.45f)
        isDanger -> palette.danger
        else -> palette.primary
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EditProfileDialog(
    palette: SecretPalette,
    me: CloudUserProfile,
    socialViewModel: SocialViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(me.displayName) }
    var bio by remember { mutableStateOf(me.bio ?: "") }
    var dpUrl by remember { mutableStateOf(me.dpUrl) }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                dpUrl = socialViewModel.compressPickedImage(uri)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            modifier = Modifier.fillMaxWidth().border(1.dp, palette.border, RoundedCornerShape(24.dp)),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit Profile", fontFamily = DisplayFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = palette.onSurface)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceVariant)
                            .clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!dpUrl.isNullOrBlank()) {
                            AsyncImage(model = dpUrl, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Pick photo", tint = palette.primary)
                        }
                    }
                    Column {
                        Text("Tap photo to update", fontSize = 12.sp, color = palette.onSurface)
                        if (!dpUrl.isNullOrBlank()) {
                            Text(
                                "Remove photo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.danger,
                                modifier = Modifier.clickable { dpUrl = "" }.padding(top = 4.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.onSurface,
                        unfocusedTextColor = palette.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.onSurface,
                        unfocusedTextColor = palette.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            if (name.trim().isEmpty()) {
                                onError("Profile name cannot be empty.")
                                return@TextButton
                            }
                            saving = true
                            socialViewModel.updateProfile(name, bio, dpUrl ?: "") { res ->
                                saving = false
                                res.onSuccess { onSaved() }
                                    .onFailure { onError(it.localizedMessage ?: "Failed to update profile.") }
                            }
                        },
                    ) {
                        Text(if (saving) "Saving…" else "Save", fontWeight = FontWeight.Bold, color = palette.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcWriteDialog(
    palette: SecretPalette,
    personalLinkUrl: String,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var hardwareStatus by remember {
        mutableStateOf(
            if (activity != null) NfcCardHelper.getStatus(activity) else NfcHardwareStatus.Unsupported,
        )
    }

    DisposableEffect(activity, personalLinkUrl) {
        if (activity != null && hardwareStatus == NfcHardwareStatus.Ready) {
            NfcCardHelper.enableWriteSession(activity, personalLinkUrl) { res ->
                res.onSuccess { msg ->
                    onStatusMessage(msg)
                    onDismiss()
                }.onFailure { err ->
                    onStatusMessage(err.localizedMessage ?: "Could not write NFC card.")
                }
            }
        }
        onDispose {
            if (activity != null) NfcCardHelper.disableWriteSession(activity)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            modifier = Modifier.fillMaxWidth().border(1.dp, palette.border, RoundedCornerShape(24.dp)),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(palette.headerBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("Link Physical NFC Card", fontFamily = DisplayFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = palette.onSurface)
                Text(
                    text = when (hardwareStatus) {
                        NfcHardwareStatus.Ready -> "Hold a writable NFC card against the back of your phone to write your personal link."
                        NfcHardwareStatus.Disabled -> "Turn on NFC in Android Settings, then open this dialog again to write your card."
                        NfcHardwareStatus.Unsupported -> "This device does not have NFC hardware. You can still copy or share your personal link directly."
                    },
                    fontSize = 13.sp,
                    color = palette.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
                if (hardwareStatus == NfcHardwareStatus.Ready) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = palette.primary, strokeWidth = 2.dp)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", fontWeight = FontWeight.Bold, color = palette.primary)
                }
            }
        }
    }
}
