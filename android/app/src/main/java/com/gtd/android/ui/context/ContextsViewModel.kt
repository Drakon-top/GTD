package com.gtd.android.ui.context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtd.android.data.remote.dto.ContextDto
import com.gtd.android.data.repository.ApiResult
import com.gtd.android.data.repository.GtdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContextUiItem(
    val id: String,
    val name: String,
    val theme: String,
    val icon: String,
    val inboxCount: Int,
)

data class ContextsUiState(
    val contexts: List<ContextUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val createError: String? = null,
)

@HiltViewModel
class ContextsViewModel @Inject constructor(
    private val repository: GtdRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContextsUiState())
    val uiState: StateFlow<ContextsUiState> = _uiState.asStateFlow()

    companion object {
        const val MAX_CONTEXTS = 5
    }

    init {
        loadContexts()
    }

    fun loadContexts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getContexts()) {
                is ApiResult.Success -> {
                    val contexts = result.data
                    val items = contexts.map { dto ->
                        viewModelScope.async { loadContextWithCount(dto) }
                    }.awaitAll()
                    _uiState.value = _uiState.value.copy(
                        contexts = items,
                        isLoading = false,
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    private suspend fun loadContextWithCount(dto: ContextDto): ContextUiItem {
        val inboxCount = when (val counts = repository.getTaskCounts(dto.id)) {
            is ApiResult.Success -> counts.data.byGtdList["INBOX"] ?: 0
            is ApiResult.Error -> 0
        }
        return ContextUiItem(
            id = dto.id,
            name = dto.name,
            theme = dto.theme,
            icon = dto.icon,
            inboxCount = inboxCount,
        )
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, createError = null)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, createError = null)
    }

    fun createContext(name: String, theme: String, icon: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            when (val result = repository.createContext(name, theme, icon)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        showCreateDialog = false,
                    )
                    loadContexts()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        createError = result.message,
                    )
                }
            }
        }
    }

    val canCreateMore: Boolean
        get() = _uiState.value.contexts.size < MAX_CONTEXTS
}
