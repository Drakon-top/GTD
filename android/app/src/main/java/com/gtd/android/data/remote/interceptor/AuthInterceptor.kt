package com.gtd.android.data.remote.interceptor

import com.gtd.android.data.local.TokenStorage
import com.gtd.android.data.remote.dto.AuthResponse
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val json: Json,
) : Interceptor {

    @Volatile
    private var isRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (isAuthEndpoint(request)) {
            return chain.proceed(addAuthHeader(request))
        }

        val response = chain.proceed(addAuthHeader(request))

        if (response.code == 401 && !isRefreshing) {
            synchronized(this) {
                if (isRefreshing) {
                    return chain.proceed(addAuthHeader(request))
                }
                isRefreshing = true
            }

            try {
                val refreshed = tryRefreshToken(chain)
                if (refreshed) {
                    response.close()
                    return chain.proceed(addAuthHeader(request))
                }
            } finally {
                isRefreshing = false
            }
        }

        return response
    }

    private fun addAuthHeader(request: Request): Request {
        val token = tokenStorage.getAccessToken() ?: return request
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun isAuthEndpoint(request: Request): Boolean {
        val path = request.url.encodedPath
        return path.contains("/auth/login") ||
                path.contains("/auth/register") ||
                path.contains("/auth/refresh")
    }

    private fun tryRefreshToken(chain: Interceptor.Chain): Boolean {
        val baseUrl = chain.request().url.toString().substringBefore("/api/v1/") + "/api/v1/"
        val refreshRequest = Request.Builder()
            .url("${baseUrl}auth/refresh")
            .post("".toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val refreshResponse = chain.proceed(refreshRequest)
            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body?.string()
                refreshResponse.close()
                if (body != null) {
                    val authResponse = json.decodeFromString<AuthResponse>(body)
                    tokenStorage.saveAccessToken(authResponse.accessToken)
                    true
                } else {
                    false
                }
            } else {
                refreshResponse.close()
                tokenStorage.clear()
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
