package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Serializable
enum class SecretThemeMode(
    val title: String,
    val subtitle: String,
) {
    INSTAGRAM("Instagram", "Gradient rose, amber & violet"),
    EMERALD("Emerald", "Green, crisp white & orange"),
    DARK("Dark", "Black, slate grey & cherry red"),
    ;

    companion object {
        fun fromRaw(raw: String?): SecretThemeMode =
            entries.firstOrNull { it.name == raw } ?: INSTAGRAM
    }
}

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val accountId: String = "",
    val ownerFirebaseUid: String = "",
    val authorizedFirebaseUids: List<String> = emptyList(),
    val displayName: String = "",
    val bio: String? = null,
    val dpUrl: String? = null,
    val secretTheme: String = SecretThemeMode.INSTAGRAM.name,
    val online: Boolean = false,
    @ServerTimestamp val lastActive: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun themeMode(): SecretThemeMode = SecretThemeMode.fromRaw(secretTheme)
}

@Keep
@IgnoreExtraProperties
data class CloudUserPrivateAccount(
    @DocumentId val id: String = "",
    val accountId: String = "",
    val phoneE164: String = "",
    val phoneVerified: Boolean = false,
    val personalLinkToken: String = "",
    val activeTokenHash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class BlockedUserDoc(
    @DocumentId val id: String = "",
    val blockedAccountId: String = "",
    val blockedDisplayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PhoneClaimDoc(
    @DocumentId val id: String = "",
    val phoneHash: String = "",
    val accountId: String = "",
    val ownerFirebaseUid: String = "",
    val passwordSalt: String = "",
    val passwordHash: String = "",
    val phoneVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class NfcLinkDoc(
    @DocumentId val id: String = "",
    val tokenHash: String = "",
    val accountId: String = "",
    val ownerFirebaseUid: String = "",
    val revoked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudConversation(
    @DocumentId val id: String = "",
    val memberAccountIds: List<String> = emptyList(),
    val memberFirebaseUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastSenderAccountId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudChatMessage(
    @DocumentId val id: String = "",
    val threadId: String = "",
    val senderAccountId: String = "",
    val recipientAccountId: String = "",
    val senderFirebaseUid: String = "",
    val memberFirebaseUids: List<String> = emptyList(),
    val text: String = "",
    val mediaDataUri: String? = null,
    val mediaKind: String? = null,
    val seen: Boolean = false,
    @ServerTimestamp val seenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun kindOrNull(): MediaKind? =
        mediaKind?.let { raw -> MediaKind.entries.firstOrNull { it.name == raw } }
}

@Keep
@IgnoreExtraProperties
data class CloudCallSignal(
    @DocumentId val id: String = "",
    val threadId: String = "",
    val senderAccountId: String = "",
    val recipientAccountId: String = "",
    val senderFirebaseUid: String = "",
    val memberFirebaseUids: List<String> = emptyList(),
    val callType: String = "AUDIO",
    val status: String = "RINGING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

object CryptoUtils {
    private val secureRandom = SecureRandom()

    fun normalizePhoneNumber(raw: String): String {
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (digitsOnly.isEmpty()) "" else if (hasPlus) "+$digitsOnly" else "+$digitsOnly"
    }

    fun isValidPhoneE164(normalized: String): Boolean {
        val digits = normalized.removePrefix("+")
        return digits.length in 7..16 && digits.all { it.isDigit() }
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(Locale.US, it) }
    }

    fun phoneClaimHash(normalizedPhone: String): String =
        sha256Hex("readingcorner::phone::v1::$normalizedPhone")

    fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(Locale.US, it) }
    }

    fun hashPasswordPbkdf2(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            12_000,
            256,
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        return encoded.joinToString("") { "%02x".format(Locale.US, it) }
    }

    fun buildPersonalCardUrl(token: String): String = "readingcorner://card/$token"

    fun buildShareableWebUrl(token: String): String = "https://readingcorner.app/card/$token"

    fun parseCardTokenFromUriString(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("readingcorner://card/") ->
                trimmed.removePrefix("readingcorner://card/").substringBefore("?").trim().takeIf { it.length >= 16 }
            trimmed.contains("/card/") ->
                trimmed.substringAfter("/card/").substringBefore("?").substringBefore("/").trim().takeIf { it.length >= 16 }
            else -> null
        }
    }
}

fun cloudThreadId(accountA: String, accountB: String): String =
    listOf(accountA, accountB).sorted().joinToString("__")
