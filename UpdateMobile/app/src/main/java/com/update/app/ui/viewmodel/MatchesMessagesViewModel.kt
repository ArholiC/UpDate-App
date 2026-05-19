package com.update.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.update.app.data.model.Message
import com.update.app.data.model.SendMessageRequest
import com.update.app.data.model.User
import com.update.app.data.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MatchesViewModel : ViewModel() {
    private val _matches = MutableStateFlow<List<User>>(emptyList())
    val matches: StateFlow<List<User>> = _matches

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadMatches(userId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = RetrofitClient.api.getMatches(userId)
                if (response.isSuccessful) {
                    _matches.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) { } finally {
                _loading.value = false
            }
        }
    }
}

class MessagesViewModel : ViewModel() {

    private val _conversations = MutableStateFlow<List<User>>(emptyList())
    val conversations: StateFlow<List<User>> = _conversations

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var pollingJob: Job? = null

    fun loadConversations(userId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = RetrofitClient.api.getConversations(userId)
                if (response.isSuccessful) {
                    _conversations.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) { } finally {
                _loading.value = false
            }
        }
    }

    fun loadMessages(myId: Int, otherId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getMessages(myId, otherId)
                if (response.isSuccessful) {
                    _messages.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) { }
        }
    }

    fun startPolling(myId: Int, otherId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                loadMessages(myId, otherId)
                delay(5000)
            }
        }
    }

    fun stopPolling() { pollingJob?.cancel() }

    fun sendMessage(myId: Int, receiverId: Int, message: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.sendMessage(
                    SendMessageRequest(sender_id = myId, receiver_id = receiverId, message = message)
                )
                loadMessages(myId, receiverId)
            } catch (e: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}