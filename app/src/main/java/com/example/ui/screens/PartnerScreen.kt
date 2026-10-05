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
import androidx.compose.foundation.clickable
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

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterdayDateStr = remember {
        val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
    }

    val childPrefs = remember { context.getSharedPreferences("family_child_prefs", Context.MODE_PRIVATE) }
    var childName by remember { mutableStateOf(childPrefs.getString("child_name", "Çocuğum") ?: "Çocuğum") }
    var showEditChildNameDialog by remember { mutableStateOf(false) }
    var newChildNameInput by remember { mutableStateOf("") }

    var childFajr by remember { mutableStateOf(childPrefs.getString("child_fajr_$todayDateStr", "PRAYED") ?: "PRAYED") }
    var childDhuhr by remember { mutableStateOf(childPrefs.getString("child_dhuhr_$todayDateStr", "PRAYED") ?: "PRAYED") }
    var childAsr by remember { mutableStateOf(childPrefs.getString("child_asr_$todayDateStr", "NONE") ?: "NONE") }
    var childMaghrib by remember { mutableStateOf(childPrefs.getString("child_maghrib_$todayDateStr", "NONE") ?: "NONE") }
    var childIsha by remember { mutableStateOf(childPrefs.getString("child_isha_$todayDateStr", "NONE") ?: "NONE") }

    val childPrayers = listOf(childFajr, childDhuhr, childAsr, childMaghrib, childIsha)

    var childYesterdayFajr by remember { mutableStateOf(childPrefs.getString("child_fajr_$yesterdayDateStr", "PRAYED") ?: "PRAYED") }
    var childYesterdayDhuhr by remember { mutableStateOf(childPrefs.getString("child_dhuhr_$yesterdayDateStr", "PRAYED") ?: "PRAYED") }
    var childYesterdayAsr by remember { mutableStateOf(childPrefs.getString("child_asr_$yesterdayDateStr", "PRAYED") ?: "PRAYED") }
    var childYesterdayMaghrib by remember { mutableStateOf(childPrefs.getString("child_maghrib_$yesterdayDateStr", "PRAYED") ?: "PRAYED") }
    var childYesterdayIsha by remember { mutableStateOf(childPrefs.getString("child_isha_$yesterdayDateStr", "NONE") ?: "NONE") }

    val childYesterdayPrayers = listOf(childYesterdayFajr, childYesterdayDhuhr, childYesterdayAsr, childYesterdayMaghrib, childYesterdayIsha)

    var selectedFamilyTab by remember { mutableStateOf(0) } // 0: Aile Tablosu, 1: Eşim, 2: Çocuğum

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

        // -----------------------------------------------------------------
        // FAMILY CIRCLE TABS: [ 👨‍👩‍👧 Aile Tablosu ]  [ 🧕 Eşim ]  [ 🧒 Çocuğum ]
        // -----------------------------------------------------------------
        item {
            Surface(
                color = EmeraldContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SoftInfoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 0: Aile Tablosu
                    Surface(
                        onClick = { selectedFamilyTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedFamilyTab == 0) EmeraldPrimary else Color.Transparent,
                        modifier = Modifier.weight(1.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👨‍👩‍👧 Aile Tablosu",
                                fontSize = 12.sp,
                                fontWeight = if (selectedFamilyTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedFamilyTab == 0) Color.White else TextPrimary
                            )
                        }
                    }

                    // Tab 1: Eşim
                    Surface(
                        onClick = { selectedFamilyTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedFamilyTab == 1) EmeraldPrimary else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧕 ${partnerInfo.partnerDisplayName.ifBlank { "Eşim" }}",
                                fontSize = 12.sp,
                                fontWeight = if (selectedFamilyTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedFamilyTab == 1) Color.White else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    // Tab 2: Çocuğum
                    Surface(
                        onClick = { selectedFamilyTab = 2 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedFamilyTab == 2) EmeraldPrimary else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧒 $childName",
                                fontSize = 12.sp,
                                fontWeight = if (selectedFamilyTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedFamilyTab == 2) Color.White else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // =================================================================
        // VIEW 0: AİLE TABLOSU (ORTAK GÖRÜNÜM)
        // =================================================================
        if (selectedFamilyTab == 0) {
            item {
                FamilySharedMatrixCard(
                    familyName = if (settings.userName.isNotBlank()) "${settings.userName.substringBefore(" ")} Ailesi" else "Ailemiz",
                    myDisplayName = if (settings.userName.isNotBlank()) settings.userName.substringBefore(" ") else "Siz (Baba)",
                    myTodayPrayer = todayPrayer,
                    spouseName = partnerInfo.partnerDisplayName.ifBlank { "Eşim" },
                    partnerInfo = partnerInfo,
                    childName = childName,
                    childPrayers = childPrayers,
                    childYesterdayPrayers = childYesterdayPrayers,
                    onSendDua = {
                        viewModel.showStatusMessage("Tüm ailenize manevi dua gönderildi: Allah ibadetlerimizi kabul etsin! 🤲")
                    },
                    onRemind = {
                        viewModel.showStatusMessage("Aile ibadet halkasına vaktin hatırlatması iletildi! 📢")
                    }
                )
            }
        }

        // =================================================================
        // VIEW 2: ÇOCUĞUM (TEŞVİK VE ÖZEL TAKİP)
        // =================================================================
        if (selectedFamilyTab == 2) {
            item {
                ChildMotivationPineCard(
                    childName = childName,
                    childPrayers = childPrayers,
                    childYesterdayPrayers = childYesterdayPrayers,
                    onEditName = {
                        newChildNameInput = childName
                        showEditChildNameDialog = true
                    },
                    onTogglePrayer = { idx ->
                        val current = childPrayers.getOrElse(idx) { "NONE" }
                        val next = when (current) {
                            "NONE" -> "PRAYED"
                            "PRAYED" -> "MISSED"
                            "MISSED" -> "EXCUSED"
                            else -> "NONE"
                        }
                        when (idx) {
                            0 -> { childFajr = next; childPrefs.edit().putString("child_fajr_$todayDateStr", next).apply() }
                            1 -> { childDhuhr = next; childPrefs.edit().putString("child_dhuhr_$todayDateStr", next).apply() }
                            2 -> { childAsr = next; childPrefs.edit().putString("child_asr_$todayDateStr", next).apply() }
                            3 -> { childMaghrib = next; childPrefs.edit().putString("child_maghrib_$todayDateStr", next).apply() }
                            4 -> { childIsha = next; childPrefs.edit().putString("child_isha_$todayDateStr", next).apply() }
                        }
                    },
                    onCongratulate = {
                        viewModel.showStatusMessage("Tebrikler ve hayır duası $childName için iletildi! 👏🌟")
                    }
                )
            }
        }

        // =================================================================
        // VIEW 1: EŞİM (EŞLEŞME DURUMUNA GÖRE KART VEYA BAĞLANTI FORMU)
        // =================================================================
        if (selectedFamilyTab == 1 && !partnerInfo.isMatched) {

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
                            text = "💡 Bu kodu namaz arkadaşına gönder. Karşı taraf kodu girdiğinde SENİN HİÇBİR KOD GİRMENE GEREK KALMADAN uygulaman otomatik olarak eşleşecektir!",
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
                            text = "Namaz arkadaşının sana gönderdiği 4 haneli davet kodunu buraya yapıştırıp eşleş:",
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
                            label = { Text("Ona vermek istediğin isim (Örn: Arkadaşım, Kardeşim, Eşim)", fontSize = 12.sp) },
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
                            text = "Konum bilgisi veya özel veriler asla paylaşılmaz. Uygulama sadece 2 namaz arkadaşı arasındadır. 3. bir kişi dahil olamaz.",
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
        if (selectedFamilyTab == 1 && partnerInfo.isMatched) {

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
                                            text = "Bağlı Namaz Arkadaşı",
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

                        val syncDateStr = remember(partnerInfo.lastSyncTime) {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(partnerInfo.lastSyncTime))
                        }
                        val todayDateStr = remember {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        }
                        val isSyncToday = (syncDateStr == todayDateStr)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Arkadaşımın Bugünkü Namaz Durumu:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isSyncToday) "Son: ${timeFormatter.format(Date(partnerInfo.lastSyncTime))}" else "Son: Dün",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // True partner statuses (Today)
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

                        // Partner Yesterday Prayer Status
                        val yesterdayList = remember(partnerInfo) {
                            listOf(
                                partnerInfo.partnerYesterdayFajr,
                                partnerInfo.partnerYesterdayDhuhr,
                                partnerInfo.partnerYesterdayAsr,
                                partnerInfo.partnerYesterdayMaghrib,
                                partnerInfo.partnerYesterdayIsha
                            )
                        }
                        val yesterdayPrayedCount = remember(yesterdayList) {
                            yesterdayList.count { it == "PRAYED" }
                        }
                        val hasYesterdayData = remember(yesterdayList) {
                            yesterdayList.any { it != "NONE" }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 Dünkü Durumu:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                            if (hasYesterdayData) {
                                Surface(
                                    color = if (yesterdayPrayedCount == 5) Color(0xFFFEF3C7) else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (yesterdayPrayedCount == 5) "5/5 Tamamlandı 🌟" else "$yesterdayPrayedCount/5 Kılındı",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (yesterdayPrayedCount == 5) Color(0xFF78350F) else Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Kayıt bekleniyor",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PartnerMiniStatusPineItem("Sabah", partnerInfo.partnerYesterdayFajr, isCompact = true)
                            PartnerMiniStatusPineItem("Öğle", partnerInfo.partnerYesterdayDhuhr, isCompact = true)
                            PartnerMiniStatusPineItem("İkindi", partnerInfo.partnerYesterdayAsr, isCompact = true)
                            PartnerMiniStatusPineItem("Akşam", partnerInfo.partnerYesterdayMaghrib, isCompact = true)
                            PartnerMiniStatusPineItem("Yatsı", partnerInfo.partnerYesterdayIsha, isCompact = true)
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

    // Edit Child Name Dialog
    if (showEditChildNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditChildNameDialog = false },
            title = { Text("Çocuğunuzun İsmini Düzenle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Aile tablosunda görünecek isim:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newChildNameInput,
                        onValueChange = { newChildNameInput = it },
                        label = { Text("Çocuğunuzun Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChildNameInput.isNotBlank()) {
                            childName = newChildNameInput.trim()
                            childPrefs.edit().putString("child_name", childName).apply()
                        }
                        showEditChildNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditChildNameDialog = false }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            }
        )
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
            title = { Text("Arkadaşının İsmini Düzenle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Namaz arkadaşına özel bir hitap veya isim verin:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPartnerNameInput,
                        onValueChange = { newPartnerNameInput = it },
                        label = { Text("İsim / Hitap (Örn: Arkadaşım, Kardeşim, Eşim)") },
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
private fun PartnerMiniStatusPineItem(name: String, status: String, isCompact: Boolean = false) {
    val circleSize = if (isCompact) 22.dp else 28.dp
    val iconSize = if (isCompact) 13.dp else 16.dp
    val nameSize = if (isCompact) 10.sp else 11.sp
    val statusSize = if (isCompact) 9.sp else 10.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, fontSize = nameSize, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
        when (status) {
            "PRAYED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = "Kıldı", tint = EmeraldPrimary, modifier = Modifier.size(iconSize))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Kıldı", fontSize = statusSize, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
            }
            "MISSED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Kılmadı", tint = Color.White, modifier = Modifier.size(iconSize))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Kılmadı", fontSize = statusSize, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
            }
            "EXCUSED" -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Muaf", tint = Color.White, modifier = Modifier.size(iconSize))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Muaf", fontSize = statusSize, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.8f))
            }
            else -> {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.size(circleSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "—", fontSize = if (isCompact) 11.sp else 14.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Bekliyor", fontSize = statusSize, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun FamilySharedMatrixCard(
    familyName: String,
    myDisplayName: String,
    myTodayPrayer: com.example.data.local.entity.DailyPrayerEntity,
    spouseName: String,
    partnerInfo: com.example.data.local.entity.PartnerInfoEntity,
    childName: String,
    childPrayers: List<String>,
    childYesterdayPrayers: List<String>,
    onSendDua: () -> Unit,
    onRemind: () -> Unit
) {
    val myPrayedCount = listOf(
        myTodayPrayer.fajrStatus,
        myTodayPrayer.dhuhrStatus,
        myTodayPrayer.asrStatus,
        myTodayPrayer.maghribStatus,
        myTodayPrayer.ishaStatus
    ).count { it == "PRAYED" }

    val spousePrayedCount = listOf(
        partnerInfo.partnerFajr,
        partnerInfo.partnerDhuhr,
        partnerInfo.partnerAsr,
        partnerInfo.partnerMaghrib,
        partnerInfo.partnerIsha
    ).count { it == "PRAYED" }

    val childPrayedCount = childPrayers.count { it == "PRAYED" }

    val totalPrayed = myPrayedCount + spousePrayedCount + childPrayedCount
    val familyPercent = ((totalPrayed / 15f) * 100).toInt()

    val spouseYesterdayCount = listOf(
        partnerInfo.partnerYesterdayFajr,
        partnerInfo.partnerYesterdayDhuhr,
        partnerInfo.partnerYesterdayAsr,
        partnerInfo.partnerYesterdayMaghrib,
        partnerInfo.partnerYesterdayIsha
    ).count { it == "PRAYED" }

    val childYesterdayCount = childYesterdayPrayers.count { it == "PRAYED" }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
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
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "$familyName İbadet Halkası",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "\"Ailene namazı emret ve onda sabırlı ol.\" (Tâhâ, 132)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Family Score Badge
            Surface(
                color = if (totalPrayed >= 12) Color(0xFFFEF3C7) else Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (totalPrayed >= 12) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌟", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bugün Aile Başarısı:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPrayed >= 12) Color(0xFF78350F) else Color.White
                        )
                    }
                    Text(
                        text = "$totalPrayed / 15 Vakit (%$familyPercent)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (totalPrayed >= 12) Color(0xFF78350F) else Color(0xFF86EFAC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Shared Prayers Table
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "VAKİT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.weight(1.1f))
                Text(text = myDisplayName.ifBlank { "Siz" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(text = spouseName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(text = childName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }

            val prayerRows = listOf(
                Pair("Sabah", Triple(myTodayPrayer.fajrStatus, partnerInfo.partnerFajr, childPrayers.getOrElse(0) { "NONE" })),
                Pair("Öğle", Triple(myTodayPrayer.dhuhrStatus, partnerInfo.partnerDhuhr, childPrayers.getOrElse(1) { "NONE" })),
                Pair("İkindi", Triple(myTodayPrayer.asrStatus, partnerInfo.partnerAsr, childPrayers.getOrElse(2) { "NONE" })),
                Pair("Akşam", Triple(myTodayPrayer.maghribStatus, partnerInfo.partnerMaghrib, childPrayers.getOrElse(3) { "NONE" })),
                Pair("Yatsı", Triple(myTodayPrayer.ishaStatus, partnerInfo.partnerIsha, childPrayers.getOrElse(4) { "NONE" }))
            )

            prayerRows.forEach { (name, tuple) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1.1f)
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        FamilyMatrixStatusCell(tuple.first)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        FamilyMatrixStatusCell(tuple.second)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        FamilyMatrixStatusCell(tuple.third)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Dünkü Aile Tablosu (Özet)
            Text(
                text = "📅 Dünkü Aile Tablosu (Özet):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = myDisplayName.ifBlank { "Siz" }, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(text = "5/5 🌟", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = spouseName, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(text = "$spouseYesterdayCount/5 🌟", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = childName, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(text = "$childYesterdayCount/5 👏", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSendDua,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aileye Dua Et", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRemind,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Vakti Hatırlat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FamilyMatrixStatusCell(status: String) {
    when (status) {
        "PRAYED" -> {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Kıldı", tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Kıldı", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }
            }
        }
        "MISSED" -> {
            Surface(
                color = Color(0xFFDC2626),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Kılmadı", tint = Color.White, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Kaza", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        "EXCUSED" -> {
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Muaf", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        else -> {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Bekliyor", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun ChildMotivationPineCard(
    childName: String,
    childPrayers: List<String>,
    childYesterdayPrayers: List<String>,
    onEditName: () -> Unit,
    onTogglePrayer: (Int) -> Unit,
    onCongratulate: () -> Unit
) {
    val childPrayedCount = childPrayers.count { it == "PRAYED" }
    val isPerfect = (childPrayedCount == 5)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🧒", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = childName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onEditName,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Adı Düzenle", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(15.dp))
                            }
                        }
                        Text(
                            text = "Namaz Alışkanlığı ve Teşvik",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Surface(
                    color = if (isPerfect) Color(0xFFFEF3C7) else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPerfect) "Günün Yıldızı 🌟" else "$childPrayedCount/5 Vakit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPerfect) Color(0xFF78350F) else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Motivation Banner
            Surface(
                color = if (isPerfect) Color(0xFFFEF3C7).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isPerfect) "🏆" else "🌟", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPerfect) {
                            "Maşallah $childName! Bugün 5 vaktin tamamını kılarak Günün Yıldızı rozetini kazandı!"
                        } else {
                            "Her secde kalbe nur, ömre bereket katar. Kılınan vakitlere dokunarak işaretleyebilirsiniz."
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isPerfect) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPerfect) Color(0xFF78350F) else Color.White,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Child Today Prayers
            Text(
                text = "$childName - Bugünkü Namazları (Dokunarak Değiştir):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val names = listOf("Sabah", "Öğle", "İkindi", "Akşam", "Yatsı")
                names.forEachIndexed { idx, name ->
                    val status = childPrayers.getOrElse(idx) { "NONE" }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onTogglePrayer(idx) }
                    ) {
                        PartnerMiniStatusPineItem(name, status)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Child Yesterday Prayers
            Text(
                text = "📅 Dünkü Durumu:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PartnerMiniStatusPineItem("Sabah", childYesterdayPrayers.getOrElse(0) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("Öğle", childYesterdayPrayers.getOrElse(1) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("İkindi", childYesterdayPrayers.getOrElse(2) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("Akşam", childYesterdayPrayers.getOrElse(3) { "NONE" }, isCompact = true)
                PartnerMiniStatusPineItem("Yatsı", childYesterdayPrayers.getOrElse(4) { "NONE" }, isCompact = true)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Congratulate button
            Button(
                onClick = onCongratulate,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "👏", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Çocuğumu Tebrik Et & Dua Gönder", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
