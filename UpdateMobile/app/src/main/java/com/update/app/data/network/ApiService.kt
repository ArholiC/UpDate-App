package com.update.app.data.network

import com.update.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ──────────────────────────────────────────────
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    // ─── Discover ──────────────────────────────────────────
    @GET("api/discover/{userId}")
    suspend fun getDiscover(
        @Path("userId") userId: Int,
        @Query("gender") gender: String? = null,
        @Query("minAge") minAge: Int? = null,
        @Query("maxAge") maxAge: Int? = null,
        @Query("zodiac") zodiac: String? = null
    ): Response<List<User>>

    // ─── Swipe ─────────────────────────────────────────────
    @POST("api/like")
    suspend fun swipe(@Body body: SwipeRequest): Response<SwipeResponse>

    // ─── Matches ───────────────────────────────────────────
    @GET("api/matches/{userId}")
    suspend fun getMatches(@Path("userId") userId: Int): Response<List<User>>

    // ─── Messages ──────────────────────────────────────────
    @GET("api/matches/{userId}")
    suspend fun getConversations(@Path("userId") userId: Int): Response<List<User>>

    @GET("api/messages/{myId}/{otherId}")
    suspend fun getMessages(
        @Path("myId") myId: Int,
        @Path("otherId") otherId: Int
    ): Response<List<Message>>

    @POST("api/messages")
    suspend fun sendMessage(@Body body: SendMessageRequest): Response<Any>

    @PUT("api/messages/read/{myId}/{otherId}")
    suspend fun markAsRead(
        @Path("myId") myId: Int,
        @Path("otherId") otherId: Int
    ): Response<Any>

    // ─── Profile ───────────────────────────────────────────
    @GET("api/me")
    suspend fun getProfile(): Response<User>

    @PUT("api/users/{id}")
    suspend fun updateProfile(
        @Path("id") id: Int,
        @Body body: UpdateProfileRequest
    ): Response<Any>

    @Multipart
    @POST("api/upload")
    suspend fun uploadProfilePic(
        @Part image: MultipartBody.Part
    ): Response<Any>

    @Multipart
    @POST("api/upload-message-image")
    suspend fun uploadMessageImage(
        @Part image: MultipartBody.Part
    ): Response<Any>

    // ─── Compatibility ─────────────────────────────────────
    @GET("api/compatibility/{myId}/{otherId}")
    suspend fun getCompatibility(
        @Path("myId") myId: Int,
        @Path("otherId") otherId: Int
    ): Response<CompatibilityResponse>

    // ─── Users ─────────────────────────────────────────────
    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>
}