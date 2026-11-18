package com.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookRepository
import com.example.data.ChatActionResult
import com.example.data.ChatStore
import com.example.data.LibraryStore
import com.example.model.AppThemeMode
import com.example.model.Book
import com.example.model.ChatState
import com.example.model.LibraryState
import com.example.model.LineSpacing
import com.example.model.MediaKind
import com.example.model.ReaderFont
import com.example.model.ReaderTheme
import com.example.model.ReaderWidth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val bookRepository = BookRepository(application)
    private val libraryStore = LibraryStore(application)
    private val chatStore = ChatStore(application)

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _booksReady = MutableStateFlow(false)
    val booksReady: StateFlow<Boolean> = _booksReady.asStateFlow()

    val libraryState: StateFlow<LibraryState> = libraryStore.stateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LibraryState(),
    )

    val chatState: StateFlow<ChatState> = chatStore.stateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChatState(),
    )

    init {
        viewModelScope.launch {
            _books.value = bookRepository.getAllBooks()
            _booksReady.value = true
        }
    }

    fun bookById(id: String): Book? = _books.value.find { it.id == id }

    // Library & preferences mutations
    fun toggleLanguage(code: String) {
        viewModelScope.launch { libraryStore.toggleLanguage(code) }
    }

    fun toggleCategory(slug: String) {
        viewModelScope.launch { libraryStore.toggleCategory(slug) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { libraryStore.completeOnboarding() }
    }

    fun setAppTheme(mode: AppThemeMode) {
        viewModelScope.launch { libraryStore.setAppTheme(mode) }
    }

    fun setReaderTheme(theme: ReaderTheme) {
        viewModelScope.launch { libraryStore.updateReaderPrefs { it.copy(theme = theme) } }
    }

    fun setReaderFont(font: ReaderFont) {
        viewModelScope.launch { libraryStore.updateReaderPrefs { it.copy(font = font) } }
    }

    fun setReaderFontSize(size: Int) {
        viewModelScope.launch {
            libraryStore.updateReaderPrefs { it.copy(fontSize = size.coerceIn(14, 28)) }
        }
    }

    fun setReaderLineSpacing(spacing: LineSpacing) {
        viewModelScope.launch { libraryStore.updateReaderPrefs { it.copy(lineSpacing = spacing) } }
    }

    fun setReaderWidth(width: ReaderWidth) {
        viewModelScope.launch { libraryStore.updateReaderPrefs { it.copy(width = width) } }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { libraryStore.updateReaderPrefs { it.copy(speechRate = rate) } }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { libraryStore.updateNotifications { it.copy(reminders = enabled) } }
    }

    fun setNewBooksEnabled(enabled: Boolean) {
        viewModelScope.launch { libraryStore.updateNotifications { it.copy(newBooks = enabled) } }
    }

    fun toggleFavorite(bookId: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val added = libraryStore.toggleFavorite(bookId)
            onResult?.invoke(added)
        }
    }

    fun toggleBookmark(bookId: String, page: Int, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val added = libraryStore.toggleBookmark(bookId, page)
            onResult?.invoke(added)
        }
    }

    fun saveProgress(bookId: String, page: Int, pages: Int) {
        viewModelScope.launch { libraryStore.saveProgress(bookId, page, pages) }
    }

    fun markCompleted(bookId: String) {
        viewModelScope.launch { libraryStore.markCompleted(bookId) }
    }

    fun resetLibrary() {
        viewModelScope.launch { libraryStore.reset() }
    }

    // Chat mutations
    fun createAccount(
        username: String,
        password: String,
        dpUri: String?,
        rememberMe: Boolean = true,
        onResult: (ChatActionResult) -> Unit,
    ) {
        viewModelScope.launch {
            val res = chatStore.createAccount(username, password, dpUri, rememberMe)
            onResult(res)
        }
    }

    fun signIn(
        username: String,
        password: String,
        rememberMe: Boolean = true,
        onResult: (ChatActionResult) -> Unit,
    ) {
        viewModelScope.launch {
            val res = chatStore.signIn(username, password, rememberMe)
            onResult(res)
        }
    }

    fun deleteChatAccount(
        username: String,
        onResult: (ChatActionResult) -> Unit,
    ) {
        viewModelScope.launch {
            val res = chatStore.deleteAccount(username)
            onResult(res)
        }
    }

    fun signOut() {
        viewModelScope.launch { chatStore.signOut() }
    }

    fun updateChatProfile(
        username: String?,
        dpUri: String?,
        bio: String?,
        onResult: (ChatActionResult) -> Unit,
    ) {
        viewModelScope.launch {
            val res = chatStore.updateProfile(username, dpUri, bio)
            onResult(res)
        }
    }

    fun sendMessage(peerId: String, text: String, mediaUri: String? = null, mediaKind: MediaKind? = null) {
        viewModelScope.launch { chatStore.sendMessage(peerId, text, mediaUri, mediaKind) }
    }

    fun markSeen(peerId: String) {
        viewModelScope.launch { chatStore.markSeen(peerId) }
    }

    fun touchPresence() {
        viewModelScope.launch { chatStore.touchPresence() }
    }

    suspend fun importPickedMedia(uri: Uri, prefix: String, ext: String = "bin"): String? =
        chatStore.copyMediaToAppStorage(uri, prefix, ext)

    fun newAudioRecordingFile(): File = chatStore.newAudioRecordingFile()
}
