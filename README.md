# 🖥️ Terminal Launcher

```text
  _____                   _             _   _                            _                     
 |_   _|___ _ __ _ __ ___ (_)_ __   __ _| | | |    __ _ _   _ _ __   ___| |__   ___ _ __ 
   | |/ _ \ '__| '_ ` _ \| | '_ \ / _` | | | |   / _` | | | | '_ \ / __| '_ \ / _ \ '__|
   | |  __/ |  | | | | | | | | | | (_| | | | |__| (_| | |_| | | | | (__| | | |  __/ |   
   |_|\___|_|  |_| |_| |_|_|_| |_|\__,_|_| |_____\__,_|\__,_|_| |_|\___|_| |_|\___|_|   
```

> **A lite, keyboard-optimized Android Home Screen Launcher built around a Linux terminal command interface.** Designed specifically for physical hardware keyboard phones (*Clicks Communicator*, *Unihertz Titan / Titan Pocket*, *BlackBerry KeyONE*) and touch Android devices.

---

```text
user@mobile:~$ infofetch
--------------------------------------------------
       __   __       
      (  \_/  )      
       \     /       
      .-'   '-.      
     /  O   O  \     
    |           |    
   .-------------.   
  /               \  
--------------------------------------------------
OS: Android 8.0+ (API 26+)
Host: Android Mobile Device
Kernel: Linux 5.x / Android Runtime
Shell: TerminalLauncher v1.2
UI Engine: Jetpack Compose
Edge-To-Edge & Full Immersion: Active (Hide Status Bar Option)
Themes: CRT Green | Amber Phosphor | OLED Black | Monochrome | Cyberpunk Cyan | Game Boy | Solarized | Dracula
ASCII Banners: Default | Bugdroid | Tux | Retro PC | Minimal | Custom Multi-Color | Off
Autocomplete: Inline Ghost Prediction & Duplicate App Auto-Disambiguation
Text Selection: Highlight & Copy Output Buffer / Clipboard Paste (Ctrl+C / Ctrl+V)
Documentation: [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) | [Developer Tutorial](docs/DEVELOPER_TUTORIAL.md)
--------------------------------------------------
```

---

## 💻 `user@mobile:~$ man terminal-launcher`

```text
NAME
       terminal-launcher - Keyboard-first Android home screen shell

SYNOPSIS
       [command] [arguments]
       <app_name_or_alias>
       <web_url>

DESCRIPTION
       Terminal Launcher replaces standard icon grids with an ultra-responsive monospaced
       shell interface. It provides instant app launching, duplicate app name auto-disambiguation, inline ghost completion, retro CRT
       visual styles, hardware keyboard shortcuts, text selection/copy-paste, a GUI options menu, storage navigation, feedback reporting, and built-in system management.

OPTIONS & COMMANDS
       core & gui:
           options / customize Open dedicated retro GUI Options Menu overlay.
           about / version    Display app version, credits, license, and repository URL.
           echo <text|$var>   Print text or shell variables ($USER, $PROMPT, $THEME, $VERSION, $OS).
           search / google / g Search Google directly in default browser (e.g. search Android 15).
           feedback <msg>     Submit feedback or bug report with automatic device specs.
           install <query>    Search & install apps directly from Google Play Store.
           cat / read <path>  Read and view text files directly inside terminal buffer.
           8bit [1..16|list]  Play 16-track 8-bit retro chiptune jukebox.
           email <to> [sub]   Dispatch pre-filled email compose (Proton Mail / Gmail).
           tx / termux <cmd>  Execute Linux commands, scripts, or package managers via Termux.
           time / date        Display current system time, date, and Unix epoch seconds.
           killall / trim     Trim background processes & free RAM (ideal for 4GB devices).
           matrix             Trigger full-screen Matrix Digital Rain easter egg.
           ls [letter|dir]    List applications, storage (ls /storage), or filter (ls o).
           cd <dir>           Navigate storage directories (e.g. cd storage0/Download).
           open <app>         Explicitly launch an application by label or package name.
           uninstall <app>    Trigger native Android uninstallation prompt for an app.
           alias <k=v>        Define custom command shortcuts (e.g. alias yt=YouTube).
           unalias <key>      Remove a custom alias.
           clear              Clear current screen line buffer (Shortcut: Ctrl + L).
           history            Display command execution history.
           help / man         Display command manual and usage documentation.
           <web_url>          Typing any URL (e.g. github.com, https://google.com) opens default browser.

       config & preferences:
           config                     Show current launcher preferences in shell.
           config set theme <theme>   Set palette: green, amber, oled, mono, cyan, gameboy, solarized, dracula.
           config set statusbar <o>   Toggle status bar full immersion (on|off).
           config set fastfetch <o>   Toggle startup system banner display.
           config set prompt <text>   Customize shell prompt string (default: user@mobile:~$ ).
           config set autocomplete <o> Toggle ghost auto-completion overlay.
           settings                   Open native Android device system settings.

       system & utilities:
           infofetch [update]   System hardware specs & real-time live refresh.
           top [memory]         System monitor & RAM heap allocation breakdown.
           battery [status]     Power state, temperature, voltage, and health metrics.
           calc <expression>    Evaluate inline mathematical equations (e.g. calc (15*4)/3).
           sys flash            Toggle flashlight / camera torch hardware.
           sys ringer <s|v|n>   Set ringer mode: silent, vibrate, normal.
           sys rotation         Toggle auto-rotation lock.
           sys statusbar        Toggle status bar full immersion.
           sys gps / hotspot    Open Location / Hotspot settings.
           sys settings <page>  Open settings page (e.g. sys settings display, sys settings app spotify).
           call <number>        Launch phone dialer or direct call intent.
           sms <number>         Launch SMS messaging.
```

---

## ⚙️ `user@mobile:~$ options`

Type `options` (or `customize`) anywhere in the terminal to launch the **GUI Options Overlay**:

* **Theme Color Palette**: CRT Green, Amber Phosphor, OLED Black, Monochrome, Cyberpunk Cyan, Game Boy Classic, Solarized Dark, Dracula Retro.
* **Font Size**: Small (12sp), Medium (15sp), Large (18sp).
* **ASCII Art Banners**: Default title logo, **Bugdroid** (Android mascot), **Tux** (Linux mascot), Retro PC, Minimal, **Custom Multi-Color Editor** (use `[#HEX]` color tags), or Off.
* **Toggles**: Startup Infofetch banner, Inline Ghost Autocomplete, and Full Immersion Status Bar hiding.
* **Prompt Editor**: Live shell prompt text editing.
* **About Card**: Displays app metadata, developer credits, MIT License, and GitHub repository URL.

---

## 🎨 `user@mobile:~$ config set theme`

Terminal Launcher supports 8 retro color schemes:

| Theme | Command | Visual Style |
| :--- | :--- | :--- |
| **CRT Green** | `config set theme green` | `#00FF66` phosphor green on dark matrix `#0A0D0A` |
| **Amber Phosphor** | `config set theme amber` | `#FFB000` warm amber text on deep brown `#0D0B08` |
| **OLED Black** | `config set theme oled` | Crisp `#FFFFFF` text on `#000000` pitch black |
| **Monochrome** | `config set theme mono` | Slate gray `#E0E0E0` on minimal dark `#121212` |
| **Cyberpunk Cyan** | `config set theme cyan` | Electric Cyan `#00F0FF` on Deep Navy `#050B14` |
| **Game Boy Classic** | `config set theme gameboy` | Retro Olive `#0F380F` & `#306230` on Olive Screen `#9BBC0F` |
| **Solarized Dark** | `config set theme solarized` | Solarized Teal `#839496` & Yellow `#B58900` on `#002B36` |
| **Dracula Retro** | `config set theme dracula` | Vibrant Purple `#BD93F9` & Pink `#FF79C6` on `#282A36` |

---

## ⌨️ `user@mobile:~$ cat /shortcuts`

```text
KEYBOARD SHORTCUTS & NAVIGATION
--------------------------------------------------
  [Up / Down Arrow]      Cycle backwards & forwards through command history
  [Tab / Spacebar]       Accept inline gray ghost auto-complete suggestion
  [Backspace]            Dismiss/suppress current ghost autocomplete prediction
  [Ctrl + C]             Copy active prompt text to clipboard
  [Ctrl + V]             Paste clipboard text into command input prompt
  [Ctrl + L]             Clear screen line buffer
  [Long Press Buffer]    Highlight, select, and copy terminal output lines
  [Enter]                Execute typed command / launch app (or dismiss Matrix)
  [Tap Anywhere]         Refocus command input prompt
--------------------------------------------------
```

---

## 🚀 `user@mobile:~$ ./build.sh`

### Requirements
* Android 8.0+ (API level 26+)
* Android Studio
* JDK 17+

### Installation & Build
```bash
# Clone repository
git clone https://github.com/CrownEmperor-dev/Terminal-Launcher.git

# Navigate to project directory
cd Terminal-Launcher

# Build debug APK
./gradlew app:assembleDebug

# Install on connected Android device via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Documentation & Roadmap
* **[Implementation Plan](docs/IMPLEMENTATION_PLAN.md)**: Living roadmap, version history, and active priority queue.
* **[Developer Tutorial Guide](docs/DEVELOPER_TUTORIAL.md)**: Complete textbook manual on how Terminal Launcher is built from scratch.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/LICENSE) file for details.

```text
user@mobile:~$ echo "Built with Jetpack Compose & Kotlin"
Built with Jetpack Compose & Kotlin
```
