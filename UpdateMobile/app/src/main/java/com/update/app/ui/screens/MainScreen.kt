package com.update.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.AuthViewModel
import com.update.app.ui.viewmodel.MessagesViewModel
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

data class TabItem(val emoji: String, val label: String)

@Composable
fun MainScreen(
    userId: Int,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onOpenChat: (Int, String) -> Unit,
    onStartCall: (Int, String, String) -> Unit = { _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val messagesVm: MessagesViewModel = viewModel()
    val unreadCount by messagesVm.unreadCount.collectAsState()

    // ── Global gelen arama dinleyici ──
    var incomingCall by remember { mutableStateOf<Triple<Int, String, String>?>(null) }
    var globalSocket by remember { mutableStateOf<Socket?>(null) }

    DisposableEffect(userId) {
        // MainScope: DisposableEffect içinde güvenli coroutine scope
        val coroutineScope = MainScope()
        if (userId > 0) {
            try {
                val s = IO.socket("http://10.0.2.2:5005", IO.Options.builder().setReconnection(true).build())
                s.on(Socket.EVENT_CONNECT) {
                    s.emit("join", userId)
                    android.util.Log.d("GlobalSocket", "Global socket joined user_$userId")
                }
                s.on("call_offer") { args ->
                    try {
                        val raw = args.getOrNull(0)
                        val obj: JSONObject? = when (raw) {
                            is JSONObject -> raw
                            is String -> JSONObject(raw)
                            else -> try { JSONObject(raw.toString()) } catch (e: Exception) { null }
                        }
                        if (obj != null) {
                            val fromId = obj.optInt("fromId")
                            val callType = obj.optString("callType", "audio")
                            val callerName = obj.optString("callerName", "Kullanıcı")
                            coroutineScope.launch { incomingCall = Triple(fromId, callerName, callType) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GlobalSocket", "call_offer parse hata: $e")
                    }
                }
                s.connect()
                globalSocket = s
            } catch (e: Exception) { android.util.Log.e("GlobalSocket", "Bağlanma hatası: $e") }
        }
        onDispose {
            globalSocket?.disconnect()
            coroutineScope.cancel()
        }
    }

    // Gelen arama dialog (tüm ekranlarda görünür)
    incomingCall?.let { (callerId, callerName, callType) ->
        AlertDialog(
            onDismissRequest = {
                globalSocket?.emit("call_reject", JSONObject().apply { put("fromId", userId); put("toId", callerId) })
                incomingCall = null
            },
            containerColor = BgCard,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(if (callType == "video") "📹 Görüntülü Arama" else "📞 Sesli Arama",
                        color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(callerName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("seni arıyor...", color = TextMuted, fontSize = 12.sp)
                }
            },
            text = null,
            confirmButton = {
                Button(
                    onClick = {
                        incomingCall = null
                        globalSocket?.emit("call_answer", JSONObject().apply { put("fromId", userId); put("toId", callerId) })
                        onStartCall(callerId, callType, callerName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text(if (callType == "video") "📹 Kabul Et" else "📞 Kabul Et", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        globalSocket?.emit("call_reject", JSONObject().apply { put("fromId", userId); put("toId", callerId) })
                        incomingCall = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) { Text("Reddet") }
            }
        )
    }

    val tabs = listOf(
        TabItem("🔥", "Keşfet"),
        TabItem("❤️", "Eşleşmeler"),
        TabItem("💬", "Mesajlar"),
        TabItem("👤", "Profil")
    )

    // Arka plan bildirim kontrolü başlat
    LaunchedEffect(userId) {
        messagesVm.startBackgroundCheck(userId)
    }

    // Tab değişince Messages tab'ına geçildiğinde okunmamışları sıfırla
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            messagesVm.clearUnread(userId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> DiscoverScreen(userId = userId)
                1 -> MatchesScreen(userId = userId, onOpenChat = onOpenChat)
                2 -> MessagesScreen(userId = userId, onOpenChat = onOpenChat, vm = messagesVm)
                3 -> ProfileScreen(userId = userId, onLogout = onLogout)
            }
        }

        // ─── Alt Navigasyon ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp))
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                val isMessages = index == 2
                val hasUnread = isMessages && unreadCount > 0

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (selected) Primary.copy(alpha = 0.2f)
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(
                            horizontal = if (selected) 14.dp else 10.dp,
                            vertical = 8.dp
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { selectedTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Emoji + badge
                        Box(contentAlignment = Alignment.TopEnd) {
                            Text(tab.emoji, fontSize = 22.sp)

                            // 🔴 Okunmamış badge
                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(if (unreadCount > 9) 18.dp else 16.dp)
                                        .clip(CircleShape)
                                        .background(Primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        if (selected) {
                            Text(
                                tab.label,
                                color = if (hasUnread) Primary else PrimaryGlow,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}