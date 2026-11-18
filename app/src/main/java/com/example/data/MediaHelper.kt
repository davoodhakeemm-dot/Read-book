package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.example.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URL

sealed class MediaEncodeResult {
    data class Ready(val dataUri: String, val kind: MediaKind) : MediaEncodeResult()
    data class LocalOnlyWarning(val localPath: String, val reason: String) : MediaEncodeResult()
    data class Failure(val message: String) : MediaEncodeResult()
}

object MediaHelper {
    suspend fun compressPickedImageToDataUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 720,
        maxBytes: Int = 260_000,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDimension * 2 ||
                boundsOpts.outHeight / sampleSize > maxDimension * 2
            ) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val ratio = minOf(
                maxDimension.toFloat() / decoded.width.coerceAtLeast(1),
                maxDimension.toFloat() / decoded.height.coerceAtLeast(1),
                1f,
            )
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true,
                ).also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }

            var quality = 82
            val out = ByteArrayOutputStream()
            do {
                out.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                quality -= 10
            } while (out.size() > maxBytes && quality >= 30)
            scaled.recycle()

            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            null
        }
    }

    suspend fun encodeSmallMediaToDataUri(
        context: Context,
        uri: Uri,
        kind: MediaKind,
        maxBytes: Int = 550_000,
    ): MediaEncodeResult = withContext(Dispatchers.IO) {
        try {
            if (kind == MediaKind.IMAGE) {
                val img = compressPickedImageToDataUri(context, uri)
                return@withContext if (img != null) {
                    MediaEncodeResult.Ready(img, MediaKind.IMAGE)
                } else {
                    MediaEncodeResult.Failure("Unable to process selected photo.")
                }
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext MediaEncodeResult.Failure("Could not read selected file.")
            val mime = context.contentResolver.getType(uri)
                ?: if (kind == MediaKind.VIDEO) "video/mp4" else "audio/mp4"
            if (bytes.size > maxBytes) {
                val ext = if (kind == MediaKind.VIDEO) "mp4" else "m4a"
                val dir = File(context.filesDir, "chat_media").apply { mkdirs() }
                val localFile = File(dir, "media_${System.currentTimeMillis()}.$ext")
                localFile.writeBytes(bytes)
                return@withContext MediaEncodeResult.LocalOnlyWarning(
                    localPath = localFile.absolutePath,
                    reason = "Media exceeds 550 KB Firestore sync size (${bytes.size / 1024} KB). Enable Firebase Storage in Firebase Console for large cross-device video uploads.",
                )
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            MediaEncodeResult.Ready("data:$mime;base64,$b64", kind)
        } catch (e: Exception) {
            MediaEncodeResult.Failure(e.localizedMessage ?: "Failed to encode media.")
        }
    }

    suspend fun encodeAudioFileToDataUri(
        file: File,
        maxBytes: Int = 450_000,
    ): MediaEncodeResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() <= 0L) {
                return@withContext MediaEncodeResult.Failure("Recording is empty.")
            }
            val bytes = file.readBytes()
            if (bytes.size > maxBytes) {
                return@withContext MediaEncodeResult.LocalOnlyWarning(
                    localPath = file.absolutePath,
                    reason = "Voice note exceeds 450 KB Firestore sync size.",
                )
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            MediaEncodeResult.Ready("data:audio/mp4;base64,$b64", MediaKind.AUDIO)
        } catch (e: Exception) {
            MediaEncodeResult.Failure(e.localizedMessage ?: "Voice encoding failed.")
        }
    }

    /** Materializes a data:image/video/audio URI into a cache File for Android VideoView / MediaPlayer playback. */
    suspend fun materializePlayableFile(context: Context, source: String, extFallback: String): File? =
        withContext(Dispatchers.IO) {
            try {
                if (!source.startsWith("data:") && !source.startsWith("http")) {
                    return@withContext File(source).takeIf { it.exists() }
                }
                val cacheDir = File(context.cacheDir, "media_preview").apply { mkdirs() }
                val hashName = "${source.hashCode().toUInt().toString(16)}.$extFallback"
                val target = File(cacheDir, hashName)
                if (target.exists() && target.length() > 0L) return@withContext target

                if (source.startsWith("data:")) {
                    val commaIdx = source.indexOf(',')
                    if (commaIdx <= 0) return@withContext null
                    val b64 = source.substring(commaIdx + 1)
                    val decoded = Base64.decode(b64, Base64.DEFAULT)
                    target.writeBytes(decoded)
                    target
                } else {
                    URL(source).openStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    target
                }
            } catch (_: Exception) {
                null
            }
        }

    /** Downloads / saves received photo, video, or audio into scoped MediaStore storage (zero permissions required on API 29+). */
    suspend fun saveMediaToDevice(
        context: Context,
        mediaSource: String,
        kind: MediaKind,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (mime, ext) = when (kind) {
                MediaKind.IMAGE -> "image/jpeg" to "jpg"
                MediaKind.VIDEO -> "video/mp4" to "mp4"
                MediaKind.AUDIO -> "audio/mp4" to "m4a"
            }
            val filename = "ReadingCorner_${System.currentTimeMillis()}.$ext"
            val bytes = readSourceBytes(context, mediaSource)
                ?: return@withContext Result.failure(IllegalStateException("Media file unavailable."))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = when (kind) {
                    MediaKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    MediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    MediaKind.AUDIO -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
                val relDir = when (kind) {
                    MediaKind.IMAGE -> "${Environment.DIRECTORY_PICTURES}/ReadingCorner"
                    MediaKind.VIDEO -> "${Environment.DIRECTORY_MOVIES}/ReadingCorner"
                    MediaKind.AUDIO -> "${Environment.DIRECTORY_DOWNLOADS}/ReadingCorner"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val destUri = resolver.insert(collection, values)
                    ?: return@withContext Result.failure(IllegalStateException("Could not create MediaStore entry."))
                resolver.openOutputStream(destUri)?.use { out ->
                    out.write(bytes)
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(destUri, values, null, null)
                Result.success("Saved to device: $filename")
            } else {
                val baseDir = context.getExternalFilesDir(
                    when (kind) {
                        MediaKind.IMAGE -> Environment.DIRECTORY_PICTURES
                        MediaKind.VIDEO -> Environment.DIRECTORY_MOVIES
                        MediaKind.AUDIO -> Environment.DIRECTORY_DOWNLOADS
                    },
                ) ?: context.filesDir
                val outFile = File(baseDir, filename)
                outFile.writeBytes(bytes)
                Result.success("Saved: ${outFile.name}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readSourceBytes(context: Context, source: String): ByteArray? {
        return when {
            source.startsWith("data:") -> {
                val commaIdx = source.indexOf(',')
                if (commaIdx <= 0) null
                else Base64.decode(source.substring(commaIdx + 1), Base64.DEFAULT)
            }
            source.startsWith("http") -> {
                URL(source).openStream().use(InputStream::readBytes)
            }
            source.startsWith("content://") -> {
                context.contentResolver.openInputStream(Uri.parse(source))?.use(InputStream::readBytes)
            }
            else -> {
                val file = File(source)
                if (file.exists()) file.readBytes() else null
            }
        }
    }
}
