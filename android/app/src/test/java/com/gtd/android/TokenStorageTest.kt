package com.gtd.android

import android.content.SharedPreferences
import com.gtd.android.data.local.TokenStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenStorageTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tokenStorage: TokenStorage

    @Before
    fun setup() {
        editor = mockk(relaxed = true)
        prefs = mockk {
            every { edit() } returns editor
            every { getString(any(), any()) } returns null
        }
        every { editor.putString(any(), any()) } returns editor
        every { editor.clear() } returns editor
        tokenStorage = TokenStorage(prefs)
    }

    @Test
    fun `should save and retrieve access token`() {
        every { prefs.getString("access_token", null) } returns "test-token"

        tokenStorage.saveAccessToken("test-token")
        val token = tokenStorage.getAccessToken()

        assertEquals("test-token", token)
        verify { editor.putString("access_token", "test-token") }
    }

    @Test
    fun `should return null when no token stored`() {
        assertNull(tokenStorage.getAccessToken())
    }

    @Test
    fun `should return isLoggedIn true when token exists`() {
        every { prefs.getString("access_token", null) } returns "test-token"
        assertTrue(tokenStorage.isLoggedIn())
    }

    @Test
    fun `should return isLoggedIn false when no token`() {
        assertFalse(tokenStorage.isLoggedIn())
    }

    @Test
    fun `should clear all data on clear`() {
        tokenStorage.clear()
        verify { editor.clear() }
        verify { editor.apply() }
    }
}
