package com.example.terminallauncher.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.Immutable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.pow

enum class LineType {
    INPUT,
    OUTPUT,
    ERROR,
    SYSTEM,
    SUCCESS,
    HEADER
}

@Immutable
data class TerminalLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: LineType = LineType.OUTPUT
)

data class CommandResult(
    val outputLines: List<TerminalLine> = emptyList(),
    val shouldClearBuffer: Boolean = false,
    val openOptionsMenu: Boolean = false,
    val openMatrixMode: Boolean = false,
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

        // Check if input matches a web URL directly
        if (isWebUrl(trimmed)) {
            return handleWebUrl(trimmed)
        }

        // Check if input matches an alias
        val resolvedCommand = currentSettings.aliases[trimmed.lowercase()] ?: trimmed
        val tokens = resolvedCommand.split(Regex("\\s+"))
        val cmd = tokens.first().lowercase()
        val args = tokens.drop(1)

        return when (cmd) {
            "help", "man" -> handleHelp()
            "about", "version" -> handleAbout()
            "time", "date" -> handleTime()
            "killall", "trim" -> handleKillall()
            "echo" -> handleEcho(args.joinToString(" "), currentSettings)
            "search", "google", "g" -> handleSearch(args.joinToString(" "))
            "install" -> handleInstall(args.joinToString(" "))
            "cat", "read", "view" -> handleCat(args.joinToString(" "))
            "8bit", "tune", "chiptune", "music", "retro" -> handle8BitTune()
            "email", "mail", "sendmail" -> handleEmail(args)
            "termux", "tx" -> handleTermux(args)
            "options", "customize", "preferences" -> CommandResult(
                outputLines = listOf(TerminalLine(text = "Opening GUI Options Menu...", type = LineType.SUCCESS)),
                openOptionsMenu = true
            )
            "matrix" -> CommandResult(
                outputLines = listOf(TerminalLine(text = "Entering Matrix Digital Rain...", type = LineType.SUCCESS)),
                openMatrixMode = true
            )
            "clear" -> CommandResult(shouldClearBuffer = true)
            "ls" -> handleLs(args, currentSettings)
            "cd" -> handleCd(args)
            "apps" -> handleAppsList()
            "open", "launch" -> handleOpenApp(args.joinToString(" "))
            "uninstall", "remove" -> handleUninstall(args.joinToString(" "))
            "alias" -> handleAlias(args, currentSettings)
            "unalias" -> handleUnalias(args, currentSettings)
            "history" -> handleHistory(currentSettings)
            "config" -> handleConfig(args, currentSettings)
            "settings" -> handleSystemSettings()
            "infofetch", "fastfetch", "neofetch" -> handleInfofetch(args, currentSettings)
            "top", "htop" -> handleTop(args)
            "battery" -> handleBattery(args)
            "calc" -> handleCalc(args.joinToString(" "))
            "sys", "toggle" -> handleSys(args)
            "call" -> handleCall(args.joinToString(" "))
            "sms" -> handleSms(args.joinToString(" "))
            "reboot", "shutdown" -> handleReboot()
            else -> handleFallback(resolvedCommand, currentSettings)
        }
    }

    fun generateInfofetchLines(settings: LauncherSettings): List<TerminalLine> {
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val ramInfo = getRamUsageInfo()
        val batInfo = getBatteryStatusInfo()
        val uptime = getUptimeString()

        val asciiArt = getAsciiArtBanner(settings.asciiArtStyle, settings)

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
        result.add(TerminalLine(text = "--------------------------------------------------", type = LineType.SYSTEM))
        if (asciiArt.isNotEmpty()) {
            asciiArt.forEach { result.add(TerminalLine(text = it, type = LineType.HEADER)) }
            result.add(TerminalLine(text = "--------------------------------------------------", type = LineType.SYSTEM))
        }
        infoDetails.forEach { result.add(TerminalLine(text = it, type = LineType.OUTPUT)) }
        result.add(TerminalLine(text = "--------------------------------------------------", type = LineType.SYSTEM))
        return result
    }

    private fun handleCat(filePath: String): CommandResult {
        if (filePath.isBlank()) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "Usage: cat <file_path> or read <file_path>", type = LineType.ERROR)),
                success = false
            )
        }

        val targetFile = resolveStoragePath(filePath)
        if (!targetFile.exists()) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "cat: file not found: '$filePath'", type = LineType.ERROR)),
                success = false
            )
        }

        if (targetFile.isDirectory) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "cat: '$filePath' is a directory. Use 'ls' or 'cd' instead.", type = LineType.ERROR)),
                success = false
            )
        }

        return try {
            val allLines = targetFile.readLines(Charsets.UTF_8)
            val linesToDisplay = allLines.take(100)
            val output = mutableListOf<TerminalLine>()
            output.add(TerminalLine(text = "=== CONTENT: ${targetFile.name} (${targetFile.length() / 1024} KB) ===", type = LineType.HEADER))
            linesToDisplay.forEach { output.add(TerminalLine(text = it, type = LineType.OUTPUT)) }

            if (allLines.size > 100) {
                output.add(TerminalLine(text = "[DISPLAYING FIRST 100 LINES OF ${allLines.size} TOTAL LINES]", type = LineType.SYSTEM))
            }
            output.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))

            CommandResult(outputLines = output)
        } catch (e: Exception) {
            CommandResult(
                outputLines = listOf(TerminalLine(text = "Unable to read file: ${e.message}", type = LineType.ERROR)),
                success = false
            )
        }
    }

    private fun handle8BitTune(): CommandResult {
        try {
            val sampleRate = 22050
            val melody = listOf(
                261.63 to 150, // C4
                329.63 to 150, // E4
                392.00 to 150, // G4
                523.25 to 300, // C5
                392.00 to 150, // G4
                523.25 to 450  // C5
            )

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufferSize.coerceAtLeast(4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()

            Thread {
                try {
                    for ((freq, durationMs) in melody) {
                        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                        val sample = ShortArray(numSamples)
                        val period = sampleRate / freq
                        for (i in 0 until numSamples) {
                            val squareVal = if ((i % period) < (period / 2)) 8000 else -8000
                            sample[i] = squareVal.toShort()
                        }
                        audioTrack.write(sample, 0, sample.size)
                    }
                    Thread.sleep(100)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }.start()

            val asciiBanner = listOf(
                TerminalLine(text = "  ┌───┐ ┌───┐ ┌───┐ ┌───┐  [ 8-BIT CHIPTUNE SYNTHESIZER ]", type = LineType.HEADER),
                TerminalLine(text = "  │ █ │ │ █ │ │ █ │ │ █ │  ♪ ♫ Playing Retro Square Wave Melody ♪ ♫", type = LineType.SUCCESS),
                TerminalLine(text = "  └───┘ └───┘ └───┘ └───┘  Synthesized PCM AudioTrack (0 MB Overhead)", type = LineType.SYSTEM)
            )
            return CommandResult(outputLines = asciiBanner)
        } catch (e: Exception) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "Unable to play 8-bit audio: ${e.message}", type = LineType.ERROR)),
                success = false
            )
        }
    }

    private fun handleEmail(args: List<String>): CommandResult {
        if (args.isEmpty()) {
            return CommandResult(
                outputLines = listOf(
                    TerminalLine(text = "Usage: email <recipient@domain> [subject] [message_body]", type = LineType.ERROR),
                    TerminalLine(text = "Example: email friend@gmail.com \"Meeting Update\" \"Hi, here is the notes.\"", type = LineType.SYSTEM)
                ),
                success = false
            )
        }

        val recipient = args[0].trim()
        val subject = if (args.size > 1) args[1].removeSurrounding("\"") else ""
        val body = if (args.size > 2) args.drop(2).joinToString(" ").removeSurrounding("\"") else ""

        val mailtoUri = "mailto:${Uri.encode(recipient)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"

        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                outputLines = listOf(
                    TerminalLine(text = "Launching email compose to '$recipient' (Proton Mail / Gmail)...", type = LineType.SUCCESS)
                )
            )
        } catch (e: Exception) {
            CommandResult(
                outputLines = listOf(TerminalLine(text = "Unable to open email client: ${e.message}", type = LineType.ERROR)),
                success = false
            )
        }
    }

    private fun handleInstall(query: String): CommandResult {
        if (query.isBlank()) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "Usage: install <app_name_or_package>", type = LineType.ERROR)),
                success = false
            )
        }

        val cleanQuery = query.trim()
        val isPackageName = cleanQuery.contains(".") && !cleanQuery.contains(" ")

        val marketUriString = if (isPackageName) {
            "market://details?id=$cleanQuery"
        } else {
            "market://search?q=${Uri.encode(cleanQuery)}&c=apps"
        }

        val webUrl = if (isPackageName) {
            "https://play.google.com/store/apps/details?id=$cleanQuery"
        } else {
            "https://play.google.com/store/search?q=${Uri.encode(cleanQuery)}&c=apps"
        }

        return try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(marketUriString)).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            CommandResult(outputLines = listOf(TerminalLine(text = "Opening Google Play Store for '$cleanQuery'...", type = LineType.SUCCESS)))
        } catch (_: Exception) {
            try {
                val genericMarketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(marketUriString)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericMarketIntent)
                CommandResult(outputLines = listOf(TerminalLine(text = "Opening App Store for '$cleanQuery'...", type = LineType.SUCCESS)))
            } catch (_: Exception) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Opening Play Store web page for '$cleanQuery'...", type = LineType.SUCCESS)))
                } catch (e: Exception) {
                    CommandResult(outputLines = listOf(TerminalLine(text = "Unable to open store for '$cleanQuery': ${e.message}", type = LineType.ERROR)), success = false)
                }
            }
        }
    }

    private fun handleTermuxSetup(): CommandResult {
        val lines = listOf(
            TerminalLine(text = "TERMUX INTEGRATION SETUP INSTRUCTIONS", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "1. Open Termux on your device.", type = LineType.OUTPUT),
            TerminalLine(text = "2. Copy and run this exact command inside Termux:", type = LineType.OUTPUT),
            TerminalLine(text = "   echo \"allow-external-apps = true\" >> ~/.termux/termux.properties && termux-reload-settings", type = LineType.SUCCESS),
            TerminalLine(text = "3. Ensure Termux is installed from F-Droid or GitHub (build 0.118+).", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM)
        )
        return CommandResult(outputLines = lines)
    }

    private fun handleTermux(args: List<String>): CommandResult {
        val termuxPackage = "com.termux"
        val pm = context.packageManager
        val isInstalled = try {
            pm.getLaunchIntentForPackage(termuxPackage) != null
        } catch (_: Exception) {
            false
        }

        if (!isInstalled) {
            return CommandResult(
                outputLines = listOf(
                    TerminalLine(text = "Termux application is not installed on this device.", type = LineType.ERROR),
                    TerminalLine(text = "Install Termux from F-Droid or GitHub to enable Termux commands.", type = LineType.SYSTEM)
                ),
                success = false
            )
        }

        val subCmd = args.firstOrNull()?.lowercase() ?: ""
        if (subCmd == "setup" || subCmd == "config") {
            return handleTermuxSetup()
        }

        if (args.isEmpty()) {
            val launchIntent = pm.getLaunchIntentForPackage(termuxPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return CommandResult(outputLines = listOf(TerminalLine(text = "Opening Termux terminal...", type = LineType.SUCCESS)))
            } else {
                return CommandResult(outputLines = listOf(TerminalLine(text = "Unable to launch Termux.", type = LineType.ERROR)), success = false)
            }
        }

        val fullCommand = args.joinToString(" ")
        val interactiveTools = setOf("nano", "vim", "vi", "emacs", "micro", "htop", "top", "less", "more", "man", "gdb", "python", "node", "bash", "zsh", "fish", "tmux", "screen")
        val isInteractive = interactiveTools.contains(subCmd)

        return try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", fullCommand))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", !isInteractive)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startService(intent)

            val msg = if (isInteractive) {
                "Launching Termux ($fullCommand)..."
            } else {
                "Sent Termux command: '$fullCommand'"
            }

            CommandResult(
                outputLines = listOf(
                    TerminalLine(text = msg, type = LineType.SUCCESS),
                    TerminalLine(text = "Tip: Run 'tx setup' for Termux external app setup instructions.", type = LineType.SYSTEM)
                )
            )
        } catch (_: Exception) {
            val launchIntent = pm.getLaunchIntentForPackage(termuxPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                CommandResult(
                    outputLines = listOf(
                        TerminalLine(text = "Opened Termux terminal for '$fullCommand'.", type = LineType.SUCCESS),
                        TerminalLine(text = "Run 'tx setup' to configure automatic background command execution.", type = LineType.SYSTEM)
                    )
                )
            } else {
                CommandResult(outputLines = listOf(TerminalLine(text = "Failed to send command to Termux.", type = LineType.ERROR)), success = false)
            }
        }
    }

    private fun handleInfofetch(args: List<String>, settings: LauncherSettings): CommandResult {
        val isUpdate = args.firstOrNull()?.lowercase() == "update"
        val lines = generateInfofetchLines(settings).toMutableList()
        if (isUpdate) {
            lines.add(0, TerminalLine(text = "[REFRESHED REAL-TIME SYSTEM INFORMATION]", type = LineType.SUCCESS))
        }
        return CommandResult(outputLines = lines)
    }

    private fun handleEcho(argText: String, settings: LauncherSettings): CommandResult {
        if (argText.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "", type = LineType.OUTPUT)))
        }

        var text = argText.trim()
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length - 1)
        }

        val userStr = settings.promptString.split("@").firstOrNull() ?: "user"
        val expandedText = text
            .replace("\$USER", userStr)
            .replace("\$PROMPT", settings.promptString)
            .replace("\$THEME", settings.theme.name.lowercase())
            .replace("\$VERSION", "v1.0.0")
            .replace("\$OS", "Android ${Build.VERSION.RELEASE}")
            .replace("\$MODEL", "${Build.MANUFACTURER} ${Build.MODEL}")

        return CommandResult(outputLines = listOf(TerminalLine(text = expandedText, type = LineType.OUTPUT)))
    }

    private fun handleSearch(query: String): CommandResult {
        if (query.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: search <query> or google <query>", type = LineType.ERROR)), success = false)
        }

        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(outputLines = listOf(TerminalLine(text = "Searching Google for '$query'...", type = LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine(text = "Unable to launch web search: ${e.message}", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleAbout(): CommandResult {
        val lines = listOf(
            TerminalLine(text = "TERMINAL LAUNCHER - ABOUT", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "App Name    : Terminal Launcher", type = LineType.OUTPUT),
            TerminalLine(text = "Version     : v1.0.0 (Build 1)", type = LineType.OUTPUT),
            TerminalLine(text = "Developer   : CrownEmperor-dev", type = LineType.OUTPUT),
            TerminalLine(text = "Framework   : Kotlin & Jetpack Compose", type = LineType.OUTPUT),
            TerminalLine(text = "License     : MIT License", type = LineType.OUTPUT),
            TerminalLine(text = "Repository  : https://github.com/CrownEmperor-dev/Terminal-Launcher", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM)
        )
        return CommandResult(outputLines = lines)
    }

    private fun handleTime(): CommandResult {
        val now = Date()
        val timeFormat = SimpleDateFormat("hh:mm:ss a (z)", Locale.US)
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
        val epochSeconds = System.currentTimeMillis() / 1000

        val lines = listOf(
            TerminalLine(text = "SYSTEM DATE & TIME", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Time        : ${timeFormat.format(now)}", type = LineType.OUTPUT),
            TerminalLine(text = "Date        : ${dateFormat.format(now)}", type = LineType.OUTPUT),
            TerminalLine(text = "Unix Epoch  : $epochSeconds seconds", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM)
        )
        return CommandResult(outputLines = lines)
    }

    private fun handleKillall(): CommandResult {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val initialRam = getRamUsageInfo()

        return try {
            val runningAppProcesses = actManager.runningAppProcesses ?: emptyList()
            var killedCount = 0
            runningAppProcesses.forEach { proc ->
                if (proc.pkgList.isNotEmpty() && proc.processName != context.packageName) {
                    proc.pkgList.forEach { pkg ->
                        try {
                            actManager.killBackgroundProcesses(pkg)
                            killedCount++
                        } catch (_: Exception) {}
                    }
                }
            }

            System.gc()
            val newRam = getRamUsageInfo()

            CommandResult(
                outputLines = listOf(
                    TerminalLine(text = "BACKGROUND PROCESS TRIM", type = LineType.HEADER),
                    TerminalLine(text = "==================================================", type = LineType.SYSTEM),
                    TerminalLine(text = "Trimmed Packages : $killedCount background processes", type = LineType.OUTPUT),
                    TerminalLine(text = "RAM Before Trim  : $initialRam", type = LineType.OUTPUT),
                    TerminalLine(text = "RAM After Trim   : $newRam", type = LineType.SUCCESS),
                    TerminalLine(text = "==================================================", type = LineType.SYSTEM)
                )
            )
        } catch (e: Exception) {
            CommandResult(
                outputLines = listOf(TerminalLine(text = "Memory trim error: ${e.message}", type = LineType.ERROR)),
                success = false
            )
        }
    }

    private fun handleSystemSettings(): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(outputLines = listOf(TerminalLine(text = "Opening Android System Settings...", type = LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine(text = "Unable to open System Settings: ${e.message}", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleUninstall(appQuery: String): CommandResult {
        if (appQuery.isBlank()) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "Usage: uninstall <app_name_or_package>", type = LineType.ERROR)),
                success = false
            )
        }

        val app = appRepository.findApp(appQuery)
        if (app == null) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "App not found for uninstallation: '$appQuery'", type = LineType.ERROR)),
                success = false
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                outputLines = listOf(TerminalLine(text = "Opening uninstallation prompt for ${app.label}...", type = LineType.SUCCESS))
            )
        } catch (e: Exception) {
            CommandResult(
                outputLines = listOf(TerminalLine(text = "Unable to launch uninstaller for ${app.label}: ${e.message}", type = LineType.ERROR)),
                success = false
            )
        }
    }

    private fun getAsciiArtBanner(style: AsciiArtStyle, settings: LauncherSettings): List<String> {
        return when (style) {
            AsciiArtStyle.DEFAULT -> listOf(
                "  _____                   _             _ ",
                " |_   _|___ _ __ _ __ ___ (_)_ __   __ _| |",
                "   | |/ _ \\ '__| '_ ` _ \\| | '_ \\ / _` | |",
                "   | |  __/ |  | | | | | | | | | | (_| | |",
                "   |_|\\___|_|  |_| |_| |_|_|_| |_|\\__,_|_|"
            )
            AsciiArtStyle.BUGDROID -> listOf(
                "       __   __       ",
                "      (  \\_/  )      ",
                "       \\     /       ",
                "      .-'   '-.      ",
                "     /  O   O  \\     ",
                "    |           |    ",
                "   .-------------.   ",
                "  /               \\  "
            )
            AsciiArtStyle.TUX -> listOf(
                "     a_a      ",
                "    o(_)_o    ",
                "   .-\"==\"-.   ",
                "  /   ||   \\  ",
                " |    ||    | ",
                "  \\_  ||  _/  ",
                "    `\"\"`\"\"`   "
            )
            AsciiArtStyle.RETRO_PC -> listOf(
                "  +---------+ ",
                "  |  >_     | ",
                "  +---------+ ",
                "  |  [===]  | ",
                "  +---------+ "
            )
            AsciiArtStyle.MINIMAL -> listOf(
                " [ TERMINAL LAUNCHER ] "
            )
            AsciiArtStyle.CUSTOM -> {
                if (settings.customAsciiArt.isBlank()) {
                    listOf(" [ CUSTOM ASCII ART - PASTE IN OPTIONS ] ")
                } else {
                    settings.customAsciiArt.split("\n")
                }
            }
            AsciiArtStyle.OFF -> emptyList()
        }
    }

    private fun handleHelp(): CommandResult {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine(text = "Terminal Launcher v1.0 - Shell Manual", type = LineType.HEADER))
        lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))
        lines.add(TerminalLine(text = "CORE COMMANDS:", type = LineType.HEADER))
        lines.add(TerminalLine(text = "  options / customize GUI visual settings menu", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  about / version    App details, license, and repository URL", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  echo <text|\$var>   Print string or shell variables", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  search <query>     Search Google in web browser (or 'google' / 'g')", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  install <query>    Search & install apps from Google Play Store", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  cat / read <path>  Read & display text files in terminal", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  8bit / tune        Play 8-bit retro chiptune audio easter egg", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  email <to> [sub]   Dispatch pre-filled email (Proton Mail / Gmail)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  tx <command>       Execute command or script via Termux (or 'termux')", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  time / date        Display system date, time, and epoch", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  killall / trim     Trim background tasks & free RAM", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  matrix             Full-screen matrix digital rain easter egg", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  ls [letter|dir]    List applications, storage (e.g. ls storage), or filter", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  cd <dir>           Change active storage directory (e.g. cd storage0/Download)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  open <app>         Launch an app by name or package", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  uninstall <app>    Trigger native uninstallation for an app", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  alias [k=v]       Set custom command shortcut (e.g., alias yt=YouTube)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  unalias <key>     Remove a custom alias", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  clear             Clear current terminal screen", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  history           Show command execution history", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  help / man        Display documentation", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "SETTINGS & PREFERENCES:", type = LineType.HEADER))
        lines.add(TerminalLine(text = "  config                     Show launcher CLI configurations", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set theme <t>       Set theme: green, amber, oled, mono", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set fastfetch <on|off> Toggle startup system banner", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set prompt <text>   Customize shell prompt string", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set autocomplete <on|off> Toggle ghost auto-completion", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  settings                   Open Android device system settings", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "SYSTEM & UTILITIES:", type = LineType.HEADER))
        lines.add(TerminalLine(text = "  infofetch [update]   System hardware specs & live refresh", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  top [memory]         System monitor & RAM usage breakdown", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  battery [status|health] Power & battery health metrics", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  calc <expr>          Solve inline math expressions", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys flash            Toggle flashlight/torch hardware", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys ringer <s|v|n>   Set ringer mode: silent, vibrate, normal", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys settings <page>  Open settings page (e.g. sys settings display)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  call <number>        Launch phone dialer", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sms <number>         Launch SMS messaging", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))
        return CommandResult(outputLines = lines)
    }

    private fun handleLs(args: List<String>, settings: LauncherSettings): CommandResult {
        val targetArg = args.firstOrNull()?.replace("\"", "")?.trim() ?: currentPath
        val lines = mutableListOf<TerminalLine>()

        when (targetArg.lowercase()) {
            "/aliases", "aliases" -> {
                lines.add(TerminalLine(text = "CONFIGURED ALIASES:", type = LineType.HEADER))
                if (settings.aliases.isEmpty()) {
                    lines.add(TerminalLine(text = "  (no aliases defined. Use 'alias key=value')", type = LineType.OUTPUT))
                } else {
                    settings.aliases.forEach { (k, v) ->
                        lines.add(TerminalLine(text = "  $k -> $v", type = LineType.OUTPUT))
                    }
                }
            }
            "/system", "system" -> {
                lines.add(TerminalLine(text = "SYSTEM COMMANDS:", type = LineType.HEADER))
                val sysCmds = listOf("options", "about", "time", "killall", "echo", "search", "install", "cat", "8bit", "email", "termux", "tx", "infofetch", "top", "battery", "calc", "sys flash", "sys wifi", "sys bt", "call", "sms", "config", "settings", "uninstall", "matrix")
                sysCmds.forEach { lines.add(TerminalLine(text = "  $it", type = LineType.OUTPUT)) }
            }
            "/storage", "storage" -> {
                lines.add(TerminalLine(text = "MOUNTED STORAGE VOLUMES:", type = LineType.HEADER))
                lines.add(TerminalLine(text = "  storage0 -> Internal Storage (${Environment.getExternalStorageDirectory().path})", type = LineType.OUTPUT))
                val sdCard = getSdCardDirectory()
                if (sdCard != null) {
                    lines.add(TerminalLine(text = "  storage1 -> MicroSD Card (${sdCard.path})", type = LineType.OUTPUT))
                } else {
                    lines.add(TerminalLine(text = "  storage1 -> MicroSD Card (Not Mounted / No SD Card)", type = LineType.SYSTEM))
                }
            }
            else -> {
                if (targetArg.startsWith("storage") || currentPath.startsWith("storage")) {
                    val fullPath = if (targetArg.startsWith("storage")) targetArg else "$currentPath/$targetArg"
                    val file = resolveStoragePath(fullPath)
                    if (file.exists() && file.isDirectory) {
                        val files = file.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
                        lines.add(TerminalLine(text = "DIRECTORY: $fullPath (${files.size} items)", type = LineType.HEADER))
                        if (files.isEmpty()) {
                            lines.add(TerminalLine(text = "  (empty directory)", type = LineType.OUTPUT))
                        } else {
                            files.forEach { item ->
                                val prefix = if (item.isDirectory) "[DIR]  " else "[FILE] "
                                val sizeStr = if (item.isDirectory) "" else " (${item.length() / 1024} KB)"
                                lines.add(TerminalLine(text = "  $prefix${item.name}$sizeStr", type = LineType.OUTPUT))
                            }
                        }
                        return CommandResult(outputLines = lines)
                    }
                }

                val allApps = appRepository.getApps()
                val filterQuery = if (targetArg != "~" && targetArg != "/apps" && targetArg != "apps") targetArg.lowercase() else ""

                val filteredApps = if (filterQuery.isNotEmpty()) {
                    allApps.filter { it.label.lowercase().startsWith(filterQuery) || it.packageName.lowercase().startsWith(filterQuery) }
                } else {
                    allApps
                }

                val headerText = if (filterQuery.isNotEmpty()) {
                    "INSTALLED APPLICATIONS MATCHING '$filterQuery' (${filteredApps.size}):"
                } else {
                    "INSTALLED APPLICATIONS (${filteredApps.size}):"
                }

                lines.add(TerminalLine(text = headerText, type = LineType.HEADER))

                if (filteredApps.isEmpty()) {
                    lines.add(TerminalLine(text = "  (no apps found matching '$filterQuery')", type = LineType.OUTPUT))
                } else {
                    filteredApps.chunked(3).forEach { row ->
                        val rowText = row.joinToString("   ") { app ->
                            val alias = settings.aliases.entries.firstOrNull { it.value.equals(app.label, ignoreCase = true) }?.key
                            if (alias != null) "${app.label} [$alias]" else app.label
                        }
                        lines.add(TerminalLine(text = rowText, type = LineType.OUTPUT))
                    }
                }
            }
        }
        return CommandResult(outputLines = lines)
    }

    private fun handleCd(args: List<String>): CommandResult {
        val path = args.firstOrNull()?.trim() ?: "~"
        when {
            path == ".." -> {
                currentPath = when {
                    currentPath == "~" || currentPath == "/" -> "~"
                    currentPath == "storage0" || currentPath == "storage1" || currentPath == "/storage" -> "~"
                    currentPath.contains("/") -> {
                        val idx = currentPath.lastIndexOf('/')
                        if (idx > 0) currentPath.substring(0, idx) else "storage0"
                    }
                    else -> "~"
                }
            }
            path == "/" || path == "~" -> currentPath = "~"
            path == "apps" || path == "/apps" -> currentPath = "/apps"
            path == "system" || path == "/system" -> currentPath = "/system"
            path == "aliases" || path == "/aliases" -> currentPath = "/aliases"
            path == "storage" || path == "/storage" -> currentPath = "/storage"
            path.startsWith("storage0") || path.startsWith("storage1") -> {
                val file = resolveStoragePath(path)
                if (file.exists() && file.isDirectory) {
                    currentPath = path
                } else {
                    return CommandResult(outputLines = listOf(TerminalLine(text = "cd: directory not found: $path", type = LineType.ERROR)), success = false)
                }
            }
            currentPath.startsWith("storage") -> {
                val targetPath = "$currentPath/$path"
                val file = resolveStoragePath(targetPath)
                if (file.exists() && file.isDirectory) {
                    currentPath = targetPath
                } else {
                    return CommandResult(outputLines = listOf(TerminalLine(text = "cd: no such directory: $path", type = LineType.ERROR)), success = false)
                }
            }
            else -> {
                return CommandResult(outputLines = listOf(TerminalLine(text = "cd: no such directory: $path", type = LineType.ERROR)), success = false)
            }
        }
        return CommandResult(outputLines = listOf(TerminalLine(text = "Directory: $currentPath", type = LineType.SUCCESS)))
    }

    private fun resolveStoragePath(path: String): File {
        val internalStorage = Environment.getExternalStorageDirectory()
        val clean = path.trim()
        return when {
            clean == "storage0" || clean == "/storage0" -> internalStorage
            clean.startsWith("storage0/") || clean.startsWith("/storage0/") -> {
                val sub = clean.removePrefix("storage0/").removePrefix("/storage0/")
                File(internalStorage, sub)
            }
            clean == "storage1" || clean == "/storage1" -> getSdCardDirectory() ?: internalStorage
            clean.startsWith("storage1/") || clean.startsWith("/storage1/") -> {
                val sd = getSdCardDirectory() ?: internalStorage
                val sub = clean.removePrefix("storage1/").removePrefix("/storage1/")
                File(sd, sub)
            }
            clean.startsWith("/") -> File(clean)
            else -> File(internalStorage, clean)
        }
    }

    private fun getSdCardDirectory(): File? {
        val dirs = context.getExternalFilesDirs(null)
        dirs.forEach { dir ->
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                var current = dir
                while (current.parentFile != null && current.parentFile?.path != "/storage") {
                    current = current.parentFile!!
                }
                return current
            }
        }
        return null
    }

    private fun handleTop(args: List<String>): CommandResult {
        val subCommand = args.firstOrNull()?.lowercase() ?: ""
        return if (subCommand == "memory" || subCommand == "mem") {
            CommandResult(outputLines = generateDetailedMemoryLines())
        } else {
            CommandResult(outputLines = generateTopLines())
        }
    }

    private fun handleBattery(args: List<String>): CommandResult {
        val subCommand = args.firstOrNull()?.lowercase() ?: ""
        return if (subCommand == "status" || subCommand == "health" || subCommand == "info") {
            CommandResult(outputLines = generateDetailedBatteryLines())
        } else {
            CommandResult(outputLines = generateBatteryLines())
        }
    }

    private fun handleAppsList(): CommandResult {
        return handleLs(listOf("/apps"), LauncherSettings())
    }

    private fun handleOpenApp(query: String): CommandResult {
        if (query.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: open <app_name_or_package>", type = LineType.ERROR)), success = false)
        }

        val duplicates = appRepository.findDuplicateApps(query)
        if (duplicates.size > 1) {
            return generateDisambiguationResult(query, duplicates)
        }

        val app = appRepository.findApp(query)
        return if (app != null) {
            val launched = appRepository.launchApp(app)
            if (launched) {
                CommandResult(
                    outputLines = listOf(TerminalLine(text = "Launching ${app.label}...", type = LineType.SUCCESS)),
                    launchedApp = app
                )
            } else {
                CommandResult(outputLines = listOf(TerminalLine(text = "Failed to launch ${app.label}", type = LineType.ERROR)), success = false)
            }
        } else {
            CommandResult(outputLines = listOf(TerminalLine(text = "App not found: '$query'", type = LineType.ERROR)), success = false)
        }
    }

    private fun generateDisambiguationResult(query: String, duplicates: List<AppModel>): CommandResult {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine(text = "Multiple apps found matching '$query' (${duplicates.size}):", type = LineType.HEADER))
        lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))

        val autoAliases = appRepository.getAutoAliases()
        duplicates.forEachIndexed { idx, app ->
            val alias = autoAliases.entries.firstOrNull { it.value.packageName == app.packageName }?.key ?: "${query.lowercase()}.${idx + 1}"
            lines.add(TerminalLine(text = "  ${idx + 1}. ${app.label} [${app.packageName}]", type = LineType.OUTPUT))
            lines.add(TerminalLine(text = "     -> Shortcut alias: $alias", type = LineType.SUCCESS))
        }
        lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))
        lines.add(TerminalLine(text = "Type the specific alias or package name to launch.", type = LineType.SYSTEM))

        return CommandResult(outputLines = lines)
    }

    private suspend fun handleAlias(args: List<String>, settings: LauncherSettings): CommandResult {
        if (args.isEmpty()) {
            return handleLs(listOf("/aliases"), settings)
        }

        val fullArg = args.joinToString(" ")
        val parts = fullArg.split("=", limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: alias <key>=<target>", type = LineType.ERROR)), success = false)
        }

        val key = parts[0].trim().lowercase()
        val target = parts[1].trim()
        settingsRepository.addAlias(key, target)
        return CommandResult(outputLines = listOf(TerminalLine(text = "Alias added: $key -> $target", type = LineType.SUCCESS)))
    }

    private suspend fun handleUnalias(args: List<String>, settings: LauncherSettings): CommandResult {
        val key = args.firstOrNull()?.trim()?.lowercase()
        if (key.isNullOrBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: unalias <key>", type = LineType.ERROR)), success = false)
        }
        settingsRepository.removeAlias(key)
        return CommandResult(outputLines = listOf(TerminalLine(text = "Alias removed: $key", type = LineType.SUCCESS)))
    }

    private fun handleHistory(settings: LauncherSettings): CommandResult {
        val lines = mutableListOf<TerminalLine>()
        lines.add(TerminalLine(text = "COMMAND HISTORY:", type = LineType.HEADER))
        if (settings.history.isEmpty()) {
            lines.add(TerminalLine(text = "  (no command history)", type = LineType.OUTPUT))
        } else {
            settings.history.forEachIndexed { idx, cmd ->
                lines.add(TerminalLine(text = "  ${idx + 1}  $cmd", type = LineType.OUTPUT))
            }
        }
        return CommandResult(outputLines = lines)
    }

    private suspend fun handleConfig(args: List<String>, settings: LauncherSettings): CommandResult {
        if (args.isEmpty()) {
            val lines = listOf(
                TerminalLine(text = "LAUNCHER CONFIGURATION:", type = LineType.HEADER),
                TerminalLine(text = "  theme          : ${settings.theme.name.lowercase()}", type = LineType.OUTPUT),
                TerminalLine(text = "  fastfetch      : ${if (settings.fastfetchOnStart) "on" else "off"}", type = LineType.OUTPUT),
                TerminalLine(text = "  prompt         : '${settings.promptString}'", type = LineType.OUTPUT),
                TerminalLine(text = "  autocomplete   : ${if (settings.autocompleteEnabled) "on" else "off"}", type = LineType.OUTPUT),
                TerminalLine(text = "  textSizeSp     : ${settings.textSizeSp}sp", type = LineType.OUTPUT),
                TerminalLine(text = "  asciiArtStyle  : ${settings.asciiArtStyle.name.lowercase()}", type = LineType.OUTPUT),
                TerminalLine(text = "--------------------------------------------------", type = LineType.SYSTEM),
                TerminalLine(text = "Usage: config set <theme|fastfetch|prompt|autocomplete|size|ascii> <value>", type = LineType.SYSTEM)
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
                        CommandResult(outputLines = listOf(TerminalLine(text = "Theme set to ${themeEnum.name.lowercase()}", type = LineType.SUCCESS)))
                    } else {
                        CommandResult(outputLines = listOf(TerminalLine(text = "Invalid theme. Options: green, amber, oled, mono", type = LineType.ERROR)), success = false)
                    }
                }
                "fastfetch", "neofetch", "infofetch" -> {
                    val enabled = value.lowercase() in listOf("on", "true", "1", "yes")
                    settingsRepository.setFastfetchOnStart(enabled)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Startup infofetch set to ${if (enabled) "on" else "off"}", type = LineType.SUCCESS)))
                }
                "prompt" -> {
                    val promptStr = if (value.endsWith(" ")) value else "$value "
                    settingsRepository.setPromptString(promptStr)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Prompt string set to '$promptStr'", type = LineType.SUCCESS)))
                }
                "autocomplete" -> {
                    val enabled = value.lowercase() in listOf("on", "true", "1", "yes")
                    settingsRepository.setAutocompleteEnabled(enabled)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Ghost auto-complete set to ${if (enabled) "on" else "off"}", type = LineType.SUCCESS)))
                }
                "size", "textsize" -> {
                    val size = value.toIntOrNull() ?: 15
                    settingsRepository.setTextSizeSp(size)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Text size set to ${size}sp", type = LineType.SUCCESS)))
                }
                "ascii", "art" -> {
                    val style = try { AsciiArtStyle.valueOf(value.uppercase()) } catch (_: Exception) { null }
                    if (style != null) {
                        settingsRepository.setAsciiArtStyle(style)
                        CommandResult(outputLines = listOf(TerminalLine(text = "ASCII art style set to ${style.name.lowercase()}", type = LineType.SUCCESS)))
                    } else {
                        CommandResult(outputLines = listOf(TerminalLine(text = "Invalid style. Options: default, bugdroid, tux, retro_pc, minimal, custom, off", type = LineType.ERROR)), success = false)
                    }
                }
                else -> CommandResult(outputLines = listOf(TerminalLine(text = "Unknown config key: $option", type = LineType.ERROR)), success = false)
            }
        }

        return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: config set <option> <value>", type = LineType.ERROR)), success = false)
    }

    private fun handleCalc(expression: String): CommandResult {
        if (expression.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: calc <expression> (e.g. calc 15 * 4.5)", type = LineType.ERROR)), success = false)
        }
        return try {
            val result = evaluateMathExpression(expression)
            CommandResult(outputLines = listOf(TerminalLine(text = "$expression = $result", type = LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine(text = "Math evaluation error: ${e.message}", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleSys(args: List<String>): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: ""
        val subArgs = args.drop(1)

        return when (action) {
            "flash", "flashlight", "torch" -> toggleFlashlight()
            "wifi" -> {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine(text = "Opened Wi-Fi Settings", type = LineType.SUCCESS)))
            }
            "bt", "bluetooth" -> {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine(text = "Opened Bluetooth Settings", type = LineType.SUCCESS)))
            }
            "silent", "sound", "ringer" -> handleRingerMode(subArgs.firstOrNull()?.lowercase() ?: "")
            "rotation" -> handleAutoRotation()
            "gps", "location" -> {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine(text = "Opened Location Settings", type = LineType.SUCCESS)))
            }
            "hotspot" -> {
                context.startActivity(Intent("android.settings.TETHER_SETTINGS").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine(text = "Opened Hotspot & Tethering Settings", type = LineType.SUCCESS)))
            }
            "settings" -> handleSysSettingsSubpage(subArgs.joinToString(" "))
            else -> CommandResult(outputLines = listOf(TerminalLine(text = "Usage: sys <flash|wifi|bt|sound|rotation|gps|hotspot|settings>", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleSysSettingsSubpage(subpage: String): CommandResult {
        if (subpage.isBlank()) return handleSystemSettings()
        val lower = subpage.lowercase()

        val intent = when {
            lower.startsWith("app ") || lower.startsWith("package ") -> {
                val appName = subpage.substringAfter(" ").trim()
                val app = appRepository.findApp(appName)
                if (app != null) {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
                } else {
                    return CommandResult(outputLines = listOf(TerminalLine(text = "App not found: '$appName'", type = LineType.ERROR)), success = false)
                }
            }
            lower.contains("display") || lower.contains("screen") -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            lower.contains("battery") || lower.contains("power") -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            lower.contains("storage") -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            lower.contains("keyboard") || lower.contains("input") -> Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            lower.contains("network") || lower.contains("data") -> Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            lower.contains("default") -> Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            lower.contains("developer") || lower.contains("dev") -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            lower.contains("accessibility") -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            lower.contains("security") || lower.contains("lock") -> Intent(Settings.ACTION_SECURITY_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        return try {
            context.startActivity(intent)
            CommandResult(outputLines = listOf(TerminalLine(text = "Opened System Settings ($subpage)...", type = LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine(text = "Unable to open subpage ($subpage): ${e.message}", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleRingerMode(mode: String): CommandResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (mode) {
            "silent" -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                CommandResult(outputLines = listOf(TerminalLine(text = "Ringer set to SILENT", type = LineType.SUCCESS)))
            }
            "vibrate" -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                CommandResult(outputLines = listOf(TerminalLine(text = "Ringer set to VIBRATE", type = LineType.SUCCESS)))
            }
            "normal", "sound" -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                CommandResult(outputLines = listOf(TerminalLine(text = "Ringer set to NORMAL", type = LineType.SUCCESS)))
            }
            else -> {
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                CommandResult(outputLines = listOf(TerminalLine(text = "Opened Sound Settings. Usage: sys sound <silent|vibrate|normal>", type = LineType.SUCCESS)))
            }
        }
    }

    private fun handleAutoRotation(): CommandResult {
        return try {
            val current = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val newMode = if (current == 1) 0 else 1
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, newMode)
            CommandResult(outputLines = listOf(TerminalLine(text = "Auto-rotation turned ${if (newMode == 1) "ON" else "OFF"}", type = LineType.SUCCESS)))
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            CommandResult(outputLines = listOf(TerminalLine(text = "Opened Display Settings for auto-rotation toggle", type = LineType.SUCCESS)))
        }
    }

    private fun toggleFlashlight(): CommandResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId, isFlashlightOn)
                CommandResult(outputLines = listOf(TerminalLine(text = "Flashlight turned ${if (isFlashlightOn) "ON" else "OFF"}", type = LineType.SUCCESS)))
            } else {
                CommandResult(outputLines = listOf(TerminalLine(text = "No camera flash hardware available", type = LineType.ERROR)), success = false)
            }
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine(text = "Camera permission or hardware error: ${e.message}", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleCall(number: String): CommandResult {
        if (number.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: call <number_or_contact>", type = LineType.ERROR)), success = false)
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return CommandResult(outputLines = listOf(TerminalLine(text = "Dialing $number...", type = LineType.SUCCESS)))
    }

    private fun handleSms(number: String): CommandResult {
        if (number.isBlank()) {
            return CommandResult(outputLines = listOf(TerminalLine(text = "Usage: sms <number>", type = LineType.ERROR)), success = false)
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return CommandResult(outputLines = listOf(TerminalLine(text = "Opening SMS to $number...", type = LineType.SUCCESS)))
    }

    private fun handleReboot(): CommandResult {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            CommandResult(outputLines = listOf(TerminalLine(text = "Opened Power / Battery menu", type = LineType.SUCCESS)))
        } catch (_: Exception) {
            CommandResult(outputLines = listOf(TerminalLine(text = "Unable to trigger power action", type = LineType.ERROR)), success = false)
        }
    }

    private fun isWebUrl(input: String): Boolean {
        val lower = input.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.") ||
               (lower.contains(".") && !lower.contains(" ") && lower.split(".").last() in listOf("com", "org", "net", "io", "dev", "edu", "gov", "app", "me", "ai", "co", "uk", "de", "fr"))
    }

    private fun handleWebUrl(input: String): CommandResult {
        val formattedUrl = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            else -> "https://$input"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(outputLines = listOf(TerminalLine(text = "Opening URL '$formattedUrl' in browser...", type = LineType.SUCCESS)))
        } catch (e: Exception) {
            CommandResult(outputLines = listOf(TerminalLine(text = "Unable to open URL: ${e.message}", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleFallback(input: String, settings: LauncherSettings): CommandResult {
        val duplicates = appRepository.findDuplicateApps(input)
        if (duplicates.size > 1) {
            return generateDisambiguationResult(input, duplicates)
        }

        val app = appRepository.findApp(input)
        if (app != null) {
            val launched = appRepository.launchApp(app)
            return if (launched) {
                CommandResult(
                    outputLines = listOf(TerminalLine(text = "Launching ${app.label}...", type = LineType.SUCCESS)),
                    launchedApp = app
                )
            } else {
                CommandResult(outputLines = listOf(TerminalLine(text = "Failed to launch ${app.label}", type = LineType.ERROR)), success = false)
            }
        }

        return CommandResult(
            outputLines = listOf(
                TerminalLine(text = "command not found: $input", type = LineType.ERROR),
                TerminalLine(text = "Type 'help' for available commands or 'options' for GUI settings.", type = LineType.SYSTEM)
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
            TerminalLine(text = "TOP / SYSTEM MONITOR", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "CPU Cores     : $cores cores active", type = LineType.OUTPUT),
            TerminalLine(text = "Memory (RAM)  : $ram", type = LineType.OUTPUT),
            TerminalLine(text = "Power State   : $battery", type = LineType.OUTPUT),
            TerminalLine(text = "System Uptime : $uptime", type = LineType.OUTPUT),
            TerminalLine(text = "Architecture  : ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Tip: Run 'top memory' for detailed memory breakdown.", type = LineType.SYSTEM)
        )
    }

    private fun generateDetailedMemoryLines(): List<TerminalLine> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedMb = totalMb - availMb
        val threshMb = memInfo.threshold / (1024 * 1024)

        val runtime = Runtime.getRuntime()
        val maxHeapMb = runtime.maxMemory() / (1024 * 1024)
        val totalHeapMb = runtime.totalMemory() / (1024 * 1024)
        val freeHeapMb = runtime.freeMemory() / (1024 * 1024)
        val usedHeapMb = totalHeapMb - freeHeapMb

        return listOf(
            TerminalLine(text = "DETAILED SYSTEM MEMORY ALLOCATION", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Total RAM Physical  : ${totalMb} MB", type = LineType.OUTPUT),
            TerminalLine(text = "RAM Currently Used  : ${usedMb} MB (${usedMb * 100 / totalMb}%)", type = LineType.OUTPUT),
            TerminalLine(text = "RAM Available Free  : ${availMb} MB", type = LineType.OUTPUT),
            TerminalLine(text = "Low Memory Warning  : ${if (memInfo.lowMemory) "YES (CRITICAL)" else "NO (HEALTHY)"}", type = LineType.OUTPUT),
            TerminalLine(text = "Low Memory Threshold: ${threshMb} MB", type = LineType.OUTPUT),
            TerminalLine(text = "--------------------------------------------------", type = LineType.SYSTEM),
            TerminalLine(text = "Process Heap Max    : ${maxHeapMb} MB", type = LineType.OUTPUT),
            TerminalLine(text = "Process Heap Total  : ${totalHeapMb} MB", type = LineType.OUTPUT),
            TerminalLine(text = "Process Heap Used   : ${usedHeapMb} MB", type = LineType.OUTPUT),
            TerminalLine(text = "Process Heap Free   : ${freeHeapMb} MB", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM)
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
            TerminalLine(text = "BATTERY HARDWARE STATUS", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Level       : $pct%", type = LineType.OUTPUT),
            TerminalLine(text = "Temperature : ${String.format(Locale.US, "%.1f", temp)} °C", type = LineType.OUTPUT),
            TerminalLine(text = "Health      : $health", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Tip: Run 'battery status' for full hardware details.", type = LineType.SYSTEM)
        )
    }

    private fun generateDetailedBatteryLines(): List<TerminalLine> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 0

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val statusStr = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val pluggedInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val pluggedStr = when (pluggedInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Power Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charger"
            else -> "Battery Only (Unplugged)"
        }

        val voltage = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0
        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val tech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        return listOf(
            TerminalLine(text = "DETAILED BATTERY & POWER METRICS", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Charge Level  : $pct%", type = LineType.OUTPUT),
            TerminalLine(text = "Power State   : $statusStr", type = LineType.OUTPUT),
            TerminalLine(text = "Power Source  : $pluggedStr", type = LineType.OUTPUT),
            TerminalLine(text = "Voltage       : ${String.format(Locale.US, "%.2f", voltage)} V", type = LineType.OUTPUT),
            TerminalLine(text = "Temperature   : ${String.format(Locale.US, "%.1f", temp)} °C", type = LineType.OUTPUT),
            TerminalLine(text = "Technology    : $tech", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM)
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
                return parseExpression()
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
