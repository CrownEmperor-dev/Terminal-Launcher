# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED META-INSTRUCTIONS FOR ALL CHAT SESSIONS & AGENTS**
> 1. **Single Source of Truth**: `docs/IMPLEMENTATION_PLAN.md` (and `implementation_plan.artifact.md`) is the master implementation plan file for Terminal Launcher. Always read and update this file regularly.
> 2. **Pre-Implementation Protocol**: When the user provides ideas or bug reports, update `docs/IMPLEMENTATION_PLAN.md` first with detailed technical strategies. Present the updated plan to the user and await explicit approval before modifying any code files.
> 3. **Execution & Version Bump Protocol**: After implementing and verifying a feature with `./gradlew app:assembleDebug` and `./gradlew app:testDebugUnitTest`:
>    - Increment version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` (Minor `+0.1.0` for features, Patch `+0.0.1` for tweaks/fixes).
>    - Output a **"Fix and New Features Log"** summary at the start of your response.
> 4. **Infofetch Version Synchronization Rule**: Before beginning any task or releasing a build, verify and update `generateInfofetchLines()` in `CommandEngine.kt` so that `infofetch` and the startup shell banner always display the current, newest version (e.g., `v1.4.0`).
> 5. **Session Override Rule**: This protocol overrides any conflicting parameters from previous chat sessions.

---

## Active Priority Queue (Next Intake Stack)

Any new feature requests, commands, or project phases will be placed at the top of this queue for execution.

### 1. In-Launcher & System Screenshot Command (`screenshot` / `sc` / `sys screenshot`) [NEXT IN QUEUE]
- **Technical Execution Strategy**:
  - **In-Launcher Window Capture (Primary)**: Use Compose `GraphicsLayer` / `drawToBitmap()` or `PixelCopy.request()` on MainActivity Window surface to capture high-res PNG image of the terminal shell.
  - **Storage Persistence**: Automatically save screenshot PNG file to `storage0/Pictures/Screenshots/screenshot_<timestamp>.png`.
  - **System Screenshot Fallback**: On Android 11+ (API 30+), support `screenshot sys` using `AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT` for full OS screen capture.
  - **Terminal Output**: Display saved PNG file path and size (e.g. `[SUCCESS]: Screenshot saved to storage0/Pictures/Screenshots/screenshot_20260228_123000.png (420 KB)`).
- **Technical Execution Strategy**:
  - **In-Launcher Window Capture (Primary)**: Use Compose `GraphicsLayer` / `drawToBitmap()` or `PixelCopy.request()` on MainActivity Window surface to capture high-res PNG image of the terminal shell.
  - **Storage Persistence**: Automatically save screenshot PNG file to `storage0/Pictures/Screenshots/screenshot_<timestamp>.png`.
  - **System Screenshot Fallback**: On Android 11+ (API 30+), support `screenshot sys` using `AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT` for full OS screen capture.
  - **Terminal Output**: Display saved PNG file path and size (e.g. `[SUCCESS]: Screenshot saved to storage0/Pictures/Screenshots/screenshot_20260228_123000.png (420 KB)`).

### 3. Enhanced `infofetch` System Metrics (CPU, Disks, App Counts)
- **Technical Execution Strategy**:
  - **CPU Specs**: Query `Build.SUPPORTED_ABIS.firstOrNull()` and `Runtime.getRuntime().availableProcessors()` (e.g., `CPU: arm64-v8a (8 Cores)`).
  - **Disk Storage**: Use `StatFs` on `Environment.getDataDirectory()` to calculate free vs. total GB (e.g., `Storage: 42.5 GB free / 128.0 GB`).
  - **App Counts**: Query `packageManager.getInstalledPackages(0)` to count user-installed apps vs. system apps (e.g., `Apps: 64 User / 180 System (244 Total)`).
  - **Banner Integration**: Render these metrics directly inside `generateInfofetchLines()` in `CommandEngine.kt`.

### 4. Direct `sys ringer` / `sys sound` Mode Control & Do Not Disturb (DND) Permission Handling
- **Reported Bug**: Executing `sys ringer` on Xiaomi Redmi Note 8 (Android 11) redirects the user to Sound Settings instead of changing the ringer mode.
- **Root Cause Analysis**:
  1. `handleRingerMode()` only matched exact strings `"silent"`, `"vibrate"`, `"normal"`. Short tokens documented in manual like `"s"`, `"v"`, `"n"`, or no sub-argument defaulted to `else` which opened `Settings.ACTION_SOUND_SETTINGS`.
  2. On Android 7.0+ (API 24+), changing `ringerMode` to `RINGER_MODE_SILENT` throws a `SecurityException` unless Do Not Disturb (DND) Access (`NotificationManager.isNotificationPolicyAccessGranted`) is granted.
- **Solution Strategy**:
  - Support all shortcuts and synonyms: `"s"`, `"silent"`, `"mute"` -> Silent Mode; `"v"`, `"vibrate"` -> Vibrate Mode; `"n"`, `"normal"`, `"sound"`, `"ring"` -> Normal Mode.
  - Cycle Mode when no argument is passed: Running `sys ringer` without arguments toggles/cycles through `NORMAL` -> `VIBRATE` -> `SILENT` -> `NORMAL` dynamically and displays current state.
  - Catch `SecurityException` gracefully on Android 7-15 and launch `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` with clear prompt: `Please grant Do Not Disturb Access for Terminal Launcher to set Silent Mode directly`.

### 5. Help & Manual (`help` / `man`) Complete Command Audit & Update
- **Goal**: Audit and update `handleHelp()` in `CommandEngine.kt` to ensure every built-in command, utility, and flag is fully listed and documented.
- **Audit Checklist**:
  - Core: `options`, `about`, `version`, `whatsnew`, `echo`, `search`, `feedback`, `install`, `cat`, `mkdir`, `rm`, `8bit`, `email`, `time`, `killall`, `trim`, `matrix`, `ls`, `cd`, `open`, `uninstall`, `alias`, `unalias`, `clear`, `history`, `help`.
  - Power & Hardware: `reboot`, `restart`, `shutdown`, `poweroff`, `screenshot`.
  - Preferences: `config` (theme, statusbar, autokeyboard, fastfetch, prompt, autocomplete), `settings`.
  - Utilities: `infofetch`, `top`, `battery`, `calc`, `sys` (`flash`, `ringer`, `statusbar`, `recents`, `storage`, `settings`, `gps`, `hotspot`).

### 6. Full `grep` Implementation & Command Piping (`|`) & Wildcards (`*.log`)
- **`grep` Command Flags**:
  - `-i` : Case-insensitive match.
  - `-v` : Invert match (output non-matching lines).
  - `-n` : Print line numbers (`12: matching line`).
  - `-c` : Count total matching lines.
  - `-r` : Recursive search inside subdirectories.
  - `-w` : Exact word boundary match (`\bpattern\b`).
- **Command Piping (`|`)**: Support piping left-hand output into right-hand `grep` filter (e.g., `ls | grep ".txt"` or `history | grep "config"`).
- **Wildcard Expansion (`*.log`, `*.txt`)**: Support searching across multiple files (e.g., `grep "error" *.log`).

---

## Completed Phases & Feature Summary

### Version 1.4.1 Patch Release [COMPLETED]
- **System Power Commands (`reboot`, `restart`, `shutdown`, `poweroff`)**: Executes direct root-privileged shutdown/reboot (`su -c "reboot"` / `su -c "reboot -p"`) with graceful fallback to Android's System Power Menu dialog on non-rooted devices.

### Version 1.4.0 Feature Release [COMPLETED]
- **Directory Creation (`mkdir`)**: `mkdir <dir_name>` creates subdirectories in active working directory or explicit storage path.
- **File & Folder Deletion (`rm`)**: `rm <file>` removes files; `rm -r <dir>` recursively deletes directory trees.
- **Shell Output Redirection (`>` & `>>`)**: `echo "text" > file.txt` (overwrite) and `time >> log.txt` (append) redirect command output to target files.
- **Dynamic Linux Terminal Prompt Path**: Shell prompt dynamically reflects active working directories (`user@mobile:~/Download$ `) upon `cd`.

### Version 1.3.6 Patch Release [COMPLETED]
- **Multi-Word Directory & File Name Support**: `handleCd`, `handleLs`, `handleCat` now join all argument tokens and strip surrounding quotes. Unquoted multi-word paths (`cd storage0/Quick Share`) and quoted paths (`cd "storage0/Quick Share"`) resolve 100% seamlessly.
- **Android 11–15 All Files Access Permission Prompt (`sys storage`)**: Added `sys storage` command launching `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` so users on Waydroid & Android 15 can grant All Files Access in 1 click to view non-media files in `ls`.

### Version 1.3.5 Patch Release [COMPLETED]
- **Case-Insensitive Storage Path Resolution**: `resolveStoragePath` performs step-by-step case-insensitive matching (`storage0/download` or `cd storage0/download` or `ls storage0/pictures` seamlessly resolves capitalized directories like `Download` and `Pictures`).
- **Storage Directory Guard in `ls`**: Queries starting with `storage0/`, `storage1/`, or `/` output explicit `ls: directory not found: <path>` errors when non-existent, instead of erroneously falling back to application searches.
- **Multi-Provider MediaStore File Enumeration**: Merges `MediaStore.Files`, `MediaStore.Downloads`, `MediaStore.Images`, `MediaStore.Video`, and `MediaStore.Audio` content queries so regular files (`.txt`, `.pdf`, `.apk`, `.zip`, `.png`, `.mp4`) render alongside directories on Waydroid (Android 12) & Android 15.
- **Complete Termux Code Purge**: Completely purged all remaining Termux helper functions (`handleTermux`/`handleTermuxSetup`) and completions from `CommandEngine.kt` and `TerminalViewModel.kt`.

### Version 1.3.2 / 1.3.4 Patch Release [COMPLETED]
- **Uninstallation Fix**: Fixed `uninstall <app>` across Android 8.1, Android 11 (MIUI), and Android 15 using canonical `Uri.fromParts` and direct App Info fallback.

### Version 1.3.1 Patch Release [COMPLETED]
- **Official System `PackageInstaller.uninstall()` Integration**: Replaced deprecated intent filters with `PackageInstaller.uninstall()`.

### Version 1.3.0 Feature Release [COMPLETED]
- **Enhanced RAM Trimmer**: Scans all installed packages (`packageManager.getInstalledPackages(0)`) and kills third-party background processes.
- **Recent Apps Overview Launcher (`trim recents` / `sys recents`)**.

### Version 1.2.3 Patch Release [COMPLETED]
- **Dual Feedback & Bug Reporting System**: GitHub Issues primary & direct email (`crownemperordev@gmail.com`) secondary.

### Version 1.2.2 Patch Release [COMPLETED]
- **Waydroid & Linux PC Cursor Focus Fix**: Active input focus and blinking cursor on boot with soft keyboard suppression.

### Version 1.2.1 Patch Release [COMPLETED]
- **Waydroid & Hardware Keyboard Optimization**: Added `autoShowKeyboard` preference key (`config set autokeyboard <on|off>`) and GUI switch toggle in `OptionsMenuDialog`.

### Version 1.2.0 Feature Release [COMPLETED]
- **Expanded Retro Color Schemes**: Added Cyberpunk Cyan, Game Boy, Solarized Dark, Dracula.
- **Multi-Color Custom ASCII Banners**: Inline `[#HEX]` color tag parsing.
- **Full Immersion Status Bar Toggle**: `config set statusbar <on|off>`.
- **Battery Metric Fix (Time Since Full Charge)**.

### Version 1.1.1 Patch Release [COMPLETED]
- **16-Track 8-Bit Retro Chiptune Jukebox** (`8bit`, `tune`).

### Version 1.1.0 Feature Release [COMPLETED]
- **Battery Estimates**, **Duplicate App Auto-Disambiguation**, **`cat <file>` Viewer**, **`install <app>` Play Store Launcher**.

### Version 1.0.0 Initial Features [COMPLETED]
- **Phase 1-4**: Performance, Storage Navigation, Direct Web URLs, `echo` variables, Custom ASCII editor, Text Selection & `Ctrl+C`/`Ctrl+V`.

---

## Deferred Backlog (Next Feature Release Intake Stack)
- `mkdir` & `rm` commands.
- Shell Output Redirection (`>` & `>>`).
- Full `grep` implementation (`-i`, `-v`, `-n`, `-c`, `-r`, `-w`), Piping (`|`), and Wildcards (`*.log`).
- Enhanced `infofetch` System Metrics (CPU, Disks, App Count).
- Termux Intent Execution Bridge (Cancelled & Purged).
- `wa <number> <message>` / `whatsapp`: Direct WhatsApp messaging.
