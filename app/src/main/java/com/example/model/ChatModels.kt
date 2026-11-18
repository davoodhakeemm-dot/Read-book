package com.example.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Serializable
data class ChatAccount(
    val id: String,
    val username: String,
    val digest: String,
    val dpUri: String? = null,
    val bio: String? = null,
    val createdAt: String,
    val online: Boolean = true,
    val lastActive: String? = null,
)

@Serializable
enum class MediaKind {
    IMAGE,
    VIDEO,
    AUDIO,
}

@Serializable
data class ChatMessage(
    val id: String,
    val thread: String,
    val from: String,
    val to: String,
    val text: String,
    val mediaUri: String? = null,
    val mediaKind: MediaKind? = null,
    val at: String,
    val seen: Boolean = false,
)

@Serializable
data class ChatState(
    val accounts: List<ChatAccount> = emptyList(),
    val session: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val rememberedUsername: String? = null,
)

fun threadKey(a: String, b: String): String = listOf(a, b).sorted().joinToString("::")

/** Salted digest matching the original JS FNV/Murmur-inspired credential digest. */
fun digestPassword(username: String, password: String): String {
    val input = "rc::${username.lowercase(Locale.ROOT)}::$password"
    var h1 = 0x811c9dc5uL
    var h2 = 0x1000193uL
    for (i in input.indices) {
        val c = input[i].code.toULong()
        h1 = ((h1 xor c) * 0x01000193uL) and 0xFFFFFFFFuL
        h2 = ((h2 + c * (i.toULong() + 7uL)) * 0x85ebca6buL) and 0xFFFFFFFFuL
    }
    return "${h1.toString(36)}${h2.toString(36)}"
}

fun initialsOf(username: String): String {
    val parts = username.replace(Regex("[._-]+"), " ").trim().split(Regex("\\s+"))
    val first = parts.getOrNull(0)?.firstOrNull()?.toString() ?: "?"
    val second = parts.getOrNull(1)?.firstOrNull()?.toString() ?: ""
    return (first + second).uppercase(Locale.ROOT)
}

fun handleOf(account: ChatAccount): String {
    val slug = account.username.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "")
    val suffix = account.id.takeLast(4)
    return "@${slug.ifEmpty { "reader" }}.$suffix"
}

private const val ONLINE_WINDOW_MS = 2 * 60 * 1000L

fun nowIsoString(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date())
}

fun parseIsoMillis(iso: String): Long? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd",
    )
    for (pattern in patterns) {
        try {
            val fmt = SimpleDateFormat(pattern, Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = fmt.parse(iso)
            if (parsed != null) return parsed.time
        } catch (_: Exception) {
        }
    }
    return null
}

fun isAccountOnline(account: ChatAccount, meId: String?): Boolean {
    if (meId != null && account.id == meId) return true
    val last = account.lastActive ?: return account.online
    val millis = parseIsoMillis(last) ?: return account.online
    return (System.currentTimeMillis() - millis) < ONLINE_WINDOW_MS
}

fun presenceLabel(account: ChatAccount, meId: String?): String {
    if (isAccountOnline(account, meId)) return "Online"
    val last = account.lastActive
    if (last != null) return "Active ${formatRelative(last)} ago"
    return "Offline"
}

fun formatTime(iso: String): String {
    val millis = parseIsoMillis(iso) ?: return ""
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

fun formatRelative(iso: String): String {
    val millis = parseIsoMillis(iso) ?: return ""
    val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
    val mins = (diff / 60_000.0).roundToInt()
    if (mins < 1) return "now"
    if (mins < 60) return "${mins}m"
    val hrs = (mins / 60.0).roundToInt()
    if (hrs < 24) return "${hrs}h"
    return "${(hrs / 24.0).roundToInt()}d"
}

fun formatShortDate(iso: String): String {
    val millis = parseIsoMillis(iso) ?: return iso
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(millis))
}
