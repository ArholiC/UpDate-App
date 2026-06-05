package com.update.app.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.update.app.data.model.LoginRequest
import com.update.app.data.model.RegisterRequest
import com.update.app.data.model.User
import com.update.app.data.network.RetrofitClient
import com.update.app.data.network.TOKEN_KEY
import com.update.app.data.network.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUserId = MutableStateFlow(0)
    val currentUserId: StateFlow<Int> = _currentUserId

    fun checkLogin(context: Context) {
        viewModelScope.launch {
            val token = context.dataStore.data.first()[TOKEN_KEY]
            _isLoggedIn.value = !token.isNullOrEmpty()
        }
    }

    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    saveToken(context, body.token)
                    _currentUserId.value = body.user.id
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(body.user)
                } else {
                    _authState.value = AuthState.Error("Email veya şifre hatalı")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sunucuya bağlanılamadı: ${e.message}")
            }
        }
    }

    fun register(context: Context, request: RegisterRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.api.register(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.token != null) {
                        saveToken(context, body.token)
                        _currentUserId.value = body.user?.id ?: 0
                        _isLoggedIn.value = true
                        _authState.value = AuthState.Success(body.user ?: com.update.app.data.model.User())
                    } else {
                        _authState.value = AuthState.Error("Sunucudan geçersiz yanıt")
                    }
                } else {
                    // Gerçek hata mesajını parse et
                    val errorBody = response.errorBody()?.string() ?: ""
                    val msg = try {
                        org.json.JSONObject(errorBody).optString("message",
                            org.json.JSONObject(errorBody).optString("error", "Kayıt başarısız (${response.code()})"))
                    } catch (e: Exception) {
                        "Kayıt başarısız (${response.code()})"
                    }
                    _authState.value = AuthState.Error(msg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Bağlantı hatası: ${e.message}")
            }
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            context.dataStore.edit { it.remove(TOKEN_KEY) }
            _currentUserId.value = 0
            _isLoggedIn.value = false
            _authState.value = AuthState.Idle
        }
    }

    private suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }
}