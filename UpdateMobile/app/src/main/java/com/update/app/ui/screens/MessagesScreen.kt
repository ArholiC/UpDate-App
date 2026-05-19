package com.update.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.update.app.ui.viewmodel.MessagesViewModel
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(userId: Int, onOpenChat: (Int, String) -> Unit, vm: MessagesViewModel = viewModel()) {
    val conversations by vm.conversations.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(Unit) { vm.loadConversations(userId) }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Text("Mesajlar 💬", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Primary) }
            conversations.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💌", fontSize = 56.sp)
                    Text("Henüz mesaj yok", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Eşleştiğin kişilerle konuşabilirsin", color = TextMuted, fontSize = 14.sp)
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(conversations) { conv -> ConvCard(conv, onOpenChat) }
            }
        }
    }
}

@Composable
fun ConvCard(user: User, onOpenChat: (Int, String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)).background(BgCard)
        .border(1.dp, Border, RoundedCornerShape(16.dp))
        .clickable { onOpenChat(user.id, user.full_name) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (!user.profile_pic.isNullOrEmpty()) {
            AsyncImage(model = "${BASE_URL}uploads/${user.profile_pic}", contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(50.dp).clip(CircleShape).background(BgInput)
                .border(1.dp, Border, CircleShape), contentAlignment = Alignment.Center) {
                Text(user.full_name.firstOrNull()?.uppercase() ?: "?", fontSize = 20.sp,
                    fontWeight = FontWeight.Black, color = PrimaryGlow)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(user.full_name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
        Text("›", fontSize = 20.sp, color = TextMuted)
    }
}

@Composable
fun ChatScreen(myId: Int, userId: Int, userName: String, onBack: () -> Unit, vm: MessagesViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(userId) { vm.startPolling(myId, userId) }
    DisposableEffect(Unit) { onDispose { vm.stopPolling() } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().background(BgCard)
            .border(bottom = 1.dp, color = Border).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryGlow)
            }
            Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                val isMe = msg.sender_id == myId
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                    Box(modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(16.dp, 16.dp, if (isMe) 4.dp else 16.dp, if (isMe) 16.dp else 4.dp))
                        .background(if (isMe) Primary else BgCard)
                        .then(if (!isMe) Modifier.border(1.dp, Border, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)) else Modifier)
                        .padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(msg.message, color = if (isMe) TextPrimary else TextSecondary, fontSize = 15.sp)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(BgCard)
            .border(top = 1.dp, color = Border).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = text, onValueChange = { text = it },
                placeholder = { Text("Mesaj yaz...", color = TextMuted) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary, focusedBorderColor = Primary,
                    unfocusedBorderColor = Border, cursorColor = PrimaryGlow,
                    focusedContainerColor = BgInput, unfocusedContainerColor = BgInput))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    vm.sendMessage(myId, userId, text.trim())
                    text = ""
                }
            }, modifier = Modifier.size(48.dp).clip(CircleShape).background(
                if (text.isNotBlank()) Primary else BgInput)) {
                Icon(Icons.AutoMirrored.Filled.Send, null,
                    tint = if (text.isNotBlank()) TextPrimary else TextMuted)
            }
        }
    }
}

fun Modifier.border(bottom: androidx.compose.ui.unit.Dp = 0.dp, top: androidx.compose.ui.unit.Dp = 0.dp, color: androidx.compose.ui.graphics.Color): Modifier {
    return this
}