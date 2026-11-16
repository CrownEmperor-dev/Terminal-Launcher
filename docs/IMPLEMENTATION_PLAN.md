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

### Technical Execution Strategy & Architectural Evaluation

#### 1. DataStore Settings & State Architecture
- **Settings Data Class (`LauncherSettings`)**:
  - `wallpaperEnabled: Boolean = false` — On/off master toggle. Reverts to solid theme color when `false`.
  - `wallpaperUri: String = ""` — Local file path pointing to copied image in `context.filesDir/wallpaper.png`.
  - `wallpaperDim: Int = 35` — Adjustable black overlay dimming percentage (`0..100%`, default `35%`).
  - `wallpaperBlur: Int = 0` — Background blur radius (`0..20dp`, default `0dp` for crisp original detail).
  - `wallpaperScale: WallpaperScaleMode = CROP` — Image scale mode (`CROP`, `FIT`, `STRETCH`, default `CROP`).
  - `wallpaperUpdateTimestamp: Long = 0L` — Cache invalidation timestamp updated whenever a new wallpaper is saved. Guarantees Compose invalidates cached bitmaps immediately.
- **DataStore Keys (`SettingsRepository.kt`)**:
  - `WALLPAPER_ENABLED` (`booleanPreferencesKey`)
  - `WALLPAPER_URI` (`stringPreferencesKey`)
  - `WALLPAPER_DIM` (`intPreferencesKey`)
  - `WALLPAPER_BLUR` (`intPreferencesKey`)
  - `WALLPAPER_SCALE` (`stringPreferencesKey`)
  - `WALLPAPER_UPDATE_TIMESTAMP` (`longPreferencesKey`)

