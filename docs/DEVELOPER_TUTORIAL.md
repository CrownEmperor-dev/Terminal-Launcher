# 📚 The Art of Android Shells: Building Terminal Launcher from Scratch
*A Complete Developer’s Textbook & Reference Manual*

---

## 📑 Textbook Table of Contents & Chapter Structure

### Chapter 1: Android System Architecture & Home Screen Launcher Mechanics
* **1.1**: What happens when an Android phone boots up?
* **1.2**: System Intent Resolvers & `CATEGORY.HOME` ([AndroidManifest.xml](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/AndroidManifest.xml))
* **1.3**: Activity Launch Modes (`singleInstance`, `excludeFromRecents`, `stateNotNeeded`)
* **1.4**: Window Soft Input Management (`adjustResize` vs `adjustPan` for hardware & software keyboards)

### Chapter 2: Modern Android Build System & Version Catalogs
* **2.1**: Anatomy of Gradle KTS ([build.gradle.kts](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/build.gradle.kts) vs `settings.gradle.kts`)
* **2.2**: Version Catalog ([gradle/libs.versions.toml](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/gradle/libs.versions.toml)) and BOM (Bill of Materials) Management
* **2.3**: Target SDK 37, Min SDK 26 (Android 8.0 Oreo), and R8 ProGuard Optimization
* **2.4**: Version Numbering Policy (`v1.1.0` - Major/Minor/Patch increment strategy)

### Chapter 3: Architecture Patterns & The MVVM Paradigm
* **3.1**: Why Model-View-ViewModel (MVVM)?
* **3.2**: Unidirectional Data Flow (UDF) in Reactive Android
* **3.3**: Separation of Concerns: View vs ViewModel vs Repositories vs Command Engines

### Chapter 4: Querying System Applications & Disambiguation Algorithms
* **4.1**: Querying `PackageManager` with `Intent.ACTION_MAIN` & `CATEGORY_LAUNCHER` ([AppRepository.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/AppRepository.kt))
* **4.2**: RAM Optimization Strategies (Zero Icon Overhead & Lazy Loading)
* **4.3**: Duplicate App Name Auto-Disambiguation Algorithm (`calculator.google` vs `calculator.sec`)
* **4.4**: Acronym & Prefix Matching for Lightning App Launching (`findApp`)

### Chapter 5: Asynchronous Persistence with Jetpack DataStore
* **5.1**: Why SharedPreferences are deprecated & why DataStore Preferences win ([SettingsRepository.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/SettingsRepository.kt))
* **5.2**: Managing Reactive State Flows (`Flow<LauncherSettings>`)
* **5.3**: Persisting Themes, Text Sizes, Prompt Strings, Custom ASCII Banners, and Command Buffer History

### Chapter 6: The Command Interpreter & Engine Operations
* **6.1**: Command Input Tokenization & Alias Resolution ([CommandEngine.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/data/CommandEngine.kt))
* **6.2**: Core Shell Utilities (`help`, `about`, `time`, `date`, `killall`, `echo` with `$USER`/`$PROMPT` expansion)
* **6.3**: Web Integration & Play Store Deep Links (`search`, `install`, `market://` intents)
* **6.4**: File Systems & Storage Traversal (`ls /storage`, `storage0`, `storage1`, `cd`, `cat`/`read`)
* **6.5**: Hardware Toggles & Sub-System Page Navigation (`sys flash`, `sys ringer`, `sys settings`)
* **6.6**: Audio Synthesis: Real-Time 8-Bit PCM Square Wave Synthesizer (`AudioTrack`)
* **6.7**: Native Email Intent Dispatching (`email` for Proton Mail & Gmail)

### Chapter 7: Reactive UI & High-Performance Rendering with Jetpack Compose
* **7.1**: Declarative UI vs Imperative XML Views ([TerminalScreen.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/TerminalScreen.kt))
* **7.2**: Screen Layout & Edge-to-Edge Bar Insets (`statusBarsPadding`, `imePadding`)
* **7.3**: The Output Terminal Buffer: `LazyColumn` & `SelectionContainer` Text Highlighting
* **7.4**: Hardware Keyboard Interception (`onPreviewKeyEvent` for Tab, Backspace, Arrows, Ctrl+C, Ctrl+V)
* **7.5**: Modern GUI Dialog Overlays ([OptionsMenuDialog.kt](file:///home/mohammed/AndroidStudioProjects/TerminalLauncher/app/src/main/java/com/example/terminallauncher/ui/OptionsMenuDialog.kt) & `MatrixRainScreen.kt`)
* **7.6**: 120Hz Refresh Rate Acceleration (`Modifier.graphicsLayer` & `@Immutable` UI State)

### Chapter 8: System Events & Dynamic Broadcast Receivers
* **8.1**: Listening to `PACKAGE_ADDED` and `PACKAGE_REMOVED` in Compose (`DisposableEffect`)
* **8.2**: Dynamic Permission Request Lifecycle (`ActivityResultContracts.RequestMultiplePermissions`)

### Chapter 9: Automated Testing, Compilation & Deployment Workflow
* **9.1**: Unit Testing with JUnit 4 & Coroutine Test Harnesses
* **9.2**: Command Line Build Commands (`assembleDebug`, `testDebugUnitTest`)
* **9.3**: Deploying & Debugging via ADB (`adb install`)

---

## 🎯 Next Steps
Whenever you are ready, we can dive deep into **Chapter 1** (or any specific chapter) with complete line-by-line explanations, code samples, and beginner tutorials!
