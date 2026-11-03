package pl.mdinstal.chatbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.mdinstal.chatbox.ui.screens.MainScreen
import pl.mdinstal.chatbox.ui.theme.MDChatboxTheme
import pl.mdinstal.chatbox.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MDChatboxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: ChatViewModel = viewModel(factory = ChatViewModel.Factory(this))
                    LaunchedEffect(Unit) {
                        if (vm.currentConversation == null) vm.newConversation()
                    }
                    MainScreen(vm)
                }
            }
        }
    }
}
