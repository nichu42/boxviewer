package de.nichu42.boxviewer.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.content.Context
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.nichu42.boxviewer.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrCodeGenerator {

    fun decodeLogo(context: Context): Bitmap? =
        BitmapFactory.decodeResource(context.resources, R.drawable.boxviewer_icon_white_bg)

    fun generateQrBitMatrix(content: String, sizePx: Int): BitMatrix {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )
        return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    }

    fun overlayLogo(qrBitmap: Bitmap, logo: Bitmap): Bitmap {
        val size = qrBitmap.width
        val result = qrBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Logo size: 22% of QR size (H error correction handles up to 30% loss)
        val logoSize = (size * 0.22f).toInt()
        val scaledLogo = logo.scale(logoSize, logoSize, filter = true)

        val left = (size - logoSize) / 2f
        val top = (size - logoSize) / 2f

        // Draw a rounded white background for the logo
        val bgPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val padding = logoSize * 0.1f
        canvas.drawRoundRect(
            left - padding,
            top - padding,
            left + logoSize + padding,
            top + logoSize + padding,
            padding * 2,
            padding * 2,
            bgPaint
        )

        canvas.drawBitmap(scaledLogo, left, top, null)
        return result
    }
}

fun BitMatrix.toBitmap(fg: Int = Color.BLACK, bg: Int = Color.WHITE): Bitmap {
    val width = this.width
    val height = this.height
    val bmp = createBitmap(width, height)
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (this[x, y]) fg else bg
        }
    }
    bmp.setPixels(pixels, 0, width, 0, 0, width, height)
    return bmp
}

@Composable
fun QrCodeCanvas(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 240.dp,
    showLogo: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val px = with(density) { sizeDp.toPx().toInt() }
    val fgArgb = androidx.compose.ui.graphics.Color.Black.toArgb()
    val bgArgb = androidx.compose.ui.graphics.Color.White.toArgb()

    val bitmapState by produceState<ImageBitmap?>(null, content, px, fgArgb, bgArgb, showLogo) {
        value = withContext(Dispatchers.Default) {
            try {
                val matrix = QrCodeGenerator.generateQrBitMatrix(content, px)
                var bitmap = matrix.toBitmap(fg = fgArgb, bg = bgArgb)
                
                if (showLogo) {
                    val logo = QrCodeGenerator.decodeLogo(context)
                    if (logo != null) {
                        bitmap = QrCodeGenerator.overlayLogo(bitmap, logo)
                    }
                }
                
                bitmap.asImageBitmap()
            } catch (_: WriterException) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        bitmapState?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = "QR Code",
                modifier = Modifier.size(sizeDp)
            )
        }
    }
}
