package pl.mdinstal.chatbox.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.mdinstal.chatbox.data.*
import pl.mdinstal.chatbox.model.*

class ChatViewModel(private val storage: ChatStorage, private val updater: Updater) : ViewModel() {

    private val claudeRepo = ClaudeRepository()
    val githubRepo = GithubRepository()

    val conversations = storage.conversationsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val claudeApiKey  = storage.claudeApiKeyFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val githubToken   = storage.githubTokenFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val githubUser    = storage.githubUserFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    var currentConversation by mutableStateOf<Conversation?>(null)
    var isLoading           by mutableStateOf(false)
    var errorMessage        by mutableStateOf<String?>(null)
    var availableModels     by mutableStateOf(listOf("claude-opus-4-5", "claude-sonnet-4-5", "claude-haiku-4-5"))

    var githubRepos     by mutableStateOf<List<GithubRepo>>(emptyList())
    var browserRepo     by mutableStateOf<GithubRepo?>(null)
    var browserPath     by mutableStateOf("")
    var browserBranch   by mutableStateOf("main")
    var browserContents by mutableStateOf<List<GithubContent>>(emptyList())
    var browserBranches by mutableStateOf<List<String>>(emptyList())
    var fileContent     by mutableStateOf<String?>(null)
    var fileName        by mutableStateOf("")

    fun newConversation(model: String = "claude-sonnet-4-5", systemPrompt: String = "") {
        currentConversation = Conversation(model = model, systemPrompt = systemPrompt)
    }

    fun selectConversation(conv: Conversation) { currentConversation = conv }

    fun deleteConversation(id: String) = viewModelScope.launch {
        storage.deleteConversation(id)
        if (currentConversation?.id == id) currentConversation = null
    }

    fun sendMessage(text: String) {
        val conv = currentConversation ?: return
        val key  = claudeApiKey.value
        if (key.isBlank()) { errorMessage = "Brak klucza API Claude!"; return }

        val userMsg     = ChatMessage(role = MessageRole.USER, content = text)
        val updatedConv = conv.copy(
            messages = conv.messages + userMsg,
            title    = if (conv.messages.isEmpty()) text.take(50) else conv.title
        )
        currentConversation = updatedConv
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            claudeRepo.sendMessage(key, updatedConv, text).fold(
                onSuccess = { reply ->
                    val finalConv = updatedConv.copy(
                        messages = updatedConv.messages + ChatMessage(role = MessageRole.ASSISTANT, content = reply)
                    )
                    currentConversation = finalConv
                    storage.saveConversation(finalConv)
                },
                onFailure = { errorMessage = "Błąd: ${it.message}" }
            )
            isLoading = false
        }
    }

    fun injectFileToChat(name: String, content: String) {
        sendMessage("📎 **$name**\n```\n$content\n```")
    }

    fun saveClaudeKey(key: String) = viewModelScope.launch { storage.saveClaudeApiKey(key) }

    fun saveGithubToken(token: String) = viewModelScope.launch {
        if (token.isBlank()) return@launch
        isLoading = true
        githubRepo.getUser(token).fold(
            onSuccess = { user -> storage.saveGithubCredentials(token, user.login) },
            onFailure = { errorMessage = "GitHub: ${it.message}" }
        )
        isLoading = false
    }

    fun loadModels() = viewModelScope.launch {
        val key = claudeApiKey.value
        if (key.isBlank()) return@launch
        claudeRepo.listModels(key).onSuccess { availableModels = it }
    }

    fun loadRepos() = viewModelScope.launch {
        isLoading = true
        githubRepo.getRepos(githubToken.value).fold(
            onSuccess = { githubRepos = it },
            onFailure = { errorMessage = it.message }
        )
        isLoading = false
    }

    fun openRepo(repo: GithubRepo) {
        browserRepo = repo; browserPath = ""; browserBranch = repo.default_branch
        fileContent = null; loadBranches(); loadContents()
    }

    fun navigateTo(path: String) { browserPath = path; loadContents() }

    fun navigateUp() {
        when {
            fileContent != null -> fileContent = null
            browserPath.contains("/") -> { browserPath = browserPath.substringBeforeLast("/"); loadContents() }
            browserPath.isNotEmpty()  -> { browserPath = ""; loadContents() }
            else -> { browserRepo = null; browserContents = emptyList() }
        }
    }

    fun switchBranch(branch: String) { browserBranch = branch; loadContents() }

    private fun loadBranches() = viewModelScope.launch {
        val repo = browserRepo ?: return@launch
        githubRepo.getBranches(repo.full_name.substringBefore("/"), repo.name, githubToken.value)
            .onSuccess { browserBranches = it }
    }

    private fun loadContents() {
        val repo = browserRepo ?: return
        viewModelScope.launch {
            isLoading = true
            githubRepo.getContents(
                repo.full_name.substringBefore("/"), repo.name,
                browserPath, browserBranch, githubToken.value
            ).fold(
                onSuccess = { browserContents = it },
                onFailure = { errorMessage = it.message }
            )
            isLoading = false
        }
    }

    fun openFile(item: GithubContent) {
        val url = item.download_url ?: return
        viewModelScope.launch {
            isLoading = true
            githubRepo.downloadFile(url).fold(
                onSuccess = { content -> fileContent = content; fileName = item.name },
                onFailure = { errorMessage = it.message }
            )
            isLoading = false
        }
    }

    // ── Aktualizacje (GitHub Releases) ──
    var updateInfo       by mutableStateOf<UpdateInfo?>(null)
    var updateStatus     by mutableStateOf("")
    var updateProgress   by mutableStateOf<Float?>(null)
    var showUpdateDialog by mutableStateOf(false)
    val currentVersion: String get() = updater.currentVersion()

    fun checkForUpdate(silent: Boolean = false) = viewModelScope.launch {
        if (!silent) updateStatus = "Sprawdzanie…"
        updater.checkForUpdate().fold(
            onSuccess = { info ->
                updateInfo = info
                if (info != null) {
                    updateStatus = "Dostępna wersja ${info.version}"
                    showUpdateDialog = true
                } else if (!silent) updateStatus = "Masz najnowszą wersję"
            },
            onFailure = { if (!silent) updateStatus = "Błąd: ${it.message}" }
        )
    }

    fun downloadAndInstall() {
        val info = updateInfo ?: return
        viewModelScope.launch {
            updateProgress = 0f
            updater.download(info) { updateProgress = it }.fold(
                onSuccess = { file ->
                    updateProgress = null
                    if (updater.install(file)) showUpdateDialog = false
                    else errorMessage = "Zezwól na instalację z tego źródła i spróbuj ponownie"
                },
                onFailure = { updateProgress = null; errorMessage = "Błąd pobierania: ${it.message}" }
            )
        }
    }

    class Factory(private val ctx: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>) = ChatViewModel(ChatStorage(ctx), Updater(ctx.applicationContext)) as T
    }
}
