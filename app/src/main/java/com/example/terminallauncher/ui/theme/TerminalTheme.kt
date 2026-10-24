package com.example.terminallauncher.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.terminallauncher.data.TerminalThemeType

data class TerminalColorScheme(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textGhost: Color,
    val errorColor: Color,
    val successColor: Color,
    val systemColor: Color,
    val headerColor: Color,
    val promptColor: Color
)

fun getTerminalColorScheme(themeType: TerminalThemeType): TerminalColorScheme {
    return when (themeType) {
        TerminalThemeType.CRT_GREEN -> TerminalColorScheme(
            background = Color(0xFF0A0D0A),
            textPrimary = Color(0xFF00FF66),
            textSecondary = Color(0xFF00CC55),
            textGhost = Color(0xFF005522),
            errorColor = Color(0xFFFF3344),
            successColor = Color(0xFF00FF99),
            systemColor = Color(0xFF008844),
            headerColor = Color(0xFF33FF88),
            promptColor = Color(0xFF00FF66)
        )
        TerminalThemeType.AMBER -> TerminalColorScheme(
            background = Color(0xFF0D0B08),
            textPrimary = Color(0xFFFFB000),
            textSecondary = Color(0xFFD49000),
            textGhost = Color(0xFF664600),
            errorColor = Color(0xFFFF4444),
            successColor = Color(0xFFFFD055),
            systemColor = Color(0xFFA87400),
            headerColor = Color(0xFFFFC233),
            promptColor = Color(0xFFFFB000)
        )
        TerminalThemeType.OLED_BLACK -> TerminalColorScheme(
            background = Color(0xFF000000),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFCCCCCC),
            textGhost = Color(0xFF555555),
            errorColor = Color(0xFFFF5555),
            successColor = Color(0xFF55FF55),
            systemColor = Color(0xFF888888),
            headerColor = Color(0xFF00E5FF),
            promptColor = Color(0xFF00E5FF)
        )
        TerminalThemeType.MONOCHROME -> TerminalColorScheme(
            background = Color(0xFF121212),
            textPrimary = Color(0xFFE0E0E0),
            textSecondary = Color(0xFFA0A0A0),
            textGhost = Color(0xFF4A4A4A),
            errorColor = Color(0xFFCF6679),
            successColor = Color(0xFFB0BEC5),
            systemColor = Color(0xFF78909C),
            headerColor = Color(0xFFECEFF1),
            promptColor = Color(0xFF90A4AE)
        )
    }
}
