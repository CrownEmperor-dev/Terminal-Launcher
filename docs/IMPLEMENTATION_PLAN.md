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

Any new feature requests, commands, or project phases will be placed at the top of this queue for execution.

*No active phase in queue. Current version: **v1.3.6 (Build 12)**. Ready for next incoming feature requests!*

---

## Completed Phases & Feature Summary

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
