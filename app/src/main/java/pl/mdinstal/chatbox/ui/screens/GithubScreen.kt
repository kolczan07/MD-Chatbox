package pl.mdinstal.chatbox.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import pl.mdinstal.chatbox.model.*
import pl.mdinstal.chatbox.viewmodel.ChatViewModel
import java.io.File

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
        vm.isEditing        -> FileEditor(vm)
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
    val context = LocalContext.current
    val content = vm.fileContent ?: return
    var showDownloadInfo by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.fileContent = null }) { Icon(Icons.Default.ArrowBack, null) }
                Text(vm.fileName, Modifier.weight(1f), fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                IconButton(onClick = { clipboard.setText(AnnotatedString(content)) }) { Icon(Icons.Default.ContentCopy, "Kopiuj") }
                IconButton(onClick = { vm.startEditing() }) { Icon(Icons.Default.Edit, "Edytuj") }
                IconButton(onClick = {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(dir, vm.fileName)
                    runCatching { file.writeText(content) }
                        .onSuccess { showDownloadInfo = "Zapisano: ${file.absolutePath}" }
                        .onFailure { showDownloadInfo = "Błąd zapisu: ${it.message}" }
                }) { Icon(Icons.Default.Download, "Pobierz") }
            }
        }
        if (showDownloadInfo != null) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(showDownloadInfo!!, modifier = Modifier.fillMaxWidth().padding(8.dp), fontSize = 11.sp)
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                vm.injectFileToChat(vm.fileName, content)
                vm.fileContent = null
                onSendToChat()
            }) {
                Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Do czatu", fontSize = 12.sp)
            }
        }
        Text(content, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileEditor(vm: ChatViewModel) {
    var commitMessage by remember { mutableStateOf("Update ${vm.fileName} via MD-Chatbox") }

    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.cancelEditing() }) { Icon(Icons.Default.Close, "Anuluj") }
                Text("Edycja: ${vm.fileName}", Modifier.weight(1f), fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Button(
                    onClick = { vm.saveEditedFile(commitMessage) },
                    enabled = !vm.isLoading
                ) {
                    if (vm.isLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Zapisz", fontSize = 12.sp)
                }
            }
        }
        HorizontalDivider()
        OutlinedTextField(
            value = commitMessage,
            onValueChange = { commitMessage = it },
            label = { Text("Commit message", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
        HorizontalDivider()
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            if (vm.editText.isEmpty()) {
                Text("Plik jest pusty", color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontSize = 12.sp)
            }
            BasicTextField(
                value = vm.editText,
                onValueChange = { vm.editText = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
