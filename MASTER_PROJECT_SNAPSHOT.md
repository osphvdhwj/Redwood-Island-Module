# Redwood Island & Nav Island: Master Project Blueprint
**DO NOT DELETE OR MODIFY THIS FILE.**
**AUTHORITATIVE SOURCE FOR AGENT CONTEXT.**

## Project Core Philosophy
Dual-island system utility for rooted Android (AOSP/HyperOS).
- **Redwood Island (Top):** Dynamic Island system. Hooks to SystemUI but operates as a managed overlay to support large UI units (Dashboard/Max Pill) without crashing the Status Bar.
- **Nav Island (Bottom):** One-of-a-kind utility that hooks directly into the Android Navigation Gesture Pill, customizing its appearance, physics, and behavior.

---

## Technical Specifications & Requirements

### 1. Redwood Island (Top)
- **Battery Ring:** A fully customizable ring showing battery percentage.
  - **Location:** Can be placed anywhere (distinguished as 'top' for naming).
  - **Dynamics:** The Dynamic Island grows *out* of this ring.
  - **Aesthetics:** Color changes based on battery levels (user-definable).
  - **Visibility:** Must not block views; includes a per-app blacklist to disable.
  - **Animation:** Supports "Breathing" and "Charging" effects.

### 2. Nav Island (Bottom)
- **Pill Hooking:** Directly modifies the `navigation_bar_item` / Pill.
- **Battery Progress:** The Pill functions as a progress bar. 
  - **Color:** Dynamic colors matching the Battery Ring configuration.
  - **Visibility:** Uses a permanent "Dull Color" background so the Pill is never lost, plus a vertical pipe separator (`|`) to indicate the current level exactly.
- **Button Nav:** If user switches to 3-button navigation, Nav Island must **disappear**. (Pending idea for buttons).
- **Expansion:** Expands into large units (Music bar/Dashboard) like Google Gemini/Lens (1x5, 2x6, etc.).
  - **Music Bar Morph:** The Pill "stretches" to become the background of the Music Bar, positioned slightly above the default nav area.

### 3. General Logic & Interaction
- **Synergy:** When battery percentage changes (e.g., 15% -> 14%), both islands pulse in sync for exactly 1 second.
- **Gestures:** Whole gesture system is fully customizable via settings (Double-tap, Swipe, Long-press).
- **Animations:** Charging animations (Glow, Gradient, Particles) are selectable from settings.
- **Haptics:** Dedicated "Haptics" settings category. Sync pulse vibration is OFF by default but togglable.
- **Touch Safety:** Must avoid full-screen window spans to prevent Android 12+ Touch Occlusion.
- **Thermal Management:** Option to "throttle" (stop) animations if device temperature exceeds thresholds.
- **Panic Button:** Quick Settings (QS) Tile to instantly kill/toggle both Islands.

---

## Future Dev Notes
- **Nav Buttons:** Need to brainstorm how to implement Nav Island logic for users who don't use gestures.
- **Auto-Detection:** Implement an auto-detection button for the Redwood Ring to find camera cutout coordinates.
- **Status Bar:** Idea for future: Auto-hide system clock when Redwood expands to prevent overlap.

