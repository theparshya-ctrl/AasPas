package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.MarkAllReadDto
import com.aaspas.customer.data.remote.dto.NotificationItemDto
import com.aaspas.customer.data.remote.dto.NotificationListDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET("api/v1/notifications")
    suspend fun listNotifications(
        @Query("unread_only") unreadOnly: Boolean = false,
    ): ApiResponseDto<NotificationListDto>

    @POST("api/v1/notifications/{notificationId}/read")
    suspend fun markRead(
        @Path("notificationId") notificationId: String,
    ): ApiResponseDto<NotificationItemDto>

    @POST("api/v1/notifications/read-all")
    suspend fun markAllRead(): ApiResponseDto<MarkAllReadDto>
}
