package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.KazaPrayerEntity
import com.example.data.model.PrayerType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun KazaScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val kazaPrayers by viewModel.kazaPrayers.collectAsState()
    val partnerInfo by viewModel.partnerInfo.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val kazaUndoMessage by viewModel.kazaUndoMessage.collectAsState()

    val childPrefs = remember { context.getSharedPreferences("family_child_prefs", Context.MODE_PRIVATE) }
    val p1Icon = remember { childPrefs.getString("p1_icon", "🧔") ?: "🧔" }
    val hasP2 = remember { childPrefs.getBoolean("has_p2", true) }
    val p2Name = remember(partnerInfo.partnerDisplayName) {
        val def = partnerInfo.partnerDisplayName.ifBlank { "Kişi 2" }
        childPrefs.getString("p2_name", def) ?: def
    }
    val p2Icon = remember { childPrefs.getString("p2_icon", "🧕") ?: "🧕" }
    val hasP3 = remember { childPrefs.getBoolean("has_p3", true) }
    val p3Name = remember { childPrefs.getString("p3_name", "Kişi 3") ?: "Kişi 3" }
    val p3Icon = remember { childPrefs.getString("p3_icon", "👦") ?: "👦" }
    val hasP4 = remember { childPrefs.getBoolean("has_p4", false) }
    val p4Name = remember { childPrefs.getString("p4_name", "Kişi 4") ?: "Kişi 4" }
    val p4Icon = remember { childPrefs.getString("p4_icon", "👧") ?: "👧" }

    var p3KazaOwed by remember { mutableStateOf(childPrefs.getInt("p3_kaza_owed", 0)) }
    var p3KazaCompleted by remember { mutableStateOf(childPrefs.getInt("p3_kaza_completed", 12)) }
    var p4KazaOwed by remember { mutableStateOf(childPrefs.getInt("p4_kaza_owed", 0)) }
    var p4KazaCompleted by remember { mutableStateOf(childPrefs.getInt("p4_kaza_completed", 5)) }

    var showBulkWizard by remember { mutableStateOf(false) }

    val totalOwed = remember(kazaPrayers) { kazaPrayers.sumOf { it.owedCount } }
    val totalCompleted = remember(kazaPrayers) { kazaPrayers.sumOf { it.completedCount } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

        // Summary Card: Pine Green with White text
        item {
            KazaSummaryPineCard(
                totalOwed = totalOwed,
                totalCompleted = totalCompleted,
                onOpenWizard = { showBulkWizard = true }
            )
        }

        // Aile Kaza Durumu (Detaysız Özet - User request)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftInfoCardBg),
                border = BorderStroke(1.dp, SoftInfoCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Diversity3,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Aile Kaza Durumu (Detaysız)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Mahremiyet Korumalı Özet Takip",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = "Özet 👁️",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Aile bireylerinin ibadet mahremiyeti için vakit dökümü yapılmaz; yalnızca toplam kaza borcu ve eda edilen vakitler genel özet olarak listelenir.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bireylerin Kaza Özet Kartları
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. Kişi (Siz)
                        FamilyMemberKazaRow(
                            icon = p1Icon,
                            name = settings.userName.ifBlank { "Kişi 1 (Siz)" },
                            owed = totalOwed,
                            completed = totalCompleted,
                            isMe = true
                        )

                        // 2. Kişi (Eş / Partner)
                        if (hasP2) {
                            FamilyMemberKazaRow(
                                icon = p2Icon,
                                name = p2Name,
                                owed = partnerInfo.partnerKazaOwed,
                                completed = partnerInfo.partnerKazaCompleted,
                                isMe = false,
                                isMatched = partnerInfo.isMatched
                            )
                        }

                        // 3. Kişi
                        if (hasP3) {
                            FamilyMemberKazaRow(
                                icon = p3Icon,
                                name = p3Name,
                                owed = p3KazaOwed,
                                completed = p3KazaCompleted,
                                isMe = false,
                                onQuickAdd = {
                                    p3KazaCompleted += 1
                                    childPrefs.edit().putInt("p3_kaza_completed", p3KazaCompleted).apply()
                                    viewModel.showStatusMessage("$p3Name için +1 kaza eda edildi! 🤲")
                                }
                            )
                        }

                        // 4. Kişi
                        if (hasP4) {
                            FamilyMemberKazaRow(
                                icon = p4Icon,
                                name = p4Name,
                                owed = p4KazaOwed,
                                completed = p4KazaCompleted,
                                isMe = false,
                                onQuickAdd = {
                                    p4KazaCompleted += 1
                                    childPrefs.edit().putInt("p4_kaza_completed", p4KazaCompleted).apply()
                                    viewModel.showStatusMessage("$p4Name için +1 kaza eda edildi! 🤲")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vakitlere Göre Kaza Borçları",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                TextButton(onClick = { showBulkWizard = true }) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Toplu Ekle", fontSize = 13.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 6 Kaza items: FAJR, DHUHR, ASR, MAGHRIB, ISHA, WITR
        val kazaMap = kazaPrayers.associateBy { it.prayerType.uppercase() }

        val prayerTypesList = listOf(
            PrayerType.FAJR,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA,
            PrayerType.WITR
        )

        for (pt in prayerTypesList) {
            val entity = kazaMap[pt.name] ?: KazaPrayerEntity(prayerType = pt.name)
            item(key = pt.name) {
                KazaPrayerPineCard(
                    prayerType = pt,
                    kazaEntity = entity,
                    onAdjustOwed = { delta -> viewModel.adjustKaza(pt.name, delta) },
                    onCompleteOne = { viewModel.completeOneKaza(pt.name) }
                )
            }
        }
    }

    // Floating Undo Bar for Kaza
    AnimatedVisibility(
        visible = kazaUndoMessage != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF86EFAC),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = kazaUndoMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = { viewModel.undoLastKaza() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFDE047))
                ) {
                    Text(
                        text = "GERİ AL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.dismissKazaUndo() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Kapat",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

    if (showBulkWizard) {
        BulkAddKazaDialog(
            onDismiss = { showBulkWizard = false },
            onConfirm = { days ->
                viewModel.addBulkKaza(days)
                showBulkWizard = false
            }
        )
    }
}

@Composable
fun KazaSummaryPineCard(
    totalOwed: Int,
    totalCompleted: Int,
    onOpenWizard: () -> Unit
) {
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
                Text(
                    text = "Kaza Namazı Özeti",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "6 Vakit Takibi",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kalan Borç Box
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "$totalOwed",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5) // Soft red accent on pine
                        )
                        Text(
                            text = "Kalan Kaza Borcu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Tamamlanan Box
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "$totalCompleted",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF86EFAC) // Mint green accent on pine
                        )
                        Text(
                            text = "Kılınan Kaza",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KazaPrayerPineCard(
    prayerType: PrayerType,
    kazaEntity: KazaPrayerEntity,
    onAdjustOwed: (Int) -> Unit,
    onCompleteOne: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${prayerType.displayNameTr} Namazı",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Farz: ${prayerType.rakats} Rekat | Kılınan: ${kazaEntity.completedCount}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Owed Counter with - and +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onAdjustOwed(-1) },
                        enabled = kazaEntity.owedCount > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (kazaEntity.owedCount > 0) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                            .testTag("kaza_minus_${prayerType.name}")
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = "Borç Azalt",
                            tint = if (kazaEntity.owedCount > 0) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${kazaEntity.owedCount}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (kazaEntity.owedCount > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                        )
                        Text(
                            text = "Borç",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = { onAdjustOwed(1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .testTag("kaza_plus_${prayerType.name}")
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Borç Arttır",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Complete 1 Kaza Button
            Button(
                onClick = onCompleteOne,
                enabled = kazaEntity.owedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = EmeraldPrimary,
                    disabledContainerColor = Color.White.copy(alpha = 0.15f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("kaza_complete_${prayerType.name}")
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (kazaEntity.owedCount > 0) EmeraldPrimary else Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (kazaEntity.owedCount > 0) "1 Vakit Kaza Kıldım (-1)" else "Kaza Borcu Yok (Elhamdülillah)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BulkAddKazaDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedDays by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Toplu Kaza Borcu Ekle", fontWeight = FontWeight.Bold, color = EmeraldPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Kaç günlük namaz kaza borcunuzu her vakte topluca eklemek istersiniz?",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(7, 30, 90, 365).forEach { days ->
                        val isSelected = selectedDays == days
                        OutlinedButton(
                            onClick = { selectedDays = days },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) EmeraldPrimary else Color.Transparent,
                                contentColor = if (isSelected) Color.White else EmeraldPrimary
                            ),
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when (days) {
                                    7 -> "1 Hafta"
                                    30 -> "1 Ay"
                                    90 -> "3 Ay"
                                    else -> "1 Yıl"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDays) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Her Vakte +$selectedDays Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = TextSecondary)
            }
        }
    )
}

@Composable
fun FamilyMemberKazaRow(
    icon: String,
    name: String,
    owed: Int,
    completed: Int,
    isMe: Boolean,
    isMatched: Boolean = true,
    onQuickAdd: (() -> Unit)? = null
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isMe) EmeraldPrimary.copy(alpha = 0.5f) else CardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = EmeraldContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Siz)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    } else if (!isMatched) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• Beklemede",
                            fontSize = 10.sp,
                            color = Color(0xFFD97706)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Borç: $owed vakit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (owed > 0) MissedRed else EmeraldPrimary
                    )
                    Text(
                        text = " • ",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Kılınan: $completed vakit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            if (onQuickAdd != null) {
                Surface(
                    onClick = onQuickAdd,
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "+1 Kıldı",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

