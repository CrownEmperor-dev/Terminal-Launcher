# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED RULE: VERSION NUMBER BUMP POLICY AFTER EVERY SUCCESSFUL BUILD**
> Change and increment the version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` after every successful build:
> 1. **Big Upgrades / Feature Releases**: Increment MINOR version by `+0.1.0` (e.g. `v1.0.0` ➔ `v1.1.0`).
> 2. **Small Changes / Tweaks / Bug Fixes**: Increment PATCH version by `+0.0.1` (e.g. `v1.2.2` ➔ `v1.2.3`).

---

## Active Priority Queue (Next Intake Stack)

Any new feature requests, commands, or project phases will be placed at the top of this queue for execution.

### Fix & Enhance `trim` / `killall` Background RAM Trimmer & Recent Apps Clearing Plan
- **Discovered Bug & Root Cause**:
  - `trim` / `killall` currently relies on `ActivityManager.runningAppProcesses`.
  - On Android 8.0+ (API 26+), `getRunningAppProcesses()` is restricted by the OS and returns 0 third-party app processes to non-system callers.
  - Because `runningAppProcesses` is empty, `killBackgroundProcesses()` was being called 0 times, resulting in 0 MB RAM freed before vs. after.

- **Proposed Solution & Technical Execution**:
  1. **Enumerate All User Packages (`PackageManager`)**:
     - Query non-system installed packages via `packageManager.getInstalledPackages(0)`.
     - Execute `ActivityManager.killBackgroundProcesses(pkgName)` for every installed third-party package (e.g. Chrome, Spotify, games).
     - Run `System.gc()` and display exact RAM MB freed before vs after.
  2. **Recent Apps Overview Launcher (`trim recents` / `sys recents`)**:
     - Add `trim recents` or `sys recents` sub-command to launch Android's native Recent Apps Overview screen via system intent / accessibility, allowing the user to tap "Clear All" with one touch.
  3. **Android Recents Security Disclosure**:
     - Display clear explanatory output in terminal detailing that Android OS restricts non-root third-party apps from programmatically swiping third-party tasks out of Recents UI (`android.permission.REMOVE_TASKS` is signature-only).

### Multi-Version Compatibility Fix for `uninstall` Command (Android 11 & Android 15 Support)
- **Problem & Scope**:
  - `uninstall <app>` works on BlackBerry KeyONE (Android 8.1 / Oreo), but fails or is intercepted on Redmi Note 8 (Android 11 / MIUI) and Moto G35 (Android 15).
- **Technical Root Cause**:
  - **Android 11 (MIUI / Redmi Note 8)**: MIUI Security restricts `Intent.ACTION_DELETE` with `package:` URI when launched from third-party home screen apps.
  - **Android 15 (Moto G35 / API 35)**: Android 14/15 restricts/deprecates unguided `ACTION_DELETE` intents for third-party launchers without explicit `ACTION_UNINSTALL_PACKAGE` or PackageInstaller APIs.

- **Proposed Multi-Stage Fallback Solutions**:
  1. **Primary Intent (Modern Android Standard)**:
     - Use `Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$packageName"))` with `EXTRA_RETURN_RESULT` extra.
  2. **Secondary Intent (Legacy Android 8.1 Fallback)**:
     - Fallback to `Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))`.
  3. **Tertiary Intent (OEM / Android 11-15 Safe Fallback)**:
     - Fallback to App Info Settings (`Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))`) where the native "Uninstall" button is guaranteed to be functional across all Android OEMs (MIUI, Motorola, Stock Android 15).

*Current version: **v1.2.3 (Build 7)**.*

---

## Completed Phases & Feature Summary

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
