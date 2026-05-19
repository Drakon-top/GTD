package com.gtd.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtd.android.data.repository.AuthRepository
import com.gtd.android.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val registrationSuccess: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword, error = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetRegistrationSuccess() {
        _uiState.value = _uiState.value.copy(registrationSuccess = false)
    }

    fun resetForm() {
        _uiState.value = AuthUiState()
    }

    fun login() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.value = state.copy(error = emailError)
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(error = "Password is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                }
                is AuthResult.Error -> {
                    val msg = when (result.code) {
                        401 -> "Invalid email or password"
                        else -> result.message
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.value = state.copy(error = emailError)
            return
        }
        if (state.password.length < 8) {
            _uiState.value = state.copy(error = "Password must be at least 8 characters")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.register(state.email.trim(), state.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registrationSuccess = true,
                    )
                }
                is AuthResult.Error -> {
                    val msg = when (result.code) {
                        409 -> "Account with this email already exists"
                        else -> result.message
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
                }
            }
        }
    }

    suspend fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    suspend fun tryRefreshSession(): Boolean {
        if (!authRepository.isLoggedIn()) return false
        return when (authRepository.refreshToken()) {
            is AuthResult.Success -> true
            is AuthResult.Error -> false
        }
    }

    private fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return "Email is required"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return "Invalid email format"
        }
        return null
    }
}
