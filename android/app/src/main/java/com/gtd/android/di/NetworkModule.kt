package com.gtd.android.di

import android.content.SharedPreferences
import com.gtd.android.BuildConfig
import com.gtd.android.data.remote.api.AuthApi
import com.gtd.android.data.remote.api.GtdApi
import com.gtd.android.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private const val COOKIE_PREFS_PREFIX = "cookie_"

    @Provides
    @Singleton
    fun provideCookieJar(prefs: SharedPreferences): CookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

        init {
            loadFromPrefs()
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val key = url.host
            store.getOrPut(key) { mutableListOf() }.let { existing ->
                for (cookie in cookies) {
                    existing.removeAll { it.name == cookie.name }
                    if (cookie.expiresAt > System.currentTimeMillis()) {
                        existing.add(cookie)
                    }
                }
            }
            persistForHost(key)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            return store[url.host]?.filter { it.expiresAt > now } ?: emptyList()
        }

        private fun persistForHost(host: String) {
            val cookies = store[host] ?: return
            val now = System.currentTimeMillis()
            val serialized = cookies
                .filter { it.expiresAt > now }
                .joinToString("|") { it.toString() }
            prefs.edit().putString("$COOKIE_PREFS_PREFIX$host", serialized).apply()
        }

        private fun loadFromPrefs() {
            val now = System.currentTimeMillis()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(COOKIE_PREFS_PREFIX) && value is String && value.isNotEmpty()) {
                    val host = key.removePrefix(COOKIE_PREFS_PREFIX)
                    val dummyUrl = HttpUrl.Builder()
                        .scheme("https")
                        .host(host)
                        .build()
                    val cookies = value.split("|").mapNotNull { raw ->
                        Cookie.parse(dummyUrl, raw)
                    }.filter { it.expiresAt > now }
                    if (cookies.isNotEmpty()) {
                        store[host] = cookies.toMutableList()
                    }
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        cookieJar: CookieJar,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideGtdApi(retrofit: Retrofit): GtdApi = retrofit.create(GtdApi::class.java)
}
