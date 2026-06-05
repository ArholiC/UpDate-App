package com.update.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.update.app.data.network.BASE_URL
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CallScreen(
    myId: Int,
    calleeId: Int,
    calleeName: String,
    callType: String,
    isCallee: Boolean = false,
    onEnd: () -> Unit
) {
    val context = LocalContext.current
    
    // WebRTC için gereken izinler
    val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )
    if (callType == "video") {
        permissions.add(Manifest.permission.CAMERA)
    }

    var permissionsGranted by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        val missingPerms = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPerms.isNotEmpty()) {
            permLauncher.launch(missingPerms.toTypedArray())
        } else {
            permissionsGranted = true
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .statusBarsPadding()
        .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (!permissionsGranted) {
            Text("Kamera ve Mikrofon izni bekleniyor...", color = Color.White, fontSize = 16.sp)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                        }

                        // Sayfa içinde gezinmeyi (ve kapanmayı) yakala
                        webViewClient = object : WebViewClient() {
                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                // Eğer url chat.html'e dönerse (arama bitmiş demektir)
                                if (url != null && url.contains("chat.html")) {
                                    onEnd()
                                }
                            }
                        }

                        // WebRTC Kamera ve Mikrofon izinlerini otomatik ver
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources) // Tüm istekleri onayla
                            }
                        }
                    }
                },
                update = { webView ->
                    val encodedName = URLEncoder.encode(calleeName, "UTF-8")
                    val mode = if (isCallee) "callee" else "caller"
                    val auto = if (isCallee) "1" else "0" // Mobilde dialog çıkıyor, o yüzden auto-accept = 1
                    
                    val url = "${BASE_URL}call.html?id=$calleeId&type=$callType&mode=$mode&name=$encodedName&auto=$auto"
                    
                    // Sadece ilk açılışta yükle
                    if (webView.url != url) {
                        webView.loadUrl(url)
                    }
                }
            )
        }
    }
}
