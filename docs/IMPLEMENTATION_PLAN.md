# Terminal Launcher Living Roadmap & Implementation Plan

> [!IMPORTANT]
> **PINNED META-INSTRUCTIONS FOR ALL CHAT SESSIONS & AGENTS**
> 1. **Single Source of Truth**: `docs/IMPLEMENTATION_PLAN.md` (and `implementation_plan.artifact.md`) is the master implementation plan file for Terminal Launcher. Always read and update this file regularly.
> 2. **Pre-Implementation Protocol**: When the user provides ideas or bug reports, update `docs/IMPLEMENTATION_PLAN.md` first with detailed technical strategies. Present the updated plan to the user and await explicit approval before modifying any code files.
> 3. **Execution & Version Bump Protocol**: After implementing and verifying a feature with `./gradlew app:assembleDebug` and `./gradlew app:testDebugUnitTest`:
>    - Increment version number in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, and `README.md` (Minor `+0.1.0` for features, Patch `+0.0.1` for tweaks/fixes).
>    - Output a **"Fix and New Features Log"** summary at the start of your response.
> 4. **Infofetch Version Synchronization Rule**: Before beginning any task or releasing a build, verify and update `generateInfofetchLines()` in `CommandEngine.kt` so that `infofetch` and the startup shell banner always display the current, newest version (e.g., `v1.5.0`).
> 5. **Session Override Rule**: This protocol overrides any conflicting parameters from previous chat sessions.

---

## Active Priority Queue: Custom Background Wallpaper Engine [VERSION 1.5.0 RELEASE]

### Technical Execution Strategy

#### 1. DataStore Settings & State Architecture
- **Settings Data Class (`LauncherSettings`)**:
  - `wallpaperEnabled: Boolean = false` — On/off master toggle. Reverts to solid theme color when `false`.
  - `wallpaperUri: String = ""` — Local file path/URI pointing to copied image in `context.filesDir/wallpaper.png`.
  - `wallpaperDim: Int = 35` — Adjustable black overlay dimming percentage (`0..100%`, default `35%`).
  - `wallpaperBlur: Int = 0` — Background blur radius (`0..20dp`, default `0dp` for crisp original detail).
  - `wallpaperScale: WallpaperScaleMode = CROP` — Image scale mode (`CROP`, `FIT`, `STRETCH`, default `CROP`).
- **DataStore Keys (`SettingsRepository.kt`)**:
  - `WALLPAPER_ENABLED` (`booleanPreferencesKey`)
  - `WALLPAPER_URI` (`stringPreferencesKey`)
  - `WALLPAPER_DIM` (`intPreferencesKey`)
  - `WALLPAPER_BLUR` (`intPreferencesKey`)
  - `WALLPAPER_SCALE` (`stringPreferencesKey`)

#### 2. Gallery Image Picker & Local Storage Persistence
- **Gallery Picker**:
  - Use `ActivityResultContracts.GetContent()` in `TerminalScreen.kt` triggered from GUI Options or `wallpaper pick` command.
  - When user selects an image from gallery, copy stream directly to `File(context.filesDir, "wallpaper.png")` to ensure background image persists reliably across reboots and permission changes!

#### 3. Compose Rendering Overlay (`TerminalScreen.kt`)
- When `wallpaperEnabled == true` and `wallpaper.png` exists:
  - Load bitmap using Compose `remember(wallpaperUri) { BitmapFactory.decodeFile(...) }` / `ImageBitmap`.
  - Apply Scale Mode:
    - `CROP` ➔ `ContentScale.Crop` (Fill screen, crop excess).
    - `FIT` ➔ `ContentScale.Fit` (Display entire image, padded).
    - `STRETCH` ➔ `ContentScale.FillBounds` (Fill screen regardless of aspect ratio).
  - Apply Blur:
    - Android 12+ (API 31+): Use `Modifier.blur(radius.dp)`.
    - Android 8.1–11 (BlackBerry / Waydroid / API 26–30): Software box blur on loaded bitmap.
  - Apply Black Dimming Overlay:
    - Overlay a `Box` with `background(Color.Black.copy(alpha = wallpaperDim / 100f))` over the wallpaper bitmap.
  - Terminal text buffer and command prompt render over the dimmed/blurred wallpaper with perfect legibility!

#### 4. GUI Options Menu Controls (`OptionsMenuDialog.kt`)
- **Master Toggle**: "Enable Custom Wallpaper" Switch.
- **Image Picker Button**: `[ SELECT IMAGE FROM GALLERY ]` button & `[ CLEAR WALLPAPER ]` button.
- **Dimming Control**: Slider or 5-stage selection chips (`0%`, `25%`, `35% (Default)`, `50%`, `75%`, `100%`).
- **Blur Control**: Slider or selection chips (`0dp (Off)`, `4dp`, `8dp`, `12dp`, `16dp`).
- **Scale Mode**: Chips (`Center Crop`, `Fit`, `Stretch`).

#### 5. CLI Commands (`CommandEngine.kt`)
- `wallpaper` / `config set wallpaper`:
  - `config set wallpaper <on|off>` — Toggle wallpaper display.
  - `config set wallpaper dim <0..100>` — Set dimming overlay percentage.
  - `config set wallpaper blur <0..20>` — Set blur radius in dp.
  - `config set wallpaper scale <crop|fit|stretch>` — Set scale mode.
  - `config set wallpaper pick` — Open gallery image picker.
  - `config set wallpaper clear` — Remove custom wallpaper image.

#### 6. Version Bump Policy
- Major Feature Release: **`v1.5.0` (Build 16)** in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, `README.md`, and `docs/IMPLEMENTATION_PLAN.md`.

---

## Proposed Changes

### Storage & Data Layer

#### [MODIFY] [SettingsRepository.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/SettingsRepository.kt)
- Add wallpaper preference keys (`WALLPAPER_ENABLED`, `WALLPAPER_URI`, `WALLPAPER_DIM`, `WALLPAPER_BLUR`, `WALLPAPER_SCALE`) and setters.

### Core Engine & UI Layer

#### [MODIFY] [CommandEngine.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/CommandEngine.kt)
- Implement `wallpaper` / `config set wallpaper` CLI commands.
- Update `infofetch` version string to `v1.5.0`.

#### [MODIFY] [TerminalScreen.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/TerminalScreen.kt)
- Add Compose background wallpaper image rendering with dimming overlay, blur, and scale modes.
- Register `ActivityResultContracts.GetContent()` launcher.

#### [MODIFY] [OptionsMenuDialog.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/OptionsMenuDialog.kt)
- Add Wallpaper settings section with enable switch, gallery picker button, dimming slider/chips, blur slider/chips, and scale mode chips.

#### [MODIFY] [TerminalViewModel.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/TerminalViewModel.kt)
- Add wallpaper state handlers and autocomplete entries.

#### [MODIFY] [build.gradle.kts](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/build.gradle.kts)
- Bump version to **`v1.5.0` (Build 16)** upon completion.

---

## Verification Plan

### Automated Tests
- Unit test verifying wallpaper preference key parsing, dimming value clamping (`0..100`), and scale mode enum resolution.

### Manual Verification
- Execute `options` -> select gallery image -> verify image renders as background on terminal shell.
- Adjust dimming slider to 35%, 50%, 75% -> verify black overlay darkens wallpaper behind terminal text.
- Adjust scale mode to Crop / Fit / Stretch -> verify wallpaper scales cleanly.
- Toggle wallpaper off -> verify terminal reverts cleanly to solid theme color.

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
- **System Power Commands (`reboot`, `restart`, `shutdown`, `poweroff`)**.

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
