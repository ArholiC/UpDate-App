package com.update.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.update.app.data.model.User
import com.update.app.data.network.BASE_URL
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.DiscoverViewModel
import kotlin.math.roundToInt

@Composable
fun DiscoverScreen(userId: Int, vm: DiscoverViewModel = viewModel()) {
    val users by vm.users.collectAsState()
    val currentIndex by vm.currentIndex.collectAsState()
    val loading by vm.loading.collectAsState()
    val matchedName by vm.matchedName.collectAsState()
    var filter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filter) { vm.loadUsers(userId) }

    if (!matchedName.isNullOrEmpty()) {
        AlertDialog(
            onDismissRequest = { vm.clearMatch() },
            containerColor = BgCard,
            title = { Text("🎉 Eşleşme!", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("${matchedName} ile eşleştin!", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.clearMatch() }) { Text("Harika!", color = PrimaryGlow) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("UpDate 💜", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
        Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Hepsi", "female" to "Kadınlar", "male" to "Erkekler").forEach { (f, label) ->
                val active = filter == f
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (active) Primary.copy(alpha = 0.25f) else BgInput)
                    .border(1.dp, if (active) Primary else Border, RoundedCornerShape(999.dp))
                    .clickable { filter = f }.padding(horizontal = 16.dp, vertical = 7.dp)) {
                    Text(label, color = if (active) PrimaryGlow else TextMuted,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                }
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(color = Primary)
                currentIndex >= users.size -> EmptyState { vm.loadUsers(userId) }
                else -> {
                    if (currentIndex + 1 < users.size) {
                        SwipeCard(user = users[currentIndex + 1], isBackground = true, onSwipe = {})
                    }
                    SwipeCard(user = users[currentIndex], isBackground = false) { isLike ->
                        vm.swipe(userId, users[currentIndex].id, isLike)
                    }
                }
            }
        }

        if (!loading && currentIndex < users.size) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Dislike.copy(alpha = 0.1f))
                    .border(2.dp, Dislike, CircleShape)
                    .clickable { vm.swipe(userId, users[currentIndex].id, false) },
                    contentAlignment = Alignment.Center) {
                    Text("✕", fontSize = 26.sp, color = Dislike)
                }
                Spacer(Modifier.width(32.dp))
                Box(modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(Like.copy(alpha = 0.1f))
                    .border(2.dp, Like, CircleShape)
                    .clickable { vm.swipe(userId, users[currentIndex].id, true) },
                    contentAlignment = Alignment.Center) {
                    Text("♥", fontSize = 30.sp, color = Like)
                }
            }
        }
    }
}

@Composable
fun SwipeCard(user: User, isBackground: Boolean, onSwipe: (Boolean) -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val rotation = (offsetX / 30f).coerceIn(-15f, 15f)
    val likeAlpha = (offsetX / 200f).coerceIn(0f, 1f)
    val nopeAlpha = (-offsetX / 200f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .width(340.dp).height(500.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .rotate(if (isBackground) 0f else rotation)
            .then(if (isBackground) Modifier else Modifier.pointerInput(user.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > 200f -> onSwipe(true)
                            offsetX < -200f -> onSwipe(false)
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
            AsyncImage(model = "${BASE_URL}uploads/${user.profile_pic}",
                contentDescription = null, contentScale = ContentScale.Crop,
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

        if (user.compatibility != null) {
            Box(modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(999.dp))
                .background(Bg.copy(alpha = 0.85f))
                .border(1.dp, PrimaryGlow, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text("%${(user.compatibility * 100).roundToInt()} uyum",
                    color = PrimaryGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (!isBackground) {
            Box(modifier = Modifier.padding(16.dp).align(Alignment.TopEnd)
                .clip(RoundedCornerShape(8.dp)).background(Like.copy(alpha = 0.15f * likeAlpha))
                .border(2.dp, Like.copy(alpha = likeAlpha), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("LIKE 💚", color = Like.copy(alpha = likeAlpha), fontWeight = FontWeight.Black)
            }
            Box(modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                .clip(RoundedCornerShape(8.dp)).background(Dislike.copy(alpha = 0.15f * nopeAlpha))
                .border(2.dp, Dislike.copy(alpha = nopeAlpha), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("NOPE ❌", color = Dislike.copy(alpha = nopeAlpha), fontWeight = FontWeight.Black)
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🌌", fontSize = 64.sp)
        Text("Herkesi gördün!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Yarın yeni profiller gelecek", color = TextMuted, fontSize = 14.sp)
        Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(999.dp)) {
            Text("Yenile")
        }
    }
}