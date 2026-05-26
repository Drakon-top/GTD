package com.gtd.android.data.repository

import com.gtd.android.data.local.GtdDatabase
import com.gtd.android.data.local.TokenStorage
import com.gtd.android.data.local.dao.UserDao
import com.gtd.android.data.local.entity.UserEntity
import com.gtd.android.data.remote.api.AuthApi
import com.gtd.android.data.remote.dto.LoginRequest
import com.gtd.android.data.remote.dto.RegisterRequest
import com.gtd.android.notification.DeviceTokenManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int = 0) : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val userDao: UserDao,
    private val database: GtdDatabase,
    private val json: Json,
    private val deviceTokenManager: DeviceTokenManager,
) {
    suspend fun login(email: String, password: String): AuthResult<Unit> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body() ?: return AuthResult.Error("Empty response")
                tokenStorage.saveAccessToken(body.accessToken)
                val userId = extractUserIdFromJwt(body.accessToken)
                tokenStorage.saveUser(userId ?: "", email)
                userDao.insert(
                    UserEntity(
                        id = userId ?: "",
                        email = email,
                        passwordHash = "",
                    )
                )
                try { deviceTokenManager.registerTokenIfNeeded() } catch (_: Exception) {}
                AuthResult.Success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                AuthResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(email: String, password: String): AuthResult<Unit> {
        return try {
            val response = authApi.register(RegisterRequest(email, password))
            if (response.isSuccessful) {
                AuthResult.Success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                AuthResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun refreshToken(): AuthResult<Unit> {
        return try {
            val response = authApi.refresh()
            if (response.isSuccessful) {
                val body = response.body() ?: return AuthResult.Error("Empty response")
                tokenStorage.saveAccessToken(body.accessToken)
                val userId = tokenStorage.getUserId()
                val email = tokenStorage.getUserEmail()
                if (!userId.isNullOrBlank()) {
                    userDao.insert(
                        UserEntity(
                            id = userId,
                            email = email ?: "",
                            passwordHash = "",
                        )
                    )
                }
                try { deviceTokenManager.registerTokenIfNeeded() } catch (_: Exception) {}
                AuthResult.Success(Unit)
            } else {
                AuthResult.Error("Token refresh failed", response.code())
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout(): AuthResult<Unit> {
        return try {
            try { deviceTokenManager.unregisterToken() } catch (_: Exception) {}
            authApi.logout()
            tokenStorage.clear()
            clearLocalDatabase()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            tokenStorage.clear()
            clearLocalDatabase()
            AuthResult.Success(Unit)
        }
    }

    private suspend fun clearLocalDatabase() {
        database.reminderDao().deleteAll()
        database.pendingChangeDao().deleteAll()
        database.taskDao().deleteAll()
        database.categoryDao().deleteAll()
        database.contextDao().deleteAll()
        database.userDao().deleteAll()
    }

    fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    private fun extractUserIdFromJwt(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val padded = when (payload.length % 4) {
                2 -> "$payload=="
                3 -> "${payload}="
                else -> payload
            }
            val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val jsonStr = String(decoded, Charsets.UTF_8)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            obj["sub"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Unknown error"
        return try {
            val obj = json.parseToJsonElement(errorBody).jsonObject
            obj["message"]?.jsonPrimitive?.content ?: "Unknown error"
        } catch (_: Exception) {
            "Unknown error"
        }
    }
}
