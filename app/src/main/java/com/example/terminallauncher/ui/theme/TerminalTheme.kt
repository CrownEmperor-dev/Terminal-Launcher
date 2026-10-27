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
        TerminalThemeType.CYAN_NEON -> TerminalColorScheme(
            background = Color(0xFF050B14),
            textPrimary = Color(0xFF00F0FF),
            textSecondary = Color(0xFF70C0E0),
            textGhost = Color(0xFF205070),
            errorColor = Color(0xFFFF0055),
            successColor = Color(0xFF00FF99),
            systemColor = Color(0xFF00A3FF),
            headerColor = Color(0xFFFF007F),
            promptColor = Color(0xFF00F0FF)
        )
        TerminalThemeType.GAMEBOY_DMG -> TerminalColorScheme(
            background = Color(0xFF9BBC0F),
            textPrimary = Color(0xFF0F380F),
            textSecondary = Color(0xFF306230),
            textGhost = Color(0xFF8BAC0F),
            errorColor = Color(0xFF8B0000),
            successColor = Color(0xFF306230),
            systemColor = Color(0xFF306230),
            headerColor = Color(0xFF0F380F),
            promptColor = Color(0xFF0F380F)
        )
        TerminalThemeType.SOLARIZED -> TerminalColorScheme(
            background = Color(0xFF002B36),
            textPrimary = Color(0xFF839496),
            textSecondary = Color(0xFF657B83),
            textGhost = Color(0xFF073642),
            errorColor = Color(0xFFDC322F),
            successColor = Color(0xFF859900),
            systemColor = Color(0xFF268BD2),
            headerColor = Color(0xFFB58900),
            promptColor = Color(0xFF2AA198)
        )
        TerminalThemeType.DRACULA -> TerminalColorScheme(
            background = Color(0xFF282A36),
            textPrimary = Color(0xFFF8F8F2),
            textSecondary = Color(0xFF6272A4),
            textGhost = Color(0xFF44475A),
            errorColor = Color(0xFFFF5555),
            successColor = Color(0xFF50FA7B),
            systemColor = Color(0xFF8BE9FD),
            headerColor = Color(0xFFBD93F9),
            promptColor = Color(0xFFFF79C6)
        )
    }
}
