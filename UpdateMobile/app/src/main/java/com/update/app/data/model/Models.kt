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
    val bio: String,
    val answers: String? = null  // JSON string: {"q1":"opt1","q2":"opt2",...}
)

data class RegisterResponse(
    val token: String? = null,
    val user: User? = null
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
    val compatibility: Double? = null,
    val match_rate: Int? = null
)

// ─── Discover ─────────────────────────────────────────────
data class DiscoverResponse(
    val users: List<User>? = null
)

// ─── Swipe ────────────────────────────────────────────────
data class SwipeRequest(
    val liked_id: Int,
    val is_like: Int, // 0=dislike, 1=like, 2=superlike
    val liker_id: Int,
)

data class SwipeResponse(
    val isMatch: Boolean = false,
    val isSuperLike: Boolean = false,
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
    val image_url: String? = null,
    val audio_url: String? = null,
    val is_read: Boolean = false,
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
    val image_url: String? = null,
    val audio_url: String? = null
)

// ─── Profile ──────────────────────────────────────────────
data class ProfileResponse(
    val user: User? = null
)

data class UpdateProfileRequest(
    val bio: String? = null,
    val interests: String? = null
)

// ─── Compatibility ────────────────────────────────────────
data class CompatibilityAnswer(
    val question: String = "",
    val answer: String = ""
)

data class CompatibilityResponse(
    val matchRate: Int = 0,
    val commonInterests: List<String> = emptyList(),
    val commonAnswers: List<CompatibilityAnswer> = emptyList(),
    val totalAnswers: Int = 0
)

// ─── Filter ───────────────────────────────────────────────
data class FilterState(
    val gender: String? = null,       // null = hepsi
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val zodiac: String? = null        // null = hepsi
)
