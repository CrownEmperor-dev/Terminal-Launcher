package com.example.terminallauncher.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.util.Locale
import kotlin.math.pow

enum class LineType {
    INPUT,
    OUTPUT,
    ERROR,
    SYSTEM,
    SUCCESS,
    HEADER
}

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT
)

data class CommandResult(
    val outputLines: List<TerminalLine> = emptyList(),
    val shouldClearBuffer: Boolean = false,
    val launchedApp: AppModel? = null,
    val success: Boolean = true
)

class CommandEngine(
    private val context: Context,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) {
    private var currentPath: String = "~"
    private var isFlashlightOn: Boolean = false

    suspend fun execute(commandInput: String, currentSettings: LauncherSettings): CommandResult {
        val trimmed = commandInput.trim()
        if (trimmed.isEmpty()) return CommandResult()

        // 1. Check if input matches an alias first
        val resolvedCommand = currentSettings.aliases[trimmed.lowercase()] ?: trimmed
        val tokens = resolvedCommand.split(Regex("\\s+"))
        val cmd = tokens.first().lowercase()
        val args = tokens.drop(1)

        return when (cmd) {
            "help", "man" -> handleHelp(args)
            "clear" -> CommandResult(shouldClearBuffer = true)
            "ls" -> handleLs(args, currentSettings)
            "cd" -> handleCd(args)
            "apps" -> handleAppsList()
            "open", "launch" -> handleOpenApp(args.joinToString(" "))
            "alias" -> handleAlias(args, currentSettings)
            "unalias" -> handleUnalias(args, currentSettings)
            "history" -> handleHistory(currentSettings)
            "config", "settings" -> handleConfig(args, currentSettings)
            "fastfetch", "neofetch" -> CommandResult(outputLines = generateFastfetchLines())
            "top", "htop" -> CommandResult(outputLines = generateTopLines())
            "battery" -> CommandResult(outputLines = generateBatteryLines())
            "calc" -> handleCalc(args.joinToString(" "))
            "sys", "toggle" -> handleSys(args)
            "call" -> handleCall(args.joinToString(" "))
            "sms" -> handleSms(args.joinToString(" "))
            "reboot", "shutdown" -> handleReboot()
            else -> handleFallback(resolvedCommand, currentSettings)
        }
    }

    fun generateFastfetchLines(): List<TerminalLine> {
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val ramInfo = getRamUsageInfo()
        val batInfo = getBatteryStatusInfo()
        val uptime = getUptimeString()

        val asciiArt = listOf(
            "  _____                   _             _ ",
            " |_   _|___ _ __ _ __ ___ (_)_ __   __ _| |",
            "   | |/ _ \\ '__| '_ ` _ \\| | '_ \\ / _` | |",
            "   | |  __/ |  | | | | | | | | | | (_| | |",
            "   |_|\\___|_|  |_| |_| |_|_|_| |_|\\__,_|_|"
        )

        val infoDetails = listOf(
            "OS: $osVersion",
            "Host: $model",
            "Kernel: Linux ${System.getProperty("os.version") ?: "Android"}",
            "Arch: $cpuArch",
            "Uptime: $uptime",
            "RAM: $ramInfo",
            "Battery: $batInfo",
            "Shell: TerminalLauncher v1.0"
        )

        val result = mutableListOf<TerminalLine>()
        result.add(TerminalLine("--------------------------------------------------", LineType.SYSTEM))
        asciiArt.forEach { result.add(TerminalLine(it, LineType.HEADER)) }
        result.add(TerminalLine("--------------------------------------------------", LineType.SYSTEM))
        infoDetails.forEach { result.add(TerminalLine(it, LineType.OUTPUT)) }
        result.add(TerminalLine("--------------------------------------------------", LineType.SYSTEM))
        return result
    }

    private fun handleHelp(args: List<String>): CommandResult {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine("Terminal Launcher v1.0 - Shell Manual", LineType.HEADER))
        lines.add(TerminalLine("==================================================", LineType.SYSTEM))
        lines.add(TerminalLine("CORE COMMANDS:", LineType.HEADER))
        lines.add(TerminalLine("  ls [dir]          List installed applications, system tools, or aliases", LineType.OUTPUT))
        lines.add(TerminalLine("  open <app>        Launch an app by name or package (or type app name directly)", LineType.OUTPUT))
        lines.add(TerminalLine("  alias [k=v]       Set custom command shortcut (e.g., alias yt=YouTube)", LineType.OUTPUT))
        lines.add(TerminalLine("  unalias <key>     Remove a custom alias", LineType.OUTPUT))
        lines.add(TerminalLine("  clear             Clear current terminal screen", LineType.OUTPUT))
        lines.add(TerminalLine("  history           Show command execution history", LineType.OUTPUT))
        lines.add(TerminalLine("  help / man        Display this help documentation", LineType.OUTPUT))
        lines.add(TerminalLine("", LineType.OUTPUT))
        lines.add(TerminalLine("SETTINGS & PREFERENCES:", LineType.HEADER))
        lines.add(TerminalLine("  config                     Show current launcher preferences", LineType.OUTPUT))
        lines.add(TerminalLine("  config set theme <t>       Set theme: green, amber, oled, mono", LineType.OUTPUT))
        lines.add(TerminalLine("  config set fastfetch <on|off> Toggle startup system banner", LineType.OUTPUT))
        lines.add(TerminalLine("  config set prompt <text>   Customize shell prompt string", LineType.OUTPUT))
        lines.add(TerminalLine("  config set autocomplete <on|off> Toggle ghost auto-completion", LineType.OUTPUT))
        lines.add(TerminalLine("", LineType.OUTPUT))
        lines.add(TerminalLine("SYSTEM & UTILITIES:", LineType.HEADER))
        lines.add(TerminalLine("  fastfetch / neofetch System overview and ASCII hardware banner", LineType.OUTPUT))
        lines.add(TerminalLine("  top / htop           Display RAM and system resource usage", LineType.OUTPUT))
        lines.add(TerminalLine("  battery              Show detailed power and battery health", LineType.OUTPUT))
        lines.add(TerminalLine("  calc <expr>          Solve inline math expressions (e.g. calc (12*4)/3)", LineType.OUTPUT))
        lines.add(TerminalLine("  sys flash            Toggle flashlight/torch hardware", LineType.OUTPUT))
        lines.add(TerminalLine("  sys wifi / bt        Open Wi-Fi / Bluetooth settings", LineType.OUTPUT))
        lines.add(TerminalLine("  call <number>        Launch phone dialer or direct call", LineType.OUTPUT))
        lines.add(TerminalLine("  sms <number>         Launch SMS messaging", LineType.OUTPUT))
        lines.add(TerminalLine("==================================================", LineType.SYSTEM))
        return CommandResult(outputLines = lines)
    }

    private fun handleLs(args: List<String>, settings: LauncherSettings): CommandResult {
        val target = args.firstOrNull()?.lowercase() ?: currentPath
        val lines = mutableListOf<TerminalLine>()

        when (target) {
            "/aliases", "aliases" -> {
                lines.add(TerminalLine("CONFIGURED ALIASES:", LineType.HEADER))
                if (settings.aliases.isEmpty()) {
                    lines.add(TerminalLine("  (no aliases defined. Use 'alias key=value')", LineType.OUTPUT))
                } else {
                    settings.aliases.forEach { (k, v) ->
                        lines.add(TerminalLine("  $k -> $v", LineType.OUTPUT))
                    }
                }
            }
            "/system", "system" -> {
                lines.add(TerminalLine("SYSTEM COMMANDS:", LineType.HEADER))
                val sysCmds = listOf("fastfetch", "top", "battery", "calc", "sys flash", "sys wifi", "sys bt", "call", "sms", "config")
                sysCmds.forEach { lines.add(TerminalLine("  $it", LineType.OUTPUT)) }
            }
            else -> {
                val apps = appRepository.getApps()
                lines.add(TerminalLine("INSTALLED APPLICATIONS (${apps.size}):", LineType.HEADER))
                apps.chunked(3).forEach { row ->
                    val rowText = row.joinToString("   ") { app ->
                        val alias = settings.aliases.entries.firstOrNull { it.value.equals(app.label, ignoreCase = true) }?.key
                        if (alias != null) "${app.label} [$alias]" else app.label
                    }
                    lines.add(TerminalLine(rowText, LineType.OUTPUT))
                }
            }
        }
        return CommandResult(outputLines = lines)
    }

    private fun handleCd(args: List<String>): CommandResult {
        val path = args.firstOrNull() ?: "~"
        currentPath = when (path) {
            "/", "~" -> "~"
            "apps", "/apps" -> "/apps"
            "system", "/system" -> "/system"
            "aliases", "/aliases" -> "/aliases"
            else -> return CommandResult(outputLines = listOf(TerminalLine("cd: no such directory: $path", LineType.ERROR)), success = false)
        }
        return CommandResult(outputLines = listOf(TerminalLine("Changed directory to $currentPath", LineType.SUCCESS)))
    }

    private fun handleAppsList(): CommandResult {
        return handleLs(listOf("/apps"), LauncherSettings())
    }

    private fun handleOpenApp(query: String): CommandResult {
        if (query.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine("Usage: open <app_name|package>", LineType.ERROR)), success = false)
        }
        val app = appRepository.findApp(query)
        return if (app != null) {
            val launched = appRepository.launchApp(app)
            if (launched) {
                CommandResult(
                    outputLines = listOf(TerminalLine("Launching ${app.label}...", LineType.SUCCESS)),
                    launchedApp = app
                )
            } else {
                CommandResult(outputLines = listOf(TerminalLine("Failed to launch ${app.label}", LineType.ERROR)), success = false)
            }
        } else {
            CommandResult(outputLines = listOf(TerminalLine("App not found: '$query'", LineType.ERROR)), success = false)
        }
    }

    private suspend fun handleAlias(args: List<String>, settings: LauncherSettings): CommandResult {
        if (args.isEmpty()) {
            return handleLs(listOf("/aliases"), settings)
        }

        val fullArg = args.joinToString(" ")
        val parts = fullArg.split("=", limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine("Usage: alias <key>=<target>", LineType.ERROR)), success = false)
        }

        val key = parts[0].trim().lowercase()
        val target = parts[1].trim()
        settingsRepository.addAlias(key, target)
        return CommandResult(outputLines = listOf(TerminalLine("Alias added: $key -> $target", LineType.SUCCESS)))
    }

    private suspend fun handleUnalias(args: List<String>, settings: LauncherSettings): CommandResult {
        val key = args.firstOrNull()?.trim()?.lowercase()
        if (key.isNullOrBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine("Usage: unalias <key>", LineType.ERROR)), success = false)
        }
        settingsRepository.removeAlias(key)
        return CommandResult(outputLines = listOf(TerminalLine("Alias removed: $key", LineType.SUCCESS)))
    }

    private fun handleHistory(settings: LauncherSettings): CommandResult {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine("COMMAND HISTORY:", LineType.HEADER))
        if (settings.history.isEmpty()) {
            lines.add(TerminalLine("  (no command history)", LineType.OUTPUT))
        } else {
            settings.history.forEachIndexed { idx, cmd ->
                lines.add(TerminalLine("  ${idx + 1}  $cmd", LineType.OUTPUT))
            }
        }
        return CommandResult(outputLines = lines)
    }

    private suspend fun handleConfig(args: List<String>, settings: LauncherSettings): CommandResult {
        if (args.isEmpty()) {
            val lines = listOf(
                TerminalLine("LAUNCHER CONFIGURATION:", LineType.HEADER),
                TerminalLine("  theme          : ${settings.theme.name.lowercase()}", LineType.OUTPUT),
                TerminalLine("  fastfetch      : ${if (settings.fastfetchOnStart) "on" else "off"}", LineType.OUTPUT),
                TerminalLine("  prompt         : '${settings.promptString}'", LineType.OUTPUT),
                TerminalLine("  autocomplete   : ${if (settings.autocompleteEnabled) "on" else "off"}", LineType.OUTPUT),
                TerminalLine("--------------------------------------------------", LineType.SYSTEM),
                TerminalLine("Usage: config set <theme|fastfetch|prompt|autocomplete> <value>", LineType.SYSTEM)
            )
            return CommandResult(outputLines = lines)
        }

        if (args.size >= 3 && args[0].lowercase() == "set") {
            val option = args[1].lowercase()
            val value = args.drop(2).joinToString(" ")

            return when (option) {
                "theme" -> {
                    val themeEnum = when (value.lowercase()) {
                        "green", "crt_green" -> TerminalThemeType.CRT_GREEN
                        "amber" -> TerminalThemeType.AMBER
                        "oled", "black" -> TerminalThemeType.OLED_BLACK
                        "mono", "monochrome" -> TerminalThemeType.MONOCHROME
                        else -> null
                    }
                    if (themeEnum != null) {
                        settingsRepository.setTheme(themeEnum)
                        CommandResult(outputLines = listOf(TerminalLine("Theme set to ${themeEnum.name.lowercase()}", LineType.SUCCESS)))
                    } else {
                        CommandResult(outputLines = listOf(TerminalLine("Invalid theme. Options: green, amber, oled, mono", LineType.ERROR)), success = false)
                    }
                }
                "fastfetch", "neofetch" -> {
                    val enabled = value.lowercase() in listOf("on", "true", "1", "yes")
                    settingsRepository.setFastfetchOnStart(enabled)
                    CommandResult(outputLines = listOf(TerminalLine("Startup fastfetch set to ${if (enabled) "on" else "off"}", LineType.SUCCESS)))
                }
                "prompt" -> {
                    val promptStr = if (value.endsWith(" ")) value else "$value "
                    settingsRepository.setPromptString(promptStr)
                    CommandResult(outputLines = listOf(TerminalLine("Prompt string set to '$promptStr'", LineType.SUCCESS)))
                }
                "autocomplete" -> {
                    val enabled = value.lowercase() in listOf("on", "true", "1", "yes")
                    settingsRepository.setAutocompleteEnabled(enabled)
                    CommandResult(outputLines = listOf(TerminalLine("Ghost auto-complete set to ${if (enabled) "on" else "off"}", LineType.SUCCESS)))
                }
                else -> CommandResult(outputLines = listOf(TerminalLine("Unknown config key: $option", LineType.ERROR)), success = false)
            }
        }

        return CommandResult(outputLines = listOf(TerminalLine("Usage: config set <option> <value>", LineType.ERROR)), success = false)
    }

    private fun handleCalc(expression: String): CommandResult {
        if (expression.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine("Usage: calc <expression> (e.g. calc 15 * 4.5)", LineType.ERROR)), success = false)
        }
        return try {
            val result = evaluateMathExpression(expression)
            CommandResult(outputLines = listOf(TerminalLine("$expression = $result", LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine("Math evaluation error: ${e.message}", LineType.ERROR)), success = false)
        }
    }

    private fun handleSys(args: List<String>): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: ""
        return when (action) {
            "flash", "flashlight", "torch" -> toggleFlashlight()
            "wifi" -> {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine("Opened Wi-Fi Settings", LineType.SUCCESS)))
            }
            "bt", "bluetooth" -> {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine("Opened Bluetooth Settings", LineType.SUCCESS)))
            }
            "silent", "sound" -> {
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine("Opened Sound Settings", LineType.SUCCESS)))
            }
            else -> CommandResult(outputLines = listOf(TerminalLine("Usage: sys <flash|wifi|bt|sound>", LineType.ERROR)), success = false)
        }
    }

    private fun toggleFlashlight(): CommandResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId, isFlashlightOn)
                CommandResult(outputLines = listOf(TerminalLine("Flashlight turned ${if (isFlashlightOn) "ON" else "OFF"}", LineType.SUCCESS)))
            } else {
                CommandResult(outputLines = listOf(TerminalLine("No camera flash hardware available", LineType.ERROR)), success = false)
            }
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine("Camera permission or hardware error: ${e.message}", LineType.ERROR)), success = false)
        }
    }

    private fun handleCall(number: String): CommandResult {
        if (number.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine("Usage: call <number_or_contact>", LineType.ERROR)), success = false)
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return CommandResult(outputLines = listOf(TerminalLine("Dialing $number...", LineType.SUCCESS)))
    }

    private fun handleSms(number: String): CommandResult {
        if (number.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine("Usage: sms <number>", LineType.ERROR)), success = false)
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return CommandResult(outputLines = listOf(TerminalLine("Opening SMS to $number...", LineType.SUCCESS)))
    }

    private fun handleReboot(): CommandResult {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            CommandResult(outputLines = listOf(TerminalLine("Opened Power / Battery menu", LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine("Unable to trigger power action", LineType.ERROR)), success = false)
        }
    }

    private fun handleFallback(input: String, settings: LauncherSettings): CommandResult {
        // Direct app search or launch
        val app = appRepository.findApp(input)
        if (app != null) {
            val launched = appRepository.launchApp(app)
            return if (launched) {
                CommandResult(
                    outputLines = listOf(TerminalLine("Launching ${app.label}...", LineType.SUCCESS)),
                    launchedApp = app
                )
            } else {
                CommandResult(outputLines = listOf(TerminalLine("Failed to launch ${app.label}", LineType.ERROR)), success = false)
            }
        }

        return CommandResult(
            outputLines = listOf(
                TerminalLine("command not found: $input", LineType.ERROR),
                TerminalLine("Type 'help' for available commands or 'ls' for app list.", LineType.SYSTEM)
            ),
            success = false
        )
    }

    private fun getRamUsageInfo(): String {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedMb = totalMb - availMb
        return "${usedMb}MB / ${totalMb}MB (${(usedMb * 100 / totalMb)}%)"
    }

    private fun getBatteryStatusInfo(): String {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 0

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return "$pct% ${if (isCharging) "(Charging)" else "(Discharging)"}"
    }

    private fun generateTopLines(): List<TerminalLine> {
        val ram = getRamUsageInfo()
        val battery = getBatteryStatusInfo()
        val cores = Runtime.getRuntime().availableProcessors()
        val uptime = getUptimeString()

        return listOf(
            TerminalLine("TOP / SYSTEM MONITOR", LineType.HEADER),
            TerminalLine("==================================================", LineType.SYSTEM),
            TerminalLine("CPU Cores     : $cores cores active", LineType.OUTPUT),
            TerminalLine("Memory (RAM)  : $ram", LineType.OUTPUT),
            TerminalLine("Power State   : $battery", LineType.OUTPUT),
            TerminalLine("System Uptime : $uptime", LineType.OUTPUT),
            TerminalLine("Architecture  : ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}", LineType.OUTPUT),
            TerminalLine("==================================================", LineType.SYSTEM)
        )
    }

    private fun generateBatteryLines(): List<TerminalLine> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 0
        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val health = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            else -> "Normal"
        }

        return listOf(
            TerminalLine("BATTERY HARDWARE STATUS", LineType.HEADER),
            TerminalLine("==================================================", LineType.SYSTEM),
            TerminalLine("Level       : $pct%", LineType.OUTPUT),
            TerminalLine("Temperature : ${String.format(Locale.US, "%.1f", temp)} °C", LineType.OUTPUT),
            TerminalLine("Health      : $health", LineType.OUTPUT),
            TerminalLine("==================================================", LineType.SYSTEM)
        )
    }

    private fun getUptimeString(): String {
        val uptimeMs = SystemClock.elapsedRealtime()
        val seconds = (uptimeMs / 1000) % 60
        val minutes = (uptimeMs / (1000 * 60)) % 60
        val hours = (uptimeMs / (1000 * 60 * 60)) % 24
        val days = uptimeMs / (1000 * 60 * 60 * 24)

        return if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m ${seconds}s"
    }

    private fun evaluateMathExpression(expr: String): Double {
        val sanitized = expr.replace(" ", "")
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < sanitized.length) sanitized[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < sanitized.length) throw RuntimeException("Unexpected character: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        eat('%'.code) -> x %= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = sanitized.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected expression factor")
                }

                if (eat('^'.code)) x = x.pow(parseFactor())

                return x
            }
        }.parse()
    }
}
