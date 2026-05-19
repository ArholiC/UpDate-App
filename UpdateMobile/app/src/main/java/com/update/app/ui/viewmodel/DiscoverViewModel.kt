package com.update.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.update.app.data.model.SwipeRequest
import com.update.app.data.model.User
import com.update.app.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _matchedName = MutableStateFlow<String?>(null)
    val matchedName: StateFlow<String?> = _matchedName

    fun loadUsers(userId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _currentIndex.value = 0
            try {
                val response = RetrofitClient.api.getDiscover(userId)
                if (response.isSuccessful) {
                    _users.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }

    fun swipe(likerId: Int, likedId: Int, isLike: Boolean) {
        viewModelScope.launch {
            val userName = _users.value.getOrNull(_currentIndex.value)?.full_name
            _currentIndex.value++
            try {
                val response = RetrofitClient.api.swipe(
                    SwipeRequest(liker_id = likerId, liked_id = likedId, is_like = if (isLike) 1 else 0)
                )
                if (response.isSuccessful && response.body()?.isMatch == true) {
                    _matchedName.value = userName
                }
            } catch (e: Exception) { }
        }
    }

    fun clearMatch() { _matchedName.value = null }
}