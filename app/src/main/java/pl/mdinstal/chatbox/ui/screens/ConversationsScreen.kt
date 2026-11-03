package pl.mdinstal.chatbox.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import pl.mdinstal.chatbox.model.Conversation
import pl.mdinstal.chatbox.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationsScreen(vm: ChatViewModel, onSelect: () -> Unit) {
    val conversations by vm.conversations.collectAsState()
    var showNewDialog  by remember { mutableStateOf(false) }
    var selectedModel  by remember { mutableStateOf("claude-sonnet-4-5") }
    var systemPrompt   by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showNewDialog = true },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Nowy czat") })
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.2f))
                    Spacer(Modifier.height(12.dp))
                    Text("Brak zapisanych rozmów", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(conversations, key = { it.id }) { conv ->
                    ConversationCard(conv,
                        isActive = vm.currentConversation?.id == conv.id,
                        onSelect = { vm.selectConversation(conv); onSelect() },
                        onDelete = { vm.deleteConversation(conv.id) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showNewDialog) {
        AlertDialog(onDismissRequest = { showNewDialog = false },
            title = { Text("Nowy czat") },
            text = {
                Column {
                    Text("Model AI:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    vm.availableModels.forEach { m ->
                        Row(Modifier.fillMaxWidth().clickable { selectedModel = m }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedModel == m, onClick = { selectedModel = m })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(m, style = MaterialTheme.typography.bodyMedium)
                                Text(when {
                                    m.contains("opus")   -> "Najinteligentniejszy"
                                    m.contains("sonnet") -> "Zbalansowany (zalecany)"
                                    m.contains("haiku")  -> "Najszybszy"
                                    else -> ""
                                }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("System prompt (opcjonalnie):", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = systemPrompt, onValueChange = { systemPrompt = it },
                        placeholder = { Text("np. Jesteś ekspertem Kotlin…") },
                        maxLines = 4, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { vm.newConversation(selectedModel, systemPrompt); systemPrompt = ""; showNewDialog = false; onSelect() }) { Text("Utwórz") }
            },
            dismissButton = { TextButton(onClick = { showNewDialog = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun ConversationCard(conv: Conversation, isActive: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }
    Card(onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(if (isActive) 4.dp else 1.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isActive) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline, null, Modifier.size(28.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.4f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(conv.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${conv.messages.size} wiad. · ${conv.model} · ${sdf.format(Date(conv.createdAt))}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                if (conv.systemPrompt.isNotBlank())
                    Text("📌 ${conv.systemPrompt.take(40)}…", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Usuń", tint = MaterialTheme.colorScheme.error.copy(0.55f))
            }
        }
    }
}
