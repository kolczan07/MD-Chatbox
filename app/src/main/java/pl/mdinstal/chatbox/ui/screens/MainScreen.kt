package pl.mdinstal.chatbox.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pl.mdinstal.chatbox.viewmodel.ChatViewModel

enum class Tab { CHAT, CONVERSATIONS, GITHUB, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: ChatViewModel) {
    var activeTab by remember { mutableStateOf(Tab.CHAT) }

    LaunchedEffect(Unit) { vm.checkForUpdate(silent = true) }

    if (vm.showUpdateDialog) {
        vm.updateInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { if (vm.updateProgress == null) vm.showUpdateDialog = false },
                title = { Text("Dostępna aktualizacja") },
                text = {
                    Column {
                        Text("Wersja ${info.version} (obecna: ${vm.currentVersion})")
                        if (info.notes.isNotBlank()) Text(info.notes.take(400))
                        vm.updateProgress?.let { LinearProgressIndicator(progress = { it }) }
                    }
                },
                confirmButton = {
                    Button(onClick = { vm.downloadAndInstall() }, enabled = vm.updateProgress == null) { Text("Aktualizuj") }
                },
                dismissButton = {
                    TextButton(onClick = { vm.showUpdateDialog = false }, enabled = vm.updateProgress == null) { Text("Później") }
                }
            )
        }
    }

    LaunchedEffect(activeTab) {
        if (activeTab == Tab.GITHUB && vm.githubRepos.isEmpty()) vm.loadRepos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (activeTab) {
                        Tab.CHAT          -> vm.currentConversation?.title ?: "MD-Chatbox"
                        Tab.CONVERSATIONS -> "Historia rozmów"
                        Tab.GITHUB        -> "GitHub"
                        Tab.SETTINGS      -> "Ustawienia"
                    })
                },
                actions = {
                    if (activeTab == Tab.CHAT) {
                        IconButton(onClick = { vm.newConversation() }) {
                            Icon(Icons.Default.Add, "Nowy czat")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == Tab.CHAT,
                    onClick  = { activeTab = Tab.CHAT },
                    icon     = { Icon(Icons.Default.Chat, null) },
                    label    = { Text("Czat") }
                )
                NavigationBarItem(
                    selected = activeTab == Tab.CONVERSATIONS,
                    onClick  = { activeTab = Tab.CONVERSATIONS },
                    icon     = { Icon(Icons.Default.FormatListBulleted, null) },
                    label    = { Text("Historia") }
                )
                NavigationBarItem(
                    selected = activeTab == Tab.GITHUB,
                    onClick  = { activeTab = Tab.GITHUB },
                    icon     = { Icon(Icons.Default.Code, null) },
                    label    = { Text("GitHub") }
                )
                NavigationBarItem(
                    selected = activeTab == Tab.SETTINGS,
                    onClick  = { activeTab = Tab.SETTINGS },
                    icon     = { Icon(Icons.Default.Settings, null) },
                    label    = { Text("Ustawienia") }
                )
            }
        }
    ) { padding ->
        vm.errorMessage?.let { err ->
            LaunchedEffect(err) { kotlinx.coroutines.delay(4000); vm.errorMessage = null }
            Snackbar(modifier = Modifier.padding(padding),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer
            ) { Text(err) }
        }
        Box(Modifier.padding(padding)) {
            when (activeTab) {
                Tab.CHAT          -> ChatScreen(vm)
                Tab.CONVERSATIONS -> ConversationsScreen(vm) { activeTab = Tab.CHAT }
                Tab.GITHUB        -> GithubScreen(vm) { activeTab = Tab.CHAT }
                Tab.SETTINGS      -> SettingsScreen(vm)
            }
        }
    }
}
