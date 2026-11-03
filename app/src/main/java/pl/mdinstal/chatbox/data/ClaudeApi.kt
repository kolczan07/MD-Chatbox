package pl.mdinstal.chatbox.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class ClaudeRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<ClaudeMessageDto>,
    val system: String? = null
)

data class ClaudeMessageDto(val role: String, val content: String)

data class ClaudeResponse(
    val id: String,
    val content: List<ClaudeContentBlock>,
    val model: String,
    val usage: ClaudeUsage
)

data class ClaudeContentBlock(val type: String, val text: String)
data class ClaudeUsage(val input_tokens: Int, val output_tokens: Int)
data class ClaudeModelsResponse(val data: List<ClaudeModelInfo>)
data class ClaudeModelInfo(val id: String, val display_name: String)

interface ClaudeApiService {
    @POST("v1/messages")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: ClaudeRequest
    ): ClaudeResponse

    @GET("v1/models")
    suspend fun listModels(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01"
    ): ClaudeModelsResponse
}

object ClaudeApiClient {
    val service: ClaudeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClaudeApiService::class.java)
    }
}
