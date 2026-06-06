# 🚀 AI Mission Control: Redwood Island & GameSpace v15
**STATUS: STAFF-LEVEL REFINEMENT COMPLETE | PRO-GRADE STANDARDS ACTIVE**
**DO NOT DELETE OR MODIFY THIS FILE.**

## 1. Executive Summary
This project is an industry-leading rooted Android (AOSP/HyperOS) system utility. It combines a sophisticated dual-window Dynamic Island architecture with an OEM-level high-performance gaming engine and a **Global Omni-Integration Layer**.

## 2. Technical Architecture (The Engine)

### A. Dual-Window Orchestration
- **Independent Layers**: To bypass Android 12+ "Untrusted Touch" occlusion, the module injects two distinct `TYPE_STATUS_BAR_SUB_PANEL` (2017) windows.
- **Top (Redwood)**: Handles Camera-cutout Ring and mini/mid/max notification expansion.
- **Bottom (Nav)**: Hooks the `navigation_bar_item` to transform the system Pill into a battery progress bar and expandable "Stretch" music bar.

### B. The Omni-Integration Layer (Ghost Satellites)
- **Architecture**: Injects lightweight, dependency-free **Ghost Satellites** into common AOSP apps (Gboard, Launcher, Photos, Messages).
- **Stealth Protocol**: Employs the `StealthInterceptor.kt` shield. It neutralizes Xposed detection by stripping NATIVE modifiers and filtering package/stack-trace references.
- **Synergy IPC**: Satellites stream events (e.g., Gboard height) to the SystemUI Brain via `IslandContentProvider` to enable real-time UI adaptation (auto-hiding the Nav Island).

### C. The Neural Core (Centralized State)
- **Single Source of Truth**: All Island state is managed by `IslandNeuralCore.kt`. 
- **UDF Pattern**: Intent → Reducer → StateFlow. Both windows observe the same stream, ensuring 100% synchronization.
- **Persistence**: State snapshots survive SystemUI restarts via JSON serialization.
- **Intelligence Active**: 
    - **Prediction Engine**: Uses `UsageStatsManager` for behavioral forecasting (10-minute horizon). Pre-warms app icons and assets before the user even taps.
    - **ML Gesture Classifier**: Online-learning Naive Bayes classifier for elite touch recognition. Adapts to user pressure and velocity profiles.

### D. Infrastructure (The Factory)

- **Kernel-Level Boost**: Reaches UID 1000 permissions to drop caches (`echo 3 > /proc/sys/vm/drop_caches`) and compact memory.
- **Hardware Controller**: Locks Snapdragon GPU clock speeds and switches CPU governors (Battery to "Wild" mode).
- **Security Emulator**: Spoofs MIUI/Xiaomi properties to unlock 120FPS and hardware-specific features in games.

## 3. UI/UX Identity (The Soul)

### A. Material Brutalism Dialect
- **Aesthetic**: Flat design, zero blurs, 100% Monet (Material You) color synchronization.
- **Typography**: Utilitarian monospaced fonts for technical precision.
- **Performance**: Optimised for locked 120FPS on premium displays.

### B. High-Fidelity Icon Packs
- Supports 9 styles: iOS (Cupertino), OxygenOS, Sam (OneUI), Pixel, Futuristic, etc.
- All icons are rendered via high-performance programmatic vector paths (`ProgrammaticIcons.kt`).

## 4. Agent Operational Protocol (How to Work)

### A. Core Skills & Skills
1. **`cavecrew`**: Use for efficient, compressed subagent delegation to save main context.
2. **`caveman`**: Use for terse, high-signal communication.
3. **`maestro`**: Use for high-level orchestration and multi-agent planning via the Maestro MCP server.
4. **`jules`**: Use for large-scale, project-wide refactoring tasks.
5. **`code-review-commons`**: Mandatory for auditing every push to ensure industry quality.

### C. Infrastructure (The Factory)
- **Cloud-First CI**: Local APK building is restricted (Android/Termux environment). Use **GitHub Actions** (`.github/workflows/build-and-verify.yml`) for all APK generation and remote test execution.
- **Dependency Management**: Centralized via **Gradle Version Catalog** (`gradle/libs.versions.toml`). Mandatory for all new library additions.
- **Diagnostics**: Use **`RedwoodLogger.kt`** for all logging. Supports priority-based output and automated crash dumps to `/sdcard/Redwood/logs/`.
- **Shadow Guardian (Audit)**: A dedicated monitoring loop that reviews AI code in real-time, enforcing architectural constraints and Termux-safe (low-RAM) instruction sets.

### D. Rules of Engagement

- **Defensive Hooking**: Always use `XposedExtensions.kt` wrappers. Never call `findAndHookMethod` directly to avoid crashing SystemUI.
- **Lifecycle Integrity**: All new components MUST implement `BackendComponent` for clean `onStart`/`onStop` handling.
- **Context Efficiency**: Combine searches/reads. Never read the same file twice in one turn.
- **Validation**: No task is done until unit tests (`app/src/test/`) pass and `logcat` confirms zero "Zombie Windows".

---
**SYNC BRIDGE**: Refer to `MASTER_PROJECT_SNAPSHOT.md` for specific implementation requirements and upcoming roadmap features.
