package pl.mdinstal.chatbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.mdinstal.chatbox.model.Conversation

private val Context.dataStore by preferencesDataStore(name = "md_chatbox_prefs")

class ChatStorage(private val context: Context) {
    private val gson = Gson()
    private val KEY_CONVERSATIONS = stringPreferencesKey("conversations")
    private val KEY_CLAUDE_API    = stringPreferencesKey("claude_api_key")
    private val KEY_GITHUB_TOKEN  = stringPreferencesKey("github_token")
    private val KEY_GITHUB_USER   = stringPreferencesKey("github_user")

    val conversationsFlow: Flow<List<Conversation>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_CONVERSATIONS] ?: return@map emptyList()
        val type = object : TypeToken<List<Conversation>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun saveConversation(conv: Conversation) {
        context.dataStore.edit { prefs ->
            val list = loadList(prefs[KEY_CONVERSATIONS]).toMutableList()
            val idx = list.indexOfFirst { it.id == conv.id }
            if (idx >= 0) list[idx] = conv else list.add(0, conv)
            prefs[KEY_CONVERSATIONS] = gson.toJson(list)
        }
    }

    suspend fun deleteConversation(id: String) {
        context.dataStore.edit { prefs ->
            val list = loadList(prefs[KEY_CONVERSATIONS]).filter { it.id != id }
            prefs[KEY_CONVERSATIONS] = gson.toJson(list)
        }
    }

    private fun loadList(json: String?): List<Conversation> {
        json ?: return emptyList()
        val type = object : TypeToken<List<Conversation>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    val claudeApiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_CLAUDE_API] ?: "" }
    val githubTokenFlow:  Flow<String> = context.dataStore.data.map { it[KEY_GITHUB_TOKEN] ?: "" }
    val githubUserFlow:   Flow<String> = context.dataStore.data.map { it[KEY_GITHUB_USER] ?: "" }

    suspend fun saveClaudeApiKey(key: String) {
        context.dataStore.edit { it[KEY_CLAUDE_API] = key }
    }

    suspend fun saveGithubCredentials(token: String, user: String) {
        context.dataStore.edit {
            it[KEY_GITHUB_TOKEN] = token
            it[KEY_GITHUB_USER]  = user
        }
    }
}
