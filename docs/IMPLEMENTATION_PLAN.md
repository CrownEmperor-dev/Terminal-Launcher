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

### 1. Multi-Engine Screenshot Capture Fix (Waydroid Black Screen & BlackBerry DTEK Denied Fix) [TOP PRIORITY]
- **Problem & Root Cause Analysis**:
  - **Waydroid (Android 12 Container)**: `PixelCopy` / hardware EGL readbacks return black/empty bitmaps due to Wayland `wl_surface` hardware frame buffer isolation.
  - **BlackBerry KeyONE (Android 8.1 / DTEK Security)**: Direct file creation in `/sdcard/Pictures/` throws `SecurityException` / `PermissionDenied` under BlackBerry DTEK enterprise storage policy rules.
- **Technical Solution Strategy**:
  - **Engine 1: Software Canvas View Rendering (`View.draw(Canvas(bitmap))`)**:
    Create a software-backed ARGB_8888 Bitmap and bind to a software `Canvas`. Call `window.decorView.draw(canvas)`. Software canvas rendering bypasses EGL/Wayland hardware buffer readbacks, eliminating the black screen issue on Waydroid!
  - **Engine 2: MediaStore ContentResolver Insertion (BlackBerry DTEK Fix)**:
    Instead of writing directly via `java.io.File`, insert screenshot image via `context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)` and write stream bytes directly to Uri output stream. This complies 100% with Android 8.1 DTEK and Scoped Storage policies across all OEMs.
  - **Engine 3: System Screenshot Action (`AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT`)**:
    Support `screenshot sys` for triggering native OS screenshot capture on Android 11+.

### 2. Fix `shutdown` / `reboot` Redirecting to Settings (Waydroid & BlackBerry KeyONE Support)
- **Problem & Root Cause Analysis**:
  - `com.android.internal.intent.action.REQUEST_SHUTDOWN` requires internal system/signature permissions. When called by a non-root app, Android OS throws a `SecurityException` or `ActivityNotFoundException`, which defaulted to opening System Settings / Battery Summary.
- **Technical Solution Strategy**:
  - **Root Direct Execution (`su` / `svc power`)**:
    Attempt `su -c reboot` / `su -c "reboot -p"` or shell runtime `svc power reboot` / `svc power shutdown`.
  - **Accessibility Power Menu (`GLOBAL_ACTION_POWER_DIALOG`)**:
    If root is unavailable, invoke `AccessibilityService.GLOBAL_ACTION_POWER_DIALOG` to bring up Android's native Power Off / Restart overlay directly on screen without opening Settings.
  - **Clean Terminal Guidance**:
    Stop opening Settings! If neither root nor Accessibility is available, print clear terminal instructions (`[POWER]: Power actions require Root or Accessibility permission. Type 'sys accessibility' to enable`).

---

## Proposed Changes

### Core Engine Layer

#### [MODIFY] [CommandEngine.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/CommandEngine.kt)
- Implement `handleGrep(args, currentSettings)` supporting `-i`, `-v`, `-n`, `-c`, `-r`, `-w`, wildcard file expansion (`*.log`), and command piping (`|`).
- Add `grep` to `execute()`, `handleHelp()`, and `ls /system`.

#### [MODIFY] [TerminalViewModel.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/TerminalViewModel.kt)
- Add `grep` completions.

#### [MODIFY] [build.gradle.kts](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/build.gradle.kts)
- Bump version to **`v1.4.3` (Build 15)** upon completion.

---

## Verification Plan

### Automated Tests
- Unit test verifying `grep` flags (`-i`, `-v`, `-n`, `-c`, `-w`), file wildcard expansion (`*.log`), and piping (`|`).

### Manual Verification
- Execute `grep -i -n "error" sample.log` -> verify line numbers and case-insensitive matching.
- Execute `ls | grep ".txt"` -> verify piped output filtering.
- Execute `grep "error" *.log` -> verify multi-file wildcard search.

---

## Completed Phases & Feature Summary

### Version 1.4.3 Feature Release [COMPLETED]
- **Full `grep` Engine & Regex Matching**: Supports searching across single files, multiple file wildcards (`*.log`), and recursive folder trees (`grep -r`).
- **`grep` Flags**: Supports `-i` (case-insensitive), `-v` (invert match), `-n` (line numbers), `-c` (count only), `-r` (recursive search), and `-w` (word boundaries).
- **Command Piping (`|`)**: Intercepts unquoted piping operators and pipes left-hand command output lines into right-hand `grep` filters (e.g., `ls | grep ".txt"` or `history | grep "config"`).

### Version 1.4.2 Feature Release [COMPLETED]
- **In-Launcher & System Screenshot Command (`screenshot` / `sc` / `sys screenshot`)**: Captures terminal screen bitmaps and automatically saves PNG files to `storage0/Pictures/Screenshots/`.
- **Enhanced `infofetch` System Metrics**: CPU specs, Disk Storage free/total GB (`StatFs`), and User/System App counts.
- **Direct `sys ringer` / `sys sound` Mode Control & DND Handler**: Short tokens (`s`, `v`, `n`), mode cycling (`NORMAL` ➔ `VIBRATE` ➔ `SILENT`), and DND permission prompting.
- **Complete Help & Manual Audit (`help` / `man`)**: Fully documented every built-in command and utility.

### Version 1.4.1 Patch Release [COMPLETED]
- **System Power Commands (`reboot`, `restart`, `shutdown`, `poweroff`)**.

### Version 1.4.0 Feature Release [COMPLETED]
- **Directory Creation (`mkdir`)**, **File & Folder Deletion (`rm`)**, **Shell Output Redirection (`>` & `>>`)**, and **Dynamic Linux Terminal Prompt Path**.

### Version 1.3.6 Patch Release [COMPLETED]
- **Multi-Word Directory Support** (`cd storage0/Quick Share`) & **All Files Access Permission Prompt** (`sys storage`).

### Version 1.3.5 Patch Release [COMPLETED]
- **Case-Insensitive Storage Path Resolution**, **Storage Directory Guard**, **Multi-Provider MediaStore File Enumeration**, and **Complete Termux Code Purge**.

### Version 1.3.2 / 1.3.4 Patch Release [COMPLETED]
- **Uninstallation Fix**: Fixed `uninstall <app>` using canonical `Uri.fromParts` and direct App Info fallback.

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
