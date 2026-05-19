package com.gtd.android.data.repository

import com.gtd.android.data.remote.api.GtdApi
import com.gtd.android.data.remote.dto.CategoryDto
import com.gtd.android.data.remote.dto.ContextDto
import com.gtd.android.data.remote.dto.CreateContextRequest
import com.gtd.android.data.remote.dto.CreateTaskRequest
import com.gtd.android.data.remote.dto.TaskCountsDto
import com.gtd.android.data.remote.dto.TaskDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

@Singleton
class GtdRepository @Inject constructor(
    private val api: GtdApi,
    private val json: Json,
) {
    suspend fun getContexts(): ApiResult<List<ContextDto>> = safeCall { api.getContexts() }

    suspend fun createContext(name: String, theme: String, icon: String): ApiResult<ContextDto> =
        safeCall { api.createContext(CreateContextRequest(name, theme, icon)) }

    suspend fun getTaskCounts(contextId: String): ApiResult<TaskCountsDto> =
        safeCall { api.getTaskCounts(contextId) }

    suspend fun getTasks(contextId: String, gtdList: String? = null): ApiResult<List<TaskDto>> =
        safeCall { api.getTasks(contextId, gtdList) }

    suspend fun createTask(contextId: String, title: String): ApiResult<TaskDto> =
        safeCall { api.createTask(contextId, CreateTaskRequest(title = title)) }

    suspend fun completeTask(taskId: String): ApiResult<TaskDto> =
        safeCall { api.completeTask(taskId) }

    suspend fun getCategories(contextId: String): ApiResult<List<CategoryDto>> =
        safeCall { api.getCategories(contextId) }

    private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<T>): ApiResult<T> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(body)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                ApiResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
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
