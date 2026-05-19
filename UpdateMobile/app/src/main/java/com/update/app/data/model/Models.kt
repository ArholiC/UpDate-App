package com.update.app.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────────
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class RegisterRequest(
    val full_name: String,
    val email: String,
    val password: String,
    val gender: String,
    val birth_date: String,
    val zodiac: String,
    val interests: String,
    val bio: String
)

data class RegisterResponse(
    val token: String,
    val user: User
)

// ─── User ─────────────────────────────────────────────────
data class User(
    val id: Int = 0,
    val full_name: String = "",
    val email: String = "",
    val bio: String? = null,
    val zodiac: String? = null,
    val gender: String? = null,
    val interests: String? = null,
    val profile_pic: String? = null,
    val birth_date: String? = null,
    val compatibility: Double? = null
)

// ─── Discover ─────────────────────────────────────────────
data class DiscoverResponse(
    val users: List<User>? = null
)

// ─── Swipe ────────────────────────────────────────────────
data class SwipeRequest(
    val liked_id: Int,
    val is_like: Int , // 1 = like, 0 = dislike
    val liker_id: Int,
)

data class SwipeResponse(
    val isMatch: Boolean = false,
    val message: String? = null
)

// ─── Matches ──────────────────────────────────────────────
data class MatchesResponse(
    val matches: List<User>? = null
)

// ─── Messages ─────────────────────────────────────────────
data class Message(
    val id: Int = 0,
    val sender_id: Int = 0,
    val receiver_id: Int = 0,
    val message: String = "",
    val created_at: String = ""

)

data class MessagesResponse(
    val messages: List<Message>? = null
)

data class ConversationsResponse(
    val conversations: List<User>? = null
)

data class SendMessageRequest(
    val receiver_id: Int,
    val message: String,
    val sender_id: Int,
)

// ─── Profile ──────────────────────────────────────────────
data class ProfileResponse(
    val user: User? = null
)

data class UpdateProfileRequest(
    val bio: String? = null,
    val interests: String? = null
)
