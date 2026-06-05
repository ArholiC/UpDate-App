package com.update.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.update.app.data.model.CompatibilityResponse
import com.update.app.data.model.FilterState
import com.update.app.data.model.User
import com.update.app.data.network.BASE_URL
import com.update.app.data.network.RetrofitClient
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.DiscoverViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ZODIACS = listOf(
    "Koç", "Boğa", "İkizler", "Yengeç", "Aslan", "Başak",
    "Terazi", "Akrep", "Yay", "Oğlak", "Kova", "Balık"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(userId: Int, vm: DiscoverViewModel = viewModel()) {
    val users by vm.users.collectAsState()
    val currentIndex by vm.currentIndex.collectAsState()
    val loading by vm.loading.collectAsState()
    val matchedName by vm.matchedName.collectAsState()
    val superLikeMessage by vm.superLikeMessage.collectAsState()
    val filter by vm.filter.collectAsState()

    var genderFilter by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showCompatModal by remember { mutableStateOf(false) }
    var compatData by remember { mutableStateOf<CompatibilityResponse?>(null) }
    var compatUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()

    // Filtre değişince yeniden yükle
    LaunchedEffect(genderFilter, filter) { vm.loadUsers(userId, genderFilter) }

    // Eşleşme dialog
    if (!matchedName.isNullOrEmpty()) {
        AlertDialog(
            onDismissRequest = { vm.clearMatch() },
            containerColor = BgCard,
            title = { Text("🎉 Eşleşme!", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("${matchedName} ile eşleştin!", color = TextSecondary) },
            confirmButton = { TextButton(onClick = { vm.clearMatch() }) { Text("Harika!", color = PrimaryGlow) } }
        )
    }

    // Süper beğeni mesajı
    if (!superLikeMessage.isNullOrEmpty()) {
        AlertDialog(
            onDismissRequest = { vm.clearSuperLike() },
            containerColor = BgCard,
            title = { Text("⭐ Süper Beğeni!", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold) },
            text = { Text(superLikeMessage ?: "", color = TextSecondary) },
            confirmButton = { TextButton(onClick = { vm.clearSuperLike() }) { Text("Tamam", color = PrimaryGlow) } }
        )
    }

    // Uyum Detay Modal
    if (showCompatModal && compatData != null && compatUser != null) {
        AlertDialog(
            onDismissRequest = { showCompatModal = false },
            containerColor = BgCard,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("💘 Uyum Detayı", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    // Büyük uyum yüzdesi
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Primary.copy(0.3f), Color.Transparent)))
                        .border(2.dp, Primary, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("%${compatData!!.matchRate}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = PrimaryGlow)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${compatUser!!.full_name} ile uyumun", color = TextMuted, fontSize = 12.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ortak İlgi Alanları
                    Text("✨ Ortak İlgi Alanları (${compatData!!.commonInterests.size})",
                        color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (compatData!!.commonInterests.isEmpty()) {
                        Text("Henüz ortak ilgi alanı yok", color = TextMuted, fontSize = 12.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.height(34.dp)) {
                            items(compatData!!.commonInterests) { interest ->
                                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .border(1.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)) {
                                    Text(interest, color = PrimaryGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    // Ortak Cevaplar
                    Text("🎯 Ortak Test Cevapları (${compatData!!.commonAnswers.size}/${compatData!!.totalAnswers})",
                        color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (compatData!!.commonAnswers.isEmpty()) {
                        Text("Ortak cevap bulunamadı", color = TextMuted, fontSize = 12.sp)
                    } else {
                        compatData!!.commonAnswers.forEach { ca ->
                            Row(modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)).background(BgInput)
                                .padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ca.question, color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("✓ ${ca.answer}", color = PrimaryGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompatModal = false }) { Text("Kapat", color = PrimaryGlow) }
            }
        )
    }

    // Filtreler BottomSheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = BgCard,
            dragHandle = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(999.dp)).background(Border))
                }
            }
        ) {
            var tempGender by remember { mutableStateOf(filter.gender ?: "all") }
            var tempZodiac by remember { mutableStateOf(filter.zodiac ?: "all") }
            var tempMinAge by remember { mutableFloatStateOf((filter.minAge ?: 18).toFloat()) }
            var tempMaxAge by remember { mutableFloatStateOf((filter.maxAge ?: 60).toFloat()) }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("🎛️ Gelişmiş Filtreler", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)

                // Cinsiyet
                Text("Cinsiyet", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "Hepsi", "kadin" to "👩 Kadın", "erkek" to "👨 Erkek").forEach { (v, label) ->
                        val sel = tempGender == v
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (sel) Primary.copy(0.25f) else BgInput)
                            .border(1.dp, if (sel) Primary else Border, RoundedCornerShape(999.dp))
                            .clickable { tempGender = v }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                            Text(label, color = if (sel) PrimaryGlow else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Yaş Aralığı
                Text("Yaş Aralığı: ${tempMinAge.roundToInt()} – ${tempMaxAge.roundToInt()}",
                    fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                RangeSlider(
                    value = tempMinAge..tempMaxAge,
                    onValueChange = { r -> tempMinAge = r.start; tempMaxAge = r.endInclusive },
                    valueRange = 18f..60f,
                    steps = 40,
                    colors = SliderDefaults.colors(activeTrackColor = Primary, thumbColor = Primary)
                )

                // Burç
                Text("Burç", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                // "Hepsi" chip
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (tempZodiac == "all") Primary.copy(0.25f) else BgInput)
                    .border(1.dp, if (tempZodiac == "all") Primary else Border, RoundedCornerShape(999.dp))
                    .clickable { tempZodiac = "all" }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text("Hepsi", color = if (tempZodiac == "all") PrimaryGlow else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(38.dp)) {
                    items(ZODIACS) { zodiac ->
                        val sel = tempZodiac == zodiac
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (sel) Primary.copy(0.25f) else BgInput)
                            .border(1.dp, if (sel) Primary else Border, RoundedCornerShape(999.dp))
                            .clickable { tempZodiac = zodiac }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                            Text(zodiac, color = if (sel) PrimaryGlow else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Uygula Butonu
                Button(
                    onClick = {
                        genderFilter = if (tempGender == "all") null else tempGender
                        vm.setFilter(FilterState(
                            gender = if (tempGender == "all") null else tempGender,
                            minAge = if (tempMinAge > 18f) tempMinAge.roundToInt() else null,
                            maxAge = if (tempMaxAge < 60f) tempMaxAge.roundToInt() else null,
                            zodiac = if (tempZodiac == "all") null else tempZodiac
                        ))
                        showFilterSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("✅  Filtreleri Uygula", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        // Top bar
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("UpDate 🔥", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            IconButton(onClick = { showFilterSheet = true },
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(BgInput)) {
                Icon(Icons.Default.Tune, contentDescription = "Filtreler", tint = TextMuted)
            }
        }

        // Cinsiyet chip'leri
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Hepsi", "kadin" to "Kadınlar", "erkek" to "Erkekler").forEach { (f, label) ->
                val active = genderFilter == f
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (active) Primary.copy(alpha = 0.25f) else BgInput)
                    .border(1.dp, if (active) Primary else Border, RoundedCornerShape(999.dp))
                    .clickable { genderFilter = f }.padding(horizontal = 16.dp, vertical = 7.dp)) {
                    Text(label, color = if (active) PrimaryGlow else TextMuted,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                currentIndex >= users.size -> EmptyState { vm.loadUsers(userId) }
                else -> {
                    if (currentIndex + 1 < users.size) {
                        SwipeCard(user = users[currentIndex + 1], isBackground = true, userId = userId,
                            onSwipe = {}, onCompatClick = {})
                    }
                    SwipeCard(
                        user = users[currentIndex],
                        isBackground = false,
                        userId = userId,
                        onSwipe = { isLike -> vm.swipe(userId, users[currentIndex].id, isLike) },
                        onCompatClick = { user ->
                            scope.launch {
                                try {
                                    val res = RetrofitClient.api.getCompatibility(userId, user.id)
                                    if (res.isSuccessful) {
                                        compatData = res.body()
                                        compatUser = user
                                        showCompatModal = true
                                    }
                                } catch (e: Exception) { }
                            }
                        }
                    )
                }
            }
        }

        // Aksiyon butonları
        if (!loading && currentIndex < users.size) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                // Dislike
                Box(modifier = Modifier.size(60.dp).clip(CircleShape)
                    .background(Dislike.copy(alpha = 0.1f)).border(2.dp, Dislike, CircleShape)
                    .clickable { vm.swipe(userId, users[currentIndex].id, 0) },
                    contentAlignment = Alignment.Center) {
                    Text("✕", fontSize = 24.sp, color = Dislike)
                }
                Spacer(Modifier.width(16.dp))
                // Süper Beğeni
                Box(modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(Color(0xFFFFD700).copy(alpha = 0.1f))
                    .border(2.dp, Color(0xFFFFD700), CircleShape)
                    .clickable { vm.swipe(userId, users[currentIndex].id, 2) },
                    contentAlignment = Alignment.Center) {
                    Text("⭐", fontSize = 22.sp)
                }
                Spacer(Modifier.width(16.dp))
                // Like
                Box(modifier = Modifier.size(68.dp).clip(CircleShape)
                    .background(Primary.copy(alpha = 0.2f)).border(2.dp, Primary, CircleShape)
                    .clickable { vm.swipe(userId, users[currentIndex].id, 1) },
                    contentAlignment = Alignment.Center) {
                    Text("♥", fontSize = 28.sp, color = Primary)
                }
            }
        }
    }
}

@Composable
fun SwipeCard(user: User, isBackground: Boolean, userId: Int, onSwipe: (Int) -> Unit, onCompatClick: (User) -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val rotation = (offsetX / 30f).coerceIn(-15f, 15f)
    val likeAlpha = (offsetX / 200f).coerceIn(0f, 1f)
    val nopeAlpha = (-offsetX / 200f).coerceIn(0f, 1f)
    val superAlpha = (-offsetY / 150f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .width(340.dp).height(500.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .rotate(if (isBackground) 0f else rotation)
            .then(if (isBackground) Modifier else Modifier.pointerInput(user.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > 200f -> onSwipe(1)     // Beğen
                            offsetX < -200f -> onSwipe(0)    // Geç
                            offsetY < -200f -> onSwipe(2)    // Süper beğeni (yukarı kaydır)
                            else -> { offsetX = 0f; offsetY = 0f }
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            })
            .clip(RoundedCornerShape(24.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(24.dp))
    ) {
        if (!user.profile_pic.isNullOrEmpty()) {
            val picUrl = if (user.profile_pic.startsWith("/")) "${BASE_URL.trimEnd('/')}${user.profile_pic}"
                         else "${BASE_URL}public/uploads/${user.profile_pic}"
            AsyncImage(model = picUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.65f))
        } else {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.65f).background(BgInput),
                contentAlignment = Alignment.Center) {
                Text(user.full_name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 64.sp, fontWeight = FontWeight.Black, color = Primary)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Bg.copy(alpha = 0.8f)))))

        // Uyum badge
        val matchRate = user.match_rate
        if (matchRate != null) {
            Box(modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(999.dp))
                .background(Bg.copy(alpha = 0.9f))
                .border(1.dp, PrimaryGlow, RoundedCornerShape(999.dp))
                .clickable(onClick = { onCompatClick(user) })
                .padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text("%$matchRate uyum 🔍", color = PrimaryGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (!isBackground) {
            // LIKE etiketi
            Box(modifier = Modifier.padding(16.dp).align(Alignment.TopEnd)
                .clip(RoundedCornerShape(8.dp)).background(Like.copy(alpha = 0.15f * likeAlpha))
                .border(2.dp, Like.copy(alpha = likeAlpha), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("LIKE 💚", color = Like.copy(alpha = likeAlpha), fontWeight = FontWeight.Black)
            }
            // NOPE etiketi
            Box(modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                .clip(RoundedCornerShape(8.dp)).background(Dislike.copy(alpha = 0.15f * nopeAlpha))
                .border(2.dp, Dislike.copy(alpha = nopeAlpha), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("NOPE ❌", color = Dislike.copy(alpha = nopeAlpha), fontWeight = FontWeight.Black)
            }
            // SUPER etiketi (yukarı kaydırma)
            Box(modifier = Modifier.align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFD700).copy(alpha = 0.2f * superAlpha))
                .border(2.dp, Color(0xFFFFD700).copy(alpha = superAlpha), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("⭐ SÜPER BEĞENİ", color = Color(0xFFFFD700).copy(alpha = superAlpha), fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(user.full_name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Text(user.zodiac ?: "", color = TextMuted, fontSize = 13.sp)
            }
            if (!user.bio.isNullOrEmpty()) {
                Text(user.bio, color = TextSecondary, fontSize = 13.sp, maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp))
            }
            val interests = user.interests?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            if (interests.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    interests.take(3).forEach { tag ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(Primary.copy(alpha = 0.15f))
                            .border(1.dp, Border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(tag, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(onRefresh: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 40.dp)) {
            Box(modifier = Modifier.size(120.dp).clip(CircleShape)
                .background(Primary.copy(alpha = 0.12f)).border(2.dp, Primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center) { Text("🔥", fontSize = 56.sp) }
            Spacer(Modifier.height(8.dp))
            Text("Herkesi gördün!", fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text("Şu an gösterilecek yeni profil yok.\nYarın tekrar kontrol et!",
                color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(999.dp), modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)) {
                Text("🔄  Yenile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}