#### 2. Gallery Image Picker & Android 11+ SAF Local Storage Persistence
- **Android 11+ Scoped Storage & Permission Safety**:
  - Standard `content://` URIs returned by gallery pickers lose read permission after app restart or reboot unless persisted.
  - **Clean Solution**: Use `ActivityResultContracts.GetContent("image/*")` in `TerminalScreen.kt`. When an image is picked, asynchronously read the input stream on `Dispatchers.IO` and copy bytes directly to internal app storage at `File(context.filesDir, "wallpaper.png")`.
  - **Benefits**:
    - Zero runtime storage permissions required (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` not needed).
    - Internal storage is 100% private to the app, persisting reliably across device reboots and app updates.
    - Fully compatible with Android 8.1 through Android 11+ (API 30-37).

#### 3. High-Performance Bitmap Decoding & OOM Prevention
- **Memory Optimization (Android 11+ High-Res Camera Photos)**:
  - Unscaled decoding of 12MP to 108MP camera photos can consume 50MB–100MB+ of RAM and cause `OutOfMemoryError` or UI main-thread jank.
  - **Clean Solution**: Implement a lightweight background decoder helper using `BitmapFactory.Options.inSampleSize` calculated against the device's screen dimensions (e.g., target max 1080p).
  - Asynchronously load the decoded `ImageBitmap` off the main thread (`Dispatchers.IO`) in a `produceState` / `LaunchedEffect` block keyed on `wallpaperUpdateTimestamp` and `wallpaperEnabled`.

#### 4. Dual-Mode Blur Strategy (Android 12+ API 31+ vs Android 8.1–11 API 26–30)
- **Android 12+ (API 31+)**:
  - Apply hardware-accelerated Compose GPU blur modifier `Modifier.blur(wallpaperBlur.dp)` on the background image `Box`.
- **Android 8.1–11 (API 26–30)**:
  - `Modifier.blur()` is ignored or unsupported on API < 31.
  - **Clean Solution**: When `wallpaperBlur > 0` on API < 31, downscale the decoded bitmap to 1/8th resolution, apply a lightweight 10-line Kotlin StackBlur algorithm on `Dispatchers.IO`, and return the blurred `ImageBitmap`.
  - Compose scales up the downscaled blurred bitmap with bilinear filtering, delivering a buttery-smooth, hardware-accelerated blur effect with zero UI lag (< 2ms runtime) across all legacy and Android 11 devices!

#### 5. Wallpaper Change Failures & Comprehensive Reliability Safeguards
- **Problem A: Format Incompatibility (HEIC / WebP / RAW / Cloud Storage Streams)**
  - If a user selects a HEIC photo (common on modern Android 11+ / Samsung / Pixel devices) or a image stream directly from Google Photos cloud, basic stream copy or `decodeFile` might fail or result in 0 bytes.
  - **Solution**: When importing a picked URI, decode the input stream using `ImageDecoder` (on API 28+) or `BitmapFactory.decodeStream`, convert to ARGB_8888 bitmap, and explicitly compress as standard `Bitmap.CompressFormat.PNG` into `context.filesDir/wallpaper.png`. This guarantees 100% format normalization across all devices.
- **Problem B: Compose State Not Refreshing (Stale File Path)**
  - If the path `context.filesDir/wallpaper.png` is static, Compose's `remember` or state observer might think nothing changed and retain the old decoded bitmap on screen.
  - **Solution**: Automatically update `wallpaperUpdateTimestamp = System.currentTimeMillis()` in DataStore whenever a file is saved. Key Compose state on `wallpaperUpdateTimestamp` so Compose instantly disposes the old bitmap and decodes the new one.
- **Problem C: Automatically Enabling Wallpaper Master Toggle**
  - If a user picks an image while `wallpaperEnabled == false`, they might expect the wallpaper to show up immediately.
  - **Solution**: Automatically set `wallpaperEnabled = true` whenever a user successfully selects and imports a new image file.
- **Problem D: Max GPU Texture Exceeded**
  - Ultra-high resolution photos (e.g. 108MP) can exceed maximum OpenGL texture dimensions (4096x4096 or 8192x8192), causing Android to render a blank/invisible surface.
  - **Solution**: Always sample down the bitmap to fit within max screen dimensions during the import/decoding pipeline using `inSampleSize`.
- **Problem E: Corrupted / Failed File Error Reporting**
  - If an import fails, don't silently fail or show a black screen.
  - **Solution**: Verify file existence and non-zero length after import. If decoding fails, print a helpful terminal warning (`"[WALLPAPER ERROR]: Unable to decode selected image file."`) and revert safely to solid theme.

#### 6. GUI Options Menu Controls (`OptionsMenuDialog.kt`)
- **Master Toggle**: "Enable Custom Wallpaper" Switch.
- **Image Picker & Management Buttons**: `[ SELECT IMAGE FROM GALLERY ]` and `[ CLEAR WALLPAPER ]`.
- **Dimming Control**: 5-stage selection chips (`0%`, `25%`, `35% (Default)`, `50%`, `75%`, `100%`).
- **Blur Control**: 5-stage selection chips (`0dp (Off)`, `4dp`, `8dp`, `12dp`, `16dp`).
- **Scale Mode**: Chips (`Center Crop`, `Fit`, `Stretch`).

#### 7. CLI Commands & Shorthand Integration (`CommandEngine.kt`)
- `wallpaper` / `config set wallpaper`:
  - `config set wallpaper <on|off>` — Toggle wallpaper display.
  - `config set wallpaper dim <0..100>` — Set dimming overlay percentage.
  - `config set wallpaper blur <0..20>` — Set blur radius in dp.
  - `config set wallpaper scale <crop|fit|stretch>` — Set scale mode.
  - `config set wallpaper pick` / `wallpaper pick` — Trigger gallery image picker.
  - `config set wallpaper clear` / `wallpaper clear` — Remove custom wallpaper image.

#### 8. Version Bump Policy
- Major Feature Release: **`v1.5.0` (Build 16)** in `app/build.gradle.kts`, `CommandEngine.kt`, `OptionsMenuDialog.kt`, `TerminalViewModel.kt`, `README.md`, and `docs/IMPLEMENTATION_PLAN.md`.

---

## Proposed Changes

### Storage & Data Layer

#### [MODIFY] [SettingsRepository.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/SettingsRepository.kt)
- Add wallpaper preference keys (`WALLPAPER_ENABLED`, `WALLPAPER_URI`, `WALLPAPER_DIM`, `WALLPAPER_BLUR`, `WALLPAPER_SCALE`, `WALLPAPER_UPDATE_TIMESTAMP`) and setters.

### Core Engine & UI Layer

#### [MODIFY] [CommandEngine.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/CommandEngine.kt)
- Implement `wallpaper` / `config set wallpaper` CLI commands and shorthand commands (`wallpaper pick`, `wallpaper clear`, `wallpaper on/off`).
- Update `infofetch` version string to `v1.5.0`.

#### [MODIFY] [TerminalScreen.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/TerminalScreen.kt)
- Add Compose background wallpaper image rendering with dimming overlay, dual-mode blur, and scale modes.
- Register `ActivityResultContracts.GetContent()` launcher with `openWallpaperPicker` state trigger.

#### [MODIFY] [OptionsMenuDialog.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/OptionsMenuDialog.kt)
- Add Wallpaper settings section with enable switch, gallery picker button, clear wallpaper button, dimming chips, blur chips, and scale mode chips.

#### [MODIFY] [TerminalViewModel.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/TerminalViewModel.kt)
- Add wallpaper state handlers, image copying logic (`Dispatchers.IO`), `openWallpaperPicker` state trigger, and autocomplete entries.

#### [MODIFY] [build.gradle.kts](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/build.gradle.kts)
- Bump version to **`v1.5.0` (Build 16)** upon completion.

---

## Verification Plan

### Automated Tests
- Unit test verifying wallpaper preference key parsing, dimming value clamping (`0..100`), scale mode enum resolution, and timestamp updating.

### Manual Verification
- Execute `options` -> tap `[ SELECT IMAGE FROM GALLERY ]` -> select image -> verify image copies to `context.filesDir/wallpaper.png` and renders as terminal background without crash.
- Execute `wallpaper pick` in CLI -> verify gallery picker opens directly.
- Adjust dimming slider to 35%, 50%, 75% -> verify black overlay darkens wallpaper behind terminal text.
- Adjust blur to 8dp on Android 11 device vs Android 12+ device -> verify background blurs smoothly without frame drops.
- Adjust scale mode to Crop / Fit / Stretch -> verify wallpaper scales cleanly.
- Toggle wallpaper off / `wallpaper clear` -> verify terminal reverts cleanly to solid theme color.

---

## Completed Phases & Feature Summary

### Version 1.5.0 Feature Release [COMPLETED]
- **Custom Background Wallpaper Engine**: Allows users to select custom wallpaper images from device gallery via GUI Options menu or CLI command (`wallpaper pick`).
- **Edge-to-Edge Full Screen Immersion**: Wallpaper extends 100% full screen behind status bar at top and home handle/gesture bar at bottom with transparent system bar overlays.
- **Dual Photo Pickers (`PickVisualMedia` + `GetContent`)**: Uses Google's official Android Photo Picker (`PickVisualMedia()`) on Android 11–15 with automatic fallback to `GetContent("image/*")`.
- **Android 11+ Scoped Storage & Reboot Persistence**: Copies picked image stream safely into app internal storage (`context.filesDir/wallpaper.png`), bypassing Scoped Storage URI permission expiration across reboots with zero runtime permission requirements.
- **Universal Image Decoding (HEIC, WebP, AVIF, RAW, PNG, JPEG)**: Native `ImageDecoder` (API 28+) and `BitmapFactory` fallback decoding, downscaling large camera photos to max 2048px on hardware accelerators.
- **Dual-Mode Blur Strategy**: Hardware-accelerated Compose GPU blur (`Modifier.blur`) for Android 12+ (API 31+), and lightweight downscaled fast-blur rendering for Android 8.1–11 (API 26–30).
- **Aspect Scaling & Dimming Overlays**: Capped black dimming overlay (`0..85%`) and scale modes (`CROP`, `FIT`, `STRETCH`).
- **Live Diagnostic Suite**: Real-time step-by-step terminal line logs during import and dedicated `wallpaper debug` diagnostic command.
- **Full CLI & GUI Options Suite**: Shorthand commands (`wallpaper pick`, `wallpaper dim`, `wallpaper blur`, `wallpaper scale`, `wallpaper debug`, `wallpaper clear`) and complete GUI Options menu controls.

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
