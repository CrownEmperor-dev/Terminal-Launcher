package com.example.terminallauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

enum class TerminalThemeType {
    CRT_GREEN,
    AMBER,
    OLED_BLACK,
    MONOCHROME,
    CYAN_NEON,
    GAMEBOY_DMG,
    SOLARIZED,
    DRACULA
}

enum class AsciiArtStyle {
    DEFAULT,
    BUGDROID,
    TUX,
    RETRO_PC,
    MINIMAL,
    CUSTOM,
    OFF
}

data class LauncherSettings(
    val theme: TerminalThemeType = TerminalThemeType.CRT_GREEN,
    val promptString: String = "user@mobile:~$ ",
    val fastfetchOnStart: Boolean = true,
    val autocompleteEnabled: Boolean = true,
    val statusBarVisible: Boolean = true,
    val lastFullChargeTimestamp: Long = 0L,
    val firstRunCompleted: Boolean = false,
    val aliases: Map<String, String> = mapOf("yt" to "YouTube", "cam" to "Camera", "notes" to "Keep"),
    val history: List<String> = emptyList(),
    val textSizeSp: Int = 15,
    val asciiArtStyle: AsciiArtStyle = AsciiArtStyle.DEFAULT,
    val customAsciiArt: String = ""
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("terminal_theme")
        val PROMPT_STRING = stringPreferencesKey("prompt_string")
        val FASTFETCH_ON_START = booleanPreferencesKey("fastfetch_on_start")
        val AUTOCOMPLETE_ENABLED = booleanPreferencesKey("autocomplete_enabled")
        val STATUS_BAR_VISIBLE = booleanPreferencesKey("status_bar_visible")
        val LAST_FULL_CHARGE_TIME = longPreferencesKey("last_full_charge_time")
        val FIRST_RUN_COMPLETED = booleanPreferencesKey("first_run_completed")
        val ALIASES_RAW = stringPreferencesKey("aliases_raw")
        val HISTORY_RAW = stringPreferencesKey("history_raw")
        val BUFFER_RAW = stringPreferencesKey("terminal_buffer_raw")
        val TEXT_SIZE_SP = intPreferencesKey("text_size_sp")
        val ASCII_ART_STYLE = stringPreferencesKey("ascii_art_style")
        val CUSTOM_ASCII_ART = stringPreferencesKey("custom_ascii_art")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME] ?: TerminalThemeType.CRT_GREEN.name
        val theme = try {
            TerminalThemeType.valueOf(themeName)
        } catch (_: Exception) {
            TerminalThemeType.CRT_GREEN
        }

        val prompt = preferences[PreferencesKeys.PROMPT_STRING] ?: "user@mobile:~$ "
        val fastfetch = preferences[PreferencesKeys.FASTFETCH_ON_START] ?: true
        val autocomplete = preferences[PreferencesKeys.AUTOCOMPLETE_ENABLED] ?: true
        val statusBar = preferences[PreferencesKeys.STATUS_BAR_VISIBLE] ?: true
        val lastCharge = preferences[PreferencesKeys.LAST_FULL_CHARGE_TIME] ?: 0L
        val firstRun = preferences[PreferencesKeys.FIRST_RUN_COMPLETED] ?: false

        val aliasesRaw = preferences[PreferencesKeys.ALIASES_RAW] ?: "yt=YouTube\ncam=Camera\nnotes=Keep"
        val aliasesMap = parseAliases(aliasesRaw)

        val historyRaw = preferences[PreferencesKeys.HISTORY_RAW] ?: ""
        val historyList = if (historyRaw.isBlank()) emptyList() else historyRaw.split("\n")

        val textSize = preferences[PreferencesKeys.TEXT_SIZE_SP] ?: 15
        val asciiStyleName = preferences[PreferencesKeys.ASCII_ART_STYLE] ?: AsciiArtStyle.DEFAULT.name
        val asciiStyle = try {
            AsciiArtStyle.valueOf(asciiStyleName)
        } catch (_: Exception) {
            AsciiArtStyle.DEFAULT
        }
        val customAscii = preferences[PreferencesKeys.CUSTOM_ASCII_ART] ?: ""

        LauncherSettings(
            theme = theme,
            promptString = prompt,
            fastfetchOnStart = fastfetch,
            autocompleteEnabled = autocomplete,
            statusBarVisible = statusBar,
            lastFullChargeTimestamp = lastCharge,
            firstRunCompleted = firstRun,
            aliases = aliasesMap,
            history = historyList,
            textSizeSp = textSize,
            asciiArtStyle = asciiStyle,
            customAsciiArt = customAscii
        )
    }

    val bufferFlow: Flow<List<TerminalLine>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.BUFFER_RAW] ?: ""
        parseBuffer(raw)
    }

    suspend fun saveTerminalBuffer(lines: List<TerminalLine>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUFFER_RAW] = serializeBuffer(lines)
        }
    }

    suspend fun clearTerminalBuffer() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUFFER_RAW] = ""
        }
    }

    suspend fun setTheme(theme: TerminalThemeType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun setPromptString(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROMPT_STRING] = prompt
        }
    }

    suspend fun setFastfetchOnStart(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FASTFETCH_ON_START] = enabled
        }
    }

    suspend fun setAutocompleteEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOCOMPLETE_ENABLED] = enabled
        }
    }

    suspend fun setStatusBarVisible(visible: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STATUS_BAR_VISIBLE] = visible
        }
    }

    suspend fun setLastFullChargeTime(timestampMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_FULL_CHARGE_TIME] = timestampMs
        }
    }

    suspend fun setFirstRunCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_RUN_COMPLETED] = completed
        }
    }

    suspend fun setTextSizeSp(textSizeSp: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_SIZE_SP] = textSizeSp
        }
    }

    suspend fun setAsciiArtStyle(style: AsciiArtStyle) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ASCII_ART_STYLE] = style.name
        }
    }

    suspend fun setCustomAsciiArt(art: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_ASCII_ART] = art
        }
    }

    suspend fun addAlias(aliasKey: String, target: String) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[PreferencesKeys.ALIASES_RAW] ?: "yt=YouTube\ncam=Camera\nnotes=Keep"
            val map = parseAliases(currentRaw).toMutableMap()
            map[aliasKey.trim().lowercase()] = target.trim()
            preferences[PreferencesKeys.ALIASES_RAW] = serializeAliases(map)
        }
    }

    suspend fun removeAlias(aliasKey: String) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[PreferencesKeys.ALIASES_RAW] ?: "yt=YouTube\ncam=Camera\nnotes=Keep"
            val map = parseAliases(currentRaw).toMutableMap()
            map.remove(aliasKey.trim().lowercase())
            preferences[PreferencesKeys.ALIASES_RAW] = serializeAliases(map)
        }
    }

    suspend fun addHistoryEntry(command: String) {
        if (command.isBlank()) return
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[PreferencesKeys.HISTORY_RAW] ?: ""
            val currentList = if (currentRaw.isBlank()) emptyList() else currentRaw.split("\n")
            val updated = (currentList + command.trim()).takeLast(100)
            preferences[PreferencesKeys.HISTORY_RAW] = updated.joinToString("\n")
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HISTORY_RAW] = ""
        }
    }

    private fun parseAliases(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                parts[0].trim().lowercase() to parts[1].trim()
            } else null
        }.toMap()
    }

    private fun serializeAliases(map: Map<String, String>): String {
        return map.entries.joinToString("\n") { "${it.key}=${it.value}" }
    }

    private fun parseBuffer(raw: String): List<TerminalLine> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\u0000").mapNotNull { entry ->
            val parts = entry.split("\u0001", limit = 2)
            if (parts.size == 2) {
                val type = try { LineType.valueOf(parts[0]) } catch (_: Exception) { LineType.OUTPUT }
                TerminalLine(text = parts[1], type = type)
            } else null
        }
    }

    private fun serializeBuffer(lines: List<TerminalLine>): String {
        return lines.takeLast(100).joinToString("\u0000") { "${it.type.name}\u0001${it.text}" }
    }
}
