package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SocialViewModel
import com.example.ui.components.TripleTapExitContainer
import com.example.ui.theme.paletteForSecretTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SecretNavTab { CHAT, PROFILE }

@Composable
fun SecretHomeScreen(
    socialViewModel: SocialViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onRequireAuth: () -> Unit,
    onBackHome: () -> Unit,
    onLogOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeAccountId by socialViewModel.activeAccountId.collectAsStateWithLifecycle()
    val me by socialViewModel.currentUserProfile.collectAsStateWithLifecycle()
    val privateAccount by socialViewModel.privateAccount.collectAsStateWithLifecycle()
    val directoryState by socialViewModel.directoryState.collectAsStateWithLifecycle()
    val blockedUsers by socialViewModel.blockedUsers.collectAsStateWithLifecycle()
    val conversations by socialViewModel.conversations.collectAsStateWithLifecycle()
    val selectedPeerId by socialViewModel.selectedPeerId.collectAsStateWithLifecycle()
    val threadMessages by socialViewModel.activeThreadMessages.collectAsStateWithLifecycle()
    val threadCalls by socialViewModel.activeThreadCalls.collectAsStateWithLifecycle()
    val nfcNotice by socialViewModel.pendingNfcNotice.collectAsStateWithLifecycle()

    LaunchedEffect(nfcNotice) {
        if (!nfcNotice.isNullOrBlank()) {
            snackbarHostState.showSnackbar(nfcNotice!!)
            socialViewModel.clearNfcNotice()
        }
    }

    LaunchedEffect(activeAccountId) {
        if (activeAccountId == null) {
            onRequireAuth()
            return@LaunchedEffect
        }
        socialViewModel.touchPresence()
        while (true) {
            delay(35_000L)
            socialViewModel.touchPresence()
        }
    }

    val currentProfile = me
    if (activeAccountId == null || currentProfile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F24)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val palette = paletteForSecretTheme(currentProfile.themeMode())
    val animatedBg by animateColorAsState(palette.background, tween(300), label = "secret_bg")
    val animatedNavBg by animateColorAsState(palette.surface, tween(300), label = "secret_nav_bg")
    var activeTab by remember { mutableStateOf(SecretNavTab.CHAT) }
    var inMobileConversation by remember { mutableStateOf(false) }
    val isWide = widthSizeClass != WindowWidthSizeClass.Compact

    fun showToast(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    TripleTapExitContainer(
        onTripleTapExit = onBackHome,
        modifier = Modifier.background(animatedBg),
    ) {
        Scaffold(
            containerColor = animatedBg,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWide && !inMobileConversation) {
                    Column {
                        HorizontalDivider(thickness = 0.7.dp, color = palette.border)
                        NavigationBar(
                            containerColor = animatedNavBg,
                            tonalElevation = 0.dp,
                        ) {
                            NavigationBarItem(
                                selected = activeTab == SecretNavTab.CHAT,
                                onClick = { activeTab = SecretNavTab.CHAT },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == SecretNavTab.CHAT) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Chat",
                                    )
                                },
                                label = { Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = palette.primary,
                                    selectedTextColor = palette.primary,
                                    indicatorColor = palette.navIndicatorBg,
                                    unselectedIconColor = palette.onSurfaceMuted,
                                    unselectedTextColor = palette.onSurfaceMuted,
                                ),
                                modifier = Modifier.testTag("secret_tab_chat"),
                            )
                            NavigationBarItem(
                                selected = activeTab == SecretNavTab.PROFILE,
                                onClick = { activeTab = SecretNavTab.PROFILE },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == SecretNavTab.PROFILE) Icons.Filled.Person else Icons.Outlined.PersonOutline,
                                        contentDescription = "Profile",
                                    )
                                },
                                label = { Text("Profile", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = palette.primary,
                                    selectedTextColor = palette.primary,
                                    indicatorColor = palette.navIndicatorBg,
                                    unselectedIconColor = palette.onSurfaceMuted,
                                    unselectedTextColor = palette.onSurfaceMuted,
                                ),
                                modifier = Modifier.testTag("secret_tab_profile"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    NavigationRail(
                        containerColor = animatedNavBg,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(Modifier.weight(1f))
                        NavigationRailItem(
                            selected = activeTab == SecretNavTab.CHAT,
                            onClick = { activeTab = SecretNavTab.CHAT },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == SecretNavTab.CHAT) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                )
                            },
                            label = { Text("Chat") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = palette.primary,
                                selectedTextColor = palette.primary,
                                indicatorColor = palette.navIndicatorBg,
                                unselectedIconColor = palette.onSurfaceMuted,
                                unselectedTextColor = palette.onSurfaceMuted,
                            ),
                        )
                        NavigationRailItem(
                            selected = activeTab == SecretNavTab.PROFILE,
                            onClick = { activeTab = SecretNavTab.PROFILE },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == SecretNavTab.PROFILE) Icons.Filled.Person else Icons.Outlined.PersonOutline,
                                    contentDescription = "Profile",
                                )
                            },
                            label = { Text("Profile") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = palette.primary,
                                selectedTextColor = palette.primary,
                                indicatorColor = palette.navIndicatorBg,
                                unselectedIconColor = palette.onSurfaceMuted,
                                unselectedTextColor = palette.onSurfaceMuted,
                            ),
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(palette.border))
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        if (activeTab == SecretNavTab.CHAT) {
                            CloudChatTab(
                                palette = palette,
                                me = currentProfile,
                                directoryState = directoryState,
                                conversations = conversations,
                                blockedUsers = blockedUsers,
                                selectedPeerId = selectedPeerId,
                                threadMessages = threadMessages,
                                threadCalls = threadCalls,
                                widthSizeClass = widthSizeClass,
                                socialViewModel = socialViewModel,
                                onShowMessage = ::showToast,
                                onConversationOpenChanged = { inMobileConversation = it },
                            )
                        } else {
                            CloudProfileTab(
                                palette = palette,
                                me = currentProfile,
                                privateAccount = privateAccount,
                                blockedUsers = blockedUsers,
                                galleryMessages = threadMessages,
                                socialViewModel = socialViewModel,
                                onShowMessage = ::showToast,
                                onLogOut = onLogOut,
                                onBackToBooks = onBackHome,
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (activeTab == SecretNavTab.CHAT) {
                        CloudChatTab(
                            palette = palette,
                            me = currentProfile,
                            directoryState = directoryState,
                            conversations = conversations,
                            blockedUsers = blockedUsers,
                            selectedPeerId = selectedPeerId,
                            threadMessages = threadMessages,
                            threadCalls = threadCalls,
                            widthSizeClass = widthSizeClass,
                            socialViewModel = socialViewModel,
                            onShowMessage = ::showToast,
                            onConversationOpenChanged = { inMobileConversation = it },
                        )
                    } else {
                        CloudProfileTab(
                            palette = palette,
                            me = currentProfile,
                            privateAccount = privateAccount,
                            blockedUsers = blockedUsers,
                            galleryMessages = threadMessages,
                            socialViewModel = socialViewModel,
                            onShowMessage = ::showToast,
                            onLogOut = onLogOut,
                            onBackToBooks = onBackHome,
                        )
                    }
                }
            }
        }
    }
}
