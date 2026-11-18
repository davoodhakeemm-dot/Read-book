package com.example.model

import kotlinx.serialization.Serializable
import kotlin.math.min
import kotlin.math.roundToInt

@Serializable
enum class ReaderTheme(val key: String, val label: String) {
    LIGHT("light", "Light"),
    SEPIA("sepia", "Sepia"),
    DARK("dark", "Night"),
}

@Serializable
enum class ReaderFont(val key: String, val label: String) {
    SERIF("serif", "Serif"),
    SANS("sans", "Sans"),
}

@Serializable
enum class LineSpacing(val key: String, val label: String, val multiplier: Float) {
    SMALL("small", "Small", 1.5f),
    MEDIUM("medium", "Medium", 1.8f),
    LARGE("large", "Large", 2.1f),
}

@Serializable
enum class ReaderWidth(val key: String, val label: String, val maxWidthDp: Int) {
    COMPACT("compact", "Compact", 560),
    NORMAL("normal", "Normal", 680),
    WIDE("wide", "Wide", 900),
}

@Serializable
enum class AppThemeMode(val key: String, val label: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    SYSTEM("system", "System"),
}

@Serializable
data class ReaderPrefs(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontSize: Int = 19,
    val font: ReaderFont = ReaderFont.SERIF,
    val lineSpacing: LineSpacing = LineSpacing.MEDIUM,
    val width: ReaderWidth = ReaderWidth.NORMAL,
    val speechRate: Float = 1f,
)

@Serializable
data class NotificationPrefs(
    val reminders: Boolean = true,
    val newBooks: Boolean = true,
)

@Serializable
data class UserProfile(
    val name: String = "Reader",
    val email: String = "reader@readingcorner.app",
)

@Serializable
data class BookProgress(
    val page: Int,
    val pages: Int,
    val updatedAt: String,
)

@Serializable
data class BookmarkEntry(
    val bookId: String,
    val page: Int,
    val at: String,
)

@Serializable
data class HistoryEntry(
    val bookId: String,
    val at: String,
)

@Serializable
data class LibraryState(
    val onboarded: Boolean = false,
    val languages: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val appTheme: AppThemeMode = AppThemeMode.SYSTEM,
    val reader: ReaderPrefs = ReaderPrefs(),
    val notifications: NotificationPrefs = NotificationPrefs(),
    val profile: UserProfile = UserProfile(),
    val progress: Map<String, BookProgress> = emptyMap(),
    val favorites: List<String> = emptyList(),
    val bookmarks: List<BookmarkEntry> = emptyList(),
    val completed: List<String> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val streak: Int = 1,
)

fun progressPercent(page: Int, pages: Int): Int {
    if (pages <= 1) return 0
    return min(100, (((page + 1).toDouble() / pages.toDouble()) * 100.0).roundToInt())
}
