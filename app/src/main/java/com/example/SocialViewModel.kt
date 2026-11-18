package com.example

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CloudSocialRepository
import com.example.data.MediaEncodeResult
import com.example.data.MediaHelper
import com.example.data.MessageNotificationHelper
import com.example.data.NfcLinkResolution
import com.example.data.PhoneLoginResult
import com.example.model.BlockedUserDoc
import com.example.model.CloudCallSignal
import com.example.model.CloudChatMessage
import com.example.model.CloudConversation
import com.example.model.CloudUserPrivateAccount
import com.example.model.CloudUserProfile
import com.example.model.MediaKind
import com.example.model.SecretThemeMode
import com.example.model.cloudThreadId
import com.google.firebase.Firebase
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface SocialUiState<out T> {
    data object Loading : SocialUiState<Nothing>
    data class Success<T>(val data: T) : SocialUiState<T>
    data class Error(val message: String) : SocialUiState<Nothing>
}

class SocialViewModel(
    private val appContext: Context,
    private val repository: CloudSocialRepository,
    val firebaseUid: String,
) : ViewModel() {

    private val _activeAccountId = MutableStateFlow<String?>(null)
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()

    private val _selectedPeerId = MutableStateFlow<String?>(null)
    val selectedPeerId: StateFlow<String?> = _selectedPeerId.asStateFlow()

    private val _pendingNfcNotice = MutableStateFlow<String?>(null)
    val pendingNfcNotice: StateFlow<String?> = _pendingNfcNotice.asStateFlow()

    val rememberedPhone: StateFlow<String?> = repository.rememberedPhoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val directoryState: StateFlow<SocialUiState<List<CloudUserProfile>>> =
        repository.observeDirectoryUsers()
            .map<List<CloudUserProfile>, SocialUiState<List<CloudUserProfile>>> { SocialUiState.Success(it) }
            .catch { error ->
                val fsErr = unwrapFirestoreError(error)
                if (fsErr == null && error is CancellationException) throw error
                emit(SocialUiState.Error((fsErr ?: error).message ?: "Failed to load directory"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SocialUiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserProfile: StateFlow<CloudUserProfile?> = _activeAccountId
        .flatMapLatest { acctId ->
            if (acctId.isNullOrBlank()) flowOf(null)
            else repository.observeUserProfile(acctId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val privateAccount: StateFlow<CloudUserPrivateAccount?> = _activeAccountId
        .flatMapLatest { acctId ->
            if (acctId.isNullOrBlank()) flowOf(null)
            else repository.observePrivateAccount(acctId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val blockedUsers: StateFlow<List<BlockedUserDoc>> = _activeAccountId
        .flatMapLatest { acctId ->
            if (acctId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeBlockedUsers(acctId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<CloudConversation>> =
        repository.observeConversations()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeThreadMessages: StateFlow<List<CloudChatMessage>> =
        combine(_activeAccountId, _selectedPeerId) { me, peer -> me to peer }
            .flatMapLatest { (me, peer) ->
                if (me.isNullOrBlank() || peer.isNullOrBlank()) flowOf(emptyList())
                else repository.observeMessages(cloudThreadId(me, peer)).catch { emit(emptyList()) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeThreadCalls: StateFlow<List<CloudCallSignal>> =
        combine(_activeAccountId, _selectedPeerId) { me, peer -> me to peer }
            .flatMapLatest { (me, peer) ->
                if (me.isNullOrBlank() || peer.isNullOrBlank()) flowOf(emptyList())
                else repository.observeCalls(cloudThreadId(me, peer)).catch { emit(emptyList()) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /**
     * Collects incoming messages across all threads to trigger system notifications
     * and collect shared media items for the Instagram-style Profile gallery.
     */
    private val lastNotifiedTimestamps = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch {
            repository.savedSessionAccountIdFlow.collect { savedId ->
                if (savedId != null && _activeAccountId.value == null) {
                    _activeAccountId.value = savedId
                } else if (savedId == null && _activeAccountId.value == null) {
                    val found = repository.findExistingAccountForCurrentGoogleUid().getOrNull()
                    if (found != null) {
                        _activeAccountId.value = found
                        repository.saveSessionAccount(found)
                    }
                }
            }
        }

        // Watch conversation updates for incoming message notifications
        viewModelScope.launch {
            var initialized = false
            conversations.collect { list ->
                val me = _activeAccountId.value ?: return@collect
                val dir = (directoryState.value as? SocialUiState.Success)?.data ?: emptyList()
                val blockedIds = blockedUsers.value.map { it.blockedAccountId }.toSet()

                list.forEach { conv ->
                    val ts = conv.lastMessageAt?.toDate()?.time ?: 0L
                    val prev = lastNotifiedTimestamps[conv.id]
                    val senderId = conv.lastSenderAccountId
                    if (initialized &&
                        ts > (prev ?: 0L) &&
                        senderId != me &&
                        senderId !in blockedIds &&
                        _selectedPeerId.value != senderId
                    ) {
                        val senderName = dir.find { it.accountId == senderId }?.displayName ?: "New Message"
                        MessageNotificationHelper.showIncomingMessageNotification(
                            context = appContext,
                            senderName = senderName,
                            body = conv.lastMessageText.ifBlank { "Sent a message" },
                            notificationId = conv.id.hashCode(),
                        )
                    }
                    if (ts > (prev ?: 0L)) {
                        lastNotifiedTimestamps[conv.id] = ts
                    }
                }
                if (list.isNotEmpty()) initialized = true
            }
        }
    }

    fun selectPeer(peerAccountId: String?) {
        _selectedPeerId.value = peerAccountId
    }

    fun clearNfcNotice() {
        _pendingNfcNotice.value = null
    }

    fun touchPresence() {
        val me = _activeAccountId.value ?: return
        viewModelScope.launch {
            repository.touchPresence(me, true)
        }
    }

    fun markThreadSeen(peerAccountId: String) {
        val me = _activeAccountId.value ?: return
        val threadId = cloudThreadId(me, peerAccountId)
        val msgs = activeThreadMessages.value
        viewModelScope.launch {
            repository.markMessagesSeen(threadId, me, msgs)
        }
    }

    suspend fun resolveNfcCardToken(token: String): NfcLinkResolution {
        val resolution = repository.resolveNfcToken(token)
        when (resolution) {
            is NfcLinkResolution.Valid -> {
                val active = _activeAccountId.value ?: repository.savedSessionAccountIdFlow.first()?.also {
                    _activeAccountId.value = it
                }
                if (active != null && active == resolution.accountId) {
                    _pendingNfcNotice.value = "Card verified — welcome back!"
                } else if (active != null && active != resolution.accountId) {
                    // Signed into a different account on this device: do NOT bypass login!
                    signOutAccount()
                    _pendingNfcNotice.value = "Please log in with phone number and password to open this personal card."
                } else {
                    _pendingNfcNotice.value = "Reading Corner card recognized. Sign in to access this account."
                }
            }
            NfcLinkResolution.Revoked -> {
                _pendingNfcNotice.value = "This lost NFC card link has been revoked and cannot open any account."
            }
            NfcLinkResolution.Invalid -> {
                _pendingNfcNotice.value = "Personal NFC card link is invalid or expired."
            }
            is NfcLinkResolution.Error -> {
                _pendingNfcNotice.value = resolution.message
            }
        }
        return resolution
    }

    fun registerAccount(
        phone: String,
        password: String,
        displayName: String,
        bio: String,
        dpDataUri: String?,
        phoneVerified: Boolean,
        rememberMe: Boolean,
        onDone: (Result<String>) -> Unit,
    ) {
        viewModelScope.launch {
            val res = repository.registerPhoneAccount(
                rawPhone = phone,
                password = password,
                displayName = displayName,
                bio = bio,
                dpDataUri = dpDataUri,
                phoneVerified = phoneVerified,
                rememberMe = rememberMe,
            )
            res.onSuccess { accountId ->
                _activeAccountId.value = accountId
            }
            onDone(res)
        }
    }

    fun loginWithPhone(
        phone: String,
        password: String,
        rememberMe: Boolean,
        onDone: (PhoneLoginResult) -> Unit,
    ) {
        viewModelScope.launch {
            val res = repository.loginWithPhoneAndPassword(phone, password, rememberMe)
            if (res is PhoneLoginResult.Success) {
                _activeAccountId.value = res.accountId
            }
            onDone(res)
        }
    }

    fun startPhoneOtp(
        activity: Activity,
        phone: String,
        onCodeSent: (String) -> Unit,
        onAutoCredential: (PhoneAuthCredential) -> Unit,
        onError: (String) -> Unit,
    ) {
        repository.startPhoneOtpVerification(activity, phone, onCodeSent, onAutoCredential, onError)
    }

    fun completeOtpPasswordReset(
        phone: String,
        credential: PhoneAuthCredential,
        newPassword: String,
        onDone: (Result<String>) -> Unit,
    ) {
        viewModelScope.launch {
            val res = repository.completeOtpVerificationAndResetPassword(phone, credential, newPassword)
            res.onSuccess { accountId ->
                _activeAccountId.value = accountId
                repository.saveSessionAccount(accountId, phone)
            }
            onDone(res)
        }
    }

    fun attachVerifiedPhone(credential: PhoneAuthCredential, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(repository.attachVerifiedPhoneToCurrentUser(credential))
        }
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        dpDataUri: String?,
        onDone: (Result<Unit>) -> Unit,
    ) {
        val me = _activeAccountId.value ?: return
        viewModelScope.launch {
            onDone(repository.updateProfile(me, displayName, bio, dpDataUri))
        }
    }

    fun setSecretTheme(mode: SecretThemeMode) {
        val me = _activeAccountId.value ?: return
        viewModelScope.launch {
            repository.updateSecretTheme(me, mode)
        }
    }

    fun regenerateNfcLink(onDone: (Result<String>) -> Unit) {
        val me = _activeAccountId.value ?: return
        viewModelScope.launch {
            onDone(repository.regenerateNfcLink(me))
        }
    }

    fun revokeLostCard(onDone: (Result<Unit>) -> Unit) {
        val me = _activeAccountId.value ?: return
        viewModelScope.launch {
            onDone(repository.revokeLostCard(me))
        }
    }

    fun blockUser(targetAccountId: String, targetDisplayName: String, onDone: (Result<Unit>) -> Unit) {
        val me = _activeAccountId.value ?: return
        viewModelScope.launch {
            onDone(repository.blockUser(me, targetAccountId, targetDisplayName))
        }
    }

    fun unblockUser(targetAccountId: String, onDone: (Result<Unit>) -> Unit) {
        val me = _activeAccountId.value ?: return
        viewModelScope.launch {
            onDone(repository.unblockUser(me, targetAccountId))
        }
    }

    fun sendMessage(
        peerProfile: CloudUserProfile,
        text: String,
        mediaDataUri: String? = null,
        mediaKind: MediaKind? = null,
        onDone: (Result<Unit>) -> Unit,
    ) {
        val sender = currentUserProfile.value ?: return
        viewModelScope.launch {
            onDone(repository.sendMessage(sender, peerProfile, text, mediaDataUri, mediaKind))
        }
    }

    fun initiateCall(peerProfile: CloudUserProfile, callType: String, onDone: (Result<String>) -> Unit) {
        val sender = currentUserProfile.value ?: return
        viewModelScope.launch {
            onDone(repository.initiateCall(sender, peerProfile, callType))
        }
    }

    fun updateCallStatus(threadId: String, callId: String, status: String) {
        viewModelScope.launch {
            repository.updateCallStatus(threadId, callId, status)
        }
    }

    suspend fun compressPickedImage(uri: Uri): String? =
        MediaHelper.compressPickedImageToDataUri(appContext, uri)

    suspend fun encodePickedMedia(uri: Uri, kind: MediaKind): MediaEncodeResult =
        MediaHelper.encodeSmallMediaToDataUri(appContext, uri, kind)

    suspend fun encodeAudioRecording(file: File): MediaEncodeResult =
        MediaHelper.encodeAudioFileToDataUri(file)

    suspend fun downloadMediaToDevice(source: String, kind: MediaKind): Result<String> =
        MediaHelper.saveMediaToDevice(appContext, source, kind)

    fun signOutAccount() {
        val prev = _activeAccountId.value
        _activeAccountId.value = null
        _selectedPeerId.value = null
        viewModelScope.launch {
            if (prev != null) repository.touchPresence(prev, false)
            repository.clearRememberedSession()
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}

class SocialViewModelFactory(
    private val appContext: Context,
    private val repository: CloudSocialRepository,
    private val firebaseUid: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SocialViewModel(appContext.applicationContext, repository, firebaseUid) as T
    }
}
