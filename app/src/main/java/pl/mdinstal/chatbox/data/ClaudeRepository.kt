package pl.mdinstal.chatbox.data

import pl.mdinstal.chatbox.model.Conversation
import pl.mdinstal.chatbox.model.ChatMessage
import pl.mdinstal.chatbox.model.MessageRole

class ClaudeRepository {
    private val api = ClaudeApiClient.service

    suspend fun sendMessage(
        apiKey: String,
        conversation: Conversation,
        userText: String
    ): Result<String> = runCatching {
        val messages = (conversation.messages + ChatMessage(
            role = MessageRole.USER, content = userText
        )).map { ClaudeMessageDto(role = it.role.name.lowercase(), content = it.content) }

        val response = api.sendMessage(
            apiKey = apiKey,
            request = ClaudeRequest(
                model = conversation.model,
                max_tokens = 8096,
                messages = messages,
                system = conversation.systemPrompt.takeIf { it.isNotBlank() }
            )
        )
        response.content.firstOrNull()?.text ?: ""
    }

    suspend fun listModels(apiKey: String): Result<List<String>> = runCatching {
        api.listModels(apiKey).data.map { it.id }
    }
}
