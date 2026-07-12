package de.nichu42.boxviewer.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import de.nichu42.boxviewer.data.api.SenseBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    private const val SHARE_BASE_URL = "https://share.boxviewer.app"

    fun buildBoxDeepLink(boxId: String): String = "$SHARE_BASE_URL/?id=$boxId"

    fun shareText(context: Context, label: String, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, label)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share senseBox Link"))
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    suspend fun shareQrImage(context: Context, bitmap: Bitmap, fileName: String, linkText: String) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "qr")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.file_provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, linkText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(shareIntent, "Share senseBox QR Code"))
        }
    }

    /**
     * Saves a QR bitmap to the device gallery via MediaStore on API 29+.
     * Returns the content Uri on success, null on failure or unsupported API.
     */
    suspend fun saveQrToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null

        val name = if (fileName.endsWith(".png")) fileName else "$fileName.png"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BoxViewer")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            null
        }
    }

    fun boxSummaryForSharing(box: SenseBox): String {
        val link = buildBoxDeepLink(box.id)
        val name = box.name
        val exposure = box.exposure ?: ""
        val sensorCount = box.sensors?.size ?: 0
        val parts = buildString {
            append("senseBox: $name")
            if (exposure.isNotBlank()) append(" ($exposure)")
            append("\nSensors: $sensorCount")
            append("\nOpen in BoxViewer: $link")
        }
        return parts
    }
}
