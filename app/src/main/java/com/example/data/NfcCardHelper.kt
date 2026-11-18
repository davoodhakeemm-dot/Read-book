package com.example.data

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

sealed class NfcHardwareStatus {
    data object Ready : NfcHardwareStatus()
    data object Disabled : NfcHardwareStatus()
    data object Unsupported : NfcHardwareStatus()
}

object NfcCardHelper {
    fun getStatus(activity: Activity): NfcHardwareStatus {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return NfcHardwareStatus.Unsupported
        return if (adapter.isEnabled) NfcHardwareStatus.Ready else NfcHardwareStatus.Disabled
    }

    fun enableWriteSession(
        activity: Activity,
        personalUrl: String,
        onResult: (Result<String>) -> Unit,
    ): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return false
        if (!adapter.isEnabled) return false

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V

        adapter.enableReaderMode(
            activity,
            { tag: Tag? ->
                if (tag == null) return@enableReaderMode
                val writeResult = writeUriToTag(tag, personalUrl)
                activity.runOnUiThread { onResult(writeResult) }
            },
            flags,
            null,
        )
        return true
    }

    fun disableWriteSession(activity: Activity) {
        try {
            NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
        } catch (_: Exception) {
        }
    }

    private fun writeUriToTag(tag: Tag, url: String): Result<String> {
        val message = NdefMessage(arrayOf(NdefRecord.createUri(url)))
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                ndef.use { conn ->
                    if (!conn.isWritable) {
                        return Result.failure(IllegalStateException("This NFC card is read-only."))
                    }
                    val size = message.toByteArray().size
                    if (conn.maxSize < size) {
                        return Result.failure(IllegalStateException("NFC card capacity is too small."))
                    }
                    conn.writeNdefMessage(message)
                }
                Result.success("NFC card programmed with your personal Reading Corner link!")
            } else {
                val formatable = NdefFormatable.get(tag)
                    ?: return Result.failure(IllegalStateException("Tag does not support NDEF format."))
                formatable.connect()
                formatable.use { fmt ->
                    fmt.format(message)
                }
                Result.success("NFC card formatted and linked to your account!")
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to write NFC card.", e))
        }
    }
}
