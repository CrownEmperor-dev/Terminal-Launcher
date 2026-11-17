package com.example.terminallauncher.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.terminallauncher.data.WallpaperScaleMode
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TerminalUiState(
    val lines: List<TerminalLine> = emptyList(),
    val inputFieldValue: TextFieldValue = TextFieldValue(""),
    val ghostSuffix: String = "",
    val settings: LauncherSettings = LauncherSettings(),
    val historyIndex: Int = -1,
    val isFirstRunPromptActive: Boolean = false,
    val isOptionsMenuVisible: Boolean = false,
    val isMatrixActive: Boolean = false,
    val openWallpaperPicker: Boolean = false,
    val wallpaperBitmap: ImageBitmap? = null
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val commandEngine = CommandEngine(application, appRepository, settingsRepository)

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var temporaryInput: String = ""
    private var isBufferLoaded: Boolean = false
    private var suppressedInput: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.bufferFlow.collectLatest { savedBuffer ->
                if (!isBufferLoaded) {
                    isBufferLoaded = true
                    if (savedBuffer.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(lines = savedBuffer)
                    } else {
                        val initialLines = mutableListOf<TerminalLine>()
                        initialLines.add(TerminalLine(text = "Welcome to Terminal Launcher v1.5.0", type = LineType.HEADER))
                        initialLines.add(TerminalLine(text = "Type 'help' for documentation, 'ls' for app list, 'options' for GUI settings.", type = LineType.SYSTEM))
                        val settings = _uiState.value.settings
                        if (settings.fastfetchOnStart) {
                            initialLines.addAll(commandEngine.generateInfofetchLines(settings))
                        }
                        _uiState.value = _uiState.value.copy(lines = initialLines)
                        settingsRepository.saveTerminalBuffer(initialLines)
                    }
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                val wallpaperFile = File(getApplication<Application>().filesDir, "wallpaper.png")
                val shouldLoad = settings.wallpaperEnabled || (wallpaperFile.exists() && wallpaperFile.length() > 0 && settings.wallpaperUri.isNotEmpty())
                val loadedBitmap = if (shouldLoad) loadWallpaperBitmap(settings.wallpaperBlur) else null
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    wallpaperBitmap = loadedBitmap
                )
            }
        }
    }

    private suspend fun loadWallpaperBitmap(blurRadius: Int = _uiState.value.settings.wallpaperBlur): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val file = File(context.filesDir, "wallpaper.png")
                if (!file.exists() || file.length() == 0L) return@withContext null

                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                var bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(file)
                    bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
                if (bitmap != null) {
                    val processed = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && blurRadius > 0) {
                        applyFastBlur(bitmap, blurRadius)
                    } else bitmap
                    processed.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun applyFastBlur(source: Bitmap, blurRadius: Int): Bitmap {
        if (blurRadius <= 0) return source
        val radius = blurRadius.coerceIn(1, 20)
        val scaleFactor = (radius / 2).coerceIn(2, 6)
        val w = (source.width / scaleFactor).coerceAtLeast(1)
        val h = (source.height / scaleFactor).coerceAtLeast(1)
        val smallBitmap = Bitmap.createScaledBitmap(source, w, h, true)

        val pix = IntArray(w * h)
        smallBitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val div = radius + radius + 1

        val r = IntArray(w * h)
        val g = IntArray(w * h)
        val b = IntArray(w * h)

        // Pass 1: Horizontal Blur
        var yw = 0
        var yi = 0
        for (_y in 0 until h) {
            var rsum = 0
            var gsum = 0
            var bsum = 0

            for (i in -radius..radius) {
                val p = pix[yw + Math.min(wm, Math.max(i, 0))]
                rsum += (p shr 16) and 0xff
                gsum += (p shr 8) and 0xff
                bsum += p and 0xff
            }

            for (x in 0 until w) {
                r[yi] = rsum / div
                g[yi] = gsum / div
                b[yi] = bsum / div

                val p1 = pix[yw + Math.min(x + radius + 1, wm)]
                val p2 = pix[yw + Math.max(x - radius, 0)]

                rsum += ((p1 shr 16) and 0xff) - ((p2 shr 16) and 0xff)
                gsum += ((p1 shr 8) and 0xff) - ((p2 shr 8) and 0xff)
                bsum += (p1 and 0xff) - (p2 and 0xff)
                yi++
            }
            yw += w
        }

        // Pass 2: Vertical Blur
        for (x in 0 until w) {
            var rsum = 0
            var gsum = 0
            var bsum = 0

            for (i in -radius..radius) {
                val idx = Math.min(hm, Math.max(i, 0)) * w + x
                rsum += r[idx]
                gsum += g[idx]
                bsum += b[idx]
            }

            for (y in 0 until h) {
                val idx = y * w + x
                val rAvg = rsum / div
                val gAvg = gsum / div
                val bAvg = bsum / div

                pix[idx] = (0xff000000.toInt()) or (rAvg shl 16) or (gAvg shl 8) or bAvg

                val p1 = Math.min(y + radius + 1, hm) * w + x
                val p2 = Math.max(y - radius, 0) * w + x

                rsum += r[p1] - r[p2]
                gsum += g[p1] - g[p2]
                bsum += b[p1] - b[p2]
            }
        }

        val blurredBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        blurredBitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return blurredBitmap
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

    fun setCustomAsciiArt(art: String) {
        viewModelScope.launch {
            settingsRepository.setCustomAsciiArt(art)
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

    fun setStatusBarVisible(visible: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStatusBarVisible(visible)
        }
    }

    fun setAutoShowKeyboard(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoShowKeyboard(enabled)
        }
    }

    fun setPromptString(prompt: String) {
        viewModelScope.launch {
            settingsRepository.setPromptString(prompt)
        }
    }

    fun sendFeedbackCommand() {
        viewModelScope.launch {
            val result = commandEngine.execute("feedback github", _uiState.value.settings)
            val updatedLines = _uiState.value.lines.toMutableList()
            updatedLines.addAll(result.outputLines)
            _uiState.value = _uiState.value.copy(lines = updatedLines)
            settingsRepository.saveTerminalBuffer(updatedLines)
        }
    }

    fun sendEmailFeedbackCommand() {
        viewModelScope.launch {
            val result = commandEngine.execute("feedback email", _uiState.value.settings)
            val updatedLines = _uiState.value.lines.toMutableList()
            updatedLines.addAll(result.outputLines)
            _uiState.value = _uiState.value.copy(lines = updatedLines)
            settingsRepository.saveTerminalBuffer(updatedLines)
        }
    }

    fun dismissGhostCompletion() {
        suppressedInput = _uiState.value.inputFieldValue.text
        _uiState.value = _uiState.value.copy(ghostSuffix = "")
    }

    fun onInputChanged(newValue: TextFieldValue) {
        val text = newValue.text
        val autocomplete = _uiState.value.settings.autocompleteEnabled
        val textLower = text.lowercase()

        val ghost = if (autocomplete && text.isNotBlank() && text != suppressedInput) {
            suppressedInput = null
            if (textLower.startsWith("uninstall ")) {
                val subQuery = text.substring("uninstall ".length)
                appRepository.getCompletionRemaining(subQuery) ?: ""
            } else if (textLower.startsWith("open ") || textLower.startsWith("launch ")) {
                val prefix = if (textLower.startsWith("open ")) "open " else "launch "
                val subQuery = text.substring(prefix.length)
                appRepository.getCompletionRemaining(subQuery) ?: ""
            } else if (textLower.startsWith("echo ")) {
                ""
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

    fun pasteText(pasted: String) {
        val current = _uiState.value.inputFieldValue
        val text = current.text
        val selection = current.selection
        val newText = if (selection.collapsed) {
            val idx = selection.start.coerceIn(0, text.length)
            text.substring(0, idx) + pasted + text.substring(idx)
        } else {
            val start = selection.min.coerceIn(0, text.length)
            val end = selection.max.coerceIn(0, text.length)
            text.substring(0, start) + pasted + text.substring(end)
        }
        val newSelectionIndex = (selection.min + pasted.length).coerceIn(0, newText.length)
        onInputChanged(TextFieldValue(newText, selection = TextRange(newSelectionIndex)))
    }

    fun submitCommand() {
        val currentText = _uiState.value.inputFieldValue.text
        val ghostSuffix = _uiState.value.ghostSuffix
        val input = if (ghostSuffix.isNotEmpty()) currentText + ghostSuffix else currentText

        val currentSettings = _uiState.value.settings
        val currentLines = _uiState.value.lines.toMutableList()

        currentLines.add(TerminalLine(text = "${currentSettings.promptString}$input", type = LineType.INPUT))

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

            if (result.openWallpaperPicker) {
                _uiState.value = _uiState.value.copy(openWallpaperPicker = true)
            }

            if (result.updatedPrompt != null) {
                settingsRepository.setPromptString(result.updatedPrompt)
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

    fun triggerWallpaperPicker() {
        _uiState.value = _uiState.value.copy(openWallpaperPicker = true)
    }

    fun onWallpaperPickerDismissed() {
        _uiState.value = _uiState.value.copy(openWallpaperPicker = false)
    }

    fun setWallpaperEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWallpaperEnabled(enabled)
        }
    }

    fun setWallpaperDim(dim: Int) {
        viewModelScope.launch {
            settingsRepository.setWallpaperDim(dim)
        }
    }

    fun setWallpaperBlur(blur: Int) {
        viewModelScope.launch {
            settingsRepository.setWallpaperBlur(blur)
            val loadedBitmap = loadWallpaperBitmap(blur)
            _uiState.value = _uiState.value.copy(wallpaperBitmap = loadedBitmap)
        }
    }

    fun setWallpaperScale(scale: WallpaperScaleMode) {
        viewModelScope.launch {
            settingsRepository.setWallpaperScale(scale)
        }
    }

    fun clearWallpaper() {
        viewModelScope.launch {
            val file = File(getApplication<Application>().filesDir, "wallpaper.png")
            if (file.exists()) file.delete()
            settingsRepository.setWallpaperEnabled(false)
            settingsRepository.setWallpaperUri("")
            _uiState.value = _uiState.value.copy(wallpaperBitmap = null)
        }
    }

    fun onWallpaperImagePicked(uri: Uri?) {
        _uiState.value = _uiState.value.copy(
            openWallpaperPicker = false,
            isOptionsMenuVisible = false
        )

        val updatedLines = _uiState.value.lines.toMutableList()
        if (uri == null) {
            updatedLines.add(TerminalLine(text = "[WALLPAPER DEBUG]: Gallery returned null URI (picker cancelled or unsupported provider).", type = LineType.ERROR))
            _uiState.value = _uiState.value.copy(lines = updatedLines)
            viewModelScope.launch { settingsRepository.saveTerminalBuffer(updatedLines) }
            return
        }

        updatedLines.add(TerminalLine(text = "[WALLPAPER DEBUG]: Received URI = $uri", type = LineType.SYSTEM))
        _uiState.value = _uiState.value.copy(lines = updatedLines)

        viewModelScope.launch {
            val context = getApplication<Application>()
            var stepLog = ""
            var errorMsg: String? = null

            val success = withContext(Dispatchers.IO) {
                try {
                    val destFile = File(context.filesDir, "wallpaper.png")
                    val tempFile = File(context.filesDir, "wallpaper_temp")
                    if (destFile.exists()) destFile.delete()
                    if (tempFile.exists()) tempFile.delete()

                    stepLog = "Step 1: Reading InputStream from ContentResolver"
                    val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: 0L

                    if (copied <= 0L || !tempFile.exists()) {
                        errorMsg = "Step 1 Failed: Copied 0 bytes from URI"
                        return@withContext false
                    }

                    stepLog = "Step 2: Stream copied (${copied / 1024} KB). Decoding bitmap..."

                    val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            val source = ImageDecoder.createSource(tempFile)
                            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                val maxDim = 2048
                                val size = info.size
                                if (size.width > maxDim || size.height > maxDim) {
                                    val scale = (Math.max(size.width, size.height).toFloat() / maxDim.toFloat())
                                    val targetWidth = (size.width / scale).toInt().coerceAtLeast(1)
                                    val targetHeight = (size.height / scale).toInt().coerceAtLeast(1)
                                    decoder.setTargetSize(targetWidth, targetHeight)
                                }
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            }
                        } catch (e: Exception) {
                            stepLog += " (ImageDecoder: ${e.localizedMessage}, using BitmapFactory)"
                            BitmapFactory.decodeFile(tempFile.absolutePath)
                        }
                    } else {
                        BitmapFactory.decodeFile(tempFile.absolutePath)
                    }

                    if (bitmap != null) {
                        stepLog += " Bitmap decoded (${bitmap.width}x${bitmap.height}). Saving PNG..."
                        FileOutputStream(destFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }
                        bitmap.recycle()
                        if (tempFile.exists()) tempFile.delete()
                        destFile.exists() && destFile.length() > 0
                    } else {
                        stepLog += " Bitmap decode returned null. Moving temp file directly..."
                        tempFile.renameTo(destFile)
                        destFile.exists() && destFile.length() > 0
                    }
                } catch (e: Exception) {
                    errorMsg = "Exception at [$stepLog]: ${e.localizedMessage}"
                    false
                }
            }

            val finalLines = _uiState.value.lines.toMutableList()
            if (success) {
                settingsRepository.updateWallpaper(uri = "wallpaper.png", enabled = true)
                val loadedBitmap = loadWallpaperBitmap()
                _uiState.value = _uiState.value.copy(
                    wallpaperBitmap = loadedBitmap,
                    isOptionsMenuVisible = false
                )
                finalLines.add(TerminalLine(text = "[WALLPAPER ENGINE]: Custom background image installed successfully!", type = LineType.SUCCESS))
                finalLines.add(TerminalLine(text = "[WALLPAPER DEBUG]: $stepLog", type = LineType.SYSTEM))
                finalLines.add(TerminalLine(text = "[WALLPAPER DEBUG]: Memory bitmap status = ${if (loadedBitmap != null) "LOADED (${loadedBitmap.width}x${loadedBitmap.height})" else "NULL"}", type = LineType.SYSTEM))
            } else {
                finalLines.add(TerminalLine(text = "[WALLPAPER ERROR]: Import failed. Detail: ${errorMsg ?: stepLog}", type = LineType.ERROR))
            }
            _uiState.value = _uiState.value.copy(lines = finalLines)
            settingsRepository.saveTerminalBuffer(finalLines)
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

    fun onPackageEvent(packageName: String, isRemoved: Boolean) {
        refreshApps()
        val text = if (isRemoved) {
            "[APP UNINSTALLED]: $packageName removed successfully."
        } else {
            "[NEW APP INSTALLED]: $packageName detected."
        }
        val line = TerminalLine(text = text, type = LineType.SUCCESS)
        val updatedLines = _uiState.value.lines.toMutableList().apply { add(line) }
        _uiState.value = _uiState.value.copy(lines = updatedLines)
        viewModelScope.launch {
            settingsRepository.saveTerminalBuffer(updatedLines)
        }
    }

    private fun getBuiltInCommandCompletion(input: String): String {
        val completions = listOf(
            "help", "options", "about", "version", "whatsnew", "changelog", "time", "date", "killall", "trim",
            "reboot", "restart", "shutdown", "poweroff", "grep", "grep -i", "grep -n", "grep -c", "grep -r",
            "echo", "search", "google", "feedback", "feedback github", "feedback email",
            "report", "report github", "report email", "matrix", "infofetch", "infofetch update",
            "fastfetch", "neofetch", "clear", "history", "config",
            "config set theme", "config set wallpaper", "config set fastfetch", "config set prompt",
            "config set autocomplete", "config set statusbar", "config set size", "config set ascii",
            "wallpaper", "wallpaper pick", "wallpaper on", "wallpaper off", "wallpaper dim", "wallpaper blur", "wallpaper scale", "wallpaper clear",
            "settings", "top", "top memory", "battery", "battery status", "battery health",
            "calc", "sys", "sys flash", "sys wifi", "sys bt", "sys sound", "sys statusbar", "sys settings",
            "alias", "unalias", "open", "launch", "install", "uninstall", "cat", "read", "8bit", "8bit list", "tune", "tune list", "email", "mail", "ls", "apps"
        )
        val inputLower = input.lowercase()
        val match = completions.firstOrNull { it.startsWith(inputLower) && it != inputLower }
        return match?.substring(input.length) ?: ""
    }
}
