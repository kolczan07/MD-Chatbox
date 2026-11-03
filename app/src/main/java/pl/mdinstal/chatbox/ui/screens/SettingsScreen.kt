package pl.mdinstal.chatbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import pl.mdinstal.chatbox.viewmodel.ChatViewModel

@Composable
fun SettingsScreen(vm: ChatViewModel) {
    val claudeKey  by vm.claudeApiKey.collectAsState()
    val githubTok  by vm.githubToken.collectAsState()
    val githubUser by vm.githubUser.collectAsState()

    var claudeInput by remember(claudeKey) { mutableStateOf(claudeKey) }
    var githubInput by remember(githubTok) { mutableStateOf(githubTok) }
    var claudeVis   by remember { mutableStateOf(false) }
    var githubVis   by remember { mutableStateOf(false) }
    var savedSnack  by remember { mutableStateOf(false) }

    LaunchedEffect(savedSnack) { if (savedSnack) { kotlinx.coroutines.delay(2000); savedSnack = false } }

    Scaffold(snackbarHost = { if (savedSnack) Snackbar(modifier = Modifier.padding(16.dp)) { Text("✅ Zapisano!") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Claude API", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = claudeInput, onValueChange = { claudeInput = it },
                            label = { Text("API Key") }, placeholder = { Text("sk-ant-…") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = if (claudeVis) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { IconButton(onClick = { claudeVis = !claudeVis }) { Icon(if (claudeVis) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.loadModels() }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Modele", fontSize = 13.sp)
                            }
                            Button(onClick = { vm.saveClaudeKey(claudeInput); savedSnack = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Zapisz", fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Dostępne modele:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Spacer(Modifier.height(4.dp))
                        vm.availableModels.forEach { Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.65f)) }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("GitHub", style = MaterialTheme.typography.titleMedium)
                        }
                        if (githubUser.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(6.dp))
                                Text("Zalogowany jako @$githubUser", color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = githubInput, onValueChange = { githubInput = it },
                            label = { Text("Personal Access Token") }, placeholder = { Text("ghp_…") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = if (githubVis) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { IconButton(onClick = { githubVis = !githubVis }) { Icon(if (githubVis) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { vm.saveGithubToken(githubInput); savedSnack = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Zapisz i zweryfikuj")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Wymagane uprawnienia: repo:read, user:read", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ℹ️ MD-Chatbox v1.0", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("• Klucze przechowywane lokalnie w DataStore\n• Historia rozmów zapisywana między sesjami\n• Pliki GitHub można wysłać bezpośrednio do Claude\n• Obsługuje wiele równoległych konwersacji",
                            fontSize = 12.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}
