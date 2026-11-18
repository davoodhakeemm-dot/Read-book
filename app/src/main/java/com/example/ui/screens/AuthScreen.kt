package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.SocialViewModel
import com.example.data.PhoneLoginResult
import com.example.model.CryptoUtils
import com.example.model.initialsOf
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.TripleTapExitContainer
import com.example.ui.theme.DisplayFontFamily
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class AuthMode { LOGIN, REGISTER }

private val GlassBorderCyan = Color(0xFF7DD3FC)
private val GlassBorderMagenta = Color(0xFFE879F9)
private val GlassAccentViolet = Color(0xFFC084FC)
private val GlassBtnStart = Color(0xFF3B82F6)
private val GlassBtnMid = Color(0xFF7C3AED)
private val GlassBtnEnd = Color(0xFFD946EF)
private val GlassErrorBg = Color(0x4DF43F5E)
private val GlassErrorText = Color(0xFFFFD6DD)
private val GlassSuccessBg = Color(0x3D10B981)
private val GlassSuccessText = Color(0xFFD1FAE5)

@Composable
fun AuthScreen(
    firebaseUser: FirebaseUser?,
    socialViewModel: SocialViewModel?,
    rememberedPhone: String?,
    nfcNotice: String?,
    onClearNfcNotice: () -> Unit,
    onSignedIn: () -> Unit,
    onBackHome: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var googleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (firebaseUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {},
                onUnauthenticated = {},
            )
        }
    }

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var phoneNumber by remember(rememberedPhone) { mutableStateOf(rememberedPhone ?: "") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dpDataUri by remember { mutableStateOf<String?>(null) }
    var rememberMe by remember(rememberedPhone) { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    // Optional real OTP verification during registration
    var regVerificationId by remember { mutableStateOf<String?>(null) }
    var regOtpCode by remember { mutableStateOf("") }
    var regPhoneVerified by remember { mutableStateOf(false) }
    var regOtpBusy by remember { mutableStateOf(false) }

    var phoneError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var infoBanner by remember { mutableStateOf<String?>(null) }
    var showForgotOtpDialog by remember { mutableStateOf(false) }

    val activeAccountIdState = socialViewModel?.activeAccountId?.collectAsStateWithLifecycle()
    val activeAccountId = activeAccountIdState?.value
    LaunchedEffect(firebaseUser, activeAccountId) {
        if (firebaseUser != null && activeAccountId != null) {
            onSignedIn()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null && socialViewModel != null) {
            scope.launch {
                dpDataUri = socialViewModel.compressPickedImage(uri)
            }
        }
    }

    fun switchMode(next: AuthMode) {
        mode = next
        phoneError = null
        nameError = null
        passwordError = null
        confirmError = null
        formError = null
        infoBanner = null
        password = ""
        confirmPassword = ""
        regVerificationId = null
        regOtpCode = ""
        regPhoneVerified = false
    }

    fun startRegOtp() {
        val activity = context as? Activity ?: return
        val vm = socialViewModel ?: return
        regOtpBusy = true
        formError = null
        infoBanner = null
        vm.startPhoneOtp(
            activity = activity,
            phone = phoneNumber,
            onCodeSent = { verId ->
                regOtpBusy = false
                regVerificationId = verId
                infoBanner = "Verification SMS sent. Enter the 6-digit code below."
            },
            onAutoCredential = { cred ->
                vm.attachVerifiedPhone(cred) { res ->
                    regOtpBusy = false
                    res.onSuccess {
                        regPhoneVerified = true
                        infoBanner = "Phone number verified."
                    }.onFailure { err ->
                        formError = err.localizedMessage ?: "Verification failed."
                    }
                }
            },
            onError = { msg ->
                regOtpBusy = false
                formError = msg
            },
        )
    }

    fun verifyRegOtpCode() {
        val verId = regVerificationId ?: return
        val vm = socialViewModel ?: return
        val code = regOtpCode.trim()
        if (code.length < 4) {
            formError = "Enter the SMS verification code."
            return
        }
        regOtpBusy = true
        val credential = PhoneAuthProvider.getCredential(verId, code)
        vm.attachVerifiedPhone(credential) { res ->
            regOtpBusy = false
            res.onSuccess {
                regPhoneVerified = true
                regVerificationId = null
                infoBanner = "Phone verified via SMS."
            }.onFailure { err ->
                formError = err.localizedMessage ?: "Invalid or expired OTP code."
            }
        }
    }

    fun submit() {
        val vm = socialViewModel
        if (firebaseUser == null || vm == null) {
            formError = "Connect your Google session below first so your phone account syncs securely."
            return
        }
        val normalizedPhone = CryptoUtils.normalizePhoneNumber(phoneNumber)
        var pPhoneErr: String? = null
        var pNameErr: String? = null
        var pPassErr: String? = null
        var pConfErr: String? = null

        if (!CryptoUtils.isValidPhoneE164(normalizedPhone)) {
            pPhoneErr = "Enter a valid phone number (with country code, e.g. +15551234567)."
        }
        if (mode == AuthMode.REGISTER && displayName.trim().isEmpty()) {
            // Free-form profile name: accept anything non-empty without character rules or suggestions!
            pNameErr = "Please enter a name."
        }
        if (password.length < 6) {
            pPassErr = "Password must be at least 6 characters."
        }
        if (mode == AuthMode.REGISTER && confirmPassword != password) {
            pConfErr = "Passwords do not match."
        }

        phoneError = pPhoneErr
        nameError = pNameErr
        passwordError = pPassErr
        confirmError = pConfErr
        formError = null
        if (pPhoneErr != null || pNameErr != null || pPassErr != null || pConfErr != null) return

        submitting = true
        if (mode == AuthMode.REGISTER) {
            vm.registerAccount(
                phone = normalizedPhone,
                password = password,
                displayName = displayName,
                bio = bio,
                dpDataUri = dpDataUri,
                phoneVerified = regPhoneVerified,
                rememberMe = rememberMe,
            ) { res ->
                submitting = false
                res.onSuccess { onSignedIn() }
                    .onFailure { err -> formError = err.localizedMessage ?: "Registration failed." }
            }
        } else {
            vm.loginWithPhone(
                phone = normalizedPhone,
                password = password,
                rememberMe = rememberMe,
            ) { res ->
                submitting = false
                when (res) {
                    is PhoneLoginResult.Success -> onSignedIn()
                    is PhoneLoginResult.RequiresGoogleOrOtp -> {
                        formError = res.message
                    }
                    is PhoneLoginResult.Error -> {
                        formError = res.message
                    }
                }
            }
        }
    }

    // Entrance & Ambient Animations
    val cardAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.94f) }
    val cardOffsetY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        launch { cardAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch {
            cardScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        }
        launch { cardOffsetY.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
    }

    val infinite = rememberInfiniteTransition(label = "glass_auth_animations")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit_angle",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse",
    )
    val rimSweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(8500, easing = LinearEasing), RepeatMode.Restart),
        label = "rim_sweep",
    )
    val shimmerFraction by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "button_shimmer",
    )

    TripleTapExitContainer(
        onTripleTapExit = onBackHome,
        modifier = Modifier.background(Color(0xFF0A0F24)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_books),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.12f)
                    .blur(26.dp),
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xB80B1437), Color(0xBD1A1442), Color(0xCC120D2E)),
                    ),
                )
                val orb1 = Offset(size.width * (0.20f + 0.08f * glowPulse), size.height * (0.18f + 0.04f * glowPulse))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xAA2563EB), Color(0x333B82F6), Color.Transparent),
                        center = orb1,
                        radius = size.minDimension * 0.65f,
                    ),
                    radius = size.minDimension * 0.65f,
                    center = orb1,
                )
                val orb2 = Offset(size.width * (0.85f - 0.08f * glowPulse), size.height * (0.82f - 0.05f * glowPulse))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x99C026D3), Color(0x338B5CF6), Color.Transparent),
                        center = orb2,
                        radius = size.minDimension * 0.68f,
                    ),
                    radius = size.minDimension * 0.68f,
                    center = orb2,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.widthIn(max = 404.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable(onClick = onBackHome)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Reading Corner",
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Reading Corner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!nfcNotice.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 404.dp)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x663B82F6))
                            .border(1.dp, GlassBorderCyan.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                            .clickable { onClearNfcNotice() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            text = nfcNotice,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                val rimStart = Offset(200f + 180f * cos(rimSweep), 200f + 180f * sin(rimSweep))
                val rimEnd = Offset(200f - 180f * cos(rimSweep), 200f - 180f * sin(rimSweep))
                val glassCardShape = RoundedCornerShape(30.dp)

                Box(
                    modifier = Modifier
                        .widthIn(max = 404.dp)
                        .fillMaxWidth()
                        .offset { IntOffset(0, cardOffsetY.value.roundToInt()) }
                        .scale(cardScale.value)
                        .alpha(cardAlpha.value)
                        .clip(glassCardShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0x523F72FF), Color(0x36342D6B), Color(0x479246C8)),
                            ),
                        )
                        .border(
                            width = 1.4.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    GlassBorderCyan.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.55f),
                                    GlassBorderMagenta.copy(alpha = 0.78f),
                                    Color.White.copy(alpha = 0.28f),
                                ),
                                start = rimStart,
                                end = rimEnd,
                            ),
                            shape = glassCardShape,
                        )
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        OrbitalGlassAvatar(
                            isCreateMode = mode == AuthMode.REGISTER,
                            dpDataUri = dpDataUri,
                            displayName = displayName,
                            orbitAngle = orbitAngle,
                            glowPulse = glowPulse,
                            onPickPhoto = {
                                if (mode == AuthMode.REGISTER && firebaseUser != null) {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                }
                            },
                        )

                        AnimatedVisibility(visible = mode == AuthMode.REGISTER && firebaseUser != null) {
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (dpDataUri != null) "Tap orb to change photo" else "Tap orb to add profile photo",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                )
                                if (dpDataUri != null) {
                                    Text(
                                        text = "Remove",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassAccentViolet,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { dpDataUri = null }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedContent(
                            targetState = mode,
                            transitionSpec = {
                                (fadeIn(tween(240)) + expandVertically()) togetherWith
                                    (fadeOut(tween(180)) + shrinkVertically())
                            },
                            label = "auth_headline",
                        ) { targetMode ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (targetMode == AuthMode.LOGIN) "Welcome Back!" else "Create Account",
                                    fontFamily = DisplayFontFamily,
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (targetMode == AuthMode.LOGIN) {
                                        "Sign in to continue your journey"
                                    } else {
                                        "One account per verified phone number"
                                    },
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.78f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Google Sign-In Gate (Mandatory Firebase Auth Session)
                        if (firebaseUser == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.11f))
                                    .border(1.dp, GlassBorderCyan.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                    .padding(14.dp),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "Connect Google to unlock cloud sync, real-time chat & personal NFC card links.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center,
                                    )
                                    GlassGradientButton(
                                        text = if (googleLoading) "Signing in…" else "Sign in with Google",
                                        icon = Icons.Filled.VerifiedUser,
                                        shimmerFraction = shimmerFraction,
                                        enabled = !googleLoading,
                                        onClick = {
                                            googleLoading = true
                                            googleError = null
                                            GoogleAuthHelper.onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onAuthSuccess = { googleLoading = false },
                                                onAuthError = { msg ->
                                                    googleLoading = false
                                                    googleError = msg
                                                },
                                                onAuthCancelled = { googleLoading = false },
                                            )
                                        },
                                        modifier = Modifier.testTag("google_sign_in_button"),
                                    )
                                    if (googleError != null) {
                                        Text(
                                            text = googleError!!,
                                            fontSize = 11.sp,
                                            color = GlassErrorText,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(GlassSuccessBg)
                                    .border(0.8.dp, Color(0x8834D399), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = GlassSuccessText,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cloud connected: ${firebaseUser.email ?: "Signed in"}",
                                    fontSize = 11.sp,
                                    color = GlassSuccessText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Phone Number Pill (Primary identifier for both Login and Register — no username needed for login!)
                        GlassPillTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it
                                phoneError = null
                                formError = null
                            },
                            placeholder = "Phone Number (e.g. +15551234567)",
                            leadingIcon = Icons.Filled.PhoneAndroid,
                            keyboardType = KeyboardType.Phone,
                            errorText = phoneError,
                            imeAction = ImeAction.Next,
                            modifier = Modifier.testTag("auth_phone_input"),
                        )

                        // Optional OTP verify strip inside Register mode
                        AnimatedVisibility(visible = mode == AuthMode.REGISTER && firebaseUser != null) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = if (regPhoneVerified) "Phone verified via SMS OTP" else "Verify phone with OTP (recommended)",
                                        fontSize = 11.sp,
                                        color = if (regPhoneVerified) GlassSuccessText else Color.White.copy(alpha = 0.72f),
                                    )
                                    if (!regPhoneVerified) {
                                        Text(
                                            text = if (regOtpBusy) "Sending…" else if (regVerificationId != null) "Resend" else "Send OTP",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassBorderCyan,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable(enabled = !regOtpBusy) { startRegOtp() }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                if (regVerificationId != null && !regPhoneVerified) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            GlassPillTextField(
                                                value = regOtpCode,
                                                onValueChange = { regOtpCode = it },
                                                placeholder = "6-digit SMS code",
                                                leadingIcon = Icons.Filled.Sms,
                                                keyboardType = KeyboardType.Number,
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .height(50.dp)
                                                .clip(CircleShape)
                                                .background(Brush.horizontalGradient(listOf(GlassBtnStart, GlassBtnEnd)))
                                                .clickable(enabled = !regOtpBusy) { verifyRegOtpCode() }
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("Verify", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Profile Name & Bio only in Register mode — allows ANY characters with zero restrictions!
                        AnimatedVisibility(
                            visible = mode == AuthMode.REGISTER,
                            enter = fadeIn(tween(240)) + expandVertically(),
                            exit = fadeOut(tween(180)) + shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassPillTextField(
                                    value = displayName,
                                    onValueChange = {
                                        displayName = it
                                        nameError = null
                                    },
                                    placeholder = "Profile Name",
                                    leadingIcon = Icons.Filled.Person,
                                    errorText = nameError,
                                    imeAction = ImeAction.Next,
                                    modifier = Modifier.testTag("auth_display_name_input"),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassPillTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    placeholder = "Short Bio (optional)",
                                    leadingIcon = Icons.Filled.Badge,
                                    imeAction = ImeAction.Next,
                                    modifier = Modifier.testTag("auth_bio_input"),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Pill
                        GlassPillTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                formError = null
                            },
                            placeholder = "Password",
                            leadingIcon = Icons.Filled.Lock,
                            isPassword = true,
                            passwordVisible = showPassword,
                            onTogglePasswordVisibility = { showPassword = !showPassword },
                            errorText = passwordError,
                            imeAction = if (mode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done,
                            onDone = { if (mode == AuthMode.LOGIN) submit() },
                            modifier = Modifier.testTag("auth_password_input"),
                        )

                        AnimatedVisibility(
                            visible = mode == AuthMode.REGISTER,
                            enter = fadeIn(tween(240)) + expandVertically(),
                            exit = fadeOut(tween(180)) + shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassPillTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        confirmError = null
                                    },
                                    placeholder = "Confirm Password",
                                    leadingIcon = Icons.Filled.Lock,
                                    isPassword = true,
                                    passwordVisible = showConfirm,
                                    onTogglePasswordVisibility = { showConfirm = !showConfirm },
                                    errorText = confirmError,
                                    imeAction = ImeAction.Done,
                                    onDone = ::submit,
                                    modifier = Modifier.testTag("auth_confirm_input"),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { rememberMe = !rememberMe }
                                    .padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(21.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (rememberMe) Brush.linearGradient(listOf(GlassBtnMid, GlassBtnEnd))
                                            else SolidColor(Color.White.copy(alpha = 0.14f)),
                                        )
                                        .border(
                                            1.dp,
                                            if (rememberMe) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f),
                                            RoundedCornerShape(6.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (rememberMe) {
                                        Icon(Icons.Filled.Check, contentDescription = "Remember me checked", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text("Remember me", fontSize = 13.sp, color = Color.White.copy(alpha = 0.88f), fontWeight = FontWeight.Medium)
                            }

                            if (mode == AuthMode.LOGIN) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassAccentViolet,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showForgotOtpDialog = true }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .testTag("auth_forgot_password"),
                                )
                            }
                        }

                        if (!infoBanner.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GlassSuccessBg)
                                    .border(1.dp, Color(0x8834D399), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                            ) {
                                Text(infoBanner!!, fontSize = 12.sp, color = GlassSuccessText, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (formError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassErrorBg)
                                    .border(1.dp, Color(0x88FB7185), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = formError!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassErrorText,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        GlassGradientButton(
                            text = if (submitting) {
                                "Please wait…"
                            } else if (mode == AuthMode.LOGIN) {
                                "Login"
                            } else {
                                "Register Account"
                            },
                            icon = if (mode == AuthMode.LOGIN) Icons.AutoMirrored.Filled.Login else Icons.Filled.PersonAdd,
                            shimmerFraction = shimmerFraction,
                            enabled = !submitting,
                            onClick = ::submit,
                            modifier = Modifier.testTag("auth_submit_button"),
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(thickness = 0.8.dp, color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (mode == AuthMode.LOGIN) "Don't have an account? " else "Already have an account? ",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        switchMode(if (mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .testTag("auth_mode_toggle"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = if (mode == AuthMode.LOGIN) "Register" else "Login",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassAccentViolet,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = GlassAccentViolet,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForgotOtpDialog && socialViewModel != null) {
        ForgotPasswordOtpDialog(
            initialPhone = phoneNumber,
            socialViewModel = socialViewModel,
            onDismiss = { showForgotOtpDialog = false },
            onResetSuccess = { updatedPhone ->
                phoneNumber = updatedPhone
                password = ""
                showForgotOtpDialog = false
                infoBanner = "Password updated via verified phone OTP. Signed in!"
                onSignedIn()
            },
        )
    }
}

@Composable
private fun ForgotPasswordOtpDialog(
    initialPhone: String,
    socialViewModel: SocialViewModel,
    onDismiss: () -> Unit,
    onResetSuccess: (String) -> Unit,
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf(initialPhone) }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNew by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var autoCredential by remember { mutableStateOf<PhoneAuthCredential?>(null) }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xEA1D1844),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.2.dp,
                    Brush.linearGradient(listOf(GlassBorderCyan.copy(alpha = 0.7f), GlassBorderMagenta.copy(alpha = 0.65f))),
                    RoundedCornerShape(26.dp),
                ),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Reset Password via OTP",
                    fontFamily = DisplayFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Verify your registered phone number with a real SMS OTP before setting a new password.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.76f),
                )

                GlassPillTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    placeholder = "Registered Phone Number (+...)",
                    leadingIcon = Icons.Filled.PhoneAndroid,
                    keyboardType = KeyboardType.Phone,
                )

                if (verificationId != null && autoCredential == null) {
                    GlassPillTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it; errorMsg = null },
                        placeholder = "6-digit SMS OTP Code",
                        leadingIcon = Icons.Filled.Sms,
                        keyboardType = KeyboardType.Number,
                    )
                }

                if (verificationId != null || autoCredential != null) {
                    GlassPillTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMsg = null },
                        placeholder = "New Password (min 6 chars)",
                        leadingIcon = Icons.Filled.Lock,
                        isPassword = true,
                    )
                    GlassPillTextField(
                        value = confirmNew,
                        onValueChange = { confirmNew = it; errorMsg = null },
                        placeholder = "Confirm New Password",
                        leadingIcon = Icons.Filled.Lock,
                        isPassword = true,
                    )
                }

                if (statusMsg != null) {
                    Text(statusMsg!!, fontSize = 12.sp, color = GlassSuccessText)
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, fontSize = 12.sp, color = GlassErrorText, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(GlassBtnStart, GlassBtnEnd)))
                            .clickable(enabled = !busy) {
                                val activity = context as? Activity
                                if (activity == null) {
                                    errorMsg = "Activity unavailable for OTP flow."
                                    return@clickable
                                }
                                if (verificationId == null && autoCredential == null) {
                                    busy = true
                                    errorMsg = null
                                    socialViewModel.startPhoneOtp(
                                        activity = activity,
                                        phone = phone,
                                        onCodeSent = { id ->
                                            busy = false
                                            verificationId = id
                                            statusMsg = "SMS OTP sent! Enter the code and choose a new password."
                                        },
                                        onAutoCredential = { cred ->
                                            busy = false
                                            autoCredential = cred
                                            statusMsg = "Phone auto-verified! Choose your new password."
                                        },
                                        onError = { err ->
                                            busy = false
                                            errorMsg = err
                                        },
                                    )
                                } else {
                                    if (newPassword.length < 6) {
                                        errorMsg = "Password must be at least 6 characters."
                                        return@clickable
                                    }
                                    if (newPassword != confirmNew) {
                                        errorMsg = "Passwords do not match."
                                        return@clickable
                                    }
                                    val cred = autoCredential ?: PhoneAuthProvider.getCredential(verificationId!!, otpCode.trim())
                                    busy = true
                                    errorMsg = null
                                    socialViewModel.completeOtpPasswordReset(phone, cred, newPassword) { res ->
                                        busy = false
                                        res.onSuccess { onResetSuccess(phone) }
                                            .onFailure { err -> errorMsg = err.localizedMessage ?: "Reset failed." }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (verificationId == null && autoCredential == null) "Send OTP" else "Verify & Reset",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbitalGlassAvatar(
    isCreateMode: Boolean,
    dpDataUri: String?,
    displayName: String,
    orbitAngle: Float,
    glowPulse: Float,
    onPickPhoto: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clickable(enabled = isCreateMode, onClick = onPickPhoto),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val haloRadius = size.minDimension * (0.46f + 0.04f * glowPulse)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x5560A5FA), Color(0x33A855F7), Color.Transparent),
                    center = center,
                    radius = haloRadius,
                ),
                radius = haloRadius,
                center = center,
            )

            rotate(degrees = -22f, pivot = center) {
                val rx = size.width * 0.44f
                val ry = size.height * 0.17f
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            GlassBorderCyan.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.9f),
                            GlassBorderMagenta.copy(alpha = 0.85f),
                            Color(0x553B82F6),
                            GlassBorderCyan.copy(alpha = 0.85f),
                        ),
                        center = center,
                    ),
                    topLeft = Offset(center.x - rx, center.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
                )
                val rad = orbitAngle * (PI / 180.0)
                val px = center.x + (rx * cos(rad)).toFloat()
                val py = center.y + (ry * sin(rad)).toFloat()
                drawCircle(color = Color.White, radius = 3.2.dp.toPx(), center = Offset(px, py))
                drawCircle(color = GlassBorderCyan.copy(alpha = 0.55f), radius = 6.5.dp.toPx(), center = Offset(px, py))
            }

            val starAlpha = 0.45f + 0.5f * glowPulse
            val sx = size.width * 0.14f
            val sy = size.height * 0.24f
            val rOuter = 6.dp.toPx()
            val rInner = 1.6.dp.toPx()
            val starPath = Path().apply {
                moveTo(sx, sy - rOuter)
                lineTo(sx + rInner, sy - rInner)
                lineTo(sx + rOuter, sy)
                lineTo(sx + rInner, sy + rInner)
                lineTo(sx, sy + rOuter)
                lineTo(sx - rInner, sy + rInner)
                lineTo(sx - rOuter, sy)
                lineTo(sx - rInner, sy - rInner)
                close()
            }
            drawPath(starPath, color = Color.White.copy(alpha = starAlpha))
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0x5993C5FD), Color(0x406D28D9), Color(0x59EC4899))))
                .border(1.4.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.25f))), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isCreateMode && dpDataUri != null) {
                AsyncImage(
                    model = dpDataUri,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (isCreateMode && displayName.trim().isNotEmpty()) {
                Text(
                    text = initialsOf(displayName),
                    fontFamily = DisplayFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Account orb",
                    tint = Color.White.copy(alpha = 0.94f),
                    modifier = Modifier.size(38.dp),
                )
            }
        }

        if (isCreateMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GlassBtnStart, GlassBtnEnd)))
                    .border(1.2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Pick photo", tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun GlassPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            errorText != null -> Color(0xFFFB7185)
            focused -> GlassBorderCyan.copy(alpha = 0.9f)
            else -> Color.White.copy(alpha = 0.34f)
        },
        animationSpec = tween(200),
        label = "field_border",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (focused) 0.18f else 0.11f,
        animationSpec = tween(200),
        label = "field_bg_alpha",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = bgAlpha))
                .border(1.2.dp, borderColor, CircleShape)
                .padding(start = 8.dp, end = if (isPassword) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(leadingIcon, contentDescription = null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(18.dp))
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(GlassBorderCyan),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                        imeAction = imeAction,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                )
            }

            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(onClick = onTogglePasswordVisibility, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassErrorText,
                modifier = Modifier.padding(start = 18.dp, top = 5.dp),
            )
        }
    }
}

@Composable
private fun GlassGradientButton(
    text: String,
    icon: ImageVector,
    shimmerFraction: Float,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "login_button_scale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.7f)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(GlassBtnStart, GlassBtnMid, GlassBtnEnd)))
            .border(1.dp, Color.White.copy(alpha = 0.38f), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val sheenX = size.width * shimmerFraction
            val sheenWidth = size.width * 0.36f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent),
                    startX = sheenX - sheenWidth / 2f,
                    endX = sheenX + sheenWidth / 2f,
                ),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
