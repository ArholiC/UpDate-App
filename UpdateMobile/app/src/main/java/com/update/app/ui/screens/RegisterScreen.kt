package com.update.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.update.app.data.model.RegisterRequest
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.AuthState
import com.update.app.ui.viewmodel.AuthViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

private val INTERESTS = listOf(
    "🎮 Oyun", "🎵 Müzik", "📚 Kitap", "🎬 Film", "🏃 Spor",
    "🍳 Yemek", "✈️ Seyahat", "🎨 Sanat", "💻 Teknoloji", "🌿 Doğa",
    "📸 Fotoğraf", "🎭 Tiyatro", "🧘 Yoga", "🎲 Masa Oyunları", "🐾 Hayvanlar"
)

private val ZODIACS = listOf("Koç","Boğa","İkizler","Yengeç","Aslan","Başak","Terazi","Akrep","Yay","Oğlak","Kova","Balık")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(authViewModel: AuthViewModel, onSuccess: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var zodiac by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var bio by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> onSuccess()
            is AuthState.Error -> errorMsg = s.message
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        // Progress bar
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (step > 0) step-- else onBack() }) {
                Text("← Geri", color = PrimaryGlow, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) Primary else BgInput))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${step+1}/4", color = TextMuted, fontSize = 12.sp)
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            when (step) {
                0 -> {
                    StepTitle("Merhaba! 👋", "Hesabını oluşturalım")
                    UpDateTextField(fullName, { fullName = it }, "Ad Soyad", "Adın ve soyadın")
                    Spacer(Modifier.height(12.dp))
                    UpDateTextField(email, { email = it }, "Email", "ornek@email.com", keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(12.dp))
                    UpDateTextField(password, { password = it }, "Şifre", "En az 6 karakter", isPassword = true)
                    Spacer(Modifier.height(24.dp))
                    StepButton("Devam →") {
                        if (fullName.isBlank() || email.isBlank() || password.length < 6)
                            errorMsg = "Tüm alanları doldurun (şifre min 6 karakter)"
                        else { errorMsg = ""; step++ }
                    }
                }
                1 -> {
                    StepTitle("Seni tanıyalım ✨", "Biraz daha bilgi")
                    Text("Cinsiyet", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Erkek", "Kadın", "Diğer").forEach { g ->
                            SelectChip(g, gender == g) { gender = g }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    UpDateTextField(birthDate, { birthDate = it }, "Doğum Tarihi", "2000-01-25", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(16.dp))
                    Text("Burç", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZODIACS.forEach { z -> SelectChip(z, zodiac == z) { zodiac = z } }
                    }
                    Spacer(Modifier.height(24.dp))
                    StepButton("Devam →") {
                        if (gender.isBlank() || birthDate.isBlank() || zodiac.isBlank())
                            errorMsg = "Tüm alanları seçin"
                        else { errorMsg = ""; step++ }
                    }
                }
                2 -> {
                    StepTitle("İlgi Alanların 🎯", "En az 3 tane seç")
                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        INTERESTS.forEach { item ->
                            val active = selectedInterests.contains(item)
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                    .background(if (active) Primary.copy(alpha = 0.25f) else BgCard)
                                    .border(1.5.dp, if (active) PrimaryGlow else Border, RoundedCornerShape(999.dp))
                                    .clickable {
                                        selectedInterests = if (active) selectedInterests - item else selectedInterests + item
                                    }.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(item, color = if (active) PrimaryGlow else TextSecondary,
                                    fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${selectedInterests.size} seçildi", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(24.dp))
                    StepButton("Devam →") {
                        if (selectedInterests.size < 3) errorMsg = "En az 3 ilgi alanı seç"
                        else { errorMsg = ""; step++ }
                    }
                }
                3 -> {
                    StepTitle("Son bir şey 🌟", "Kendinden kısaca bahset")
                    OutlinedTextField(value = bio, onValueChange = { bio = it },
                        placeholder = { Text("Merhaba, ben...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary, focusedBorderColor = Primary,
                            unfocusedBorderColor = Border, cursorColor = PrimaryGlow,
                            focusedContainerColor = BgInput, unfocusedContainerColor = BgInput))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        authViewModel.register(context, RegisterRequest(
                            full_name = fullName.trim(), email = email.trim(), password = password,
                            gender = gender, birth_date = birthDate, zodiac = zodiac,
                            interests = selectedInterests.joinToString(", "), bio = bio.trim()))
                    }, modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(999.dp)) {
                        if (authState is AuthState.Loading)
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Hesabı Oluştur 🚀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            if (errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errorMsg, color = Dislike, fontSize = 13.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextPrimary)
    Text(subtitle, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(999.dp)) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
        .background(if (selected) Primary.copy(alpha = 0.25f) else BgInput)
        .border(1.5.dp, if (selected) Primary else Border, RoundedCornerShape(999.dp))
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, color = if (selected) PrimaryGlow else TextMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}
