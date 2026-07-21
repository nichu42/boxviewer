package de.nichu42.boxviewer.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.nichu42.boxviewer.R
import de.nichu42.boxviewer.data.api.SenseBox
import de.nichu42.boxviewer.util.QrCodeCanvas
import de.nichu42.boxviewer.util.QrCodeGenerator
import de.nichu42.boxviewer.util.ShareUtils
import de.nichu42.boxviewer.util.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareQrDialog(
    box: SenseBox,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val shareClipboardLabelMsg = stringResource(R.string.share_clipboard_label)
    val shareSubjectMsg = stringResource(R.string.share_subject)
    val shareQrFailedMsg = stringResource(R.string.share_qr_failed)
    val shareQrSavedSuccessMsg = stringResource(R.string.share_qr_saved_success)
    val shareQrSaveRequiresAndroid10Msg = stringResource(R.string.share_qr_save_requires_android_10)
    val shareQrSaveFailedMsg = stringResource(R.string.share_qr_save_failed)

    val link = remember(box.id) { ShareUtils.buildBoxDeepLink(box.id) }
    val summaryText = remember(box) { ShareUtils.boxSummaryForSharing(box) }
    val fileName = remember(box.id) { "qr_${box.id}.png" }

    var isExporting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.share_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = box.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                QrCodeCanvas(
                    content = link,
                    sizeDp = 220.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = link,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                label = { Text(stringResource(R.string.share_deep_link_label), fontSize = 11.sp) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        ShareUtils.copyToClipboard(context, shareClipboardLabelMsg, link)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_copy), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        ShareUtils.shareText(context, shareSubjectMsg, summaryText)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share_link), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isExporting) return@OutlinedButton
                        isExporting = true
                        coroutineScope.launch {
                            try {
                                val matrix = withContext(Dispatchers.Default) {
                                    QrCodeGenerator.generateQrBitMatrix(link, 1024)
                                }
                                val bitmap = withContext(Dispatchers.Default) {
                                    var bmp = matrix.toBitmap()
                                    val logo = QrCodeGenerator.decodeLogo(context)
                                    if (logo != null) {
                                        bmp = QrCodeGenerator.overlayLogo(bmp, logo)
                                    }
                                    bmp
                                }
                                ShareUtils.shareQrImage(context, bitmap, fileName, summaryText)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, shareQrFailedMsg, Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share_qr), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        if (isExporting) return@OutlinedButton
                        isExporting = true
                        coroutineScope.launch {
                            try {
                                val matrix = withContext(Dispatchers.Default) {
                                    QrCodeGenerator.generateQrBitMatrix(link, 1024)
                                }
                                val bitmap = withContext(Dispatchers.Default) {
                                    var bmp = matrix.toBitmap()
                                    val logo = QrCodeGenerator.decodeLogo(context)
                                    if (logo != null) {
                                        bmp = QrCodeGenerator.overlayLogo(bmp, logo)
                                    }
                                    bmp
                                }
                                val savedUri = ShareUtils.saveQrToGallery(context, bitmap, fileName)
                                withContext(Dispatchers.Main) {
                                    if (savedUri != null) {
                                            Toast.makeText(context, shareQrSavedSuccessMsg, Toast.LENGTH_SHORT).show()
                                    } else {
                                            Toast.makeText(context, shareQrSaveRequiresAndroid10Msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, shareQrSaveFailedMsg, Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_save_qr), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}
