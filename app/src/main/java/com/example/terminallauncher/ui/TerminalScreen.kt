package com.example.terminallauncher.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.terminallauncher.data.LineType
import com.example.terminallauncher.data.TerminalLine
import com.example.terminallauncher.data.TerminalThemeType
import com.example.terminallauncher.ui.theme.TerminalColorScheme
import com.example.terminallauncher.ui.theme.getTerminalColorScheme

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = remember(uiState.settings.theme) { getTerminalColorScheme(uiState.settings.theme) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // First-run runtime permissions check (Camera for flashlight, Call Phone for dialer)
    val permissionsToRequest = remember {
        listOf(Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshApps()
    }

    LaunchedEffect(Unit) {
        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // Auto-scroll list when new output lines arrive
    LaunchedEffect(uiState.lines.size) {
        if (uiState.lines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.lines.size - 1)
        }
    }

    // Auto-focus prompt input on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
            }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Output Line Buffer
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(uiState.lines) { line ->
                    TerminalLineView(line = line, colorScheme = colorScheme)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Command Prompt Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.settings.promptString,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.promptColor
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                Box(modifier = Modifier.weight(1f)) {
                    // Ghost text preview auto-complete overlay
                    val inputText = uiState.inputFieldValue.text
                    val ghostText = uiState.ghostSuffix

                    if (ghostText.isNotEmpty() && inputText.isNotEmpty()) {
                        Text(
                            text = inputText + ghostText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                color = colorScheme.textGhost
                            )
                        )
                    }

                    BasicTextField(
                        value = uiState.inputFieldValue,
                        onValueChange = { viewModel.onInputChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.isCtrlPressed && event.key == Key.L) {
                                    viewModel.clearScreen()
                                    true
                                } else when (event.key) {
                                    Key.DirectionUp -> {
                                        viewModel.navigateHistoryUp()
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        viewModel.navigateHistoryDown()
                                        true
                                    }
                                    Key.Tab -> {
                                        if (uiState.ghostSuffix.isNotEmpty()) {
                                            viewModel.acceptGhostCompletion()
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            color = colorScheme.textPrimary
                        ),
                        cursorBrush = SolidColor(colorScheme.promptColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.submitCommand() }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalLineView(
    line: TerminalLine,
    colorScheme: TerminalColorScheme = getTerminalColorScheme(TerminalThemeType.CRT_GREEN)
) {
    val textColor = when (line.type) {
        LineType.INPUT -> colorScheme.textPrimary
        LineType.OUTPUT -> colorScheme.textSecondary
        LineType.ERROR -> colorScheme.errorColor
        LineType.SYSTEM -> colorScheme.systemColor
        LineType.SUCCESS -> colorScheme.successColor
        LineType.HEADER -> colorScheme.headerColor
    }

    val fontWeight = when (line.type) {
        LineType.HEADER, LineType.INPUT -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    Text(
        text = line.text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            color = textColor
        ),
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
