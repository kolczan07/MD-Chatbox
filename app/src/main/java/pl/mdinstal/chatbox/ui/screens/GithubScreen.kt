package pl.mdinstal.chatbox.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import pl.mdinstal.chatbox.model.*
import pl.mdinstal.chatbox.viewmodel.ChatViewModel

@Composable
fun GithubScreen(vm: ChatViewModel, onSendToChat: () -> Unit) {
    val githubToken by vm.githubToken.collectAsState()
    val githubUser  by vm.githubUser.collectAsState()

    if (githubToken.isBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(0.3f))
                Spacer(Modifier.height(16.dp))
                Text("Skonfiguruj token GitHub", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Spacer(Modifier.height(8.dp))
                Text("Przejdź do zakładki ⚙️ Ustawienia", color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
            }
        }
        return
    }

    when {
        vm.fileContent != null -> FileViewer(vm, onSendToChat)
        vm.browserRepo != null -> FileBrowser(vm)
        else                   -> RepoList(vm, githubUser)
    }
}

@Composable
private fun RepoList(vm: ChatViewModel, githubUser: String) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("@$githubUser", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.loadRepos() }) { Icon(Icons.Default.Refresh, "Odśwież") }
        }
        HorizontalDivider()
        if (vm.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.githubRepos, key = { it.id }) { repo ->
                Card(onClick = { vm.openRepo(repo) }, elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (repo.private) Icons.Default.Lock else Icons.Default.FolderOpen, null,
                            Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(repo.name, style = MaterialTheme.typography.bodyLarge)
                            if (!repo.description.isNullOrBlank())
                                Text(repo.description, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repo.language?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary); Spacer(Modifier.width(8.dp)) }
                                Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFC107))
                                Text(repo.stargazers_count.toString(), fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FileBrowser(vm: ChatViewModel) {
    val repo = vm.browserRepo ?: return
    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.navigateUp() }) { Icon(Icons.Default.ArrowBack, null) }
                    Column(Modifier.weight(1f)) {
                        Text(repo.name, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                        if (vm.browserPath.isNotEmpty())
                            Text(vm.browserPath, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (vm.browserBranches.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(onClick = { expanded = true }, label = { Text(vm.browserBranch, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.AccountTree, null, Modifier.size(14.dp)) })
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                vm.browserBranches.forEach { b ->
                                    DropdownMenuItem(text = { Text(b) }, onClick = { vm.switchBranch(b); expanded = false })
                                }
                            }
                        }
                    }
                }
                if (vm.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
            items(vm.browserContents, key = { it.sha }) { item ->
                val isDir = item.type == "dir"
                Row(Modifier.fillMaxWidth().clickable { if (isDir) vm.navigateTo(item.path) else vm.openFile(item) }
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile, null, Modifier.size(22.dp),
                        tint = if (isDir) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                    Spacer(Modifier.width(14.dp))
                    Text(item.name, Modifier.weight(1f), fontSize = 14.sp)
                    if (!isDir) Text("${item.size / 1024 + 1} KB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.2f))
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
            }
        }
    }
}

@Composable
private fun FileViewer(vm: ChatViewModel, onSendToChat: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val content   = vm.fileContent ?: return
    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.fileContent = null }) { Icon(Icons.Default.ArrowBack, null) }
                Text(vm.fileName, Modifier.weight(1f), fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                IconButton(onClick = { clipboard.setText(AnnotatedString(content)) }) { Icon(Icons.Default.ContentCopy, "Kopiuj") }
                Button(onClick = { vm.injectFileToChat(vm.fileName, content); onSendToChat() }, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Do czatu", fontSize = 12.sp)
                }
            }
        }
        HorizontalDivider()
        Text(content, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
