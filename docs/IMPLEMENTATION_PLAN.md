# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED META-INSTRUCTIONS FOR ALL CHAT SESSIONS & AGENTS**
> 1. **Single Source of Truth**: `docs/IMPLEMENTATION_PLAN.md` (and `implementation_plan.artifact.md`) is the master implementation plan file for Terminal Launcher. Always read and update this file regularly.
> 2. **Pre-Implementation Protocol**: When the user provides ideas or bug reports, update `docs/IMPLEMENTATION_PLAN.md` first with detailed technical strategies. Present the updated plan to the user and await explicit approval before modifying any code files.
> 3. **Execution & Version Bump Protocol**: After implementing and verifying a feature with `./gradlew app:assembleDebug` and `./gradlew app:testDebugUnitTest`:
>    - Increment version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` (Minor `+0.1.0` for features, Patch `+0.0.1` for tweaks/fixes).
>    - Output a **"Fix and New Features Log"** summary at the start of your response.
> 4. **Infofetch Version Synchronization Rule**: Before beginning any task or releasing a build, verify and update `generateInfofetchLines()` in `CommandEngine.kt` so that `infofetch` and the startup shell banner always display the current, newest version (e.g., `v1.4.3`).
> 5. **Session Override Rule**: This protocol overrides any conflicting parameters from previous chat sessions.

---

## Active Priority Queue (Next Intake Stack)

Any new feature requests, commands, or project phases will be placed at the top of this queue for execution.

*No active phase in queue. Current version: **v1.4.3 (Build 15)**. Ready for next incoming feature requests!*

---

## Completed Phases & Feature Summary

### Version 1.4.3 Feature Release [COMPLETED]
- **Full `grep` Engine & Regex Matching**: Supports searching across single files, multiple file wildcards (`*.log`), and recursive folder trees (`grep -r`).
- **`grep` Flags**: Supports `-i` (case-insensitive), `-v` (invert match), `-n` (line numbers), `-c` (count only), `-r` (recursive search), and `-w` (word boundaries).
- **Command Piping (`|`)**: Intercepts unquoted piping operators and pipes left-hand command output lines into right-hand `grep` filters (e.g., `ls | grep ".txt"` or `history | grep "config"`).
- **Multi-Engine Screenshot Capture**: Software Canvas View Rendering (`View.draw(Canvas(bitmap))`) bypassing Wayland frame buffer readbacks (Waydroid black screen fix) + MediaStore Stream Insertion complying with BlackBerry DTEK security policies.
- **Enhanced `infofetch` Metrics**: Added hardware CPU specs (cores & ABI), Disk Storage free vs total GB (`StatFs`), and User vs System App counts.
- **Direct `sys ringer` / `sys sound` Mode Control**: Supports short tokens (`s`, `v`, `n`), mode cycling when no argument passed, and DND permission catch (`Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`).

### Version 1.4.2 Feature Release [COMPLETED]
- **In-Launcher & System Screenshot Command (`screenshot` / `sc` / `sys screenshot`)**: Captures terminal screen bitmaps and automatically saves PNG files to `storage0/Pictures/Screenshots/`.
- **Complete Help & Manual Audit (`help` / `man`)**: Fully documented every built-in command and utility.

### Version 1.4.1 Patch Release [COMPLETED]
- **System Power Commands (`reboot`, `restart`, `shutdown`, `poweroff`)**: Direct root reboot/shutdown + System Power Menu fallback.

### Version 1.4.0 Feature Release [COMPLETED]
- **Directory Creation (`mkdir`)**, **File & Folder Deletion (`rm`)**, **Shell Output Redirection (`>` & `>>`)**, and **Dynamic Linux Terminal Prompt Path**.

### Version 1.3.6 Patch Release [COMPLETED]
- **Multi-Word Directory Support** (`cd storage0/Quick Share`) & **All Files Access Permission Prompt** (`sys storage`).

### Version 1.3.5 Patch Release [COMPLETED]
- **Case-Insensitive Storage Path Resolution**, **Storage Directory Guard**, **Multi-Provider MediaStore File Enumeration**, and **Complete Termux Code Purge**.

### Version 1.3.2 / 1.3.4 Patch Release [COMPLETED]
- **Uninstallation Fix**: Fixed `uninstall <app>` across Android 8.1, Android 11 (MIUI), and Android 15 using canonical `Uri.fromParts` and direct App Info fallback.

### Version 1.3.1 Patch Release [COMPLETED]
- **Official System `PackageInstaller.uninstall()` Integration**.

### Version 1.3.0 Feature Release [COMPLETED]
- **Enhanced RAM Trimmer** & **Recent Apps Overview Launcher (`trim recents`)**.

### Version 1.2.3 Patch Release [COMPLETED]
- **Dual Feedback & Bug Reporting System**.

### Version 1.2.2 Patch Release [COMPLETED]
- **Waydroid & Linux PC Cursor Focus Fix**.

### Version 1.2.1 Patch Release [COMPLETED]
- **Waydroid & Hardware Keyboard Optimization**.

### Version 1.2.0 Feature Release [COMPLETED]
- **Expanded Retro Color Schemes**, **Multi-Color Custom ASCII Banners**, **Full Immersion Status Bar Toggle**, **Battery Metric Fix**, **User Feedback & Bug Reporting System**.

### Version 1.1.1 Patch Release [COMPLETED]
- **16-Track 8-Bit Retro Chiptune Jukebox** (`8bit`, `tune`).

### Version 1.1.0 Feature Release [COMPLETED]
- **Battery Estimates**, **Duplicate App Auto-Disambiguation**, **`cat <file>` Viewer**, **`install <app>` Play Store Launcher**.

### Version 1.0.0 Initial Features [COMPLETED]
- **Phase 1-4**: Performance, Storage Navigation, Direct Web URLs, `echo` variables, Custom ASCII editor, Text Selection & `Ctrl+C`/`Ctrl+V`.

---

## Deferred Backlog (Next Feature Release Intake Stack)
- Termux Intent Execution Bridge (Cancelled & Purged).
- `wa <number> <message>` / `whatsapp`: Direct WhatsApp messaging.
