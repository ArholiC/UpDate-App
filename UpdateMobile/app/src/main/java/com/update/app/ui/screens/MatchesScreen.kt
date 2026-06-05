package com.update.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.update.app.data.model.User
import com.update.app.data.network.BASE_URL
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.MatchesViewModel

@Composable
fun MatchesScreen(userId: Int, onOpenChat: (Int, String) -> Unit, vm: MatchesViewModel = viewModel()) {
    val matches by vm.matches.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(Unit) { vm.loadMatches(userId) }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Eşleşmeler ❤️", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text("${matches.size} eşleşme", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            matches.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💔", fontSize = 56.sp)
                    Text("Henüz eşleşme yok", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Keşfet ekranından beğeni gönder!", color = TextMuted, fontSize = 14.sp)
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(matches) { match -> MatchCard(match, onOpenChat) }
            }
        }
    }
}

@Composable
fun MatchCard(match: User, onOpenChat: (Int, String) -> Unit) {
    val interests = match.interests?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)).background(BgCard)
        .border(1.dp, Border, RoundedCornerShape(16.dp))
        .clickable { onOpenChat(match.id, match.full_name) }
        .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!match.profile_pic.isNullOrEmpty()) {
            val picUrl = if (match.profile_pic.startsWith("/")) "${BASE_URL.trimEnd('/')}${match.profile_pic}"
                         else "${BASE_URL}public/uploads/${match.profile_pic}"
            AsyncImage(model = picUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(CircleShape))
        } else {
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(BgInput)
                .border(1.dp, Border, CircleShape), contentAlignment = Alignment.Center) {
                Text(match.full_name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 24.sp, fontWeight = FontWeight.Black, color = PrimaryGlow)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(match.full_name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(match.zodiac ?: "", color = TextMuted, fontSize = 12.sp)
            }
            if (!match.bio.isNullOrEmpty()) {
                Text(match.bio, color = TextSecondary, fontSize = 13.sp, maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp))
            }
            if (interests.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    interests.take(3).forEach { tag ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            .background(Primary.copy(alpha = 0.15f))
                            .border(1.dp, Border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(tag, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("💬", fontSize = 20.sp)
    }
}