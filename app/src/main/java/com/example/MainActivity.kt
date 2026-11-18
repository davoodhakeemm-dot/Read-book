package com.example

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CryptoUtils
import com.example.ui.navigation.ReadingCornerNavHost
import com.example.ui.theme.ReadingCornerTheme

class MainActivity : ComponentActivity() {
    private var pendingNfcToken by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNfcToken = extractNfcOrDeepLinkToken(intent)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val books by viewModel.books.collectAsStateWithLifecycle()
            val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
            val chatState by viewModel.chatState.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            ReadingCornerTheme(appThemeMode = libraryState.appTheme) {
                ReadingCornerNavHost(
                    viewModel = viewModel,
                    books = books,
                    libraryState = libraryState,
                    chatState = chatState,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    pendingNfcToken = pendingNfcToken,
                    onConsumeNfcToken = { pendingNfcToken = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNfcOrDeepLinkToken(intent)?.let { token ->
            pendingNfcToken = token
        }
    }

    private fun extractNfcOrDeepLinkToken(intent: Intent?): String? {
        if (intent == null) return null
        intent.dataString?.let { raw ->
            CryptoUtils.parseCardTokenFromUriString(raw)?.let { return it }
        }
        val rawMsgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }
        rawMsgs?.forEach { msg ->
            val ndef = msg as? NdefMessage ?: return@forEach
            ndef.records.forEach { rec ->
                val uri = rec.toUri()?.toString()
                CryptoUtils.parseCardTokenFromUriString(uri)?.let { return it }
            }
        }
        return null
    }
}
