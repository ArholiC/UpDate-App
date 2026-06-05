package com.update.app.ui.screens

import android.app.DatePickerDialog
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
import org.json.JSONObject
import java.util.Calendar

private val INTERESTS = listOf(
    "🎮 Oyun", "🎵 Müzik", "📚 Kitap", "🎬 Film", "🏃 Spor",
    "🍳 Yemek", "✈️ Seyahat", "🎨 Sanat", "💻 Teknoloji", "🌿 Doğa",
    "📸 Fotoğraf", "🎭 Tiyatro", "🧘 Yoga", "🎲 Masa Oyunları", "🐾 Hayvanlar"
)

private val ZODIACS = listOf("Koç","Boğa","İkizler","Yengeç","Aslan","Başak","Terazi","Akrep","Yay","Oğlak","Kova","Balık")

// Web ile birebir aynı 10 soru
private val QUESTIONS = listOf(
    Pair("q1", Pair("İdeal bir ilk buluşma sence nasıl olmalı?",
        listOf("Sakin bir kahve içmek", "Doğada yürüyüş yapmak", "Eğlenceli bir aktivite", "Şık bir akşam yemeği"))),
    Pair("q2", Pair("Bir ilişkide senin için en önemli şey nedir?",
        listOf("Güven ve sadakat", "Birlikte çok eğlenebilmek", "Ortak ilgi alanları", "Tutku ve heyecan"))),
    Pair("q3", Pair("Boş bir hafta sonunu nasıl değerlendirirsin?",
        listOf("Evde dizi/film izleyerek", "Arkadaşlarla dışarı çıkarak", "Yeni yerler keşfederek", "Hobilerime vakit ayırarak"))),
    Pair("q4", Pair("Karşı tarafta seni ilk bakışta en çok ne etkiler?",
        listOf("Zekası ve esprileri", "Gülüşü ve bakışları", "Özgüveni ve duruşu", "Samimiyeti ve nezaketi"))),
    Pair("q5", Pair("Stresli bir günün ardından nasıl rahatlarsın?",
        listOf("Müzik veya film ile", "Spor/Yürüyüş yaparak", "Sevdiğim biriyle konuşarak", "Sessizce yalnız kalarak"))),
    Pair("q6", Pair("Hayata bakış açını hangisi daha iyi özetler?",
        listOf("Anı yaşamak, akışına bırakmak", "Planlı ve hedefe odaklı olmak", "Sürekli yeni şeyler öğrenmek", "Huzurlu ve dengeli yaşamak"))),
    Pair("q7", Pair("Biriyle tartışırken genellikle nasıl davranırsın?",
        listOf("Sakin kalıp dinlemeye çalışırım", "Hemen çözüm üretmek isterim", "Kendi düşüncemi hararetle savunurum", "Biraz uzaklaşıp düşünmeye ihtiyacım olur"))),
    Pair("q8", Pair("Sence aşkın en güzel hali hangisidir?",
        listOf("En iyi arkadaşınla sevgili olmak", "Her an beraber olup her şeyi paylaşmak", "Birbirini özgür bırakarak büyümek", "Sürprizler ve romantizmle dolu olması"))),
    Pair("q9", Pair("Para harcama alışkanlığın nasıldır?",
        listOf("Deneyimlere (Seyahat vb.) harcarım", "Gelecek için birikim yaparım", "Teknoloji ve hobilere harcarım", "Sevdiklerime ve yemeğe harcarım"))),
    Pair("q10", Pair("Birini gerçekten tanıdığını ne zaman anlarsın?",
        listOf("Birlikte tatile çıktığımızda", "Zor bir gününde yanında olduğumda", "Beraber çok gülebildiğimizde", "Derin konularda konuşabildiğimizde")))
)

