package com.update.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.update.app.data.model.FilterState
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

    private val _superLikeMessage = MutableStateFlow<String?>(null)
    val superLikeMessage: StateFlow<String?> = _superLikeMessage

    // Aktif filtreler
    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter

    fun setFilter(newFilter: FilterState) {
        _filter.value = newFilter
    }

    fun loadUsers(userId: Int, genderFilter: String? = null) {
        val f = _filter.value
        val gender = genderFilter ?: f.gender
        viewModelScope.launch {
            _loading.value = true
            _currentIndex.value = 0
            try {
                val response = RetrofitClient.api.getDiscover(
                    userId = userId,
                    gender = if (gender.isNullOrEmpty() || gender == "all") null else gender,
                    minAge = f.minAge,
                    maxAge = f.maxAge,
                    zodiac = if (f.zodiac.isNullOrEmpty() || f.zodiac == "all") null else f.zodiac
                )
                if (response.isSuccessful) {
                    _users.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }

    // is_like: 0=dislike, 1=like, 2=superlike
    fun swipe(likerId: Int, likedId: Int, isLike: Int) {
        viewModelScope.launch {
            val userName = _users.value.getOrNull(_currentIndex.value)?.full_name
            _currentIndex.value++
            try {
                val response = RetrofitClient.api.swipe(
                    SwipeRequest(liker_id = likerId, liked_id = likedId, is_like = isLike)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.isMatch == true) {
                        _matchedName.value = userName
                    } else if (isLike == 2) {
                        _superLikeMessage.value = "${userName ?: "Kullanıcı"} ⭐ Süper Beğeni Gönderildi!"
                    }
                }
            } catch (e: Exception) { }
        }
    }

    fun clearMatch() { _matchedName.value = null }
    fun clearSuperLike() { _superLikeMessage.value = null }
}