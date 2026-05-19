package com.gtd.android.data.remote.api

import com.gtd.android.data.remote.dto.AuthResponse
import com.gtd.android.data.remote.dto.LoginRequest
import com.gtd.android.data.remote.dto.RegisterRequest
import com.gtd.android.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
