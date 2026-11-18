package com.example.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ChatAccount
import com.example.model.ChatMessage
import com.example.model.ChatState
import com.example.model.MediaKind
import com.example.model.digestPassword
import com.example.model.nowIsoString
import com.example.model.threadKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

private val Context.chatDataStore: DataStore<Preferences> by preferencesDataStore(name = "reading_corner_chat")

sealed class ChatActionResult {
    data object Success : ChatActionResult()
    data class Error(val message: String) : ChatActionResult()
}

class ChatStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateKey = stringPreferencesKey("reading_corner_chat_v2")

    val stateFlow: Flow<ChatState> = context.chatDataStore.data.map { prefs ->
        val raw = prefs[stateKey]
        if (raw.isNullOrBlank()) {
            ChatState()
        } else {
            try {
                val parsed = json.decodeFromString<ChatState>(raw)
                val ids = parsed.accounts.map { it.id }.toSet()
                parsed.copy(messages = parsed.messages.filter { it.from in ids && it.to in ids })
            } catch (_: Exception) {
                ChatState()
            }
        }
    }

    private suspend fun updateState(transform: (ChatState) -> ChatState) {
        context.chatDataStore.edit { prefs ->
            val current = prefs[stateKey]?.let {
                try {
                    json.decodeFromString<ChatState>(it)
                } catch (_: Exception) {
                    ChatState()
                }
            } ?: ChatState()
            val updated = transform(current)
            val trimmed = updated.copy(messages = updated.messages.takeLast(400))
            prefs[stateKey] = json.encodeToString(trimmed)
        }
    }

    /** Persist a picked content:// URI to app-private storage so it remains readable across restarts. */
    suspend fun copyMediaToAppStorage(uri: Uri, prefix: String, ext: String = "bin"): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "chat_media").apply { mkdirs() }
                val mime = context.contentResolver.getType(uri)
                val resolvedExt = when {
                    mime?.contains("png") == true -> "png"
                    mime?.contains("webp") == true -> "webp"
                    mime?.contains("jpeg") == true || mime?.contains("jpg") == true -> "jpg"
                    mime?.contains("mp4") == true -> "mp4"
                    mime?.contains("m4a") == true -> "m4a"
                    mime?.contains("mp3") == true -> "mp3"
                    else -> ext
                }
                val file = File(dir, "${prefix}_${System.currentTimeMillis()}.$resolvedExt")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
                file.absolutePath
            } catch (_: Exception) {
                null
            }
        }

    fun newAudioRecordingFile(): File {
        val dir = File(context.filesDir, "chat_media").apply { mkdirs() }
        return File(dir, "voice_${System.currentTimeMillis()}.m4a")
    }

    suspend fun createAccount(
        username: String,
        password: String,
        dpUri: String?,
        rememberMe: Boolean = true,
    ): ChatActionResult {
        val name = username.trim()
        var result: ChatActionResult = ChatActionResult.Success
        updateState { s ->
            if (s.accounts.any { it.username.equals(name, ignoreCase = true) }) {
                result = ChatActionResult.Error("That username is already taken.")
                s
            } else {
                val account = ChatAccount(
                    id = "u-${System.currentTimeMillis().toString(36)}",
                    username = name,
                    digest = digestPassword(name, password),
                    dpUri = dpUri,
                    createdAt = nowIsoString(),
                    online = true,
                    lastActive = nowIsoString(),
                )
                s.copy(
                    accounts = s.accounts + account,
                    session = account.id,
                    rememberedUsername = if (rememberMe) name else null,
                )
            }
        }
        return result
    }

    suspend fun signIn(
        username: String,
        password: String,
        rememberMe: Boolean = true,
    ): ChatActionResult {
        val name = username.trim().lowercase(Locale.ROOT)
        var result: ChatActionResult = ChatActionResult.Success
        updateState { s ->
            val account = s.accounts.find { it.username.lowercase(Locale.ROOT) == name }
            when {
                account == null -> {
                    result = ChatActionResult.Error("No account found with that username.")
                    s
                }
                account.digest != digestPassword(account.username, password) -> {
                    result = ChatActionResult.Error("Incorrect password. Please try again.")
                    s
                }
                else -> {
                    val now = nowIsoString()
                    s.copy(
                        session = account.id,
                        accounts = s.accounts.map {
                            if (it.id == account.id) it.copy(online = true, lastActive = now) else it
                        },
                        rememberedUsername = if (rememberMe) account.username else null,
                    )
                }
            }
        }
        return result
    }

    suspend fun deleteAccount(username: String): ChatActionResult {
        val name = username.trim().lowercase(Locale.ROOT)
        if (name.isEmpty()) return ChatActionResult.Error("Enter a username to reset.")
        var result: ChatActionResult = ChatActionResult.Success
        updateState { s ->
            val target = s.accounts.find { it.username.lowercase(Locale.ROOT) == name }
            if (target == null) {
                result = ChatActionResult.Error("No local account found for \"$username\".")
                s
            } else {
                s.copy(
                    accounts = s.accounts.filterNot { it.id == target.id },
                    messages = s.messages.filterNot { it.from == target.id || it.to == target.id },
                    session = if (s.session == target.id) null else s.session,
                    rememberedUsername = if (s.rememberedUsername?.equals(target.username, ignoreCase = true) == true) null else s.rememberedUsername,
                )
            }
        }
        return result
    }

    suspend fun signOut() {
        updateState { it.copy(session = null) }
    }

    suspend fun updateProfile(username: String?, dpUri: String?, bio: String?): ChatActionResult {
        var result: ChatActionResult = ChatActionResult.Success
        updateState { s ->
            val id = s.session
            if (id == null) {
                result = ChatActionResult.Error("You are not signed in.")
                return@updateState s
            }
            val cleanName = username?.trim()
            if (cleanName != null && s.accounts.any {
                    it.id != id && it.username.equals(cleanName, ignoreCase = true)
                }
            ) {
                result = ChatActionResult.Error("That username is already taken.")
                return@updateState s
            }
            s.copy(
                accounts = s.accounts.map { a ->
                    if (a.id == id) {
                        a.copy(
                            username = cleanName ?: a.username,
                            dpUri = dpUri?.ifEmpty { null },
                            bio = bio ?: a.bio,
                        )
                    } else {
                        a
                    }
                },
            )
        }
        return result
    }

    suspend fun sendMessage(
        peerId: String,
        text: String,
        mediaUri: String? = null,
        mediaKind: MediaKind? = null,
    ) {
        updateState { s ->
            val me = s.session ?: return@updateState s
            val msg = ChatMessage(
                id = "$peerId-${System.currentTimeMillis()}-me",
                thread = threadKey(me, peerId),
                from = me,
                to = peerId,
                text = text,
                mediaUri = mediaUri,
                mediaKind = mediaKind,
                at = nowIsoString(),
                seen = false,
            )
            s.copy(messages = s.messages + msg)
        }
    }

    suspend fun markSeen(peerId: String) {
        updateState { s ->
            val me = s.session ?: return@updateState s
            val key = threadKey(me, peerId)
            if (s.messages.none { it.thread == key && it.from == me && !it.seen }) return@updateState s
            s.copy(
                messages = s.messages.map { m ->
                    if (m.thread == key && m.from == me) m.copy(seen = true) else m
                },
            )
        }
    }

    suspend fun touchPresence() {
        updateState { s ->
            val id = s.session ?: return@updateState s
            val now = nowIsoString()
            s.copy(
                accounts = s.accounts.map { a ->
                    if (a.id == id) a.copy(online = true, lastActive = now) else a
                },
            )
        }
    }
}

fun conversationFor(messages: List<ChatMessage>, meId: String, peerId: String): List<ChatMessage> {
    val key = threadKey(meId, peerId)
    return messages.filter { it.thread == key }.sortedBy { it.at }
}

fun lastMessageFor(messages: List<ChatMessage>, meId: String, peerId: String): ChatMessage? =
    conversationFor(messages, meId, peerId).lastOrNull()

fun unreadCount(messages: List<ChatMessage>, meId: String, peerId: String): Int {
    val key = threadKey(meId, peerId)
    return messages.count { it.thread == key && it.to == meId && !it.seen }
}
