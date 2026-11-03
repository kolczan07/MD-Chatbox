package pl.mdinstal.chatbox.model

data class GithubRepo(
    val id: Long,
    val name: String,
    val full_name: String,
    val description: String?,
    val html_url: String,
    val stargazers_count: Int,
    val language: String?,
    val private: Boolean,
    val default_branch: String = "main"
)

data class GithubContent(
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val download_url: String?,
    val sha: String
)

data class GithubUser(
    val login: String,
    val name: String?,
    val avatar_url: String,
    val public_repos: Int,
    val followers: Int
)

data class GithubBranch(val name: String)
