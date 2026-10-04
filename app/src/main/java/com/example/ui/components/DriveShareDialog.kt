package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ApkSharingHelper

@Composable
fun DriveShareDialog(
    viewModel: MainViewModel,
    inviteCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    val downloadUrl = if (settings.driveShareUrl.isBlank() || settings.driveShareUrl.contains("tinyurl")) {
        ApkSharingHelper.DEFAULT_DOWNLOAD_URL
    } else {
        settings.driveShareUrl
    }
    val fullMessage = remember(inviteCode, downloadUrl) {
        ApkSharingHelper.buildInvitationMessage(inviteCode, downloadUrl)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, CardBorderColor),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Namaz Arkadaşını Davet Et",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Doğrudan APK indirme linki ve kod ile paylaş",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = CardBorderColor)

                // Message Preview Card
                Text(
                    text = "Gönderilecek Davet Mesajı (Önizleme):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftInfoCardBg),
                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = fullMessage,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF1E3A34)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick copy message button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Namaz Arkadaşım Daveti", fullMessage))
                                Toast.makeText(context, "Davet mesajı ve link kopyalandı!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tüm Mesajı ve Linki Kopyala", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Action 1: WhatsApp
                Button(
                    onClick = {
                        ApkSharingHelper.shareViaWhatsApp(context, fullMessage)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp ile Gönder",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Action 2: System Chooser (SMS, Telegram, Mail, etc.)
                Button(
                    onClick = {
                        ApkSharingHelper.shareViaSystemChooser(context, fullMessage)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diğer Uygulamalarla Paylaş (SMS, vb.)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Action 3: Copy Code Only
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Davet Kodu", inviteCode))
                        Toast.makeText(context, "Davet Kodu ($inviteCode) kopyalandı!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sadece Davet Kodunu Kopyala ($inviteCode)", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}
