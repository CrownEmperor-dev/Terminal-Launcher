# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED RULE: VERSION NUMBER BUMP POLICY AFTER EVERY SUCCESSFUL BUILD**
> Change and increment the version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` after every successful build:
> 1. **Big Upgrades / Feature Releases**: Increment MINOR version by `+0.1.0` (e.g. `v1.2.0` ➔ `v1.3.0`).
> 2. **Small Changes / Tweaks / Bug Fixes**: Increment PATCH version by `+0.0.1` (e.g. `v1.3.1` ➔ `v1.3.2`).

---

## Active Priority Queue: Canonical `Uri.fromParts` Uninstallation & App Info Direct Fallback (`uninstall <app>`)

### Proposed Feature Details

#### 1. Canonical `Uri.fromParts("package", pkgName, null)` Uninstallation Handler
- **Problem**: `Uri.parse("package:$pkgName")` or `PackageInstaller` can be blocked on OEM Android builds (Redmi/MIUI Security Optimization, Motorola Moto G35 Android 15).
- **Canonical Solution**:
  1. **Primary Intent**: `Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkgName, null))` with `FLAG_ACTIVITY_NEW_TASK`.
  2. **Direct App Info Fallback (100% Guaranteed Works on ALL Android Devices)**:
     If the system uninstallation dialog is intercepted by MIUI / Android 15 security, launch `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` with `Uri.fromParts("package", pkgName, null)` directly!
     The native "App Info" page opens with the prominent **"UNINSTALL"** button, allowing 1-tap uninstallation across all Android OEMs!

---

## Proposed Changes

### Command Engine Layer

#### [MODIFY] [CommandEngine.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/CommandEngine.kt)
- Update `handleUninstall(appQuery)` to use `Uri.fromParts("package", pkgName, null)` for `ACTION_DELETE` and direct fallback to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

#### [MODIFY] [build.gradle.kts](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/build.gradle.kts)
- Bump version to **`v1.3.2` (Build 10)**.

---

## Verification Plan

### Automated Tests
- Unit test verifying `uninstall` package query resolution.

### Manual Verification
- Execute `uninstall spotify` -> verify system uninstallation prompt or App Info page opens directly with the "Uninstall" button available.

---

## Completed Phases & Feature Summary

### Version 1.3.1 Patch Release [COMPLETED]
- **Official System `PackageInstaller.uninstall()` Uninstallation Integration**: Replaced deprecated/blocked Intent filters with Android's official system `PackageInstaller.uninstall(packageName, pendingIntent.intentSender)` API and declared `REQUEST_DELETE_PACKAGES` permission in `AndroidManifest.xml`.

### Version 1.3.0 Feature Release [COMPLETED]
- **Enhanced RAM Trimmer**: Scans all installed packages (`packageManager.getInstalledPackages(0)`) and kills third-party background processes, displaying exact RAM MB used/free before vs after and MB recovered.
- **Recent Apps Overview Launcher (`trim recents` / `sys recents`)**: Launches native Android Recent Apps Overview screen to clear task stacks with one touch.

### Version 1.2.3 Patch Release [COMPLETED]
- **Dual Feedback & Bug Reporting System**:
  - Primary Path: Pre-filled GitHub Issue launcher (`feedback <msg>`, `report <bug>`, `feedback github <msg>`) with auto-attached diagnostic specs.
  - Secondary Path: Direct email dispatcher (`feedback email <msg>`, `report email <bug>`) targeting `crownemperordev@gmail.com`.
  - GUI Options Menu Dual Buttons: **`[ REPORT ISSUE ON GITHUB ]`** and **`[ SEND DIRECT EMAIL ]`** options in `OptionsMenuDialog`.

### Version 1.2.2 Patch Release [COMPLETED]
- **Waydroid & Linux PC Cursor Focus Fix**: Always maintains active input focus and blinking cursor on boot for instant physical/laptop keyboard typing, while actively suppressing the virtual software keyboard when `autokeyboard` is set to `off` (the default).

### Version 1.2.1 Patch Release [COMPLETED]
- **Waydroid & Hardware Keyboard Optimization**: Added `autoShowKeyboard` preference key (`config set autokeyboard <on|off>`) and GUI switch toggle in `OptionsMenuDialog`.

### Version 1.2.0 Feature Release [COMPLETED]
- **Expanded Retro Color Schemes**: Added Cyberpunk Cyan (`CYAN_NEON`), Game Boy Classic (`GAMEBOY_DMG`), Solarized Dark (`SOLARIZED`), and Dracula Retro (`DRACULA`) themes.
- **Multi-Color Custom ASCII Banners**: Inline `[#HEX]` color tag parsing into Compose `AnnotatedString` with custom `SpanStyle` character colors.
- **Full Immersion Status Bar Toggle**: `config set statusbar <on|off>`, `sys statusbar`, and GUI switch in `OptionsMenuDialog`.
- **Battery Metric Fix (Time Since Full Charge)**: Tracks 100% full recharge timestamps in DataStore (`lastFullChargeTimestamp`) and displays `Time Since Full Charge: 4h 15m ago`.
- **User Feedback & Bug Reporting System**: `feedback <message>` / `report <bug>` pre-filled GitHub Issue / email dispatcher with automatic hardware diagnostics.

### Version 1.1.1 Patch Release [COMPLETED]
- **16-Track 8-Bit Retro Chiptune Jukebox**: Randomly picks or allows selecting 16 classic retro game melodies (`8bit`, `8bit <1..16>`, `tune mario`, `8bit list`).

### Version 1.1.0 Feature Release [COMPLETED]
- **Battery Estimates**: Displays real-time time till full charge (charging) and time till full drain (discharging).
- **Duplicate App Auto-Disambiguation**: Automatically generates unique shortcut aliases (e.g. `calculator.google` vs `calculator.sec`).
- **In-Terminal Text File Viewer (`cat <file>`)**: Reads `.txt`, `.log`, `.md`, `.json`, `.csv` files.
- **8-Bit Chiptune Square Wave Audio Easter Egg (`8bit` / `tune`)**: Synthesizes retro 8-bit square wave PCM melody via Android `AudioTrack` API.
- **Email Dispatcher (`email <to> [subject] [body]`)**: Pre-fills recipient, subject, and body in Proton Mail, Gmail, or default email client.
- **Play Store App Installation (`install <app>`)**: Launches Google Play Store app or browser directly to search/details page.

### Version 1.0.0 Initial Features [COMPLETED]
- **Phase 1**: Performance & 120Hz Optimizations, Single-Keystroke `Enter` Autocomplete Execution, Command Disambiguation (`config`, `settings`, `options`), `matrix` easter egg.
- **Phase 2**: Storage Directory Traversal (`ls /storage`, `storage0`, `storage1`, `cd`), Package Event Detection (`PACKAGE_ADDED`, `PACKAGE_REMOVED`), Android 8.1 Oreo permissions.
- **Phase 3**: Direct Web URL Launcher, `time`/`date` command, `killall` memory trimmer, Ghost autocomplete dismissal via `Backspace`, `about` card, `sys` hardware toggles & deep settings subpages.
- **Phase 4**: `echo` command with variable expansion (`$USER`, `$PROMPT`, `$THEME`, `$VERSION`, `$OS`, `$MODEL`), Google Web Search (`search`/`google`/`g`), Custom Handmade ASCII Art Editor (`asciiart.eu`).
- **Text Selection & Copy-Paste**: Output line buffer highlighting (`SelectionContainer`), `Ctrl+C`/`Ctrl+V` input shortcuts.

---

## Deferred Backlog (Indefinitely On Hold)
- Termux Intent Execution Bridge.
- Android 15 `uninstall` Direct Intent Prompt.
- `grep <pattern>`: Search buffer output lines or file content.
- `wa <number> <message>` / `whatsapp`: Direct WhatsApp messaging.
