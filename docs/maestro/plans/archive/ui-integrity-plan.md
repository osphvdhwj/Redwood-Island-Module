# Implementation Plan: UI Rework & Integrity Overhaul

## Phase 1: Design UI Architecture
- Analyze current `DynamicIslandView.kt` and `MD3Theme.kt`.
- Specify flat UI elements, removal of blurs, and strict Material You adherence.

## Phase 2: Design State Engine
- Centralize state in `IslandNeuralCore`.
- Eliminate `IslandEventBus` race conditions.

## Phase 3: Implement Material Brutalism UI
- Strip `AGSL` and `RenderEffect`.
- Apply solid colors from `md_theme_dark_surface`.
- Replace spring physics with tween animations.

## Phase 4: Refactor IslandController State
- Enforce strict Unidirectional Data Flow.
- Ensure perfect sync between Top and Bottom islands.
