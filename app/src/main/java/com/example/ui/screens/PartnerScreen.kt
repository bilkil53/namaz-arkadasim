package com.example.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DriveShareDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PartnerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val partnerInfo by viewModel.partnerInfo.collectAsState()
    val todayPrayer by viewModel.todayPrayer.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isRefreshing by viewModel.isRefreshingPartner.collectAsState()

    var showUnmatchDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newPartnerNameInput by remember { mutableStateOf("") }

    var showEditMyNameDialog by remember { mutableStateOf(false) }
    var myNewNameInput by remember { mutableStateOf("") }

    var inviteCodeInput by remember { mutableStateOf("") }
    var customNicknameInput by remember { mutableStateOf("") }
    var showDriveDialog by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Native Android Google Account Picker Launcher
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val displayName = accountName.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                viewModel.loginWithGoogleBrowser(email = accountName, name = displayName)
            } else {
                viewModel.loginWithGoogleBrowser()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status banner if present
        if (statusMessage != null) {
            item {
                Surface(
                    color = EmeraldContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage ?: "",
                            color = OnEmeraldContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // USER PROFILE CARD (Always visible whether matched or not!)
        // -----------------------------------------------------------------
        item {
            if (settings.isLoggedIn) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (settings.userName.ifBlank { "M" }).take(1).uppercase(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = settings.userName.ifBlank { "Mümin" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        myNewNameInput = settings.userName
                                        showEditMyNameDialog = true
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "İsmi Düzenle",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            if (settings.userEmail.isNotBlank()) {
                                Text(
                                    text = settings.userEmail,
                                    fontSize = 12.sp,
                                    color = Color(0xFF86EFAC)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 3.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Google Hesabı Aktif ✓",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.logoutUser() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Çıkış Yap",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // If not logged in, show prominent Google Login Card
                Card(
                    onClick = {
                        var intentHandled = false
                        try {
                            val chooseAccountIntent = AccountManager.newChooseAccountIntent(
                                null, null, arrayOf("com.google"), null, null, null, null
                            )
                            googleAccountPickerLauncher.launch(chooseAccountIntent)
                            intentHandled = true
                        } catch (_: Exception) { }

                        if (!intentHandled) {
                            val gmailIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                            if (gmailIntent != null) {
                                try {
                                    context.startActivity(gmailIntent)
                                    intentHandled = true
                                } catch (_: Exception) { }
                            }
                        }

                        if (!intentHandled) {
                            viewModel.loginWithGoogleBrowser()
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("google_login_button")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("G", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google ile Devam Et", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Profilini oluştur ve yedeklerini güvenle tut", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // =================================================================
        // SCENARIO 1: NOT MATCHED YET (Only show invite & pairing forms)
        // =================================================================
        if (!partnerInfo.isMatched) {

            // 1. Prominent Friend Invite Link Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Share,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "1. Kişi: Namaz Arkadaşı Daveti",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Davet Kodun: ${partnerInfo.myInviteCode}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF86EFAC),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "💡 Bu kodu eşine / arkadaşına gönder. Karşı taraf kodu girdiğinde SENİN HİÇBİR KOD GİRMENE GEREK KALMADAN uygulaman otomatik olarak eşleşecektir!",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color.White.copy(alpha = 0.92f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showDriveDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Davet Et & Paylaş", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Davet Kodu", partnerInfo.myInviteCode))
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kodu Kopyala", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Enter Partner's Code Card (For User 2)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "2. Kişi: Sana Gelen Davet Kodunu Gir",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Arkadaşının veya eşinin sana gönderdiği 4 haneli davet kodunu buraya yapıştırıp eşleş:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it.uppercase() },
                            label = { Text("Davet Kodu (Örn: ARK-1234)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customNicknameInput,
                            onValueChange = { customNicknameInput = it },
                            label = { Text("Ona vermek istediğin isim (Örn: Eşim, Canım)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (inviteCodeInput.isNotBlank()) {
                                    viewModel.matchPartnerWithCode(
                                        code = inviteCodeInput.trim(),
                                        partnerName = customNicknameInput.trim()
                                    )
                                    inviteCodeInput = ""
                                    customNicknameInput = ""
                                }
                            },
                            enabled = inviteCodeInput.isNotBlank() && !isRefreshing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Eşleşiliyor...")
                            } else {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hemen Eşleş", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Privacy Notice
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftInfoCardBg),
                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Konum bilgisi veya özel veriler asla paylaşılmaz. Uygulama sadece 2 kişi (eş/arkadaş) arasındadır. 3. bir kişi dahil olamaz.",
                            fontSize = 12.sp,
                            color = Color(0xFF2C4A42),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // =================================================================
        // SCENARIO 2: MATCHED (Exclusively between 2 people - NO INVITE ICONS)
        // =================================================================
        if (partnerInfo.isMatched) {

            // Active Partner Match Card (Pine Green with White Text)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = partnerInfo.partnerDisplayName.ifBlank { "Namaz Arkadaşım" },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                newPartnerNameInput = partnerInfo.partnerDisplayName
                                                showEditNameDialog = true
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Adı Değiştir", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(15.dp))
                                        }
                                    }

                                    if (partnerInfo.partnerEmail.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = partnerInfo.partnerEmail,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Bağlı Eş / Arkadaş",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { viewModel.refreshPartnerData() },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Yenile", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { showUnmatchDialog = true },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.LinkOff, contentDescription = "Eşleşmeyi Bitir", tint = Color(0xFFFCA5A5), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Eşimin Bugünkü Namaz Durumu:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Son: ${timeFormatter.format(Date(partnerInfo.lastSyncTime))}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // True partner statuses
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PartnerMiniStatusPineItem("Sabah", partnerInfo.partnerFajr)
                            PartnerMiniStatusPineItem("Öğle", partnerInfo.partnerDhuhr)
                            PartnerMiniStatusPineItem("İkindi", partnerInfo.partnerAsr)
                            PartnerMiniStatusPineItem("Akşam", partnerInfo.partnerMaghrib)
                            PartnerMiniStatusPineItem("Yatsı", partnerInfo.partnerIsha)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kaza İlerlemesi:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${partnerInfo.partnerKazaCompleted} kaza kıldı  •  ${partnerInfo.partnerKazaOwed} borç",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC)
                            )
                        }
                    }
                }
            }
        }
    }

    // Google Drive & WhatsApp / Mail Sharing Dialog (Only callable in non-matched state)
    if (showDriveDialog && !partnerInfo.isMatched) {
        DriveShareDialog(
            viewModel = viewModel,
            inviteCode = partnerInfo.myInviteCode,
            onDismiss = { showDriveDialog = false }
        )
    }

    // Edit My Profile Name Dialog
    if (showEditMyNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditMyNameDialog = false },
            title = { Text("Profil İsmini Düzenle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Uygulama içinde ve eşleşmelerde görünecek adınız:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = myNewNameInput,
                        onValueChange = { myNewNameInput = it },
                        label = { Text("Adınız / Takma Adınız") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (myNewNameInput.isNotBlank()) {
                            viewModel.updateMyProfileName(myNewNameInput.trim())
                        }
                        showEditMyNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMyNameDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
    }

    // Edit Partner Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Eşinin İsmini Düzenle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Eşine veya namaz arkadaşına özel bir hitap verin:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPartnerNameInput,
                        onValueChange = { newPartnerNameInput = it },
                        label = { Text("İsim / Takma Ad (Örn: Eşim, Canım, Babam)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPartnerNameInput.isNotBlank()) {
                            viewModel.updatePartnerName(newPartnerNameInput.trim())
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
    }

    // Unmatch Confirmation Dialog
    if (showUnmatchDialog) {
        AlertDialog(
            onDismissRequest = { showUnmatchDialog = false },
            title = { Text("Eşleşmeyi Sonlandır", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("${partnerInfo.partnerDisplayName} ile olan ibadet eşleşmesini sonlandırmak istediğinizden emin misiniz? Eşleşme sonlandırıldıktan sonra yeni bir arkadaş davet edebilirsiniz.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unmatchPartner()
                        showUnmatchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                ) {
                    Text("Bağlantıyı Kes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnmatchDialog = false }) {
                    Text("Vazgeç", color = EmeraldPrimary)
                }
            }
        )
    }
}

@Composable
private fun PartnerMiniStatusPineItem(name: String, status: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
        Spacer(modifier = Modifier.height(4.dp))
        when (status) {
            "PRAYED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = "Kıldı", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "Kıldı", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
            }
            "MISSED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Kılmadı", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "Kılmadı", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
            }
            "EXCUSED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Muaf", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "Muaf", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.8f))
            }
            else -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "—", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "Bekliyor", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
