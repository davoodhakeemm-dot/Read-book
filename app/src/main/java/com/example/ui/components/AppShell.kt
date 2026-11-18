package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalReadingColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ShellTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    CATEGORIES("Categories", Icons.Filled.GridView, Icons.Filled.GridView),
    LANGUAGES("Languages", Icons.Filled.Language, Icons.Filled.Language),
    LIBRARY("Library", Icons.Filled.Bookmarks, Icons.Outlined.LocalLibrary),
    FAVORITES("Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    HISTORY("History", Icons.Filled.History, Icons.Filled.History),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Filled.Settings),
}

private val MOBILE_TABS = listOf(
    ShellTab.HOME,
    ShellTab.EXPLORE,
    ShellTab.LIBRARY,
    ShellTab.FAVORITES,
    ShellTab.PROFILE,
)

private val SIDEBAR_TABS = listOf(
    ShellTab.HOME,
    ShellTab.EXPLORE,
    ShellTab.CATEGORIES,
    ShellTab.LANGUAGES,
    ShellTab.LIBRARY,
    ShellTab.FAVORITES,
    ShellTab.HISTORY,
    ShellTab.PROFILE,
    ShellTab.SETTINGS,
)

private const val SECRET_HOLD_MS = 7000L

@Composable
fun SecretLogoTrigger(
    onTap: () -> Unit,
    onSecretUnlock: () -> Unit,
    onHoldProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val began = System.currentTimeMillis()
                    var unlocked = false
                    var movedTooMuch = false

                    val progressJob: Job = scope.launch {
                        while (!unlocked && !movedTooMuch) {
                            val elapsed = System.currentTimeMillis() - began
                            val p = (elapsed.toFloat() / SECRET_HOLD_MS).coerceIn(0f, 1f)
                            onHoldProgress(p)
                            if (elapsed >= SECRET_HOLD_MS) {
                                unlocked = true
                                onHoldProgress(0f)
                                onSecretUnlock()
                                break
                            }
                            delay(80L)
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        val dist = (change.position - startPos).getDistance()
                        if (dist > 28f) {
                            movedTooMuch = true
                            break
                        }
                        if (!change.pressed) {
                            if (!unlocked && !movedTooMuch && (System.currentTimeMillis() - began) < 400L) {
                                onTap()
                            }
                            break
                        }
                    }
                    progressJob.cancel()
                    onHoldProgress(0f)
                }
            },
    ) {
        content()
    }
}

@Composable
fun AppShell(
    activeTab: ShellTab?,
    widthSizeClass: WindowWidthSizeClass,
    onSelectTab: (ShellTab) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onSecretUnlock: () -> Unit,
    content: @Composable () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val isWide = widthSizeClass != WindowWidthSizeClass.Compact
    var holdProgress by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Persistent Sidebar on Tablet / Expanded
                Column(
                    modifier = Modifier
                        .width(236.dp)
                        .fillMaxHeight()
                        .background(readingColors.sidebar)
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 20.dp),
                ) {
                    SecretLogoTrigger(
                        onTap = { onSelectTab(ShellTab.HOME) },
                        onSecretUnlock = onSecretUnlock,
                        onHoldProgress = { holdProgress = it },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("shell_logo"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Reading Corner",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp),
                            )
                            Text(
                                text = "Reading\nCorner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 20.sp,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SIDEBAR_TABS.forEach { tab ->
                            val selected = activeTab == tab
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) readingColors.sidebarAccent
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                    )
                                    .clickable { onSelectTab(tab) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("sidebar_nav_${tab.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp),
                                )
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(readingColors.heroBrush)
                            .padding(14.dp),
                    ) {
                        Column {
                            Text(
                                text = "Keep reading",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pick up exactly where you left off.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            BrandButton(
                                text = "My Library",
                                onClick = { onSelectTab(ShellTab.LIBRARY) },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .statusBarsPadding(),
                ) {
                    content()
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    ) {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                SecretLogoTrigger(
                                    onTap = { onSelectTab(ShellTab.HOME) },
                                    onSecretUnlock = onSecretUnlock,
                                    onHoldProgress = { holdProgress = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("shell_logo"),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = "Reading Corner",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp),
                                        )
                                        Text(
                                            text = "Reading Corner",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = onOpenSearch,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                            .testTag("top_search_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Search books",
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenSettings,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                            .testTag("top_settings_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = readingColors.border.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                        windowInsets = WindowInsets.navigationBars,
                        tonalElevation = 2.dp,
                    ) {
                        MOBILE_TABS.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = readingColors.sidebarAccent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}"),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    content()
                }
            }
        }

        // Subtle secret hold progress indicator at top edge (>= 45% like web SecretGesture.tsx)
        if (holdProgress >= 0.45f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .fillMaxWidth(holdProgress)
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
            )
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(12.dp))
            action()
        }
    }
}
