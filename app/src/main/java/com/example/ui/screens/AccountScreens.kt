package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.CATEGORIES
import com.example.model.LANGUAGES
import com.example.model.LibraryState
import com.example.model.LineSpacing
import com.example.model.ReaderFont
import com.example.model.ReaderTheme
import com.example.ui.components.PageHeader
import com.example.ui.components.PillChip
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalReadingColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    libraryState: LibraryState,
    widthSizeClass: WindowWidthSizeClass,
    onOpenSettings: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val inProgressCount = remember(libraryState.progress, libraryState.completed) {
        libraryState.progress.keys.count { it !in libraryState.completed }
    }

    val stats = listOf(
        Triple("Reading", inProgressCount, Icons.AutoMirrored.Filled.MenuBook),
        Triple("Completed", libraryState.completed.size, Icons.Filled.CheckCircle),
        Triple("Favorites", libraryState.favorites.size, Icons.Filled.Favorite),
        Triple("Day streak", libraryState.streak, Icons.Filled.LocalFireDepartment),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.Compact) 16.dp else 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            PageHeader(
                title = "Profile",
                subtitle = "Your reading life at a glance.",
                action = {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSettings)
                            .testTag("profile_settings_button"),
                        shape = CircleShape,
                        color = readingColors.card,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )

            // User banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(readingColors.heroBrush)
                    .padding(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(readingColors.brandBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = libraryState.profile.name.take(1).uppercase(),
                            fontFamily = DisplayFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = libraryState.profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = libraryState.profile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Stats grid
            val statCols = if (widthSizeClass == WindowWidthSizeClass.Compact) 2 else 4
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.chunked(statCols).forEach { rowStats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowStats.forEach { (label, value, icon) ->
                            StatCard(
                                label = label,
                                value = value,
                                icon = icon,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your languages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (libraryState.languages.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        libraryState.languages.forEach { code ->
                            val lang = LANGUAGES.find { it.code == code }
                            Surface(
                                shape = CircleShape,
                                color = readingColors.card,
                                shadowElevation = 2.dp,
                            ) {
                                Text(
                                    text = "${lang?.flag ?: ""} ${lang?.name ?: code}".trim(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No languages selected yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your interests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (libraryState.categories.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        libraryState.categories.forEach { slug ->
                            val cat = CATEGORIES.find { it.slug == slug }
                            Surface(
                                shape = CircleShape,
                                color = readingColors.card,
                                shadowElevation = 2.dp,
                            ) {
                                Text(
                                    text = "${cat?.icon ?: ""} ${cat?.name ?: slug}".trim(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No categories selected yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val readingColors = LocalReadingColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = readingColors.card,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value.toString(),
                fontFamily = DisplayFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    libraryState: LibraryState,
    widthSizeClass: WindowWidthSizeClass,
    onSetAppTheme: (AppThemeMode) -> Unit,
    onSetReaderTheme: (ReaderTheme) -> Unit,
    onSetReaderFont: (ReaderFont) -> Unit,
    onSetReaderLineSpacing: (LineSpacing) -> Unit,
    onSetReminders: (Boolean) -> Unit,
    onSetNewBooks: (Boolean) -> Unit,
    onChangeLanguages: () -> Unit,
    onChangeCategories: () -> Unit,
    onResetData: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.Compact) 16.dp else 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PageHeader(
                    title = "Settings",
                    subtitle = "Appearance, reading defaults and data.",
                )

                SettingsCard(title = "Appearance") {
                    SettingsRow(label = "App theme") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppThemeMode.entries.forEach { mode ->
                                PillChip(
                                    label = mode.label,
                                    selected = libraryState.appTheme == mode,
                                    onClick = { onSetAppTheme(mode) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                SettingsCard(title = "Reading defaults") {
                    SettingsRow(label = "Reader theme") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReaderTheme.entries.forEach { t ->
                                PillChip(
                                    label = t.label,
                                    selected = libraryState.reader.theme == t,
                                    onClick = { onSetReaderTheme(t) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    SettingsRow(label = "Font") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReaderFont.entries.forEach { f ->
                                PillChip(
                                    label = f.label,
                                    selected = libraryState.reader.font == f,
                                    onClick = { onSetReaderFont(f) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    SettingsRow(label = "Line spacing") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LineSpacing.entries.forEach { s ->
                                PillChip(
                                    label = s.label,
                                    selected = libraryState.reader.lineSpacing == s,
                                    onClick = { onSetReaderLineSpacing(s) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                SettingsCard(title = "Notifications") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Daily reading reminder",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Switch(
                            checked = libraryState.notifications.reminders,
                            onCheckedChange = onSetReminders,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "New book announcements",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Switch(
                            checked = libraryState.notifications.newBooks,
                            onCheckedChange = onSetNewBooks,
                        )
                    }
                }

                SettingsCard(title = "Preferences") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PillChip(
                            label = "Change languages",
                            selected = false,
                            onClick = onChangeLanguages,
                        )
                        PillChip(
                            label = "Change categories",
                            selected = false,
                            onClick = onChangeCategories,
                        )
                    }
                }

                SettingsCard(title = "Data") {
                    Text(
                        text = "Your progress, favorites and bookmarks are stored on this device only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                onResetData()
                                scope.launch { snackbarHostState.showSnackbar("Reading data reset") }
                            }
                            .testTag("reset_reading_data_button"),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Reset all reading data",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = readingColors.card,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(
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
