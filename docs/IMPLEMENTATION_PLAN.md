# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED META-INSTRUCTIONS FOR ALL CHAT SESSIONS & AGENTS**
> 1. **Single Source of Truth**: `docs/IMPLEMENTATION_PLAN.md` (and `implementation_plan.artifact.md`) is the master implementation plan file for Terminal Launcher. Always read and update this file regularly.
> 2. **Pre-Implementation Protocol**: When the user provides ideas or bug reports, update `docs/IMPLEMENTATION_PLAN.md` first with detailed technical strategies. Present the updated plan to the user and await explicit approval before modifying any code files.
> 3. **Execution & Version Bump Protocol**: After implementing and verifying a feature with `./gradlew app:assembleDebug` and `./gradlew app:testDebugUnitTest`:
>    - Increment version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` (Minor `+0.1.0` for features, Patch `+0.0.1` for tweaks/fixes).
>    - Output a **"Fix and New Features Log"** summary at the start of your response.
> 4. **Session Override Rule**: This protocol overrides any conflicting parameters from previous chat sessions.

---

## Active Priority Queue (Next Intake Stack)

### 1. Canonical `Uri.fromParts` Uninstallation & Direct App Info Fallback (`uninstall <app>`) [TOP PRIORITY]
- **Problem**: `PackageInstaller.uninstall()` or `ACTION_DELETE` gets intercepted/blocked on MIUI (Redmi Note 8) and Motorola (Android 15) OEM security policies unless special developer options are enabled.
- **Solution Strategy**:
  1. **Primary Intent**: `Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkgName, null))` with `FLAG_ACTIVITY_NEW_TASK`.
  2. **Direct App Info Page Fallback (100% Guaranteed to Work on ALL Android Devices)**:
     If the system uninstallation dialog is intercepted by MIUI or Android 15, Terminal Launcher immediately opens the native App Info Settings page (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, `Uri.fromParts("package", pkgName, null)`).
     The native App Info page opens instantly with the prominent **"UNINSTALL"** button, allowing 1-tap uninstallation across all Android OEMs (MIUI, Motorola, Samsung, Pixel, Android 15)!

### 2. Complete Removal of Termux Implementation
- **Reasoning**: Termux's background service (`com.termux.app.RunCommandService`) relies on internal Termux Linux socket paths (`/data/data/com.termux/files/usr/bin/...`) and requires `allow-external-apps = true` in `~/.termux/termux.properties` plus explicit `termux-reload-settings`. On modern Android 11–15 OEMs (MIUI, Motorola, OneUI), background service intents across non-system app boundaries are blocked by OS security, creating fragile "spaghetti" fallback code that degrades user experience.
- **Execution Plan**:
  - Remove `handleTermux`, `handleTermuxSetup`, `tx`, `termux` routing in `CommandEngine.kt`.
  - Remove `<uses-permission android:name="com.termux.permission.RUN_COMMAND" />` in `AndroidManifest.xml`.
  - Remove Termux sub-command completions in `TerminalViewModel.kt`.

### 3. Enhanced `infofetch` System Metrics & New Features log.
- Expand `infofetch` output to include:
    - **CPU Specs**: Active CPU Cores, Max Frequency, Architecture ABI.
    - **Disk Storage**: Internal Storage total/used/free MB/GB & Mounted MicroSD Storage.
    - **Installed App Count**: Total user apps installed and total system packages count.
- Show new features and fixes log after each app update. show as a message box after installation and as a summary in options menu.

### 4. Fix `ls` File Visibility on Android 15 (Moto G35 / API 35)
- **Problem**: On Android 15, `ls` lists directories but hides regular files (`.txt`, `.pdf`, `.apk`) due to Android 15 scoped storage file enumeration filters.
- **Solution Strategy**:
  - Update `handleLs()` to use `File.listFiles()` with explicit `MediaStore.Files` / `Environment.isExternalStorageManager()` query fallbacks to guarantee regular files display alongside directories across all Android 15 devices.

### 5. Dynamic Linux Terminal Prompt Path (`user@mobile:~/Download$ `)
- **Feature Goal**: Mirror standard Linux bash shells where the prompt reflects the current active directory.
- **Execution Logic**:
  - Root: `user@mobile:~$ `
  - Inside `storage0/Download`: `user@mobile:~/Download$ `
  - Inside `storage0/Download/Docs`: `user@mobile:~/Download/Docs$ `
  - Update `TerminalViewModel` state and prompt calculation dynamically when `cd` changes `currentPath`.

### 6. File Creation (`mkdir`), Deletion (`rm`), and Output Redirection (`echo "text" > file.txt`)
- **`mkdir <dir_name>`**: Creates new subdirectory inside active `cd` folder or path.
- **`rm <file_or_dir>`**: Deletes specified file or directory (`rm -r` for recursive folder removal).
- **Shell Redirection (`>` & `>>`)**:
  - Parse input for `>` (overwrite) and `>>` (append).
  - Execute left-hand command (e.g. `echo "Hello World" > notes.txt` or `time >> log.txt`) and write/append output text directly to target file in active `cd` directory.

### 7. Full `grep` Implementation & Command Piping (`|`) & Wildcards (`*.log`)
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

## Proposed Changes

### Core Engine & Architecture Layer

#### [MODIFY] [CommandEngine.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/CommandEngine.kt)
- Update `handleUninstall(appQuery)` to use `Uri.fromParts("package", pkgName, null)` for `ACTION_DELETE` and direct fallback to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
- Remove all Termux code completely.
- Fix Android 15 file visibility in `handleLs()`.
- Implement dynamic prompt formatting reflecting active `cd` folder.
- Implement `mkdir`, `rm`, output redirection (`>` / `>>`), `grep` with flags (`-i`, `-v`, `-n`, `-c`, `-r`, `-w`), piping (`|`), and wildcards (`*.log`).
- Expand `infofetch` with CPU, Disk Storage, and App Count metrics.

#### [MODIFY] [AndroidManifest.xml](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/AndroidManifest.xml)
- Remove `com.termux.permission.RUN_COMMAND`.

#### [MODIFY] [TerminalViewModel.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/TerminalViewModel.kt)
- Update completions for `mkdir`, `rm`, `grep`, and dynamic prompt state.

---

## Verification Plan

### Automated Tests
- Unit test verifying `uninstall` package query resolution.
- Unit test verifying `grep` flags (`-i`, `-v`, `-n`, `-c`, `-w`), file wildcard expansion (`*.log`), and piping (`|`).
- Unit test verifying file creation (`mkdir`), deletion (`rm`), and shell redirection (`>`).

### Manual Verification
- Execute `uninstall spotify` -> verify system uninstallation prompt or App Info page opens directly with the "Uninstall" button available.
- Execute `cd storage0/Download` -> verify prompt updates to `user@mobile:~/Download$ `.
- Execute `ls` on Android 15 -> verify both files and folders display.
- Execute `echo "Hello World" > hello.txt` -> verify file created and `cat hello.txt` reads "Hello World".
- Execute `grep -i -n "hello" hello.txt` -> verify line number and text output.
- Execute `ls | grep ".txt"` -> verify piped output filtering works cleanly.
- Execute `infofetch` -> verify CPU cores, Disk Storage, and Total App Count display.

---

## Completed Phases & Feature Summary

### Version 1.3.1 Patch Release [COMPLETED]
- **Official System `PackageInstaller.uninstall()` Integration**: Replaced deprecated intent filters with `PackageInstaller.uninstall()`.

### Version 1.3.0 Feature Release [COMPLETED]
- **Enhanced RAM Trimmer**: Scans all installed packages (`packageManager.getInstalledPackages(0)`) and kills third-party background processes.
- **Recent Apps Overview Launcher (`trim recents` / `sys recents`)**.

### Version 1.2.3 Patch Release [COMPLETED]
- **Dual Feedback & Bug Reporting System**: GitHub Issues primary & direct email (`crownemperordev@gmail.com`) secondary.

### Version 1.2.2 Patch Release [COMPLETED]
- **Waydroid & Linux PC Cursor Focus Fix**: Active input focus and blinking cursor on boot with soft keyboard suppression.

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

## Deferred Backlog (Indefinitely On Hold)
- Termux Intent Execution Bridge (Removed & Cancelled).
- Android 15 `uninstall` Direct Intent Prompt.(Working on).

