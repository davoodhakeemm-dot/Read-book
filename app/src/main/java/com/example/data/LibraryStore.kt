package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import com.example.model.BookProgress
import com.example.model.BookmarkEntry
import com.example.model.HistoryEntry
import com.example.model.LibraryState
import com.example.model.NotificationPrefs
import com.example.model.ReaderPrefs
import com.example.model.nowIsoString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(name = "reading_corner_library")

class LibraryStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateKey = stringPreferencesKey("reading_corner_v1")

    val stateFlow: Flow<LibraryState> = context.libraryDataStore.data.map { prefs ->
        val raw = prefs[stateKey]
        if (raw.isNullOrBlank()) {
            LibraryState()
        } else {
            try {
                json.decodeFromString<LibraryState>(raw)
            } catch (_: Exception) {
                LibraryState()
            }
        }
    }

    private suspend fun updateState(transform: (LibraryState) -> LibraryState) {
        context.libraryDataStore.edit { prefs ->
            val current = prefs[stateKey]?.let {
                try {
                    json.decodeFromString<LibraryState>(it)
                } catch (_: Exception) {
                    LibraryState()
                }
            } ?: LibraryState()
            val updated = transform(current)
            prefs[stateKey] = json.encodeToString(updated)
        }
    }

    suspend fun toggleLanguage(code: String) {
        updateState { s ->
            val next = if (code in s.languages) s.languages - code else s.languages + code
            s.copy(languages = next)
        }
    }

    suspend fun toggleCategory(slug: String) {
        updateState { s ->
            val next = if (slug in s.categories) s.categories - slug else s.categories + slug
            s.copy(categories = next)
        }
    }

    suspend fun completeOnboarding() {
        updateState { it.copy(onboarded = true) }
    }

    suspend fun setAppTheme(theme: AppThemeMode) {
        updateState { it.copy(appTheme = theme) }
    }

    suspend fun updateReaderPrefs(transform: (ReaderPrefs) -> ReaderPrefs) {
        updateState { it.copy(reader = transform(it.reader)) }
    }

    suspend fun updateNotifications(transform: (NotificationPrefs) -> NotificationPrefs) {
        updateState { it.copy(notifications = transform(it.notifications)) }
    }

    suspend fun toggleFavorite(bookId: String): Boolean {
        var added = false
        updateState { s ->
            if (bookId in s.favorites) {
                added = false
                s.copy(favorites = s.favorites - bookId)
            } else {
                added = true
                s.copy(favorites = listOf(bookId) + s.favorites)
            }
        }
        return added
    }

    suspend fun toggleBookmark(bookId: String, page: Int): Boolean {
        var added = false
        updateState { s ->
            val exists = s.bookmarks.any { it.bookId == bookId && it.page == page }
            if (exists) {
                added = false
                s.copy(bookmarks = s.bookmarks.filterNot { it.bookId == bookId && it.page == page })
            } else {
                added = true
                val entry = BookmarkEntry(bookId = bookId, page = page, at = nowIsoString())
                s.copy(bookmarks = listOf(entry) + s.bookmarks)
            }
        }
        return added
    }

    suspend fun saveProgress(bookId: String, page: Int, pages: Int) {
        updateState { s ->
            val at = nowIsoString()
            val history = (listOf(HistoryEntry(bookId, at)) + s.history.filterNot { it.bookId == bookId })
                .take(40)
            val completed = if (page >= pages - 1 && bookId !in s.completed) {
                listOf(bookId) + s.completed
            } else {
                s.completed
            }
            val progress = s.progress + (bookId to BookProgress(page = page, pages = pages, updatedAt = at))
            s.copy(progress = progress, history = history, completed = completed)
        }
    }

    suspend fun markCompleted(bookId: String) {
        updateState { s ->
            if (bookId in s.completed) s else s.copy(completed = listOf(bookId) + s.completed)
        }
    }

    suspend fun reset() {
        updateState { LibraryState() }
    }
}
