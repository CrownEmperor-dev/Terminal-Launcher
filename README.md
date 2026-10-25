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
Shell: TerminalLauncher v1.0
UI Engine: Jetpack Compose
Edge-To-Edge: Active (Transparent System Bars)
Themes: CRT Green | Amber Phosphor | OLED Black | Monochrome
ASCII Banners: Default | Bugdroid | Tux | Retro PC | Minimal | Off
Autocomplete: Inline Ghost Prediction
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

DESCRIPTION
       Terminal Launcher replaces standard icon grids with an ultra-responsive monospaced
       shell interface. It provides instant app launching, inline ghost completion, retro CRT
       visual styles, hardware keyboard shortcuts, a GUI options menu, storage navigation, and built-in system management.

OPTIONS & COMMANDS
       core & gui:
           options / customize Open dedicated retro GUI Options Menu overlay.
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

       config & preferences:
           config                     Show current launcher preferences in shell.
           config set theme <theme>   Set palette: green, amber, oled, mono.
           config set size <sp>       Set font size (e.g., config set size 15).
           config set ascii <style>   Set banner style: default, bugdroid, tux, retro_pc, minimal, off.
           config set fastfetch <on|off> Toggle startup system banner display.
           config set prompt <text>   Customize shell prompt string (default: user@mobile:~$ ).
           config set autocomplete <on|off> Toggle ghost auto-completion overlay.
           settings                   Open native Android device system settings.

       system & utilities:
           infofetch [update]   System hardware specs & real-time live refresh.
           top [memory]         System monitor & RAM heap allocation breakdown.
           battery [status]     Power state, temperature, voltage, and health metrics.
           calc <expression>    Evaluate inline mathematical equations (e.g. calc (15*4)/3).
           sys flash            Toggle flashlight / camera torch hardware.
           sys wifi / bt / sound Open system settings panels directly.
           call <number>        Launch phone dialer or direct call intent.
           sms <number>         Launch SMS messaging.
```

---

## ⚙️ `user@mobile:~$ options`

Type `options` (or `customize`) anywhere in the terminal to launch the **GUI Options Overlay**:

* **Theme Color Palette**: CRT Green, Amber Phosphor, OLED Black, Monochrome.
* **Font Size**: Small (12sp), Medium (15sp), Large (18sp).
* **ASCII Art Banners**: Default title logo, **Bugdroid** (Android mascot), **Tux** (Linux mascot), Retro PC, Minimal, or Off.
* **Toggles**: Startup Infofetch banner and Inline Ghost Autocomplete.
* **Prompt Editor**: Live shell prompt text editing.

---

## 🎨 `user@mobile:~$ config set theme`

Terminal Launcher supports 4 retro color schemes:

| Theme | Command | Visual Style |
| :--- | :--- | :--- |
| **CRT Green** | `config set theme green` | `#00FF66` phosphor green on dark matrix `#0A0D0A` |
| **Amber Phosphor** | `config set theme amber` | `#FFB000` warm amber text on deep brown `#0D0B08` |
| **OLED Black** | `config set theme oled` | Crisp `#FFFFFF` text on `#000000` pitch black |
| **Monochrome** | `config set theme mono` | Slate gray `#E0E0E0` on minimal dark `#121212` |

---

## ⌨️ `user@mobile:~$ cat /shortcuts`

```text
KEYBOARD SHORTCUTS & NAVIGATION
--------------------------------------------------
  [Up / Down Arrow]      Cycle backwards & forwards through command history
  [Tab / Spacebar]       Accept inline gray ghost auto-complete suggestion
  [Ctrl + L]             Clear screen line buffer
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

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/LICENSE) file for details.

```text
user@mobile:~$ echo "Built with Jetpack Compose & Kotlin"
Built with Jetpack Compose & Kotlin
```
