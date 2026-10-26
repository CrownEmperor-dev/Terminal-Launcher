package com.example.terminallauncher.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
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
    val fontSize = uiState.settings.textSizeSp.sp
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    // First-run runtime permissions check
    val permissionsToRequest = remember {
        val list = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list
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

    // Dynamic Package Event BroadcastReceiver (Detect app installs & uninstalls)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                val pkgName = intent.data?.schemeSpecificPart ?: return
                when (action) {
                    Intent.ACTION_PACKAGE_REMOVED -> viewModel.onPackageEvent(pkgName, isRemoved = true)
                    Intent.ACTION_PACKAGE_ADDED -> viewModel.onPackageEvent(pkgName, isRemoved = false)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
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

    // Render Matrix Rain Easter Egg overlay if requested
    if (uiState.isMatrixActive) {
        MatrixRainScreen(
            onDismiss = { viewModel.closeMatrixMode() }
        )
        return
    }

    // Render GUI Options Dialog overlay if requested
    if (uiState.isOptionsMenuVisible) {
        OptionsMenuDialog(
            settings = uiState.settings,
            colorScheme = colorScheme,
            onDismiss = { viewModel.closeOptionsMenu() },
            onSetTheme = { viewModel.setTheme(it) },
            onSetTextSize = { viewModel.setTextSize(it) },
            onSetAsciiStyle = { viewModel.setAsciiStyle(it) },
            onSetCustomAscii = { viewModel.setCustomAsciiArt(it) },
            onSetFastfetch = { viewModel.setFastfetchOnStart(it) },
            onSetAutocomplete = { viewModel.setAutocompleteEnabled(it) },
            onSetPrompt = { viewModel.setPromptString(it) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusRequester.requestFocus()
            }
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Output Line Buffer - Wrapped in SelectionContainer for highlighting & copying!
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.lines,
                        key = { it.id }
                    ) { line ->
                        TerminalLineView(line = line, colorScheme = colorScheme, fontSize = fontSize)
                    }
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
                        fontSize = fontSize,
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
                                fontSize = fontSize,
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
                                if (event.isCtrlPressed && (event.key == Key.L)) {
                                    viewModel.clearScreen()
                                    true
                                } else if (event.isCtrlPressed && (event.key == Key.V)) {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrEmpty()) {
                                        viewModel.pasteText(clipText)
                                        true
                                    } else false
                                } else if (event.isCtrlPressed && (event.key == Key.C)) {
                                    if (uiState.inputFieldValue.text.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(uiState.inputFieldValue.text))
                                        true
                                    } else false
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
                                    Key.Backspace -> {
                                        if (uiState.ghostSuffix.isNotEmpty()) {
                                            viewModel.dismissGhostCompletion()
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
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
    colorScheme: TerminalColorScheme = getTerminalColorScheme(TerminalThemeType.CRT_GREEN),
    fontSize: TextUnit = 15.sp
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
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor
        ),
        modifier = Modifier
            .graphicsLayer()
            .padding(vertical = 1.dp)
    )
}
