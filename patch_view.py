import re

with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'r') as f:
    content = f.read()

# Update PointerInput Tap Gestures
old_tap = """                    // 🚀 UNIFIED TAP & DRAG ENGINE
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isSquished = true
                                tryAwaitRelease() // Waits for the user to lift their finger
                                isSquished = false
                            },
                            onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                            onDoubleTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) },
                            onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
                        )
                    }"""

new_tap = """                    // 🚀 UNIFIED TAP & DRAG ENGINE
                    .pointerInput(state) {
                        if (state != IslandState.TYPE_3_MAX) { // 🚀 UX FIX: Disable root squish/tap in MAX state so sliders work flawlessly!
                            detectTapGestures(
                                onPress = {
                                    isSquished = true
                                    tryAwaitRelease()
                                    isSquished = false
                                },
                                onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                                onDoubleTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) },
                                onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
                            )
                        }
                    }"""

content = content.replace(old_tap, new_tap)

# Strip Ripple from Split Pill
old_split = """                            .clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape)
                            // 🚀 THE FIX: Independent interaction!
                            .clickable { onSplitPillClick?.invoke() }, """

new_split = """                            .clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape)
                            // 🚀 UX FIX: Remove the cheap grey Android ripple effect
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSplitPillClick?.invoke() }, """

content = content.replace(old_split, new_split)

with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'w') as f:
    f.write(content)
