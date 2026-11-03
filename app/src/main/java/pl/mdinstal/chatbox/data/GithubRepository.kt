package pl.mdinstal.chatbox.data

import okhttp3.OkHttpClient
import okhttp3.Request
import pl.mdinstal.chatbox.model.*
import java.util.Base64

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

    /** Pobiera surową treść pliku (działa też dla prywatnych repo, bo dodajemy token). */
    suspend fun downloadFile(url: String, token: String = ""): Result<String> = runCatching {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (token.isNotBlank()) header("Authorization", "token $token")
            }
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} ${response.message}")
            }
            response.body?.string() ?: error("Pusta odpowiedź serwera")
        }
    }

    /**
     * Edytuje plik w repo. Zwraca nową treść z odpowiedzi (lub pusty string).
     * Wymaga: owner, repo, path, branch, aktualny sha, nowa treść, token, commit message.
     */
    suspend fun updateFile(
        owner: String,
        repo: String,
        path: String,
        branch: String,
        currentSha: String,
        newContent: String,
        token: String,
        commitMessage: String
    ): Result<Unit> = runCatching {
        val encoded = Base64.getEncoder().encodeToString(newContent.toByteArray(Charsets.UTF_8))
        api.putContents(
            owner = owner,
            repo = repo,
            path = path,
            token = "token $token",
            body = GithubPutContentRequest(
                message = commitMessage,
                content = encoded,
                sha = currentSha,
                branch = branch
            )
        )
        Unit
    }
}
