package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import com.example.model.BlockedUserDoc
import com.example.model.CloudCallSignal
import com.example.model.CloudChatMessage
import com.example.model.CloudConversation
import com.example.model.CloudUserPrivateAccount
import com.example.model.CloudUserProfile
import com.example.model.CryptoUtils
import com.example.model.MediaKind
import com.example.model.NfcLinkDoc
import com.example.model.PhoneClaimDoc
import com.example.model.SecretThemeMode
import com.example.model.cloudThreadId
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private val Context.cloudSessionDataStore by preferencesDataStore(name = "reading_corner_cloud_session")

sealed class NfcLinkResolution {
    data class Valid(val accountId: String) : NfcLinkResolution()
    data object Revoked : NfcLinkResolution()
    data object Invalid : NfcLinkResolution()
    data class Error(val message: String) : NfcLinkResolution()
}

sealed class PhoneLoginResult {
    data class Success(val accountId: String) : PhoneLoginResult()
    data class RequiresGoogleOrOtp(val accountId: String, val message: String) : PhoneLoginResult()
    data class Error(val message: String) : PhoneLoginResult()
}

class CloudSocialRepository(
    private val appContext: Context,
    private val db: FirebaseFirestore,
) {
    constructor(context: Context) : this(
        appContext = context.applicationContext,
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private val auth = Firebase.auth
    private val activeAccountKey = stringPreferencesKey("active_account_id")
    private val rememberedPhoneKey = stringPreferencesKey("remembered_phone")

    private fun requireFirebaseUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google first to connect securely.")

    val savedSessionAccountIdFlow: Flow<String?> = appContext.cloudSessionDataStore.data.map { prefs ->
        prefs[activeAccountKey]?.takeIf { it.isNotBlank() }
    }

    val rememberedPhoneFlow: Flow<String?> = appContext.cloudSessionDataStore.data.map { prefs ->
        prefs[rememberedPhoneKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun saveSessionAccount(accountId: String?, rememberedPhone: String? = null) {
        appContext.cloudSessionDataStore.edit { prefs ->
            if (accountId.isNullOrBlank()) prefs.remove(activeAccountKey)
            else prefs[activeAccountKey] = accountId

            if (rememberedPhone != null) {
                if (rememberedPhone.isBlank()) prefs.remove(rememberedPhoneKey)
                else prefs[rememberedPhoneKey] = rememberedPhone
            }
        }
    }

    suspend fun clearRememberedSession() {
        appContext.cloudSessionDataStore.edit { prefs ->
            prefs.remove(activeAccountKey)
        }
    }

    fun observeDirectoryUsers(): Flow<List<CloudUserProfile>> = flow {
        requireFirebaseUid()
        val path = "users"
        emit(emptyList())
        db.collection("users")
            .snapshots()
            .map { snap ->
                snap.toObjects(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeUserProfile(accountId: String): Flow<CloudUserProfile?> = flow {
        requireFirebaseUid()
        val path = "users/$accountId"
        db.collection("users").document(accountId)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                unwrapAndLog(error, OperationType.GET, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observePrivateAccount(accountId: String): Flow<CloudUserPrivateAccount?> = flow {
        requireFirebaseUid()
        val path = "users/$accountId/private/account"
        db.collection("users").document(accountId).collection("private").document("account")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(CloudUserPrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                unwrapAndLog(error, OperationType.GET, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeBlockedUsers(accountId: String): Flow<List<BlockedUserDoc>> = flow {
        requireFirebaseUid()
        val path = "users/$accountId/blocked"
        db.collection("users").document(accountId).collection("blocked")
            .snapshots()
            .map { snap ->
                snap.toObjects(BlockedUserDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeConversations(): Flow<List<CloudConversation>> = flow {
        val uid = requireFirebaseUid()
        val path = "conversations"
        db.collection("conversations")
            .whereArrayContains("memberFirebaseUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CloudConversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastMessageAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeMessages(threadId: String): Flow<List<CloudChatMessage>> = flow {
        val uid = requireFirebaseUid()
        val path = "conversations/$threadId/messages"
        db.collection("conversations").document(threadId).collection("messages")
            .whereArrayContains("memberFirebaseUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CloudChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
            }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeCalls(threadId: String): Flow<List<CloudCallSignal>> = flow {
        val uid = requireFirebaseUid()
        val path = "conversations/$threadId/calls"
        db.collection("conversations").document(threadId).collection("calls")
            .whereArrayContains("memberFirebaseUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CloudCallSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .take(5)
            }
            .catch { error ->
                unwrapAndLog(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    /** Finds an existing Reading Corner account linked to the signed-in Google UID if any. */
    suspend fun findExistingAccountForCurrentGoogleUid(): Result<String?> {
        val path = "users"
        return try {
            val uid = requireFirebaseUid()
            val query = db.collection("users")
                .whereArrayContains("authorizedFirebaseUids", uid)
                .limit(1)
                .get()
                .await()
            val foundId = query.documents.firstOrNull()?.id
            Result.success(foundId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Registers a brand new Reading Corner account tied uniquely to one phone number.
     * Any characters are permitted in [displayName]. Automatically mints a 256-bit personal NFC link token.
     */
    suspend fun registerPhoneAccount(
        rawPhone: String,
        password: String,
        displayName: String,
        bio: String,
        dpDataUri: String?,
        phoneVerified: Boolean,
        rememberMe: Boolean,
    ): Result<String> {
        val normalizedPhone = CryptoUtils.normalizePhoneNumber(rawPhone)
        if (!CryptoUtils.isValidPhoneE164(normalizedPhone)) {
            return Result.failure(IllegalArgumentException("Enter a valid phone number with country code (e.g. +15551234567)."))
        }
        val cleanName = displayName.trim()
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Profile name is required."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val phoneHash = CryptoUtils.phoneClaimHash(normalizedPhone)
        val claimPath = "phone_claims/$phoneHash"
        return try {
            val uid = requireFirebaseUid()
            val existingClaim = db.collection("phone_claims").document(phoneHash).get().await()
            if (existingClaim.exists()) {
                return Result.failure(IllegalStateException("An account already exists for this phone number. Sign in or use Forgot Password."))
            }

            val accountId = "rc_${System.currentTimeMillis().toString(36)}_${CryptoUtils.generateSalt().take(6)}"
            val salt = CryptoUtils.generateSalt()
            val passwordHash = CryptoUtils.hashPasswordPbkdf2(password, salt)
            val personalToken = CryptoUtils.generateSecureToken()
            val tokenHash = CryptoUtils.sha256Hex(personalToken)

            val publicProfile = buildMap<String, Any> {
                put("accountId", accountId)
                put("ownerFirebaseUid", uid)
                put("authorizedFirebaseUids", listOf(uid))
                put("displayName", cleanName.take(80))
                if (bio.trim().isNotEmpty()) put("bio", bio.trim().take(240))
                if (!dpDataUri.isNullOrBlank()) put("dpUrl", dpDataUri)
                put("secretTheme", SecretThemeMode.INSTAGRAM.name)
                put("online", true)
                put("lastActive", FieldValue.serverTimestamp())
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            val privateAccount = mapOf(
                "accountId" to accountId,
                "phoneE164" to normalizedPhone,
                "phoneVerified" to phoneVerified,
                "personalLinkToken" to personalToken,
                "activeTokenHash" to tokenHash,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            val phoneClaim = mapOf(
                "phoneHash" to phoneHash,
                "accountId" to accountId,
                "ownerFirebaseUid" to uid,
                "passwordSalt" to salt,
                "passwordHash" to passwordHash,
                "phoneVerified" to phoneVerified,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            val nfcLink = mapOf(
                "tokenHash" to tokenHash,
                "accountId" to accountId,
                "ownerFirebaseUid" to uid,
                "revoked" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            // Order matters because security rules check existence of /users/{accountId}
            db.collection("users").document(accountId).set(publicProfile).await()
            db.collection("users").document(accountId).collection("private").document("account").set(privateAccount).await()
            db.collection("phone_claims").document(phoneHash).set(phoneClaim).await()
            db.collection("nfc_links").document(tokenHash).set(nfcLink).await()

            saveSessionAccount(
                accountId = accountId,
                rememberedPhone = if (rememberMe) normalizedPhone else "",
            )
            Result.success(accountId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, claimPath)
            Result.failure(e)
        }
    }

    /**
     * Logs in using ONLY phone number and password (never requires username).
     * Verifies PBKDF2 password digest against `/phone_claims/{phoneHash}`.
     */
    suspend fun loginWithPhoneAndPassword(
        rawPhone: String,
        password: String,
        rememberMe: Boolean,
    ): PhoneLoginResult {
        val normalizedPhone = CryptoUtils.normalizePhoneNumber(rawPhone)
        if (!CryptoUtils.isValidPhoneE164(normalizedPhone)) {
            return PhoneLoginResult.Error("Enter a valid phone number.")
        }
        val phoneHash = CryptoUtils.phoneClaimHash(normalizedPhone)
        val claimPath = "phone_claims/$phoneHash"
        return try {
            val uid = requireFirebaseUid()
            val claimSnap = db.collection("phone_claims").document(phoneHash).get().await()
            if (!claimSnap.exists()) {
                return PhoneLoginResult.Error("No account found for this phone number.")
            }
            val claim = claimSnap.toObject(PhoneClaimDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return PhoneLoginResult.Error("Account record is unavailable.")

            val expectedHash = CryptoUtils.hashPasswordPbkdf2(password, claim.passwordSalt)
            if (expectedHash != claim.passwordHash) {
                return PhoneLoginResult.Error("Incorrect password.")
            }

            val userRef = db.collection("users").document(claim.accountId)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                return PhoneLoginResult.Error("Account profile no longer exists.")
            }
            val profile = userSnap.toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return PhoneLoginResult.Error("Account profile unavailable.")

            if (uid in profile.authorizedFirebaseUids) {
                userRef.update(
                    mapOf(
                        "online" to true,
                        "lastActive" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                saveSessionAccount(
                    accountId = claim.accountId,
                    rememberedPhone = if (rememberMe) normalizedPhone else "",
                )
                return PhoneLoginResult.Success(claim.accountId)
            }

            // Different Google account on this device: can link automatically if Firebase Phone Auth verified this phone
            val verifiedPhoneMatches = auth.currentUser?.phoneNumber == normalizedPhone
            if (verifiedPhoneMatches) {
                val updatedAuthUids = (profile.authorizedFirebaseUids + uid).distinct().take(10)
                userRef.update(
                    mapOf(
                        "authorizedFirebaseUids" to updatedAuthUids,
                        "online" to true,
                        "lastActive" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                saveSessionAccount(
                    accountId = claim.accountId,
                    rememberedPhone = if (rememberMe) normalizedPhone else "",
                )
                PhoneLoginResult.Success(claim.accountId)
            } else {
                PhoneLoginResult.RequiresGoogleOrOtp(
                    accountId = claim.accountId,
                    message = "Password matched. To protect your account on a new device, verify via SMS OTP or sign in with the Google account linked to this profile.",
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, claimPath)
            PhoneLoginResult.Error(e.localizedMessage ?: "Sign in failed.")
        }
    }

    /** Sends a real SMS OTP using Firebase PhoneAuthProvider. Never simulates OTPs. */
    fun startPhoneOtpVerification(
        activity: Activity,
        rawPhone: String,
        onCodeSent: (verificationId: String) -> Unit,
        onVerifiedCredential: (PhoneAuthCredential) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val normalizedPhone = CryptoUtils.normalizePhoneNumber(rawPhone)
        if (!CryptoUtils.isValidPhoneE164(normalizedPhone)) {
            onFailed("Enter a valid phone number in international format (e.g. +15551234567).")
            return
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onVerifiedCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w("PhoneOTP", "Firebase PhoneAuth failed", e)
                val msg = e.localizedMessage ?: "SMS verification unavailable."
                onFailed("Phone OTP could not be sent: $msg (Ensure Phone sign-in provider and SHA-1 are enabled in Firebase Console.)")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                onCodeSent(verificationId)
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(normalizedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /** Applies a verified PhoneAuthCredential onto the signed-in Google Firebase user and resets password in Firestore. */
    suspend fun completeOtpVerificationAndResetPassword(
        rawPhone: String,
        credential: PhoneAuthCredential,
        newPassword: String,
    ): Result<String> {
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        val normalizedPhone = CryptoUtils.normalizePhoneNumber(rawPhone)
        val phoneHash = CryptoUtils.phoneClaimHash(normalizedPhone)
        val claimPath = "phone_claims/$phoneHash"
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("Sign in with Google first before resetting password."))

            currentUser.updatePhoneNumber(credential).await()

            val claimSnap = db.collection("phone_claims").document(phoneHash).get().await()
            if (!claimSnap.exists()) {
                return Result.failure(IllegalStateException("No Reading Corner account exists for $normalizedPhone."))
            }
            val claim = claimSnap.toObject(PhoneClaimDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Account claim unavailable."))

            val newSalt = CryptoUtils.generateSalt()
            val newDigest = CryptoUtils.hashPasswordPbkdf2(newPassword, newSalt)

            db.collection("phone_claims").document(phoneHash).update(
                mapOf(
                    "passwordSalt" to newSalt,
                    "passwordHash" to newDigest,
                    "phoneVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            // Also ensure this Firebase UID is authorized on the user profile (rules verify phone_number)
            val userRef = db.collection("users").document(claim.accountId)
            val userSnap = userRef.get().await()
            val profile = userSnap.toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (profile != null && currentUser.uid !in profile.authorizedFirebaseUids) {
                userRef.update(
                    mapOf(
                        "authorizedFirebaseUids" to (profile.authorizedFirebaseUids + currentUser.uid).distinct().take(10),
                        "online" to true,
                        "lastActive" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }

            Result.success(claim.accountId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, claimPath)
            Result.failure(e)
        }
    }

    /** Verifies a PhoneAuthCredential during registration (no password reset). */
    suspend fun attachVerifiedPhoneToCurrentUser(credential: PhoneAuthCredential): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("Sign in with Google first."))
            currentUser.updatePhoneNumber(credential).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveNfcToken(rawToken: String): NfcLinkResolution {
        val cleanToken = rawToken.trim()
        if (cleanToken.length < 16) return NfcLinkResolution.Invalid
        val tokenHash = CryptoUtils.sha256Hex(cleanToken)
        val path = "nfc_links/$tokenHash"
        return try {
            requireFirebaseUid()
            val snap = db.collection("nfc_links").document(tokenHash).get().await()
            if (!snap.exists()) return NfcLinkResolution.Invalid
            val doc = snap.toObject(NfcLinkDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return NfcLinkResolution.Invalid
            if (doc.revoked) NfcLinkResolution.Revoked else NfcLinkResolution.Valid(doc.accountId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            NfcLinkResolution.Error(e.localizedMessage ?: "Unable to verify card link.")
        }
    }

    suspend fun regenerateNfcLink(accountId: String): Result<String> {
        val privatePath = "users/$accountId/private/account"
        return try {
            val uid = requireFirebaseUid()
            val privateRef = db.collection("users").document(accountId).collection("private").document("account")
            val privateSnap = privateRef.get().await()
            val oldDoc = privateSnap.toObject(CloudUserPrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            if (oldDoc != null && oldDoc.activeTokenHash.isNotBlank()) {
                try {
                    db.collection("nfc_links").document(oldDoc.activeTokenHash).update(
                        mapOf(
                            "revoked" to true,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                } catch (_: Exception) {
                }
            }

            val newToken = CryptoUtils.generateSecureToken()
            val newHash = CryptoUtils.sha256Hex(newToken)
            db.collection("nfc_links").document(newHash).set(
                mapOf(
                    "tokenHash" to newHash,
                    "accountId" to accountId,
                    "ownerFirebaseUid" to uid,
                    "revoked" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            privateRef.update(
                mapOf(
                    "personalLinkToken" to newToken,
                    "activeTokenHash" to newHash,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            Result.success(newToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, privatePath)
            Result.failure(e)
        }
    }

    suspend fun revokeLostCard(accountId: String): Result<Unit> {
        val privatePath = "users/$accountId/private/account"
        return try {
            requireFirebaseUid()
            val privateRef = db.collection("users").document(accountId).collection("private").document("account")
            val snap = privateRef.get().await()
            val doc = snap.toObject(CloudUserPrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Private account record not found."))

            if (doc.activeTokenHash.isNotBlank()) {
                db.collection("nfc_links").document(doc.activeTokenHash).update(
                    mapOf(
                        "revoked" to true,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }

            privateRef.update(
                mapOf(
                    "personalLinkToken" to "",
                    "activeTokenHash" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, privatePath)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        accountId: String,
        displayName: String,
        bio: String,
        dpUrl: String?,
    ): Result<Unit> {
        val cleanName = displayName.trim()
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty."))
        }
        val path = "users/$accountId"
        return try {
            requireFirebaseUid()
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName.take(80),
                "bio" to bio.trim().take(240),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (dpUrl != null) {
                if (dpUrl.isBlank()) updates["dpUrl"] = FieldValue.delete()
                else updates["dpUrl"] = dpUrl
            }
            db.collection("users").document(accountId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSecretTheme(accountId: String, theme: SecretThemeMode): Result<Unit> {
        val path = "users/$accountId"
        return try {
            requireFirebaseUid()
            db.collection("users").document(accountId).update(
                mapOf(
                    "secretTheme" to theme.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun touchPresence(accountId: String, online: Boolean = true): Result<Unit> {
        val path = "users/$accountId"
        return try {
            if (auth.currentUser == null) return Result.success(Unit)
            db.collection("users").document(accountId).update(
                mapOf(
                    "online" to online,
                    "lastActive" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun blockUser(
        myAccountId: String,
        targetAccountId: String,
        targetDisplayName: String,
    ): Result<Unit> {
        val path = "users/$myAccountId/blocked/$targetAccountId"
        return try {
            requireFirebaseUid()
            db.collection("users").document(myAccountId)
                .collection("blocked").document(targetAccountId)
                .set(
                    mapOf(
                        "blockedAccountId" to targetAccountId,
                        "blockedDisplayName" to targetDisplayName.ifBlank { "Reader" }.take(80),
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(myAccountId: String, targetAccountId: String): Result<Unit> {
        val path = "users/$myAccountId/blocked/$targetAccountId"
        return try {
            requireFirebaseUid()
            db.collection("users").document(myAccountId)
                .collection("blocked").document(targetAccountId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun isBlockedEitherWay(accountA: String, accountB: String): Boolean {
        return try {
            val aBlockedB = db.collection("users").document(accountA)
                .collection("blocked").document(accountB).get().await().exists()
            if (aBlockedB) return true
            val bBlockedA = db.collection("users").document(accountB)
                .collection("blocked").document(accountA).get().await().exists()
            bBlockedA
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendMessage(
        senderProfile: CloudUserProfile,
        peerProfile: CloudUserProfile,
        text: String,
        mediaDataUri: String? = null,
        mediaKind: MediaKind? = null,
    ): Result<Unit> {
        val threadId = cloudThreadId(senderProfile.accountId, peerProfile.accountId)
        val msgRef = db.collection("conversations").document(threadId).collection("messages").document()
        return try {
            val uid = requireFirebaseUid()
            val memberUids = (senderProfile.authorizedFirebaseUids + peerProfile.authorizedFirebaseUids + uid)
                .distinct()
                .take(20)
            val previewText = text.trim().ifEmpty {
                when (mediaKind) {
                    MediaKind.IMAGE -> "Sent a photo"
                    MediaKind.VIDEO -> "Sent a video"
                    MediaKind.AUDIO -> "Voice note"
                    null -> ""
                }
            }.take(500)

            val convRef = db.collection("conversations").document(threadId)
            val convSnap = convRef.get().await()
            if (!convSnap.exists()) {
                convRef.set(
                    mapOf(
                        "memberAccountIds" to listOf(senderProfile.accountId, peerProfile.accountId).sorted(),
                        "memberFirebaseUids" to memberUids,
                        "lastMessageText" to previewText,
                        "lastSenderAccountId" to senderProfile.accountId,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            } else {
                convRef.update(
                    mapOf(
                        "memberFirebaseUids" to memberUids,
                        "lastMessageText" to previewText,
                        "lastSenderAccountId" to senderProfile.accountId,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }

            val payload = buildMap<String, Any> {
                put("threadId", threadId)
                put("senderAccountId", senderProfile.accountId)
                put("recipientAccountId", peerProfile.accountId)
                put("senderFirebaseUid", uid)
                put("memberFirebaseUids", memberUids)
                put("text", text.trim().take(4000))
                if (!mediaDataUri.isNullOrBlank()) put("mediaDataUri", mediaDataUri)
                if (mediaKind != null) put("mediaKind", mediaKind.name)
                put("seen", false)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            msgRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            val friendly = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Message blocked or cannot be delivered to this user."
            } else {
                e.localizedMessage ?: "Failed to send message."
            }
            Result.failure(IllegalStateException(friendly, e))
        }
    }

    suspend fun markMessagesSeen(threadId: String, myAccountId: String, messages: List<CloudChatMessage>): Result<Unit> {
        val unseenIncoming = messages.filter { it.recipientAccountId == myAccountId && !it.seen && it.id.isNotBlank() }
        if (unseenIncoming.isEmpty()) return Result.success(Unit)
        return try {
            requireFirebaseUid()
            val batch = db.batch()
            unseenIncoming.take(50).forEach { msg ->
                val ref = db.collection("conversations").document(threadId).collection("messages").document(msg.id)
                batch.update(
                    ref,
                    mapOf(
                        "seen" to true,
                        "seenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "conversations/$threadId/messages")
            Result.failure(e)
        }
    }

    suspend fun initiateCall(
        senderProfile: CloudUserProfile,
        peerProfile: CloudUserProfile,
        callType: String,
    ): Result<String> {
        val threadId = cloudThreadId(senderProfile.accountId, peerProfile.accountId)
        val callRef = db.collection("conversations").document(threadId).collection("calls").document()
        return try {
            val uid = requireFirebaseUid()
            val memberUids = (senderProfile.authorizedFirebaseUids + peerProfile.authorizedFirebaseUids + uid)
                .distinct()
                .take(20)
            callRef.set(
                mapOf(
                    "threadId" to threadId,
                    "senderAccountId" to senderProfile.accountId,
                    "recipientAccountId" to peerProfile.accountId,
                    "senderFirebaseUid" to uid,
                    "memberFirebaseUids" to memberUids,
                    "callType" to callType,
                    "status" to "RINGING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(callRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, callRef.path)
            val friendly = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Call blocked: blocked users cannot initiate calls."
            } else {
                e.localizedMessage ?: "Failed to initiate call."
            }
            Result.failure(IllegalStateException(friendly, e))
        }
    }

    suspend fun updateCallStatus(threadId: String, callId: String, status: String): Result<Unit> {
        val callPath = "conversations/$threadId/calls/$callId"
        return try {
            requireFirebaseUid()
            db.collection("conversations").document(threadId).collection("calls").document(callId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, callPath)
            Result.failure(e)
        }
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser != null) {
            handleFirestoreError(ex, op, path)
        }
    }
}
