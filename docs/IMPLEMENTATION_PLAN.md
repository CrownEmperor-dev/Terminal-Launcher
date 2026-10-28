# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED RULE: VERSION NUMBER BUMP POLICY AFTER EVERY SUCCESSFUL BUILD**
> Change and increment the version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` after every successful build:
> 1. **Big Upgrades / Feature Releases**: Increment MINOR version by `+0.1.0` (e.g. `v1.0.0` ➔ `v1.1.0`).
> 2. **Small Changes / Tweaks / Bug Fixes**: Increment PATCH version by `+0.0.1` (e.g. `v1.2.2` ➔ `v1.2.3`).

---

## Active Priority Queue (Next Intake Stack)

Any new feature requests, commands, or project phases will be placed at the top of this queue for execution.

### Enhanced `trim` / `killall` Command & Recent Apps Clearing Plan
- **Problem**: Running `trim` or `killall` frees background process RAM via `ActivityManager.killBackgroundProcesses()`, but does not remove third-party apps from the Android System Recent Apps overview screen (Recents UI), making it seem like "it doesn't do much".
- **Android Security Constraints**:
  - Android 8.0+ (API 26+) strictly restricts standard non-root applications from programmatically closing or swiping away third-party app tasks from the System Recents screen (`android.permission.REMOVE_TASKS` is a signature/system-only permission).
  - `ActivityManager.appTasks` can only remove the current launcher's own task stack.

- **Proposed Solutions & Execution Options**:
  1. **Option A: Enhanced Memory Trimmer with RAM Delta Metrics**
     - Measure RAM usage before and after `killBackgroundProcesses()` + `System.gc()`.
     - Output exact MB of RAM freed (e.g. `[TRIM SUCCESS]: Terminated background processes across 14 apps. Freed 342 MB RAM.`).
     - Display clear status explaining Android's Recents security model.
  2. **Option B: Direct Recent Apps Overview Launcher (`trim recents` / `sys recents`)**
     - Add `trim recents` or `sys recents` command that opens Android's System Recent Apps overview screen via `AccessibilityService.GLOBAL_ACTION_RECENTS` or System Service Intent, allowing the user to tap "Clear All" instantly.
  3. **Option C: Optional Accessibility Service Helper (`AccessibilityService`)**
     - Provide an optional Accessibility Service module in Terminal Launcher. When enabled by user in Android Settings, the service can perform `GLOBAL_ACTION_RECENTS` and automatically perform a "Clear All" swipe action when `trim` or `clear-recents` is run.

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
