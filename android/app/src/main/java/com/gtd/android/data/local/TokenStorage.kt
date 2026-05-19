package com.gtd.android.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorage @Inject constructor(
    private val prefs: SharedPreferences,
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_TOKEN_REGISTERED = "fcm_token_registered"
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveUser(userId: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun saveLastSyncTimestamp(timestamp: String) {
        prefs.edit().putString(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    fun getLastSyncTimestamp(): String? = prefs.getString(KEY_LAST_SYNC_TIMESTAMP, null)

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)

    fun setFcmTokenRegistered(registered: Boolean) {
        prefs.edit().putBoolean(KEY_FCM_TOKEN_REGISTERED, registered).apply()
    }

    fun isFcmTokenRegistered(): Boolean = prefs.getBoolean(KEY_FCM_TOKEN_REGISTERED, false)

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null
}
