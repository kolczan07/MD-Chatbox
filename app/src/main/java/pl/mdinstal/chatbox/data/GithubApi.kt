package pl.mdinstal.chatbox.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.mdinstal.chatbox.model.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class GithubPutContentRequest(
    val message: String,
    val content: String,        // base64
    val sha: String,            // aktualny SHA pliku (z GitHubContent)
    val branch: String? = null
)

data class GithubPutContentResponse(
    val content: GithubContent?,
    val commit: GithubCommit?
)

data class GithubCommit(
    val sha: String,
    val message: String
)

interface GithubApiService {
    @GET("user")
    suspend fun getAuthenticatedUser(@Header("Authorization") token: String): GithubUser

    @GET("user/repos")
    suspend fun getMyRepos(
        @Header("Authorization") token: String,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated"
    ): List<GithubRepo>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Header("Authorization") token: String,
        @Query("ref") branch: String
    ): List<GithubContent>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String
    ): List<GithubBranch>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Header("Authorization") token: String,
        @Body body: GithubPutContentRequest
    ): GithubPutContentResponse
}

object GithubApiClient {
    val service: GithubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    })
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApiService::class.java)
    }
}
