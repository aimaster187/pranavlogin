package com.example.data.remote

import com.example.data.model.BotActionRequest
import com.example.data.model.GenericApiResponse
import com.example.data.model.HealthResponse
import com.example.data.model.RegisterBotRequest
import com.example.data.model.RegisterBotResponse
import com.example.data.model.SyncAppRequest
import com.example.data.model.SyncDeviceRequest
import com.example.data.model.SyncDialogRequest
import com.example.data.model.SyncEventRequest
import com.example.data.model.SyncLicenseRequest
import com.example.data.model.SyncWorkspaceRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface PranavBackendService {
    @GET("/api/v1/health")
    suspend fun getHealth(): HealthResponse

    @POST("/api/v1/admin/workspace")
    suspend fun syncWorkspace(
        @Header("Authorization") bearerAuth: String,
        @Body request: SyncWorkspaceRequest
    ): GenericApiResponse

    @POST("/api/v1/admin/apps")
    suspend fun syncApp(
        @Header("Authorization") bearerAuth: String,
        @Body request: SyncAppRequest
    ): GenericApiResponse

    @DELETE("/api/v1/admin/apps/{appId}")
    suspend fun deleteApp(
        @Header("Authorization") bearerAuth: String,
        @Path("appId") appId: String
    ): GenericApiResponse

    @POST("/api/v1/admin/dialogs")
    suspend fun syncDialog(
        @Header("Authorization") bearerAuth: String,
        @Body request: SyncDialogRequest
    ): GenericApiResponse

    @POST("/api/v1/admin/devices")
    suspend fun syncDevice(
        @Header("Authorization") bearerAuth: String,
        @Body request: SyncDeviceRequest
    ): GenericApiResponse

    @POST("/api/v1/admin/licenses")
    suspend fun syncLicense(
        @Header("Authorization") bearerAuth: String,
        @Body request: SyncLicenseRequest
    ): GenericApiResponse

    @POST("/api/v1/admin/events")
    suspend fun syncEvent(
        @Header("Authorization") bearerAuth: String,
        @Body request: SyncEventRequest
    ): GenericApiResponse

    @POST("/api/v1/bots")
    suspend fun registerBot(
        @Header("Authorization") bearerAuth: String,
        @Body request: RegisterBotRequest
    ): RegisterBotResponse

    @POST("/api/v1/bots/{botId}/action")
    suspend fun performBotAction(
        @Header("Authorization") bearerAuth: String,
        @Path("botId") botId: String,
        @Body request: BotActionRequest
    ): GenericApiResponse

    @DELETE("/api/v1/bots/{botId}")
    suspend fun deleteBot(
        @Header("Authorization") bearerAuth: String,
        @Path("botId") botId: String
    ): GenericApiResponse
}

object PranavBackendClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun create(baseUrl: String): PranavBackendService {
        val normalized = baseUrl.trim().trimEnd('/').let {
            if (it.startsWith("http://") || it.startsWith("https://")) "$it/" else "https://$it/"
        }
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PranavBackendService::class.java)
    }
}
