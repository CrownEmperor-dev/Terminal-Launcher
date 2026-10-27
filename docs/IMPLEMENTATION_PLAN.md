# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED RULE: VERSION NUMBER BUMP POLICY AFTER EVERY SUCCESSFUL BUILD**
> Change and increment the version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` after every successful build:
> 1. **Big Upgrades / Feature Releases**: Increment MINOR version by `+0.1.0` (e.g. `v1.0.0` ➔ `v1.1.0`).
> 2. **Small Changes / Tweaks / Bug Fixes**: Increment PATCH version by `+0.0.1` (e.g. `v1.0.0` ➔ `v1.0.1`).

---

## Active Priority Queue (Next Intake Stack)

Any new feature requests, commands, or project phases will be placed at the top of this queue for execution.

*No active phase in queue. Current version: **v1.1.0 (Build 2)**. Ready for next incoming feature requests!*

---

## Completed Phases & Feature Summary

### Version 1.1.0 Feature Release [COMPLETED]
- **Battery Estimates**: Displays real-time time till full charge (charging) and time till full drain (discharging) in `infofetch`, `battery`, and `battery status`.
- **Duplicate App Auto-Disambiguation**: Automatically generates unique shortcut aliases (e.g. `calculator.google` vs `calculator.sec`) when multiple installed apps share the exact same name.
- **In-Terminal Text File Viewer (`cat <file>`)**: Reads `.txt`, `.log`, `.md`, `.json`, `.csv` files directly inside terminal buffer.
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
