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

    // Okunmamış mesaj sayısı
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private var pollingJob: Job? = null
    private var bgPollingJob: Job? = null
    private var lastSeenMessageId = 0

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
                delay(2000)
            }
        }
    }

    fun stopPolling() { pollingJob?.cancel() }

    // Arka planda unread kontrol — _messages'a DOKUNMAZ
    fun startBackgroundCheck(userId: Int) {
        bgPollingJob?.cancel()
        bgPollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val res = RetrofitClient.api.getConversations(userId)
                    if (res.isSuccessful) {
                        val convs = res.body() ?: emptyList()
                        _conversations.value = convs
                        var newCount = 0
                        for (conv in convs) {
                            val msgRes = RetrofitClient.api.getMessages(userId, conv.id)
                            if (msgRes.isSuccessful) {
                                val msgs = msgRes.body() ?: emptyList()
                                val lastMsg = msgs.lastOrNull()
                                if (lastMsg != null
                                    && lastMsg.id > lastSeenMessageId
                                    && lastMsg.sender_id != userId) {
                                    newCount++
                                }
                            }
                        }
                        _unreadCount.value = newCount
                    }
                } catch (e: Exception) { }
                delay(10000)
            }
        }
    }

    fun stopBackgroundCheck() { bgPollingJob?.cancel() }

    // Mesajlar tab'ına geçince sıfırla
    fun clearUnread(userId: Int) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.api.getConversations(userId)
                if (res.isSuccessful) {
                    val convs = res.body() ?: emptyList()
                    for (conv in convs) {
                        val msgRes = RetrofitClient.api.getMessages(userId, conv.id)
                        if (msgRes.isSuccessful) {
                            val msgs = msgRes.body() ?: emptyList()
                            val lastId = msgs.lastOrNull()?.id ?: 0
                            if (lastId > lastSeenMessageId) lastSeenMessageId = lastId
                        }
                    }
                }
            } catch (e: Exception) { }
            _unreadCount.value = 0
        }
    }

    // Mesajları okundu olarak işaretle
    fun markAsRead(myId: Int, otherId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.markAsRead(myId, otherId)
                // Lokal state'i de güncelle
                val updated = _messages.value.map { msg ->
                    if (msg.receiver_id == myId && !msg.is_read) msg.copy(is_read = true) else msg
                }
                _messages.value = updated
            } catch (e: Exception) { }
        }
    }

    fun sendMessage(myId: Int, receiverId: Int, message: String, imageUrl: String? = null, audioUrl: String? = null) {
        viewModelScope.launch {
            try {
                RetrofitClient.api.sendMessage(
                    SendMessageRequest(
                        sender_id = myId,
                        receiver_id = receiverId,
                        message = message,
                        image_url = imageUrl,
                        audio_url = audioUrl
                    )
                )
                loadMessages(myId, receiverId)
            } catch (e: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        bgPollingJob?.cancel()
    }
}