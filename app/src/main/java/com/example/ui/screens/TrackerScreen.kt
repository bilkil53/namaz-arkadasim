package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyPrayerTimes
import com.example.ui.components.DriveShareDialog
import com.example.util.ApkSharingHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TrackerScreen(
    viewModel: MainViewModel,
    onNavigateToPartner: () -> Unit,
    onRequestPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayPrayer by viewModel.todayPrayer.collectAsState()
    val partnerInfo by viewModel.partnerInfo.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isUpdatingLocation by viewModel.isUpdatingLocation.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val hasLocationPermission = remember(context, isUpdatingLocation) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Completed prayers count (0..5)
    val prayerStatuses = remember(todayPrayer) {
        listOf(
            todayPrayer.fajrStatus,
            todayPrayer.dhuhrStatus,
            todayPrayer.asrStatus,
            todayPrayer.maghribStatus,
            todayPrayer.ishaStatus
        )
    }
    val prayedCount = remember(prayerStatuses) {
        prayerStatuses.count { it == "PRAYED" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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

        // Permission notice if GPS is not yet granted
        if (!hasLocationPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftInfoCardBg),
                    border = BorderStroke(1.dp, SoftInfoCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Konum ve Bildirim İzni",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Diyanet vakitlerini ve ezan bildirimlerini tam zamanında alabilmek için izinleri onaylayın.",
                                fontSize = 11.sp,
                                color = Color(0xFF2C4A42),
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestPermissions,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("İzin Ver", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Top Greeting Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (settings.isLoggedIn && settings.userEmail.isNotBlank()) "HESAP: ${settings.userEmail.uppercase()}" else "NAMAZ ARKADAŞIM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        letterSpacing = 0.5.sp
                    )
                    val greetingTitle = if (settings.isLoggedIn && settings.userName.isNotBlank()) {
                        "Selamün aleyküm,\n${settings.userName}"
                    } else {
                        "Selamün aleyküm"
                    }
                    Text(
                        text = greetingTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 27.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${viewModel.getDisplayDateTitle()} · ${if (isUpdatingLocation) "GPS aranıyor..." else "${settings.cityName} (Diyanet)"}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Direct GPS Update Button
                Surface(
                    onClick = { viewModel.updateLocationGps() },
                    shape = CircleShape,
                    color = EmeraldContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.MyLocation,
                            contentDescription = "GPS ile Güncelle",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Hero Card: Deep Pine Green ("SIRADAKİ VAKİT")
        item {
            HeroPineCard(
                prayerTimes = prayerTimes,
                cityName = settings.cityName,
                onRefresh = { viewModel.updateLocationGps() }
            )
        }

        // Section: "Bugünün takibi" with 0/5 badge and 5 segments
        item {
            DailyTrackingHeader(
                prayedCount = prayedCount,
                prayerStatuses = prayerStatuses
            )
        }

        // 5 Daily Prayers Cards (Styled in the exact beloved Pine Green with White text)
        item {
            PrayerListItemPineCard(
                name = "Sabah",
                time = prayerTimes.imsak,
                status = todayPrayer.fajrStatus,
                icon = Icons.Outlined.WbTwilight,
                onToggle = {
                    val next = getNextStatus(todayPrayer.fajrStatus)
                    viewModel.setPrayerStatus("FAJR", next)
                }
            )
        }

        item {
            PrayerListItemPineCard(
                name = "Öğle",
                time = prayerTimes.ogle,
                status = todayPrayer.dhuhrStatus,
                icon = Icons.Outlined.WbSunny,
                onToggle = {
                    val next = getNextStatus(todayPrayer.dhuhrStatus)
                    viewModel.setPrayerStatus("DHUHR", next)
                }
            )
        }

        item {
            PrayerListItemPineCard(
                name = "İkindi",
                time = prayerTimes.ikindi,
                status = todayPrayer.asrStatus,
                icon = Icons.Outlined.Brightness5,
                onToggle = {
                    val next = getNextStatus(todayPrayer.asrStatus)
                    viewModel.setPrayerStatus("ASR", next)
                }
            )
        }

        item {
            PrayerListItemPineCard(
                name = "Akşam",
                time = prayerTimes.aksam,
                status = todayPrayer.maghribStatus,
                icon = Icons.Outlined.NightsStay,
                onToggle = {
                    val next = getNextStatus(todayPrayer.maghribStatus)
                    viewModel.setPrayerStatus("MAGHRIB", next)
                }
            )
        }

        item {
            PrayerListItemPineCard(
                name = "Yatsı",
                time = prayerTimes.yatsi,
                status = todayPrayer.ishaStatus,
                icon = Icons.Outlined.Bedtime,
                onToggle = {
                    val next = getNextStatus(todayPrayer.ishaStatus)
                    viewModel.setPrayerStatus("ISHA", next)
                }
            )
        }
    }
}

private fun getNextStatus(current: String): String {
    return when (current) {
        "NONE" -> "PRAYED"
        "PRAYED" -> "MISSED"
        "MISSED" -> "EXCUSED"
        else -> "NONE"
    }
}

@Composable
private fun HeroPineCard(
    prayerTimes: DailyPrayerTimes,
    cityName: String,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_countdown_card")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            // Header: Bell + "SIRADAKİ VAKİT" + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "SIRADAKİ VAKİT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Yenile",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Prayer Name + Countdown & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = prayerTimes.nextPrayerName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val remainingMinutes = (prayerTimes.nextPrayerRemainingMillis / (60 * 1000)).coerceAtLeast(0)
                    val hours = remainingMinutes / 60
                    val mins = remainingMinutes % 60
                    Text(
                        text = if (hours > 0) "$hours sa $mins dk sonra" else "$mins dk sonra",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                val nextTime = when (prayerTimes.nextPrayerName) {
                    "İmsak / Sabah" -> prayerTimes.imsak
                    "Öğle" -> prayerTimes.ogle
                    "İkindi" -> prayerTimes.ikindi
                    "Akşam" -> prayerTimes.aksam
                    "Yatsı" -> prayerTimes.yatsi
                    else -> prayerTimes.imsak
                }

                Text(
                    text = nextTime,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Text(
                text = "Diyanet İşleri Başkanlığı resmi vakitleri GPS konumunuza göre gösterilmektedir.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun DailyTrackingHeader(
    prayedCount: Int,
    prayerStatuses: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bugünün takibi",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Surface(
                color = EmeraldContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$prayedCount/5",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Niyetinle başla; küçük bir işaret sürdürülebilirliği güçlendirir.",
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5-Segment Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            prayerStatuses.forEach { status ->
                val isDone = status == "PRAYED"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDone) EmeraldPrimary else SegmentInactive)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Vakite dokundukça Kıldı → Kılmadı → Muaf → Bekliyor sırasıyla ilerler.",
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

/**
 * Beautiful Prayer Card styled in the signature Pine Green with White text
 * as requested ("SIRADAKİ vakit yazan arka plan yeşil rengi çok güzel hepsi bu şekilde olsun").
 */
@Composable
private fun PrayerListItemPineCard(
    name: String,
    time: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Translucent white box with white icon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val statusText = when (status) {
                        "PRAYED" -> "✓ Kılındı"
                        "MISSED" -> "✕ Kılınmadı"
                        "EXCUSED" -> "— Muaf"
                        else -> "Dokunarak işaretle"
                    }
                    val statusColor = when (status) {
                        "PRAYED" -> Color(0xFF86EFAC) // Mint green
                        "MISSED" -> Color(0xFFFCA5A5) // Soft coral red
                        "EXCUSED" -> Color(0xFFE5E7EB)
                        else -> Color.White.copy(alpha = 0.75f)
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = if (status != "NONE") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = time,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Circular Indicator
                when (status) {
                    "PRAYED" -> {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Kılındı",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    "MISSED" -> {
                        Surface(
                            shape = CircleShape,
                            color = MissedRed,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Kılınmadı",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    "EXCUSED" -> {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Remove,
                                    contentDescription = "Muaf",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        // Empty white ring
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    radius = (size.minDimension / 2f) - 1.5.dp.toPx(),
                                    style = Stroke(width = 1.8.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
