package com.update.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.update.app.data.network.BASE_URL
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.ProfileViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

private val INTERESTS = listOf(
    "🎮 Oyun", "🎵 Müzik", "📚 Kitap", "🎬 Film", "🏃 Spor",
    "🍳 Yemek", "✈️ Seyahat", "🎨 Sanat", "💻 Teknoloji", "🌿 Doğa",
    "📸 Fotoğraf", "🎭 Tiyatro", "🧘 Yoga", "🎲 Masa Oyunları", "🐾 Hayvanlar"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(userId: Int, onLogout: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsState()
    val loading by vm.loading.collectAsState()
    val saveSuccess by vm.saveSuccess.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) { vm.loadProfile() }
    LaunchedEffect(profile) {
        profile?.let {
            bio = it.bio ?: ""
            selectedInterests = it.interests?.split(",")?.map { s -> s.trim() }?.filter { s -> s.isNotEmpty() }?.toSet() ?: emptySet()
        }
    }
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) { editing = false; vm.clearSaveSuccess() }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadPhoto(context, it) }
    }

    if (loading && profile == null) {
        Box(Modifier.fillMaxSize().background(Bg), Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()
        .verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Profilim 👤", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            TextButton(onClick = onLogout) { Text("Çıkış", color = Dislike, fontWeight = FontWeight.SemiBold) }
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.clickable { photoPicker.launch("image/*") }) {
                if (!profile?.profile_pic.isNullOrEmpty()) {
                    val picUrl = profile?.profile_pic?.let {
                        if (it.startsWith("/")) "${BASE_URL.trimEnd('/')}$it"
                        else "${BASE_URL}public/uploads/$it"
                    } ?: ""
                    AsyncImage(model = picUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp).clip(CircleShape))
                } else {
                    Box(Modifier.size(100.dp).clip(CircleShape).background(BgInput)
                        .border(2.dp, Primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text(profile?.full_name?.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 40.sp, fontWeight = FontWeight.Black, color = PrimaryGlow)
                    }
                }
                Box(modifier = Modifier.size(28.dp).align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(14.dp)).background(BgCard)
                    .border(1.dp, Border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center) { Text("📷", fontSize = 14.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Text(profile?.full_name ?: "", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text(profile?.email ?: "", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(modifier = Modifier.weight(1f), icon = "♊", label = "Burç", value = profile?.zodiac ?: "—")
            InfoCard(modifier = Modifier.weight(1f), icon = "⚧", label = "Cinsiyet", value = profile?.gender ?: "—")
        }

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp)).background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Hakkında", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (!editing) {
                    TextButton(onClick = { editing = true }) { Text("Düzenle ✏️", color = PrimaryGlow, fontSize = 13.sp) }
                }
            }
            if (editing) {
                OutlinedTextField(value = bio, onValueChange = { bio = it },
                    placeholder = { Text("Kendinden bahset...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary, focusedBorderColor = Primary,
                        unfocusedBorderColor = Border, cursorColor = PrimaryGlow,
                        focusedContainerColor = BgInput, unfocusedContainerColor = BgInput))
            } else {
                Text(bio.ifEmpty { "Henüz bir şey yazılmamış..." }, color = TextSecondary, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp)).background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text("İlgi Alanları", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp))
            val displayInterests = if (editing) INTERESTS else selectedInterests.toList()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayInterests.forEach { item ->
                    val active = selectedInterests.contains(item)
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (active) Primary.copy(alpha = 0.25f) else BgInput)
                        .border(1.5.dp, if (active) PrimaryGlow else Border, RoundedCornerShape(999.dp))
                        .then(if (editing) Modifier.clickable {
                            selectedInterests = if (active) selectedInterests - item else selectedInterests + item
                        } else Modifier).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(item, color = if (active) PrimaryGlow else TextMuted,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                    }
                }
            }
        }

        if (editing) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { editing = false; vm.loadProfile() }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, Border)) {
                    Text("İptal", color = TextSecondary)
                }
                Button(onClick = { vm.updateProfile(userId, bio, selectedInterests.joinToString(", ")) },
                    modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(999.dp)) {
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun InfoCard(modifier: Modifier, icon: String, label: String, value: String) {
    Column(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(BgCard)
        .border(1.dp, Border, RoundedCornerShape(16.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextMuted, fontSize = 11.sp)
        Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}