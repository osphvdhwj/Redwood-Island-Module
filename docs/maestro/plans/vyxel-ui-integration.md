# Implementation Plan: Vyxel UI & LiquidGlass Integration

## Phase 1: Resource Acquisition & Analysis
- Gather code for LiquidGlass Theme (Light/Dark) from user-provided snippets or repository (awaiting clarification).
- Identify 'Material 3 Expressive' UI patterns (shapes, typography, animations).
- Collect expanded language strings and font files.

## Phase 2: Design System Overhaul
- Implement `LiquidGlassDesignSystem.kt` in `ui/design`.
- Integrate `Material 3 Expressive` configuration in `MD3Theme.kt`.
- Add new `FontFamily` definitions and custom fonts to `res/font`.

## Phase 3: Global Theme Engine Update
- Update `SettingsState` and `IslandNeuralCore` to support the new UI modes.
- Implement a 'Visual Dialect' for LiquidGlass (Glassmorphism, Blurs, Fluency).
- Integrate expanded i18n support in `res/values-*`.

## Phase 4: UI Refinement & Synergy
- Apply LiquidGlass aesthetic to all Island types (Ring, Pill, Cube, Nav).
- Enhance the Dashboard with Expressive UI elements.
- Verify 120Hz performance with the new blur/glass effects.
