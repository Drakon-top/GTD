package com.gtd.android.data.remote.api

import com.gtd.android.data.remote.dto.CategoryDto
import com.gtd.android.data.remote.dto.ContextDto
import com.gtd.android.data.remote.dto.CreateCategoryRequest
import com.gtd.android.data.remote.dto.CreateContextRequest
import com.gtd.android.data.remote.dto.CreateReminderRequest
import com.gtd.android.data.remote.dto.CreateTaskRequest
import com.gtd.android.data.remote.dto.MoveTaskRequest
import com.gtd.android.data.remote.dto.ReminderDto
import com.gtd.android.data.remote.dto.SyncPullResponse
import com.gtd.android.data.remote.dto.SyncPushRequest
import com.gtd.android.data.remote.dto.SyncPushResponse
import com.gtd.android.data.remote.dto.TaskCountsDto
import com.gtd.android.data.remote.dto.TaskDto
import com.gtd.android.data.remote.dto.UpdateTaskRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GtdApi {

    // Contexts
    @GET("contexts")
    suspend fun getContexts(): Response<List<ContextDto>>

    @POST("contexts")
    suspend fun createContext(@Body request: CreateContextRequest): Response<ContextDto>

    @GET("contexts/{id}")
    suspend fun getContext(@Path("id") id: String): Response<ContextDto>

    @PUT("contexts/{id}")
    suspend fun updateContext(@Path("id") id: String, @Body request: CreateContextRequest): Response<ContextDto>

    @DELETE("contexts/{id}")
    suspend fun deleteContext(@Path("id") id: String): Response<Unit>

    // Task Counts
    @GET("contexts/{contextId}/tasks/counts")
    suspend fun getTaskCounts(@Path("contextId") contextId: String): Response<TaskCountsDto>

    // Tasks
    @GET("contexts/{contextId}/tasks")
    suspend fun getTasks(
        @Path("contextId") contextId: String,
        @Query("gtd_list") gtdList: String? = null,
    ): Response<List<TaskDto>>

    @POST("contexts/{contextId}/tasks")
    suspend fun createTask(
        @Path("contextId") contextId: String,
        @Body request: CreateTaskRequest,
    ): Response<TaskDto>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: String): Response<TaskDto>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body request: UpdateTaskRequest): Response<TaskDto>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>

    @PATCH("tasks/{id}/move")
    suspend fun moveTask(@Path("id") id: String, @Body request: MoveTaskRequest): Response<TaskDto>

    @PATCH("tasks/{id}/complete")
    suspend fun completeTask(@Path("id") id: String): Response<TaskDto>

    // Subtasks
    @POST("tasks/{taskId}/subtasks")
    suspend fun createSubtask(
        @Path("taskId") taskId: String,
        @Body request: CreateTaskRequest,
    ): Response<TaskDto>

    @GET("tasks/{taskId}/subtasks")
    suspend fun getSubtasks(@Path("taskId") taskId: String): Response<List<TaskDto>>

    // Categories
    @GET("contexts/{contextId}/categories")
    suspend fun getCategories(@Path("contextId") contextId: String): Response<List<CategoryDto>>

    @POST("contexts/{contextId}/categories")
    suspend fun createCategory(
        @Path("contextId") contextId: String,
        @Body request: CreateCategoryRequest,
    ): Response<CategoryDto>

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body request: CreateCategoryRequest): Response<CategoryDto>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    // Reminders
    @GET("tasks/{taskId}/reminders")
    suspend fun getReminders(@Path("taskId") taskId: String): Response<List<ReminderDto>>

    @POST("tasks/{taskId}/reminders")
    suspend fun createReminder(
        @Path("taskId") taskId: String,
        @Body request: CreateReminderRequest,
    ): Response<ReminderDto>

    @PUT("reminders/{id}")
    suspend fun updateReminder(@Path("id") id: String, @Body request: CreateReminderRequest): Response<ReminderDto>

    @DELETE("reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<Unit>

    // Sync
    @POST("sync/push")
    suspend fun syncPush(@Body request: SyncPushRequest): Response<SyncPushResponse>

    @GET("sync/pull")
    suspend fun syncPull(@Query("since") since: String): Response<SyncPullResponse>
}
