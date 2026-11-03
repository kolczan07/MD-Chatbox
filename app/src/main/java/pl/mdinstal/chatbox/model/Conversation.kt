package pl.mdinstal.chatbox.model

import java.util.UUID

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Nowy czat",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val model: String = "claude-sonnet-4-5",
    val systemPrompt: String = ""
)