// Doğum tarihinden burç hesapla
fun calculateZodiac(day: Int, month: Int): String {
    return when {
        (month == 3 && day >= 21) || (month == 4 && day <= 19) -> "Koç"
        (month == 4 && day >= 20) || (month == 5 && day <= 20) -> "Boğa"
        (month == 5 && day >= 21) || (month == 6 && day <= 20) -> "İkizler"
        (month == 6 && day >= 21) || (month == 7 && day <= 22) -> "Yengeç"
        (month == 7 && day >= 23) || (month == 8 && day <= 22) -> "Aslan"
        (month == 8 && day >= 23) || (month == 9 && day <= 22) -> "Başak"
        (month == 9 && day >= 23) || (month == 10 && day <= 22) -> "Terazi"
        (month == 10 && day >= 23) || (month == 11 && day <= 21) -> "Akrep"
        (month == 11 && day >= 22) || (month == 12 && day <= 21) -> "Yay"
        (month == 12 && day >= 22) || (month == 1 && day <= 19) -> "Oğlak"
        (month == 1 && day >= 20) || (month == 2 && day <= 18) -> "Kova"
        else -> "Balık"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(authViewModel: AuthViewModel, onSuccess: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    // 5 step: 0=Temel, 1=Cinsiyet/Burç, 2=İlgi, 3=Sorular, 4=Bio
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

    // Soru cevapları
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> onSuccess()
            is AuthState.Error -> errorMsg = s.message
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        // Progress bar — 5 adım
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (step == 3 && currentQuestionIndex > 0) {
                    currentQuestionIndex--
                } else if (step > 0) {
                    step--
                    if (step == 3) currentQuestionIndex = QUESTIONS.size - 1
                } else {
                    onBack()
                }
            }) {
                Text("← Geri", color = PrimaryGlow, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { i ->
                    Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) Primary else BgInput))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${step+1}/5", color = TextMuted, fontSize = 12.sp)
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
                        listOf("Erkek" to "erkek", "Kadın" to "kadin").forEach { (label, value) ->
                            SelectChip(label, gender == value) { gender = value }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Doğum Tarihi — DatePicker Dialog
                    Text("Doğum Tarihi", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgInput)
                            .border(1.dp, if (birthDate.isNotEmpty()) Primary else Border, RoundedCornerShape(14.dp))
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val m = month + 1
                                        birthDate = "%04d-%02d-%02d".format(year, m, day)
                                        // Otomatik burç hesapla
                                        zodiac = calculateZodiac(day, m)
                                    },
                                    cal.get(Calendar.YEAR) - 20,
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).apply {
                                    // Max tarih: 18 yaş
                                    datePicker.maxDate = System.currentTimeMillis() - (18L * 365 * 24 * 60 * 60 * 1000)
                                }.show()
                            }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (birthDate.isEmpty()) "Tarihe dokunarak seç 📅" else birthDate,
                                color = if (birthDate.isEmpty()) TextMuted else TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (birthDate.isEmpty()) FontWeight.Normal else FontWeight.SemiBold
                            )
                            if (birthDate.isNotEmpty()) {
                                Text("✓", color = Primary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }

                    // Burç — Otomatik hesaplandıysa göster, yoksa manuel seç
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Burç", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (zodiac.isNotEmpty()) {
                            Text("✨ Otomatik: $zodiac", color = PrimaryGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZODIACS.forEach { z -> SelectChip(z, zodiac == z) { zodiac = z } }
                    }

                    Spacer(Modifier.height(24.dp))
                    StepButton("Devam →") {
                        if (gender.isBlank() || birthDate.isBlank() || zodiac.isBlank())
                            errorMsg = "Cinsiyet, doğum tarihi ve burç seçiniz"
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
                        else { errorMsg = ""; currentQuestionIndex = 0; step++ }
                    }
                }
                3 -> {
                    // 10 Uyum Sorusu
                    val q = QUESTIONS[currentQuestionIndex]
                    val qId = q.first
                    val qText = q.second.first
                    val qOpts = q.second.second

                    Text("Soru ${currentQuestionIndex + 1}/10 🔥",
                        color = TextMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp))

                    Text(qText, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary,
                        modifier = Modifier.padding(bottom = 24.dp))

                    // Progress dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 20.dp)) {
                        repeat(10) { i ->
                            Box(modifier = Modifier.size(if (i == currentQuestionIndex) 10.dp else 6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    when {
                                        i < currentQuestionIndex -> Primary
                                        i == currentQuestionIndex -> PrimaryGlow
                                        else -> BgInput
                                    }
                                ))
                        }
                    }

                    qOpts.forEach { opt ->
                        val isSelected = answers[qId] == opt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Primary.copy(alpha = 0.25f) else BgCard)
                                .border(1.5.dp, if (isSelected) Primary else Border, RoundedCornerShape(16.dp))
                                .clickable {
                                    answers[qId] = opt
                                    if (currentQuestionIndex < QUESTIONS.size - 1) {
                                        currentQuestionIndex++
                                    } else {
                                        errorMsg = ""
                                        step++
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(opt, color = if (isSelected) PrimaryGlow else TextSecondary,
                                fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                4 -> {
                    StepTitle("Son bir şey 🌟", "Kendinden kısaca bahset")
                    OutlinedTextField(value = bio, onValueChange = { bio = it },
                        placeholder = { Text("Merhaba, ben...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary, focusedBorderColor = Primary,
                            unfocusedBorderColor = Border, cursorColor = PrimaryGlow,
                            focusedContainerColor = BgInput, unfocusedContainerColor = BgInput))
                    Spacer(Modifier.height(24.dp))

                    // Cevapları JSON'a çevir
                    val answersJson = try {
                        JSONObject().apply {
                            answers.forEach { (k, v) -> put(k, v) }
                        }.toString()
                    } catch (e: Exception) { "{}" }

                    Button(onClick = {
                        authViewModel.register(context, RegisterRequest(
                            full_name = fullName.trim(),
                            email = email.trim(),
                            password = password,
                            gender = gender,
                            birth_date = birthDate,
                            zodiac = zodiac,
                            interests = selectedInterests.joinToString(", "),
                            bio = bio.trim(),
                            answers = answersJson
                        ))
                    }, modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(999.dp)) {
                        if (authState is AuthState.Loading)
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Hesabı Oluştur 🚀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    if (errorMsg.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Dislike.copy(alpha = 0.1f))
                            .border(1.dp, Dislike.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)) {
                            Text("⚠️ $errorMsg", color = Dislike, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (step != 4 && errorMsg.isNotEmpty()) {
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
