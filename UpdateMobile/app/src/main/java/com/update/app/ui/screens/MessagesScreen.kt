package com.update.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.update.app.data.model.User
import com.update.app.data.network.BASE_URL
import com.update.app.ui.theme.*
import com.update.app.ui.viewmodel.MessagesViewModel
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

// ─── Konuşmalar Listesi ───────────────────────────────────────────────────
@Composable
fun MessagesScreen(userId: Int, onOpenChat: (Int, String) -> Unit, vm: MessagesViewModel = viewModel()) {
    val conversations by vm.conversations.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(userId) { vm.loadConversations(userId) }

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
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable { onOpenChat(user.id, user.full_name) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!user.profile_pic.isNullOrEmpty()) {
            val picUrl = if (user.profile_pic.startsWith("/")) "${BASE_URL.trimEnd('/')}${user.profile_pic}"
                         else "${BASE_URL}public/uploads/${user.profile_pic}"
            AsyncImage(model = picUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Primary.copy(0.2f))
                .border(2.dp, Primary.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Text(user.full_name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 22.sp, fontWeight = FontWeight.Black, color = PrimaryGlow)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.full_name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text("Sohbete gitmek için dokun 💬", color = TextMuted, fontSize = 12.sp, maxLines = 1)
        }
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary.copy(0.15f)),
            contentAlignment = Alignment.Center) {
            Text("›", fontSize = 20.sp, color = PrimaryGlow, fontWeight = FontWeight.Black)
        }
    }
}

