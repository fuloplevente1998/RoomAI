package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    private val SUPPORTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    /**
     * Validates that the uploaded Uri is a valid JPG, JPEG, PNG or WebP image
     * and decodes + compresses it so it stays well under 20MB for Gemini AI processing.
     */
    suspend fun loadAndCompressUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1536
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)?.lowercase()
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString()).lowercase()

            if (mimeType != null && mimeType !in SUPPORTED_MIME_TYPES) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "Nem támogatott képformátum ($mimeType). Kérjük, JPG, JPEG, PNG vagy WebP fájlt tölts fel."
                    )
                )
            } else if (mimeType == null && extension.isNotBlank() && extension !in SUPPORTED_EXTENSIONS) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "Nem támogatott fájlkiterjesztés (.$extension). Támogatott formátumok: JPG, JPEG, PNG, WebP."
                    )
                )
            }

            // 1. Decode bounds first
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("A kiválasztott képfájl sérült vagy nem olvasható.")
                )
            }

            // 2. Calculate sample size
            var inSampleSize = 1
            while (
                boundsOptions.outWidth / inSampleSize > maxDimension * 2 ||
                boundsOptions.outHeight / inSampleSize > maxDimension * 2
            ) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(
                IllegalArgumentException("Nem sikerült megnyitni a kiválasztott képet.")
            )

            // 3. Resize to maxDimension preserving aspect ratio
            val scaledBitmap = scaleDownIfNeeded(rawBitmap, maxDimension)

            // 4. Compress to JPEG quality 88 to ensure optimal size (< 2MB) for AI processing
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, outputStream)
            val compressedBytes = outputStream.toByteArray()
            val finalBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                ?: scaledBitmap

            Result.success(finalBitmap)
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException(
                    e.localizedMessage ?: "Hiba történt a kép feldolgozása közben."
                )
            )
        }
    }

    fun loadDrawableBitmap(context: Context, drawableResId: Int, maxDimension: Int = 1280): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeResource(context.resources, drawableResId, options)
        return scaleDownIfNeeded(raw, maxDimension)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val (targetWidth, targetHeight) = if (ratio > 1f) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Saves a Bitmap to internal app storage under /files/designs/ and returns the absolute path.
     */
    suspend fun saveBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        prefix: String
    ): String = withContext(Dispatchers.IO) {
        val designsDir = File(context.filesDir, "designs").apply {
            if (!exists()) mkdirs()
        }
        val file = File(designsDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        file.absolutePath
    }

    /**
     * Loads a Bitmap from an internal storage file path or drawable resource identifier.
     */
    suspend fun loadBitmapFromPathOrRes(context: Context, pathOrRes: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (pathOrRes.startsWith("res:")) {
                    val resId = pathOrRes.removePrefix("res:").toIntOrNull() ?: return@withContext null
                    loadDrawableBitmap(context, resId)
                } else {
                    val file = File(pathOrRes)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Saves a Bitmap to the user's Android MediaStore Gallery (Pictures/RoomAI).
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String,
        highResolution: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "RoomAI_${title.replace(" ", "_")}_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RoomAI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Nem sikerült létrehozni a fájlt a galériában."))

            resolver.openOutputStream(imageUri)?.use { out ->
                val quality = if (highResolution) 98 else 88
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.success("Sikeresen mentve a Galériába (Pictures/RoomAI/$fileName)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares a Bitmap using Android Share Sheet via FileProvider.
     */
    suspend fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        styleName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "images").apply {
                if (!exists()) mkdirs()
            }
            val sharedFile = File(cacheDir, "RoomAI_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(sharedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, sharedFile)

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Nézd meg a RoomAI belsőépítész által készített $styleName stílusú szobatervemet!"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Látványterv megosztása").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a temporary Uri in cache/images/ for Camera capture.
     */
    fun createCameraCaptureUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(cacheDir, "camera_room_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}
