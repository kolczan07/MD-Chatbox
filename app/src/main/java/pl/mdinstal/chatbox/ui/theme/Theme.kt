package pl.mdinstal.chatbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary          = Color(0xFF7C9EFF),
    onPrimary        = Color(0xFF001A72),
    primaryContainer = Color(0xFF0E2EA8),
    secondary        = Color(0xFF9ECAFF),
    tertiary         = Color(0xFFB8EFCB),
    background       = Color(0xFF0F1117),
    surface          = Color(0xFF1A1D2E),
    surfaceVariant   = Color(0xFF252840)
)

private val LightColors = lightColorScheme(
    primary          = Color(0xFF1A3EBF),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    secondary        = Color(0xFF1A5FA0),
    tertiary         = Color(0xFF006E26),
    background       = Color(0xFFF5F6FF),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFE8EAFF)
)

@Composable
fun MDChatboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography(),
        content     = content
    )
}
