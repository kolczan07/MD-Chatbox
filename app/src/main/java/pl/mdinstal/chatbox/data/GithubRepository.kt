package pl.mdinstal.chatbox.data

import okhttp3.OkHttpClient
import okhttp3.Request
import pl.mdinstal.chatbox.model.*

class GithubRepository {
    private val api = GithubApiClient.service
    private val httpClient = OkHttpClient()

    suspend fun getUser(token: String): Result<GithubUser> = runCatching {
        api.getAuthenticatedUser("token $token")
    }

    suspend fun getRepos(token: String): Result<List<GithubRepo>> = runCatching {
        api.getMyRepos("token $token")
    }

    suspend fun getContents(
        owner: String, repo: String, path: String, branch: String, token: String
    ): Result<List<GithubContent>> = runCatching {
        api.getContents(owner, repo, path, "token $token", branch)
            .sortedWith(compareBy({ it.type != "dir" }, { it.name }))
    }

    suspend fun getBranches(owner: String, repo: String, token: String): Result<List<String>> =
        runCatching {
            api.getBranches(owner, repo, "token $token").map { it.name }
        }

    suspend fun downloadFile(url: String): Result<String> = runCatching {
        val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
        response.body?.string() ?: ""
    }
}
