package com.gtd.android.notification

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.gtd.android.data.local.TokenStorage
import com.gtd.android.data.remote.api.GtdApi
import com.gtd.android.data.remote.dto.RegisterDeviceTokenRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTokenManager @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val api: GtdApi,
    private val tokenStorage: TokenStorage,
) {
    companion object {
        private const val TAG = "DeviceTokenManager"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_TOKEN_REGISTERED = "fcm_token_registered"
    }

    suspend fun registerTokenIfNeeded() {
        if (!tokenStorage.isLoggedIn()) return

        try {
            val token = firebaseMessaging.token.await()
            val savedToken = tokenStorage.getFcmToken()

            if (token != savedToken || !tokenStorage.isFcmTokenRegistered()) {
                registerTokenOnServer(token)
                tokenStorage.saveFcmToken(token)
                tokenStorage.setFcmTokenRegistered(true)
                Log.d(TAG, "FCM token registered: ${token.take(10)}...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token", e)
        }
    }

    suspend fun onNewToken(token: String) {
        if (!tokenStorage.isLoggedIn()) {
            tokenStorage.saveFcmToken(token)
            tokenStorage.setFcmTokenRegistered(false)
            return
        }

        try {
            registerTokenOnServer(token)
            tokenStorage.saveFcmToken(token)
            tokenStorage.setFcmTokenRegistered(true)
            Log.d(TAG, "New FCM token registered: ${token.take(10)}...")
        } catch (e: Exception) {
            tokenStorage.saveFcmToken(token)
            tokenStorage.setFcmTokenRegistered(false)
            Log.e(TAG, "Failed to register new FCM token", e)
        }
    }

    suspend fun unregisterToken() {
        val token = tokenStorage.getFcmToken() ?: return
        try {
            api.unregisterDeviceToken(token)
            tokenStorage.setFcmTokenRegistered(false)
            Log.d(TAG, "FCM token unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister FCM token", e)
        }
    }

    private suspend fun registerTokenOnServer(token: String) {
        val request = RegisterDeviceTokenRequest(
            token = token,
            deviceType = "ANDROID",
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
        val response = api.registerDeviceToken(request)
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to register token: ${response.code()}")
        }
    }
}
