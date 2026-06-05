package com.update.app.data.network

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Emülatörde 10.0.2.2 → bilgisayarının localhost'u (Docker dışarıya 5005 portundan yayın yapıyor)
// Gerçek telefonda: aynı WiFi'de PC'nin IP'si (ipconfig'den bak) + port 5005
const val BASE_URL = "http://10.0.2.2:5005/"
// ⬆ Gerçek telefon kullanıyorsan bunu yorum yap, altını aç:
// const val BASE_URL = "http://10.31.161.24:5005/"

val Context.dataStore by preferencesDataStore(name = "update_prefs")
val TOKEN_KEY = stringPreferencesKey("jwt_token")

object RetrofitClient {

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val authInterceptor = Interceptor { chain ->
        val token = appContext?.let { ctx ->
            runBlocking {
                ctx.dataStore.data.first()[TOKEN_KEY]
            }
        }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrEmpty()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
