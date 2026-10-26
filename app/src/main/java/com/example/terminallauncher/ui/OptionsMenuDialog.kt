package com.example.terminallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.terminallauncher.data.AsciiArtStyle
import com.example.terminallauncher.data.LauncherSettings
import com.example.terminallauncher.data.TerminalThemeType
import com.example.terminallauncher.ui.theme.TerminalColorScheme

@Composable
fun OptionsMenuDialog(
    settings: LauncherSettings,
    colorScheme: TerminalColorScheme,
    onDismiss: () -> Unit,
    onSetTheme: (TerminalThemeType) -> Unit,
    onSetTextSize: (Int) -> Unit,
    onSetAsciiStyle: (AsciiArtStyle) -> Unit,
    onSetCustomAscii: (String) -> Unit,
    onSetFastfetch: (Boolean) -> Unit,
    onSetAutocomplete: (Boolean) -> Unit,
    onSetPrompt: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf(settings.promptString) }
    var customAsciiInput by remember { mutableStateOf(settings.customAsciiArt) }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, colorScheme.promptColor, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background)
                    .verticalScroll(scrollState)
            ) {
                // Title Header
                Text(
                    text = "[ LAUNCHER OPTIONS ]",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.headerColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Selection
                Text(
                    text = "THEME COLOR PALETTE:",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.promptColor
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TerminalThemeType.entries.forEach { theme ->
                        val isSelected = settings.theme == theme
                        val label = when (theme) {
                            TerminalThemeType.CRT_GREEN -> "Green"
                            TerminalThemeType.AMBER -> "Amber"
                            TerminalThemeType.OLED_BLACK -> "OLED"
                            TerminalThemeType.MONOCHROME -> "Mono"
                        }
                        OptionChip(
                            label = label,
                            isSelected = isSelected,
                            colorScheme = colorScheme,
                            onClick = { onSetTheme(theme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Size Selection
                Text(
                    text = "TERMINAL TEXT SIZE:",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.promptColor
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(12 to "12sp (S)", 15 to "15sp (M)", 18 to "18sp (L)").forEach { (size, label) ->
                        OptionChip(
                            label = label,
                            isSelected = settings.textSizeSp == size,
                            colorScheme = colorScheme,
                            onClick = { onSetTextSize(size) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ASCII Banner Selection
                Text(
                    text = "ASCII ART BANNER:",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.promptColor
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            AsciiArtStyle.DEFAULT to "Default",
                            AsciiArtStyle.BUGDROID to "Bugdroid",
                            AsciiArtStyle.TUX to "Tux"
                        ).forEach { (style, label) ->
                            OptionChip(
                                label = label,
                                isSelected = settings.asciiArtStyle == style,
                                colorScheme = colorScheme,
                                onClick = { onSetAsciiStyle(style) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            AsciiArtStyle.RETRO_PC to "Retro PC",
                            AsciiArtStyle.MINIMAL to "Minimal",
                            AsciiArtStyle.CUSTOM to "Custom",
                            AsciiArtStyle.OFF to "Off"
                        ).forEach { (style, label) ->
                            OptionChip(
                                label = label,
                                isSelected = settings.asciiArtStyle == style,
                                colorScheme = colorScheme,
                                onClick = { onSetAsciiStyle(style) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (settings.asciiArtStyle == AsciiArtStyle.CUSTOM) {
                    val asciiFocusRequester = remember { FocusRequester() }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PASTE CUSTOM ASCII ART (asciiart.eu):",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colorScheme.textSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .border(1.dp, colorScheme.promptColor, RoundedCornerShape(4.dp))
                            .clickable { asciiFocusRequester.requestFocus() }
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = customAsciiInput,
                            onValueChange = {
                                customAsciiInput = it
                                onSetCustomAscii(it)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(asciiFocusRequester),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = colorScheme.textPrimary
                            ),
                            cursorBrush = SolidColor(colorScheme.promptColor),
                            singleLine = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Startup Infofetch Banner:",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = colorScheme.textPrimary
                        )
                    )
                    Switch(
                        checked = settings.fastfetchOnStart,
                        onCheckedChange = { onSetFastfetch(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.background,
                            checkedTrackColor = colorScheme.promptColor,
                            uncheckedThumbColor = colorScheme.textGhost,
                            uncheckedTrackColor = colorScheme.background
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ghost Autocomplete:",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = colorScheme.textPrimary
                        )
                    )
                    Switch(
                        checked = settings.autocompleteEnabled,
                        onCheckedChange = { onSetAutocomplete(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.background,
                            checkedTrackColor = colorScheme.promptColor,
                            uncheckedThumbColor = colorScheme.textGhost,
                            uncheckedTrackColor = colorScheme.background
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prompt String
                Text(
                    text = "SHELL PROMPT STRING:",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.promptColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colorScheme.promptColor, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = promptInput,
                        onValueChange = {
                            promptInput = it
                            onSetPrompt(it)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = colorScheme.textPrimary
                        ),
                        cursorBrush = SolidColor(colorScheme.promptColor),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About Info Card
                Text(
                    text = "[ ABOUT TERMINAL LAUNCHER ]",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.promptColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colorScheme.textGhost, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Terminal Launcher v1.0.0 (MIT License)",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.textPrimary
                        )
                    )
                    Text(
                        text = "Developed by CrownEmperor-dev",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colorScheme.textSecondary
                        )
                    )
                    Text(
                        text = "github.com/CrownEmperor-dev/Terminal-Launcher",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = colorScheme.systemColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.promptColor,
                        contentColor = colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "[ CLOSE / SAVE ]",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionChip(
    label: String,
    isSelected: Boolean,
    colorScheme: TerminalColorScheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) colorScheme.promptColor else colorScheme.background
    val textColor = if (isSelected) colorScheme.background else colorScheme.textPrimary
    val borderColor = colorScheme.promptColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .background(bgColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        )
    }
}
