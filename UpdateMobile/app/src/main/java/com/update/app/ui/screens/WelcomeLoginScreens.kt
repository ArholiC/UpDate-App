package com.update.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.AuthState
import com.update.app.ui.viewmodel.AuthViewModel

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Box(
            modifier = Modifier.size(300.dp).offset(x = 80.dp, y = (-60).dp)
                .background(Brush.radialGradient(listOf(Primary.copy(alpha = 0.25f), Color.Transparent)), RoundedCornerShape(150.dp))
        )
        Box(
            modifier = Modifier.size(200.dp).offset(x = (-40).dp, y = 400.dp)
                .background(Brush.radialGradient(listOf(PrimaryLight.copy(alpha = 0.2f), Color.Transparent)), RoundedCornerShape(100.dp))
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔥", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("UpDate", fontSize = 48.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = (-2).sp)
            Text("Hobilerle bağlan. Anlarla buluş.", fontSize = 15.sp, color = TextSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 48.dp))
            listOf("🎯" to "Jaccard algoritmasıyla akıllı eşleşme",
                   "✨" to "Ortak ilgi alanlarına göre %uyum skoru",
                   "💬" to "Sadece eşleşenler mesajlaşabilir").forEach { (icon, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp)).background(BgCard)
                        .border(1.dp, Border, RoundedCornerShape(12.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(text, color = TextSecondary, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
            Button(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(999.dp)) {
                Text("Hesap Oluştur", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Border)) {
                Text("Zaten hesabım var", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onSuccess: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> onSuccess()
            is AuthState.Error -> errorMsg = s.message
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())
        .padding(24.dp).statusBarsPadding()) {
        TextButton(onClick = onBack) { Text("← Geri", color = PrimaryGlow, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(16.dp))
        Text("Tekrar hoş geldin ❤️", fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        Text("Hesabına giriş yap", color = TextSecondary, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))
        UpDateTextField(value = email, onValueChange = { email = it }, label = "Email",
            placeholder = "ornek@email.com",
            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextMuted) }, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        UpDateTextField(value = password, onValueChange = { password = it }, label = "Şifre",
            placeholder = "••••••••",
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextMuted) }, isPassword = true)
        if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(errorMsg, color = Dislike, fontSize = 13.sp) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { authViewModel.login(context, email.trim(), password) },
            modifier = Modifier.fillMaxWidth().height(52.dp), enabled = authState !is AuthState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(999.dp)) {
            if (authState is AuthState.Loading)
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Giriş Yap", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun UpDateTextField(value: String, onValueChange: (String) -> Unit, label: String,
    placeholder: String = "", leadingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text, isPassword: Boolean = false, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextMuted) }, leadingIcon = leadingIcon,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary, focusedBorderColor = Primary,
                unfocusedBorderColor = Border, cursorColor = PrimaryGlow,
                focusedContainerColor = BgInput, unfocusedContainerColor = BgInput))
    }
}
