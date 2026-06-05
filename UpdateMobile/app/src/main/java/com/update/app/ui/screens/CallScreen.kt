package com.update.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.update.app.data.network.BASE_URL
import com.update.app.ui.theme.*
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun CallScreen(
    myId: Int,
    calleeId: Int,
    calleeName: String,
    callType: String,         // "audio" veya "video"
    isCallee: Boolean = false, // true = gelen arama (biz aranan tarafız)
    onEnd: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var callStatus by remember {
        mutableStateOf(
            if (isCallee) "✅ Bağlantı kuruldu"
            else if (callType == "video") "📹 Bağlanıyor..."
            else "📞 Bağlanıyor..."
        )
    }
    var isConnected by remember { mutableStateOf(isCallee) } // Callee = zaten bağlı
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var timerSecs by remember { mutableStateOf(0) }
    var socket by remember { mutableStateOf<Socket?>(null) }
    var calleeProfilePic by remember { mutableStateOf<String?>(null) }

    // Kamera izni
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) Toast.makeText(context, "Kamera izni reddedildi", Toast.LENGTH_SHORT).show()
    }

    // Kamera önizleme state (video için)
    val previewView = remember { if (callType == "video") PreviewView(context) else null }

    // Pulsating animasyon (sadece bağlanırken)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(1f, 1.6f,
        infiniteRepeatable(tween(1800, easing = EaseOut), RepeatMode.Restart), label = "p1")
    val pulseScale2 by infiniteTransition.animateFloat(1f, 2.2f,
        infiniteRepeatable(tween(1800, 400, easing = EaseOut), RepeatMode.Restart), label = "p2")

    // Timer
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) { delay(1000); timerSecs++ }
        }
    }
    val timerStr = remember(timerSecs) {
        "${(timerSecs / 60).toString().padStart(2, '0')}:${(timerSecs % 60).toString().padStart(2, '0')}"
    }

    // Kamera izni iste (video arama)
    LaunchedEffect(callType) {
        if (callType == "video" && !hasCameraPermission) {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Socket.IO bağlantısı
    DisposableEffect(myId) {
        val scope = MainScope()
        try {
            val s = IO.socket("http://10.0.2.2:5000", IO.Options.builder().setReconnection(true).build())
            s.on(Socket.EVENT_CONNECT) {
                s.emit("join", myId)
                if (!isCallee) {
                    // CALLER: teklif gönder
                    val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                    val myName = prefs.getString("full_name", "Kullanıcı") ?: "Kullanıcı"
                    val offer = JSONObject().apply {
                        put("fromId", myId)
                        put("toId", calleeId)
                        put("callType", callType)
                        put("callerName", myName)
                        put("offer", JSONObject())
                    }
                    s.emit("call_offer", offer)
                    android.util.Log.d("CallScreen", "call_offer gönderildi → $calleeId")
                }
                // CALLEE: mainScreen'de zaten call_answer gönderildi, sadece join yeterli
            }

            s.on("call_answer") { _ ->
                scope.launch {
                    isConnected = true
                    callStatus = "✅ Bağlantı kuruldu"
                    android.util.Log.d("CallScreen", "call_answer alındı, bağlandı!")
                }
            }

            s.on("call_end") { _ ->
                scope.launch {
                    callStatus = "📵 Arama bitti"
                    delay(600)
                    onEnd()
                }
            }

            s.on("call_reject") { _ ->
                scope.launch {
                    callStatus = "❌ Reddedildi"
                    delay(1000)
                    onEnd()
                }
            }

            s.connect()
            socket = s
        } catch (e: Exception) {
            android.util.Log.e("CallScreen", "Socket hatası: $e")
        }

        onDispose {
            socket?.emit("call_end", JSONObject().apply { put("fromId", myId); put("toId", calleeId) })
            socket?.disconnect()
            scope.cancel()
        }
    }

    // Profil bilgisi
    LaunchedEffect(calleeId) {
        try {
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.net.URL("$BASE_URL/api/users/$calleeId").readText()
            }
            calleeProfilePic = org.json.JSONObject(res).optString("profile_pic", null)
        } catch (e: Exception) {}
    }

    // Kamera başlatma (video için)
    LaunchedEffect(callType, hasCameraPermission, previewView) {
        if (callType == "video" && hasCameraPermission && previewView != null) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("CallScreen", "Kamera başlatma hatası: $e")
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {}
        }
    }

    // ─── UI ───────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050309))) {

        // Arkaplan glow
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0x25FF416C), Color(0xFF050309)), radius = 700f)
        ))

        // VİDEO ARMA: Kamera önizleme
        if (callType == "video" && hasCameraPermission && previewView != null) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            // Kamera üzerinde koyu overlay
            Box(modifier = Modifier.fillMaxSize().background(Color(0x55000000)))
        }

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // Tip etiketi
            Text(
                text = if (callType == "video") "📹 Görüntülü Arama" else "📞 Sesli Arama",
                color = Color.White.copy(0.55f),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(32.dp))

            // Avatar + Pulse
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                // Pulse halkaları (sadece bağlanırken)
                if (!isConnected) {
                    Box(Modifier.size(200.dp).scale(pulseScale2).clip(CircleShape).background(Color(0x18FF416C)))
                    Box(Modifier.size(160.dp).scale(pulseScale1).clip(CircleShape).background(Color(0x28FF416C)))
                }

                // Video aramada kamera açıksa avatar gösterme
                val showAvatar = !(callType == "video" && hasCameraPermission && isConnected)
                if (showAvatar) {
                    val picUrl = calleeProfilePic?.let {
                        if (it.startsWith("/")) "${BASE_URL.trimEnd('/')}$it" else it
                    }
                    if (picUrl != null) {
                        AsyncImage(
                            model = picUrl, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                                .border(3.dp, Color(0x80FF416C), CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(130.dp).clip(CircleShape)
                                .background(Color(0x40FF416C))
                                .border(3.dp, Color(0x80FF416C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                calleeName.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF416C)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // İsim
            Text(calleeName, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)

            Spacer(Modifier.height(10.dp))

            // Durum / Timer
            if (isConnected) {
                Text(timerStr, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dotAnim by infiniteTransition.animateFloat(
                        0.3f, 1f,
                        infiniteRepeatable(tween(700), RepeatMode.Reverse),
                        label = "dot"
                    )
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF416C).copy(dotAnim)))
                    Text(callStatus, fontSize = 14.sp, color = Color.White.copy(0.55f))
                }
            }

            Spacer(Modifier.weight(1f))

            // ─── Kontrol Butonları ───
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sessiz
                CallButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Sessiz" else "Mikrofon",
                    active = isMuted,
                    size = 58
                ) { isMuted = !isMuted }

                // Kapat (büyük, kırmızı)
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))))
                        .clickable {
                            socket?.emit("call_end", JSONObject().apply {
                                put("fromId", myId); put("toId", calleeId)
                            })
                            onEnd()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Kapat",
                        tint = Color.White, modifier = Modifier.size(30.dp))
                }

                // Hoparlör
                CallButton(
                    icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    label = if (isSpeakerOn) "Hoparlör" else "Kulaklık",
                    active = false,
                    size = 58
                ) { isSpeakerOn = !isSpeakerOn }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    size: Int,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier.size(size.dp).clip(CircleShape)
                .background(if (active) Color(0x40FF416C) else Color.White.copy(0.08f))
                .border(
                    1.5.dp,
                    if (active) Color(0x80FF416C) else Color.White.copy(0.15f),
                    CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
