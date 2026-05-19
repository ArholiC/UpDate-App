package com.update.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.update.app.data.model.UpdateProfileRequest
import com.update.app.data.model.User
import com.update.app.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileViewModel : ViewModel() {

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun loadProfile() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = RetrofitClient.api.getProfile()
                if (response.isSuccessful) {
                    _profile.value = response.body()
                }
            } catch (e: Exception) { } finally {
                _loading.value = false
            }
        }
    }

    fun updateProfile(userId: Int, bio: String, interests: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.updateProfile(userId, UpdateProfileRequest(bio = bio, interests = interests))
                _saveSuccess.value = true
                loadProfile()
            } catch (e: Exception) { }
        }
    }

    fun uploadPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = stream.readBytes()
                stream.close()
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("profil_resmi", "profile.jpg", requestBody)
                RetrofitClient.api.uploadProfilePic(part)
                loadProfile()
            } catch (e: Exception) { }
        }
    }

    fun clearSaveSuccess() { _saveSuccess.value = false }
}