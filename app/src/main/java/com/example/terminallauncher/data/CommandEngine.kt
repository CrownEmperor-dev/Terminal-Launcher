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
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.Immutable
import com.example.terminallauncher.MainActivity
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
    val success: Boolean = true,
    val updatedPrompt: String? = null
)

data class ChiptuneSong(
    val id: Int,
    val title: String,
    val notes: List<Pair<Double, Int>>
)

class CommandEngine(
    private val context: Context,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) {
    private var currentPath: String = "~"
    private var isFlashlightOn: Boolean = false

    private val chiptuneJukebox = listOf(
        ChiptuneSong(
            id = 1,
            title = "Super Mario Bros (Overworld)",
            notes = listOf(659.25 to 150, 659.25 to 150, 0.0 to 100, 659.25 to 150, 0.0 to 100, 523.25 to 150, 659.25 to 150, 783.99 to 300, 392.00 to 300)
        ),
        ChiptuneSong(
            id = 2,
            title = "The Legend of Zelda (Main Theme)",
            notes = listOf(466.16 to 300, 349.23 to 150, 466.16 to 150, 523.25 to 150, 587.33 to 150, 622.25 to 150, 698.46 to 450)
        ),
        ChiptuneSong(
            id = 3,
            title = "Tetris (Korobeiniki)",
            notes = listOf(659.25 to 300, 493.88 to 150, 523.25 to 150, 587.33 to 300, 523.25 to 150, 493.88 to 150, 440.00 to 300)
        ),
        ChiptuneSong(
            id = 4,
            title = "Pac-Man (Intro Theme)",
            notes = listOf(493.88 to 120, 987.77 to 120, 739.99 to 120, 622.25 to 120, 987.77 to 120, 739.99 to 120, 622.25 to 180)
        ),
        ChiptuneSong(
            id = 5,
            title = "Mega Man 2 (Dr. Wily Stage 1)",
            notes = listOf(311.13 to 150, 415.30 to 150, 466.16 to 150, 554.37 to 150, 523.25 to 150, 466.16 to 150, 415.30 to 300)
        ),
        ChiptuneSong(
            id = 6,
            title = "Pokémon Red/Blue (Title Theme)",
            notes = listOf(392.00 to 150, 523.25 to 150, 659.25 to 150, 783.99 to 300, 698.46 to 150, 659.25 to 150, 587.33 to 300)
        ),
        ChiptuneSong(
            id = 7,
            title = "Sonic the Hedgehog (Green Hill)",
            notes = listOf(659.25 to 200, 587.33 to 200, 523.25 to 200, 493.88 to 200, 440.00 to 200, 493.88 to 200, 523.25 to 300)
        ),
        ChiptuneSong(
            id = 8,
            title = "Castlevania (Vampire Killer)",
            notes = listOf(440.00 to 150, 523.25 to 150, 659.25 to 150, 587.33 to 150, 523.25 to 150, 493.88 to 150, 440.00 to 300)
        ),
        ChiptuneSong(
            id = 9,
            title = "Metroid (Title Theme)",
            notes = listOf(261.63 to 200, 311.13 to 200, 392.00 to 200, 466.16 to 200, 523.25 to 200, 466.16 to 200, 392.00 to 300)
        ),
        ChiptuneSong(
            id = 10,
            title = "Final Fantasy (Victory Fanfare)",
            notes = listOf(523.25 to 120, 523.25 to 120, 523.25 to 120, 523.25 to 300, 415.30 to 300, 466.16 to 300, 523.25 to 450)
        ),
        ChiptuneSong(
            id = 11,
            title = "Street Fighter II (Ryu Stage)",
            notes = listOf(440.00 to 150, 440.00 to 150, 523.25 to 150, 440.00 to 150, 587.33 to 150, 523.25 to 150, 493.88 to 300)
        ),
        ChiptuneSong(
            id = 12,
            title = "DuckTales (Moon Stage)",
            notes = listOf(523.25 to 150, 587.33 to 150, 659.25 to 150, 783.99 to 150, 659.25 to 150, 783.99 to 150, 880.00 to 300)
        ),
        ChiptuneSong(
            id = 13,
            title = "Contra (Title Theme)",
            notes = listOf(329.63 to 150, 392.00 to 150, 493.88 to 150, 659.25 to 150, 587.33 to 150, 523.25 to 150, 493.88 to 300)
        ),
        ChiptuneSong(
            id = 14,
            title = "Kirby's Dream Land (Green Greens)",
            notes = listOf(523.25 to 150, 659.25 to 150, 783.99 to 150, 1046.50 to 300, 987.77 to 150, 880.00 to 150, 783.99 to 300)
        ),
        ChiptuneSong(
            id = 15,
            title = "Doom (E1M1 At Doom's Gate)",
            notes = listOf(164.81 to 120, 164.81 to 120, 329.63 to 120, 164.81 to 120, 164.81 to 120, 293.66 to 120, 164.81 to 120, 164.81 to 120, 261.63 to 180)
        ),
        ChiptuneSong(
            id = 16,
            title = "Nyan Cat (Space Chiptune)",
            notes = listOf(739.99 to 120, 830.61 to 120, 554.37 to 120, 622.25 to 120, 493.88 to 120, 554.37 to 120, 622.25 to 180)
        )
    )

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
            "killall", "trim" -> handleKillall(args)
            "echo" -> handleEcho(args.joinToString(" "), currentSettings)
            "search", "google", "g" -> handleSearch(args.joinToString(" "))
            "feedback", "report", "bug" -> handleFeedback(args.joinToString(" "), currentSettings)
            "install" -> handleInstall(args.joinToString(" "))
            "cat", "read", "view" -> handleCat(args.joinToString(" "))
            "8bit", "tune", "chiptune", "music", "retro" -> handle8BitTune(args)
            "email", "mail", "sendmail" -> handleEmail(args)
            "whatsnew", "changelog", "news" -> handleWhatsNew()
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
            "cd" -> handleCd(args, currentSettings)
            "apps" -> handleAppsList()
            "open", "launch" -> handleOpenApp(args.joinToString(" "))
            "uninstall", "remove" -> handleUninstall(args.joinToString(" "))
            "alias" -> handleAlias(args, currentSettings)
            "unalias" -> handleUnalias(args)
            "history" -> handleHistory(currentSettings)
            "config" -> handleConfig(args, currentSettings)
            "settings" -> handleSystemSettings()
            "infofetch", "fastfetch", "neofetch" -> handleInfofetch(args, currentSettings)
            "top", "htop" -> handleTop(args)
            "battery" -> handleBattery(currentSettings)
            "calc" -> handleCalc(args.joinToString(" "))
            "sys", "toggle" -> handleSys(args, currentSettings)
            "call" -> handleCall(args.joinToString(" "))
            "sms" -> handleSms(args.joinToString(" "))
            "reboot", "shutdown" -> handleReboot()
            else -> handleFallback(resolvedCommand)
        }
    }

    private suspend fun handleInfofetch(args: List<String>, settings: LauncherSettings): CommandResult {
        val isUpdate = args.firstOrNull()?.lowercase() == "update"
        val lines = generateInfofetchLines(settings).toMutableList()
        if (isUpdate) {
            lines.add(0, TerminalLine(text = "[REFRESHED REAL-TIME SYSTEM INFORMATION]", type = LineType.SUCCESS))
        }
        return CommandResult(outputLines = lines)
    }

    suspend fun generateInfofetchLines(settings: LauncherSettings): List<TerminalLine> {
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val ramInfo = getRamUsageInfo()
        val batInfo = getBatteryStatusInfo(settings)
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
            "Shell: TerminalLauncher v1.3.6"
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

    private fun handleFeedback(userMsg: String, currentSettings: LauncherSettings): CommandResult {
        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val version = "v1.3.6"

        val trimmedMsg = userMsg.trim()
        val isForceEmail = trimmedMsg.lowercase().startsWith("email ") || trimmedMsg.lowercase() == "email"
        val messageBody = when {
            isForceEmail -> trimmedMsg.substringAfter("email").trim().ifBlank { "User Feedback Report" }
            trimmedMsg.isBlank() -> "User Feedback Report"
            else -> trimmedMsg
        }

        val formattedBody = """
            |--- USER FEEDBACK REPORT ---
            |Message: $messageBody
            |
            |--- DIAGNOSTIC METRICS ---
            |Launcher Version: $version
            |Device Model: $model
            |OS Version: $os
            |Theme: ${currentSettings.theme.name.lowercase()}
            |RAM: ${getRamUsageInfo()}
        """.trimMargin()

        val developerEmail = "crownemperordev@gmail.com"
        val mailtoUri = "mailto:$developerEmail?subject=${Uri.encode("Terminal Launcher $version Feedback")}&body=${Uri.encode(formattedBody)}"
        val githubIssueUrl = "https://github.com/CrownEmperor-dev/Terminal-Launcher/issues/new?title=${Uri.encode("User Feedback / Bug Report")}&body=${Uri.encode(formattedBody)}"

        if (isForceEmail) {
            return try {
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(emailIntent)
                CommandResult(
                    outputLines = listOf(
                        TerminalLine(text = "Opening email compose to '$developerEmail' with pre-filled feedback & diagnostic metrics...", type = LineType.SUCCESS)
                    )
                )
            } catch (e: Exception) {
                CommandResult(
                    outputLines = listOf(TerminalLine(text = "Unable to launch email client: ${e.message}", type = LineType.ERROR)),
                    success = false
                )
            }
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubIssueUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                outputLines = listOf(
                    TerminalLine(text = "Opening GitHub Issues with pre-filled feedback & diagnostic metrics...", type = LineType.SUCCESS)
                )
            )
        } catch (_: Exception) {
            try {
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(emailIntent)
                CommandResult(
                    outputLines = listOf(
                        TerminalLine(text = "Opening email compose to '$developerEmail' pre-filled with feedback & diagnostic metrics...", type = LineType.SUCCESS)
                    )
                )
            } catch (e: Exception) {
                CommandResult(
                    outputLines = listOf(TerminalLine(text = "Unable to launch feedback dispatcher: ${e.message}", type = LineType.ERROR)),
                    success = false
                )
            }
        }
    }

    private fun handleCat(filePath: String): CommandResult {
        val cleanPath = filePath.trim().removeSurrounding("\"").removeSurrounding("'")
        if (cleanPath.isBlank()) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "Usage: cat <file_path> or read <file_path>", type = LineType.ERROR)),
                success = false
            )
        }

        val targetFile = resolveStoragePath(cleanPath)
        if (!targetFile.exists()) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "cat: file not found: '$cleanPath'", type = LineType.ERROR)),
                success = false
            )
        }

        if (targetFile.isDirectory) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "cat: '$cleanPath' is a directory. Use 'ls' or 'cd' instead.", type = LineType.ERROR)),
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

    private fun handle8BitTune(args: List<String>): CommandResult {
        val arg = args.firstOrNull()?.lowercase()?.trim() ?: ""

        if (arg == "list" || arg == "playlist") {
            val lines = mutableListOf<TerminalLine>()
            lines.add(TerminalLine(text = "8-BIT CHIPTUNE JUKEBOX PLAYLIST (16 TRACKS):", type = LineType.HEADER))
            lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))
            chiptuneJukebox.forEach { song ->
                lines.add(TerminalLine(text = "  Track #${song.id} : ${song.title}", type = LineType.OUTPUT))
            }
            lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))
            lines.add(TerminalLine(text = "Usage: '8bit <1..16>' or 'tune <name>' (e.g. 8bit 3 or tune mario)", type = LineType.SYSTEM))
            return CommandResult(outputLines = lines)
        }

        val targetSong = when {
            arg.isEmpty() -> chiptuneJukebox.random()
            arg.toIntOrNull() != null -> {
                val trackNum = arg.toInt()
                chiptuneJukebox.firstOrNull { it.id == trackNum } ?: chiptuneJukebox.random()
            }
            else -> {
                chiptuneJukebox.firstOrNull { it.title.lowercase().contains(arg) } ?: chiptuneJukebox.random()
            }
        }

        return try {
            val sampleRate = 22050
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
                    for ((freq, durationMs) in targetSong.notes) {
                        if (freq <= 0.0) {
                            Thread.sleep(durationMs.toLong())
                            continue
                        }
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
                TerminalLine(text = "  ┌───┐ ┌───┐ ┌───┐ ┌───┐  [ 8-BIT RETRO JUKEBOX - TRACK #${targetSong.id} / 16 ]", type = LineType.HEADER),
                TerminalLine(text = "  │ █ │ │ █ │ │ █ │ │ █ │  ♪ ♫ Now Playing: ${targetSong.title} ♪ ♫", type = LineType.SUCCESS),
                TerminalLine(text = "  └───┘ └───┘ └───┘ └───┘  Synthesized Square Wave PCM (0 MB Overhead)", type = LineType.SYSTEM)
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

    private fun handleWhatsNew(): CommandResult {
        val lines = listOf(
            TerminalLine(text = "TERMINAL LAUNCHER - WHAT'S NEW IN V1.3.6", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "• Multi-Word Folder Support: 'cd storage0/Quick Share' & 'ls \"Quick Share\"' work seamlessly", type = LineType.OUTPUT),
            TerminalLine(text = "• All Files Access Prompt  : Run 'sys storage' to grant All Files Access on Android 11-15", type = LineType.OUTPUT),
            TerminalLine(text = "• Case-Insensitive Storage: 'ls storage0/download' & 'cd storage0/download' match 'Download'", type = LineType.OUTPUT),
            TerminalLine(text = "• Storage Directory Guard  : 'ls storage0/invalid' outputs explicit error instead of app list", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM)
        )
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
            .replace("\$VERSION", "v1.3.6")
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
            TerminalLine(text = "Version     : v1.3.6 (Build 12)", type = LineType.OUTPUT),
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

    private fun handleKillall(args: List<String>): CommandResult {
        val subArg = args.firstOrNull()?.lowercase()?.trim() ?: ""

        if (subArg == "recents" || subArg == "tasks") {
            return try {
                val intent = Intent("com.android.systemui.recents.SHOW_RECENTS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(
                    outputLines = listOf(
                        TerminalLine(text = "RECENT APPS OVERVIEW LAUNCHED", type = LineType.HEADER),
                        TerminalLine(text = "==================================================", type = LineType.SYSTEM),
                        TerminalLine(text = "Notice: Android OS restricts non-root launchers from programmatically swiping third-party tasks out of Recents UI.", type = LineType.SYSTEM),
                        TerminalLine(text = "Tap 'Clear All' in Recents UI to close task stacks.", type = LineType.SUCCESS),
                        TerminalLine(text = "==================================================", type = LineType.SYSTEM)
                    )
                )
            } catch (_: Exception) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    CommandResult(
                        outputLines = listOf(
                            TerminalLine(text = "Opened System Settings. Recents overview unavailable on this OEM.", type = LineType.SYSTEM)
                        )
                    )
                } catch (e: Exception) {
                    CommandResult(outputLines = listOf(TerminalLine(text = "Unable to launch Recents UI: ${e.message}", type = LineType.ERROR)), success = false)
                }
            }
        }

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availBeforeMb = memInfo.availMem / (1024 * 1024)
        val usedBeforeMb = totalMb - availBeforeMb

        return try {
            val pm = context.packageManager
            val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }

            var killedCount = 0
            val selfPkg = context.packageName

            installedPackages.forEach { pkgInfo ->
                val pkgName = pkgInfo.packageName
                if (pkgName != selfPkg && pkgName != "android" && pkgName != "com.android.systemui") {
                    try {
                        actManager.killBackgroundProcesses(pkgName)
                        killedCount++
                    } catch (_: Exception) {}
                }
            }

            System.gc()
            actManager.getMemoryInfo(memInfo)
            val availAfterMb = memInfo.availMem / (1024 * 1024)
            val usedAfterMb = totalMb - availAfterMb
            val freedMb = (availAfterMb - availBeforeMb).coerceAtLeast(0)

            CommandResult(
                outputLines = listOf(
                    TerminalLine(text = "BACKGROUND PROCESS TRIM & RAM RECOVERY", type = LineType.HEADER),
                    TerminalLine(text = "==================================================", type = LineType.SYSTEM),
                    TerminalLine(text = "Scanned Packages : ${installedPackages.size} installed apps", type = LineType.OUTPUT),
                    TerminalLine(text = "Trimmed Packages : $killedCount background processes", type = LineType.OUTPUT),
                    TerminalLine(text = "RAM Before Trim  : $usedBeforeMb MB used ($availBeforeMb MB free)", type = LineType.OUTPUT),
                    TerminalLine(text = "RAM After Trim   : $usedAfterMb MB used ($availAfterMb MB free)", type = LineType.SUCCESS),
                    TerminalLine(text = "RAM Memory Freed : $freedMb MB recovered", type = LineType.SUCCESS),
                    TerminalLine(text = "==================================================", type = LineType.SYSTEM),
                    TerminalLine(text = "Tip: Run 'trim recents' to launch Recent Apps Overview.", type = LineType.SYSTEM)
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
        val cleanQuery = appQuery.trim().removeSurrounding("\"").removeSurrounding("'")
        if (cleanQuery.isBlank()) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "Usage: uninstall <app_name_or_package>", type = LineType.ERROR)),
                success = false
            )
        }

        val app = appRepository.findApp(cleanQuery)
        if (app == null) {
            return CommandResult(
                outputLines = listOf(TerminalLine(text = "App not found for uninstallation: '$cleanQuery'", type = LineType.ERROR)),
                success = false
            )
        }

        val pkgName = app.packageName
        val label = app.label

        return try {
            val deleteIntent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkgName, null)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(deleteIntent)
            CommandResult(
                outputLines = listOf(
                    TerminalLine(text = "Opening system uninstallation prompt for $label ($pkgName)...", type = LineType.SUCCESS)
                )
            )
        } catch (_: Exception) {
            try {
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkgName, null)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                CommandResult(
                    outputLines = listOf(
                        TerminalLine(text = "Opened App Info for $label ($pkgName)...", type = LineType.SUCCESS),
                        TerminalLine(text = "Tap 'Uninstall' on the App Info page to remove.", type = LineType.SYSTEM)
                    )
                )
            } catch (e: Exception) {
                CommandResult(
                    outputLines = listOf(TerminalLine(text = "Unable to launch uninstaller for $label: ${e.message}", type = LineType.ERROR)),
                    success = false
                )
            }
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
        lines.add(TerminalLine(text = "Terminal Launcher v1.3.6 - Shell Manual", type = LineType.HEADER))
        lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))
        lines.add(TerminalLine(text = "CORE COMMANDS:", type = LineType.HEADER))
        lines.add(TerminalLine(text = "  options / customize GUI visual settings menu", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  about / version    App details, license, and repository URL", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  echo <text|\$var>   Print string or shell variables", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  search <query>     Search Google in web browser (or 'google' / 'g')", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  feedback <message> Send feedback or report bugs with device specs", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  install <query>    Search & install apps from Google Play Store", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  cat / read <path>  Read & display text files in terminal", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  8bit [1..16|list]  Play 16-track 8-bit retro chiptune jukebox", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  email <to> [sub]   Dispatch pre-filled email (Proton Mail / Gmail)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  time / date        Display system date, time, and epoch", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  killall / trim     Trim background tasks, recover RAM, or 'trim recents'", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  matrix             Full-screen matrix digital rain easter egg", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  ls [letter|dir]    List applications, storage (e.g. ls storage), or filter", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  cd <dir>           Change active storage directory (e.g. cd storage0/Download)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  open <app>         Explicitly launch an app by name or package", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  uninstall <app>    Trigger native uninstallation for an app", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  alias [k=v]       Set custom command shortcut (e.g., alias yt=YouTube)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  unalias <key>     Remove a custom alias", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  clear             Clear current terminal screen", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  history           Show command execution history", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  help / man        Display documentation", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "SETTINGS & PREFERENCES:", type = LineType.HEADER))
        lines.add(TerminalLine(text = "  config                     Show launcher CLI configurations", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set theme <t>       Set theme: green, amber, oled, mono, cyan, gameboy, solarized, dracula", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set statusbar <o>   Toggle status bar full immersion (on|off)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set autokeyboard <o> Toggle soft keyboard auto-show on launch (on|off)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set fastfetch <o>   Toggle startup system banner", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set prompt <text>   Customize shell prompt string", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  config set autocomplete <o> Toggle ghost auto-completion", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  settings                   Open Android device system settings", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "SYSTEM & UTILITIES:", type = LineType.HEADER))
        lines.add(TerminalLine(text = "  infofetch [update]   System hardware specs & live refresh", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  top [memory]         System monitor & RAM usage breakdown", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  battery [status|health] Power & battery health metrics", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  calc <expr>          Solve inline math expressions", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys flash            Toggle flashlight/torch hardware", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys ringer <s|v|n>   Set ringer mode: silent, vibrate, normal", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys statusbar        Toggle status bar full immersion", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys recents          Launch Recent Apps Overview screen", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys storage          Open All Files Access permission settings", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sys settings <page>  Open settings page (e.g. sys settings display)", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  call <number>        Launch phone dialer", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "  sms <number>         Launch SMS messaging", type = LineType.OUTPUT))
        lines.add(TerminalLine(text = "==================================================", type = LineType.SYSTEM))
        return CommandResult(outputLines = lines)
    }

    private fun handleLs(args: List<String>, settings: LauncherSettings): CommandResult {
        val rawTarget = args.joinToString(" ").trim().removeSurrounding("\"").removeSurrounding("'")
        val targetArg = if (rawTarget.isBlank()) currentPath else rawTarget
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
                val sysCmds = listOf("options", "about", "time", "killall", "trim", "echo", "search", "feedback", "install", "cat", "8bit", "email", "infofetch", "top", "battery", "calc", "sys flash", "sys wifi", "sys bt", "call", "sms", "config", "settings", "uninstall", "matrix")
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
                val isExplicitStorageQuery = targetArg.startsWith("storage0") || targetArg.startsWith("storage1") || targetArg.startsWith("/")
                val isCurrentPathStorage = currentPath.startsWith("storage")

                if (isExplicitStorageQuery || isCurrentPathStorage) {
                    val fullPath = if (targetArg.startsWith("storage") || targetArg.startsWith("/")) {
                        targetArg
                    } else if (targetArg.isNotEmpty() && targetArg != "~" && targetArg != "apps" && targetArg != "/apps") {
                        "$currentPath/$targetArg"
                    } else {
                        currentPath
                    }

                    val file = resolveStoragePath(fullPath)
                    if (file.exists() && file.isDirectory) {
                        val files = listFilesWithMediaStoreFallback(file).sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                        lines.add(TerminalLine(text = "DIRECTORY: $fullPath (${files.size} items)", type = LineType.HEADER))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                            lines.add(TerminalLine(text = "Notice: All Files Access permission is required on Android 11+ to view non-media files. Type 'sys storage' to grant.", type = LineType.SYSTEM))
                        }
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
                    } else if (isExplicitStorageQuery) {
                        return CommandResult(
                            outputLines = listOf(TerminalLine(text = "ls: directory not found: $fullPath", type = LineType.ERROR)),
                            success = false
                        )
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

    private fun listFilesWithMediaStoreFallback(dir: File): List<File> {
        val results = mutableListOf<File>()
        val directFiles = dir.listFiles()
        if (directFiles != null) {
            results.addAll(directFiles)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DISPLAY_NAME
                )
                val pathPrefix = dir.absolutePath
                val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
                val selectionArgs = arrayOf("$pathPrefix/%")

                val contentUris = listOf(
                    MediaStore.Files.getContentUri("external"),
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                )

                for (uri in contentUris) {
                    try {
                        val cursor = context.contentResolver.query(
                            uri,
                            projection,
                            selection,
                            selectionArgs,
                            null
                        )
                        cursor?.use { c ->
                            val dataIdx = c.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                            if (dataIdx != -1) {
                                while (c.moveToNext()) {
                                    val path = c.getString(dataIdx) ?: continue
                                    val itemFile = File(path)
                                    if (itemFile.parentFile?.absolutePath == dir.absolutePath && results.none { it.name.equals(itemFile.name, ignoreCase = true) }) {
                                        results.add(itemFile)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        return results
    }

    fun getCurrentPrompt(settings: LauncherSettings): String {
        val basePrompt = settings.promptString
        val baseUser = if (basePrompt.contains("@")) {
            basePrompt.substringBefore("@") + "@" + basePrompt.substringAfter("@").substringBefore(":")
        } else {
            "user@mobile"
        }
        val dirSegment = when {
            currentPath == "~" || currentPath == "/" -> "~"
            currentPath == "storage0" || currentPath == "/storage0" -> "~"
            currentPath.startsWith("storage0/") -> "~/" + currentPath.removePrefix("storage0/").removePrefix("/")
            currentPath == "storage1" || currentPath == "/storage1" -> "~/storage1"
            currentPath.startsWith("storage1/") -> "~/storage1/" + currentPath.removePrefix("storage1/").removePrefix("/")
            currentPath.startsWith("/") -> currentPath
            else -> "~/$currentPath"
        }
        return "$baseUser:$dirSegment$ "
    }

    private fun handleCd(args: List<String>, settings: LauncherSettings): CommandResult {
        val rawPath = args.joinToString(" ").trim().removeSurrounding("\"").removeSurrounding("'")
        val path = if (rawPath.isBlank()) "~" else rawPath

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
        val newPrompt = getCurrentPrompt(settings)
        return CommandResult(
            outputLines = listOf(TerminalLine(text = "Directory: $currentPath", type = LineType.SUCCESS)),
            updatedPrompt = newPrompt
        )
    }

    private fun resolveStoragePath(path: String): File {
        val internalStorage = Environment.getExternalStorageDirectory()
        val clean = path.trim()
        val sdCard = getSdCardDirectory() ?: internalStorage

        return when {
            clean == "storage0" || clean == "/storage0" -> internalStorage
            clean == "storage1" || clean == "/storage1" -> sdCard
            clean.startsWith("storage0/") || clean.startsWith("/storage0/") -> {
                val sub = clean.removePrefix("storage0/").removePrefix("/storage0/")
                resolveCaseInsensitivePath(internalStorage, sub)
            }
            clean.startsWith("storage1/") || clean.startsWith("/storage1/") -> {
                val sub = clean.removePrefix("storage1/").removePrefix("/storage1/")
                resolveCaseInsensitivePath(sdCard, sub)
            }
            clean.startsWith("/") -> File(clean)
            else -> resolveCaseInsensitivePath(internalStorage, clean)
        }
    }

    private fun resolveCaseInsensitivePath(baseDir: File, subPath: String): File {
        if (subPath.isBlank()) return baseDir
        val segments = subPath.split("/").filter { it.isNotEmpty() }
        var current = baseDir

        for (segment in segments) {
            val directFile = File(current, segment)
            if (directFile.exists()) {
                current = directFile
            } else {
                val children = current.listFiles()
                val caseMatch = children?.firstOrNull { it.name.equals(segment, ignoreCase = true) }
                if (caseMatch != null) {
                    current = caseMatch
                } else {
                    return File(current, segment)
                }
            }
        }
        return current
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

    private suspend fun handleBattery(settings: LauncherSettings): CommandResult {
        return CommandResult(outputLines = generateDetailedBatteryLines(settings))
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

    private suspend fun handleUnalias(args: List<String>): CommandResult {
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
                TerminalLine(text = "  statusBar      : ${if (settings.statusBarVisible) "visible" else "hidden (full immersion)"}", type = LineType.OUTPUT),
                TerminalLine(text = "  autoKeyboard   : ${if (settings.autoShowKeyboard) "on (auto-show)" else "off (tap to show)"}", type = LineType.OUTPUT),
                TerminalLine(text = "  textSizeSp     : ${settings.textSizeSp}sp", type = LineType.OUTPUT),
                TerminalLine(text = "  asciiArtStyle  : ${settings.asciiArtStyle.name.lowercase()}", type = LineType.OUTPUT),
                TerminalLine(text = "--------------------------------------------------", type = LineType.SYSTEM),
                TerminalLine(text = "Usage: config set <theme|fastfetch|prompt|autocomplete|statusbar|autokeyboard|size|ascii> <value>", type = LineType.SYSTEM)
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
                        "cyan", "neon" -> TerminalThemeType.CYAN_NEON
                        "gameboy", "dmg" -> TerminalThemeType.GAMEBOY_DMG
                        "solarized" -> TerminalThemeType.SOLARIZED
                        "dracula" -> TerminalThemeType.DRACULA
                        else -> null
                    }
                    if (themeEnum != null) {
                        settingsRepository.setTheme(themeEnum)
                        CommandResult(outputLines = listOf(TerminalLine(text = "Theme set to ${themeEnum.name.lowercase()}", type = LineType.SUCCESS)))
                    } else {
                        CommandResult(outputLines = listOf(TerminalLine(text = "Invalid theme. Options: green, amber, oled, mono, cyan, gameboy, solarized, dracula", type = LineType.ERROR)), success = false)
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
                "autokeyboard", "keyboard" -> {
                    val enabled = value.lowercase() in listOf("on", "true", "1", "yes")
                    settingsRepository.setAutoShowKeyboard(enabled)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Auto-show soft keyboard on launch set to ${if (enabled) "on" else "off"}", type = LineType.SUCCESS)))
                }
                "statusbar", "immersion" -> {
                    val visible = value.lowercase() !in listOf("off", "false", "0", "hide")
                    settingsRepository.setStatusBarVisible(visible)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Status bar set to ${if (visible) "visible" else "hidden (full immersion)"}", type = LineType.SUCCESS)))
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

    private suspend fun handleSys(args: List<String>, settings: LauncherSettings): CommandResult {
        val action = args.firstOrNull()?.lowercase() ?: ""
        val subArgs = args.drop(1)

        return when (action) {
            "flash", "flashlight", "torch" -> toggleFlashlight()
            "statusbar", "immersion" -> {
                val newVisible = !settings.statusBarVisible
                settingsRepository.setStatusBarVisible(newVisible)
                CommandResult(outputLines = listOf(TerminalLine(text = "Status bar set to ${if (newVisible) "visible" else "hidden (full immersion)"}", type = LineType.SUCCESS)))
            }
            "storage", "files", "permissions" -> handleAllFilesPermissionRequest()
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
            else -> CommandResult(outputLines = listOf(TerminalLine(text = "Usage: sys <flash|statusbar|storage|wifi|bt|sound|rotation|gps|hotspot|settings>", type = LineType.ERROR)), success = false)
        }
    }

    private fun handleAllFilesPermissionRequest(): CommandResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(outputLines = listOf(TerminalLine(text = "Opened All Files Access Permission Settings...", type = LineType.SUCCESS)))
            } catch (_: Exception) {
                try {
                    val genericIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(genericIntent)
                    CommandResult(outputLines = listOf(TerminalLine(text = "Opened All Files Access Permission Settings...", type = LineType.SUCCESS)))
                } catch (e: Exception) {
                    CommandResult(outputLines = listOf(TerminalLine(text = "Unable to open permission settings: ${e.message}", type = LineType.ERROR)), success = false)
                }
            }
        } else {
            return CommandResult(outputLines = listOf(TerminalLine(text = "All Files Access permission is not required on Android 10 and below.", type = LineType.SYSTEM)))
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

    private fun handleFallback(input: String): CommandResult {
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

    private suspend fun getBatteryEstimateString(pct: Int, isCharging: Boolean, isFull: Boolean, settings: LauncherSettings): String {
        val batManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val chargeTimeRemainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && batManager != null) {
            batManager.computeChargeTimeRemaining()
        } else -1L

        if (isFull || pct == 100) {
            if (settings.lastFullChargeTimestamp == 0L || (System.currentTimeMillis() - settings.lastFullChargeTimestamp > 300000)) {
                settingsRepository.setLastFullChargeTime(System.currentTimeMillis())
            }
            return "100% (Fully Charged)"
        }

        val lastFullMs = settings.lastFullChargeTimestamp
        val timeSinceFullStr = if (lastFullMs > 0L && !isCharging) {
            val elapsedMs = System.currentTimeMillis() - lastFullMs
            val mins = (elapsedMs / (1000 * 60)) % 60
            val hrs = elapsedMs / (1000 * 60 * 60)
            if (hrs > 0) "Full charge: ${hrs}h ${mins}m ago" else "Full charge: ${mins}m ago"
        } else if (!isCharging) {
            val uptimeMs = SystemClock.elapsedRealtime()
            val mins = (uptimeMs / (1000 * 60)) % 60
            val hrs = uptimeMs / (1000 * 60 * 60)
            if (hrs > 0) "Boot charge: ${hrs}h ${mins}m ago" else "Boot charge: ${mins}m ago"
        } else ""

        return when {
            isCharging -> {
                if (chargeTimeRemainingMs > 0) {
                    val mins = (chargeTimeRemainingMs / (1000 * 60)) % 60
                    val hrs = chargeTimeRemainingMs / (1000 * 60 * 60)
                    val timeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                    "$pct% (Charging, $timeStr till full)"
                } else {
                    "$pct% (Charging)"
                }
            }
            else -> {
                val totalEstMins = (pct * 12).toLong() // Heuristic estimate: 100% = ~20h
                val hrs = totalEstMins / 60
                val mins = totalEstMins % 60
                val timeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                val prefix = if (timeSinceFullStr.isNotEmpty()) "$timeSinceFullStr | " else ""
                "$pct% (Discharging, $prefix~$timeStr till drain)"
            }
        }
    }

    private suspend fun getBatteryStatusInfo(settings: LauncherSettings): String {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 0

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        return getBatteryEstimateString(pct, isCharging, isFull, settings)
    }

    private fun generateTopLines(): List<TerminalLine> {
        val ram = getRamUsageInfo()
        val battery = "Run 'battery' for live estimates"
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

    private suspend fun generateBatteryLines(settings: LauncherSettings): List<TerminalLine> {
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

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING
        val isFull = statusInt == BatteryManager.BATTERY_STATUS_FULL
        val estString = getBatteryEstimateString(pct, isCharging, isFull, settings)

        return listOf(
            TerminalLine(text = "BATTERY HARDWARE STATUS", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Level & Est : $estString", type = LineType.OUTPUT),
            TerminalLine(text = "Temperature : ${String.format(Locale.US, "%.1f", temp)} °C", type = LineType.OUTPUT),
            TerminalLine(text = "Health      : $health", type = LineType.OUTPUT),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Tip: Run 'battery status' for full hardware details.", type = LineType.SYSTEM)
        )
    }

    private suspend fun generateDetailedBatteryLines(settings: LauncherSettings): List<TerminalLine> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 0

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING
        val isFull = statusInt == BatteryManager.BATTERY_STATUS_FULL
        val statusStr = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full (100%)"
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

        val estimateText = getBatteryEstimateString(pct, isCharging, isFull, settings)

        return listOf(
            TerminalLine(text = "DETAILED BATTERY & POWER METRICS", type = LineType.HEADER),
            TerminalLine(text = "==================================================", type = LineType.SYSTEM),
            TerminalLine(text = "Charge Level  : $pct%", type = LineType.OUTPUT),
            TerminalLine(text = "Power State   : $statusStr", type = LineType.OUTPUT),
            TerminalLine(text = "Estimate      : $estimateText", type = LineType.SUCCESS),
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