// ─── Typing Balonu ──────────────────────────────────────────────────────
@Composable
fun TypingBubble() {
    val inf = rememberInfiniteTransition(label = "")

    @Composable
    fun dot(delay: Int) = inf.animateFloat(0f, -8f,
        infiniteRepeatable(keyframes { durationMillis = 1200; 0f at 0; -8f at 300 + delay; 0f at 600 + delay },
            RepeatMode.Restart), label = "").value

    val d1 = dot(0); val d2 = dot(150); val d3 = dot(300)

    Row(modifier = Modifier.clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
        .background(BgCard).border(1.dp, Border, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
        .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(d1, d2, d3).forEach { off ->
            Box(modifier = Modifier.size(8.dp).offset(y = off.dp).clip(CircleShape).background(Primary))
        }
    }
}

// ─── Ses Balonunun Dalga Animasyonu ────────────────────────────────────
@Composable
fun AudioWaveIcon(isPlaying: Boolean) {
    val inf = rememberInfiniteTransition(label = "wave")
    val heights = (1..6).map { i ->
        if (isPlaying) inf.animateFloat(4f, 18f,
            infiniteRepeatable(tween(300 + i * 60, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "w$i").value
        else 6f
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        heights.forEach { h ->
            Box(modifier = Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.7f)))
        }
    }
}

// ─── Sohbet Ekranı ──────────────────────────────────────────────────────
@Composable
fun ChatScreen(
    myId: Int, userId: Int, userName: String, onBack: () -> Unit,
    onStartCall: (Int, String, String) -> Unit,  // (userId, callType, name)
    vm: MessagesViewModel = viewModel()
) {
    val messages by vm.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var socket by remember { mutableStateOf<Socket?>(null) }
    val context = LocalContext.current
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var playingAudioId by remember { mutableStateOf<Int?>(null) }
    val mediaPlayers = remember { mutableMapOf<Int, MediaPlayer>() }
    var incomingCall by remember { mutableStateOf<Triple<Int, String, String>?>(null) } // (callerId, name, type)

    val token = remember {
        context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }

    // Ses kaydedici
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val imageUrl: String? = uploadMsgImageFromUri(context, uri, token)
            if (imageUrl != null) vm.sendMessage(myId, userId, "", imageUrl)
        }
    }

    // RECORD_AUDIO runtime izin launcher
    // pendingStartRecording: izin verildikten sonra LaunchedEffect ile kaydı başlat
    var pendingStartRecording by remember { mutableStateOf(false) }

    val audioPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingStartRecording = true  // LaunchedEffect bunu yakalar
        } else {
            Toast.makeText(context, "Mikrofon izni gerekli! Ayarlardan izin ver.", Toast.LENGTH_LONG).show()
        }
    }

    // Socket.IO
    DisposableEffect(myId) {
        try {
            val s = IO.socket("http://10.0.2.2:5000", IO.Options.builder().setReconnection(true).build())
            s.on(Socket.EVENT_CONNECT) { s.emit("join", myId) }
            s.on("user_typing") { args ->
                val obj = args[0] as? JSONObject
                if (obj?.optInt("fromId") == userId) isTyping = true
            }
            s.on("user_stop_typing") { args ->
                val obj = args[0] as? JSONObject
                if (obj?.optInt("fromId") == userId) isTyping = false
            }
            s.on("messages_read") { scope.launch { vm.loadMessages(myId, userId) } }
            s.on("call_offer") { args ->
                try {
                    // Socket.IO args[0] JSONObject veya String olabilir
                    val obj: JSONObject? = when (val raw = args.getOrNull(0)) {
                        is JSONObject -> raw
                        is String -> JSONObject(raw)
                        is org.json.JSONObject -> raw
                        else -> {
                            // Socket.IO kütüphanesi bazen farklı bir nesne döner
                            try { JSONObject(raw.toString()) } catch (e: Exception) { null }
                        }
                    }
                    if (obj != null) {
                        val fromId = obj.optInt("fromId")
                        val callType = obj.optString("callType", "audio")
                        val callerName = obj.optString("callerName", "Kullanıcı")
                        scope.launch { incomingCall = Triple(fromId, callerName, callType) }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Socket", "call_offer parse hatası: $e, args: ${args.toList()}")
                }
            }
            s.connect()
            socket = s
        } catch(e: Exception) {}

        onDispose {
            socket?.emit("stop_typing", JSONObject().apply { put("fromId", myId); put("toId", userId) })
            socket?.disconnect()
            vm.stopPolling()
            mediaPlayers.values.forEach { it.release() }
        }
    }

    LaunchedEffect(userId) { vm.startPolling(myId, userId); vm.markAsRead(myId, userId) }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    var typingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    fun onType() {
        socket?.emit("typing", JSONObject().apply { put("fromId", myId); put("toId", userId) })
        typingJob?.cancel()
        typingJob = scope.launch { delay(2000); socket?.emit("stop_typing", JSONObject().apply { put("fromId", myId); put("toId", userId) }) }
    }

    // Ses kaydı
    fun startRecordingFn() {
        if (isRecording) return
        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.mp4")
            audioFile = file
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            Toast.makeText(context, "Kayıt başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestAndStartRecording() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            startRecordingFn()
        } else {
            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // pendingStartRecording: izin verildikten sonra kaydı başlat
    LaunchedEffect(pendingStartRecording) {
        if (pendingStartRecording) {
            pendingStartRecording = false
            startRecordingFn()
        }
    }


    fun stopRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            isRecording = false
            val file = audioFile ?: return
            scope.launch {
                val audioUrl = uploadAudioFile(file, token)
                if (audioUrl != null) {
                    vm.sendMessage(myId, userId, "", null, audioUrl)
                }
            }
        } catch(e: Exception) { isRecording = false }
    }

    // Tam ekran resim dialog
    if (fullscreenImageUrl != null) {
        Dialog(onDismissRequest = { fullscreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.95f))
                .clickable { fullscreenImageUrl = null }, contentAlignment = Alignment.Center) {
                AsyncImage(model = fullscreenImageUrl, contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(16.dp)))
            }
        }
    }

    // Gelen arama dialog
    incomingCall?.let { (callerId, callerName, callType) ->
        AlertDialog(
            onDismissRequest = {
                socket?.emit("call_reject", JSONObject().apply { put("fromId", myId); put("toId", callerId) })
                incomingCall = null
            },
            containerColor = BgCard,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(if (callType == "video") "📹 Görüntülü Arama" else "📞 Sesli Arama",
                        color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(callerName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = null,
            confirmButton = {
                Button(onClick = {
                    incomingCall = null
                    onStartCall(callerId, callType, callerName)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))) {
                    Text("📞 Kabul Et", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    socket?.emit("call_reject", JSONObject().apply { put("fromId", myId); put("toId", callerId) })
                    incomingCall = null
                }) { Text("📵 Reddet", color = Dislike) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().background(BgCard).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryGlow) }
            Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.weight(1f))
            // Sesli arama
            IconButton(onClick = { onStartCall(userId, "audio", userName) }) {
                Icon(Icons.Default.Call, contentDescription = "Sesli Arama", tint = PrimaryGlow)
            }
            // Görüntülü arama
            IconButton(onClick = { onStartCall(userId, "video", userName) }) {
                Icon(Icons.Default.Videocam, contentDescription = "Görüntülü Arama", tint = PrimaryGlow)
            }
        }

        // Mesajlar
        LazyColumn(state = listState, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                val isMe = msg.sender_id == myId
                val date = try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(msg.created_at.take(19)) } catch(e: Exception) { null }
                val timeStr = date?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it) } ?: ""

                Column(modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {

                    when {
                        // Ses mesajı
                        !msg.audio_url.isNullOrEmpty() -> {
                            val audioUrl = if (msg.audio_url.startsWith("/")) "${BASE_URL.trimEnd('/')}${msg.audio_url}" else "${BASE_URL}${msg.audio_url}"
                            val isPlaying = playingAudioId == msg.id
                            Row(modifier = Modifier.widthIn(max = 260.dp)
                                .clip(RoundedCornerShape(16.dp, 16.dp, if (isMe) 4.dp else 16.dp, if (isMe) 16.dp else 4.dp))
                                .background(if (isMe) Primary else BgCard)
                                .then(if (!isMe) Modifier.border(1.dp, Border, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)) else Modifier)
                                .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color.White.copy(0.2f)).clickable {
                                        // MediaPlayer
                                        if (isPlaying) {
                                            mediaPlayers[msg.id]?.pause()
                                            playingAudioId = null
                                        } else {
                                            mediaPlayers.values.forEach { it.pause() }
                                            playingAudioId = null
                                    val mp = MediaPlayer().apply {
                                        setDataSource(audioUrl)
                                        setOnCompletionListener { playingAudioId = null }
                                        setOnPreparedListener { start() }
                                        setOnErrorListener { _, _, _ -> playingAudioId = null; true }
                                        prepareAsync() // Main thread'i bloklamaz!
                                    }
                                    mediaPlayers[msg.id] = mp
                                    playingAudioId = msg.id
                                        }
                                    }, contentAlignment = Alignment.Center) {
                                    Text(if (isPlaying) "⏸" else "▶", fontSize = 16.sp, color = Color.White)
                                }
                                AudioWaveIcon(isPlaying = isPlaying)
                                Text("🎤", fontSize = 14.sp, color = Color.White.copy(0.7f))
                            }
                        }
                        // Resim mesajı
                        !msg.image_url.isNullOrEmpty() -> {
                            val imgUrl = if (msg.image_url.startsWith("/")) "${BASE_URL.trimEnd('/')}${msg.image_url}" else "${BASE_URL}${msg.image_url}"
                            Box(modifier = Modifier.widthIn(max = 240.dp)
                                .clip(RoundedCornerShape(16.dp, 16.dp, if (isMe) 4.dp else 16.dp, if (isMe) 16.dp else 4.dp))
                                .clickable { fullscreenImageUrl = imgUrl }) {
                                AsyncImage(model = imgUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 280.dp))
                            }
                        }
                        // Metin
                        else -> Box(modifier = Modifier.widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(16.dp, 16.dp, if (isMe) 4.dp else 16.dp, if (isMe) 16.dp else 4.dp))
                            .background(if (isMe) Primary else BgCard)
                            .then(if (!isMe) Modifier.border(1.dp, Border, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)) else Modifier)
                            .padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(msg.message, color = TextPrimary, fontSize = 15.sp)
                        }
                    }

                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.End) {
                        Text(timeStr, fontSize = 9.sp, color = TextMuted)
                        if (isMe) {
                            Spacer(Modifier.width(3.dp))
                            Text(if (msg.is_read) "✓✓" else "✓", fontSize = 9.sp,
                                color = if (msg.is_read) Color(0xFF4ADE80) else TextMuted,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (isTyping) item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { TypingBubble() }
            }
        }

        // Input
        Row(modifier = Modifier.fillMaxWidth().background(BgCard).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // Fotoğraf
            IconButton(onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(BgInput)) {
                Icon(Icons.Default.Image, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
            // Ses kaydı butonu (basılı tut)
            Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(if (isRecording) Primary else BgInput)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        requestAndStartRecording()
                        tryAwaitRelease()
                        stopRecording()
                    })
                }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, null, tint = if (isRecording) Color.White else TextMuted,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
            OutlinedTextField(value = text, onValueChange = { text = it; if (it.isNotEmpty()) onType() },
                placeholder = { Text("Mesaj yaz...", color = TextMuted) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Primary, unfocusedBorderColor = Border,
                    cursorColor = PrimaryGlow, focusedContainerColor = BgInput, unfocusedContainerColor = BgInput))
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    typingJob?.cancel()
                    socket?.emit("stop_typing", JSONObject().apply { put("fromId", myId); put("toId", userId) })
                    vm.sendMessage(myId, userId, text.trim())
                    text = ""
                }
            }, modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(if (text.isNotBlank()) Primary else BgInput)) {
                Icon(Icons.AutoMirrored.Filled.Send, null,
                    tint = if (text.isNotBlank()) Color.White else TextMuted)
            }
        }
    }
}

// ─── Ses dosyası yükleme ────────────────────────────────────────────────
suspend fun uploadAudioFile(file: File, token: String): String? {
    return try {
        val requestBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("audio", file.name, requestBody)
        val client = OkHttpClient()
        val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM).addPart(part).build()
        val request = Request.Builder()
            .url("http://10.0.2.2:5000/api/upload-audio")
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return null
            JSONObject(body).optString("url", null)
        } else null
    } catch(e: Exception) { null }
}

// ─── Fotoğraf yükleme yardımcı fonksiyonu ─────────────────────────
suspend fun uploadMsgImageFromUri(context: android.content.Context, uri: Uri, token: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("msg_img_", ".jpg", context.cacheDir)
        FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }

        val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", tempFile.name, requestBody)

        val client = OkHttpClient()
        val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addPart(part).build()
        val request = Request.Builder()
            .url("http://10.0.2.2:5000/api/upload-message-image")
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return null
            val json = org.json.JSONObject(body)
            json.optString("url", null)
        } else null
    } catch (e: Exception) { null }
}