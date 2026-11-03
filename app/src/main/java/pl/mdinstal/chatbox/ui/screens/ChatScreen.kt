package pl.mdinstal.chatbox.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import pl.mdinstal.chatbox.model.*
import pl.mdinstal.chatbox.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(vm: ChatViewModel) {
    val conv = vm.currentConversation
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(conv?.messages?.size) {
        val size = conv?.messages?.size ?: 0
        if (size > 0) listState.animateScrollToItem(size - 1)
    }

    // Zmiana 1: Dodano Modifier.imePadding() do głównego Column
    Column(
        Modifier
            .fillMaxSize()
            .imePadding() 
    ) {
        if (conv != null) {
            Surface(tonalElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = {}, label = { Text(conv.model, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.SmartToy, null, Modifier.size(14.dp)) })
                    if (conv.systemPrompt.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        AssistChip(onClick = {}, label = { Text("System prompt", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.PushPin, null, Modifier.size(14.dp)) })
                    }
                }
            }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (conv == null || conv.messages.isEmpty()) {
                item { EmptyState() }
            } else {
                items(conv.messages, key = { it.id }) { msg -> MessageBubble(msg) }
                if (vm.isLoading) { item { TypingBubble() } }
            }
        }

        InputBar(text = inputText, onChange = { inputText = it },
            onSend = { val t = inputText.trim(); if (t.isNotBlank()) { vm.sendMessage(t); inputText = "" } },
            enabled = !vm.isLoading && vm.currentConversation != null)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SmartToy, null, Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        Spacer(Modifier.height(20.dp))
        Text("MD-Chatbox", style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        Spacer(Modifier.height(8.dp))
        Text("Napisz pierwszą wiadomość", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(msg: ChatMessage) {
    val clipboard = LocalClipboardManager.current
    val isUser = msg.role == MessageRole.USER
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) Avatar(false)
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.padding(horizontal = 6.dp)) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 18.dp else 4.dp, topEnd = if (isUser) 4.dp else 18.dp,
                    bottomStart = 18.dp, bottomEnd = 18.dp),
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 290.dp).combinedClickable(
                    onClick = {}, onLongClick = { clipboard.setText(AnnotatedString(msg.content)) })
            ) {
                Text(msg.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 14.sp, lineHeight = 20.sp,
                    fontFamily = if (msg.content.contains("```")) FontFamily.Monospace else FontFamily.Default)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sdf.format(Date(msg.timestamp)), fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ContentCopy, null,
                    Modifier.size(14.dp).clickable { clipboard.setText(AnnotatedString(msg.content)) },
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
            }
        }
        if (isUser) Avatar(true)
    }
}

@Composable
private fun Avatar(isUser: Boolean) {
    Box(Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape)
        .background(if (isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center) {
        Icon(if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
            null, Modifier.size(18.dp),
            tint = if (isUser) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun TypingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(false); Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant) {
            LinearProgressIndicator(Modifier.width(90.dp).padding(vertical = 16.dp, horizontal = 20.dp))
        }
    }
}

@Composable
private fun InputBar(text: String, onChange: (String) -> Unit, onSend: () -> Unit, enabled: Boolean) {
    Surface(tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(value = text, onValueChange = onChange, modifier = Modifier.weight(1f),
                placeholder = { Text("Napisz wiadomość…") }, maxLines = 6,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default))
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSend, enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.Send, "Wyślij", Modifier.size(22.dp))
            }
        }
    }
}
