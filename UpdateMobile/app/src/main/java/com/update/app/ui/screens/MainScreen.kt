package com.update.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.AuthViewModel

data class TabItem(val emoji: String, val label: String)

@Composable
fun MainScreen(
    userId: Int,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onOpenChat: (Int, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("🔥", "Keşfet"),
        TabItem("💜", "Eşleşmeler"),
        TabItem("💬", "Mesajlar"),
        TabItem("👤", "Profil")
    )

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> DiscoverScreen(userId = userId)
                1 -> MatchesScreen(userId = userId, onOpenChat = onOpenChat)
                2 -> MessagesScreen(userId = userId, onOpenChat = onOpenChat)
                3 -> ProfileScreen(userId = userId, onLogout = onLogout)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(BgCard)
            .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp))
            .navigationBarsPadding().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) Primary.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 8.dp)
                    .then(Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { selectedTab = index }),
                    contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tab.emoji, fontSize = 22.sp)
                        if (selected) {
                            Text(tab.label, color = PrimaryGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}