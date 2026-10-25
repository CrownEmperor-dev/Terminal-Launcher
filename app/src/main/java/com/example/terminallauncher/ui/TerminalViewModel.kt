package com.example.terminallauncher.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.terminallauncher.data.AppRepository
import com.example.terminallauncher.data.AsciiArtStyle
import com.example.terminallauncher.data.CommandEngine
import com.example.terminallauncher.data.CommandResult
import com.example.terminallauncher.data.LauncherSettings
import com.example.terminallauncher.data.LineType
import com.example.terminallauncher.data.SettingsRepository
import com.example.terminallauncher.data.TerminalLine
import com.example.terminallauncher.data.TerminalThemeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val inputFieldValue: TextFieldValue = TextFieldValue(""),
    val ghostSuffix: String = "",
    val settings: LauncherSettings = LauncherSettings(),
    val historyIndex: Int = -1,
    val isFirstRunPromptActive: Boolean = false,
    val isOptionsMenuVisible: Boolean = false,
    val isMatrixActive: Boolean = false
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val commandEngine = CommandEngine(application, appRepository, settingsRepository)

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var temporaryInput: String = ""
    private var isBufferLoaded: Boolean = false

    init {
        viewModelScope.launch {
            settingsRepository.bufferFlow.collectLatest { savedBuffer ->
                if (!isBufferLoaded) {
                    isBufferLoaded = true
                    if (savedBuffer.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(lines = savedBuffer)
                    } else {
                        val initialLines = mutableListOf<TerminalLine>()
                        initialLines.add(TerminalLine(text = "Welcome to Terminal Launcher v1.0", type = LineType.HEADER))
                        initialLines.add(TerminalLine(text = "Type 'help' for documentation, 'ls' for app list, 'options' for GUI settings.", type = LineType.SYSTEM))
                        val settings = _uiState.value.settings
                        if (settings.fastfetchOnStart) {
                            initialLines.addAll(commandEngine.generateInfofetchLines(settings.asciiArtStyle))
                        }
                        _uiState.value = _uiState.value.copy(lines = initialLines)
                        settingsRepository.saveTerminalBuffer(initialLines)
                    }
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    fun openOptionsMenu() {
        _uiState.value = _uiState.value.copy(isOptionsMenuVisible = true)
    }

    fun closeOptionsMenu() {
        _uiState.value = _uiState.value.copy(isOptionsMenuVisible = false)
    }

    fun closeMatrixMode() {
        _uiState.value = _uiState.value.copy(isMatrixActive = false)
    }

    fun setTheme(theme: TerminalThemeType) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setTextSize(textSizeSp: Int) {
        viewModelScope.launch {
            settingsRepository.setTextSizeSp(textSizeSp)
        }
    }

    fun setAsciiStyle(style: AsciiArtStyle) {
        viewModelScope.launch {
            settingsRepository.setAsciiArtStyle(style)
        }
    }

    fun setFastfetchOnStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFastfetchOnStart(enabled)
        }
    }

    fun setAutocompleteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutocompleteEnabled(enabled)
        }
    }

    fun setPromptString(prompt: String) {
        viewModelScope.launch {
            settingsRepository.setPromptString(prompt)
        }
    }

    fun onInputChanged(newValue: TextFieldValue) {
        val text = newValue.text
        val autocomplete = _uiState.value.settings.autocompleteEnabled
        val textLower = text.lowercase()

        val ghost = if (autocomplete && text.isNotBlank()) {
            if (textLower.startsWith("uninstall ")) {
                val subQuery = text.substring("uninstall ".length)
                appRepository.getCompletionRemaining(subQuery) ?: ""
            } else if (textLower.startsWith("open ") || textLower.startsWith("launch ")) {
                val prefix = if (textLower.startsWith("open ")) "open " else "launch "
                val subQuery = text.substring(prefix.length)
                appRepository.getCompletionRemaining(subQuery) ?: ""
            } else {
                val appCompletion = appRepository.getCompletionRemaining(text)
                if (appCompletion != null) {
                    appCompletion
                } else {
                    getBuiltInCommandCompletion(text)
                }
            }
        } else {
            ""
        }

        _uiState.value = _uiState.value.copy(
            inputFieldValue = newValue,
            ghostSuffix = ghost
        )
    }

    fun acceptGhostCompletion() {
        val currentText = _uiState.value.inputFieldValue.text
        val suffix = _uiState.value.ghostSuffix
        if (suffix.isNotEmpty()) {
            val completedText = currentText + suffix
            _uiState.value = _uiState.value.copy(
                inputFieldValue = TextFieldValue(completedText, selection = TextRange(completedText.length)),
                ghostSuffix = ""
            )
        }
    }

    fun submitCommand() {
        val currentText = _uiState.value.inputFieldValue.text
        val ghostSuffix = _uiState.value.ghostSuffix
        // Direct single-keystroke execution when ghost autocomplete is active
        val input = if (ghostSuffix.isNotEmpty()) currentText + ghostSuffix else currentText

        val currentSettings = _uiState.value.settings
        val currentLines = _uiState.value.lines.toMutableList()

        // Append user prompt entry line
        currentLines.add(TerminalLine(text = "${currentSettings.promptString}$input", type = LineType.INPUT))

        // Reset input field and history traversal
        _uiState.value = _uiState.value.copy(
            inputFieldValue = TextFieldValue(""),
            ghostSuffix = "",
            historyIndex = -1,
            lines = currentLines
        )

        if (input.isBlank()) return

        viewModelScope.launch {
            settingsRepository.addHistoryEntry(input)
            val result: CommandResult = commandEngine.execute(input, currentSettings)

            if (result.openOptionsMenu) {
                _uiState.value = _uiState.value.copy(isOptionsMenuVisible = true)
            }

            if (result.openMatrixMode) {
                _uiState.value = _uiState.value.copy(isMatrixActive = true)
            }

            if (result.shouldClearBuffer) {
                _uiState.value = _uiState.value.copy(lines = emptyList())
                settingsRepository.clearTerminalBuffer()
            } else {
                val updatedLines = _uiState.value.lines.toMutableList()
                updatedLines.addAll(result.outputLines)
                _uiState.value = _uiState.value.copy(lines = updatedLines)
                settingsRepository.saveTerminalBuffer(updatedLines)
            }
        }
    }

    fun navigateHistoryUp() {
        val history = _uiState.value.settings.history
        if (history.isEmpty()) return

        val currentIndex = _uiState.value.historyIndex
        val newIndex = if (currentIndex == -1) {
            temporaryInput = _uiState.value.inputFieldValue.text
            history.size - 1
        } else {
            (currentIndex - 1).coerceAtLeast(0)
        }

        val historyCmd = history[newIndex]
        _uiState.value = _uiState.value.copy(
            historyIndex = newIndex,
            inputFieldValue = TextFieldValue(historyCmd, selection = TextRange(historyCmd.length)),
            ghostSuffix = ""
        )
    }

    fun navigateHistoryDown() {
        val history = _uiState.value.settings.history
        val currentIndex = _uiState.value.historyIndex
        if ((currentIndex == -1) || history.isEmpty()) return

        val newIndex = currentIndex + 1
        if (newIndex >= history.size) {
            _uiState.value = _uiState.value.copy(
                historyIndex = -1,
                inputFieldValue = TextFieldValue(temporaryInput, selection = TextRange(temporaryInput.length)),
                ghostSuffix = ""
            )
        } else {
            val historyCmd = history[newIndex]
            _uiState.value = _uiState.value.copy(
                historyIndex = newIndex,
                inputFieldValue = TextFieldValue(historyCmd, selection = TextRange(historyCmd.length)),
                ghostSuffix = ""
            )
        }
    }

    fun clearScreen() {
        _uiState.value = _uiState.value.copy(lines = emptyList())
        viewModelScope.launch {
            settingsRepository.clearTerminalBuffer()
        }
    }

    fun refreshApps() {
        appRepository.refreshApps()
    }

    private fun getBuiltInCommandCompletion(input: String): String {
        val completions = listOf(
            "help", "options", "matrix", "infofetch", "infofetch update",
            "fastfetch", "neofetch", "clear", "history", "config",
            "config set theme", "config set fastfetch", "config set prompt",
            "config set autocomplete", "config set size", "config set ascii",
            "settings", "top", "top memory", "battery", "battery status", "battery health",
            "calc", "sys", "sys flash", "sys wifi", "sys bt", "sys sound", "sys settings",
            "alias", "unalias", "open", "launch", "uninstall", "ls", "apps"
        )
        val inputLower = input.lowercase()
        val match = completions.firstOrNull { it.startsWith(inputLower) && it != inputLower }
        return match?.substring(input.length) ?: ""
    }
}
