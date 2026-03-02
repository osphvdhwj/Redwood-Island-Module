import re

def patch_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Define the old block to match exactly
    search_pattern = r"    private fun resolveHighestPriority\(\) \{.*?(?=    fun hookFrameworkNotifications)"

    replacement = """    private fun resolveHighestPriority() {
        val island = islandViewRef?.get() ?: return

        // Filter out dismissed notifications
        val available = activeActivities.values.filter { !dismissedActivities.contains(it.id) }
        val sorted = available.sortedByDescending { it.type.priority }

        island.post {
            val primary = sorted.getOrNull(0)
            val secondary = sorted.getOrNull(1)
            val currentVisualState = island.islandState.value

            island.clearLiveActivityUI()
            island.clearSecondaryActivityUI()

            // 1. CRITICAL OVERRIDE: Alarms and Calls don't care about locks, they demand the screen.
            val isCritical = primary?.type == ActivityType.ALARM || primary?.type == ActivityType.CALL

            // 2. INTERACTION LOCK: If the user is currently using the expanded pill, don't interrupt them.
            if (isUserExpanded && !isCritical) {
                if (primary != null) {
                    // Update the data silently in the background so it's ready when they collapse the pill
                    island.updateLiveActivity(primary.title, primary.dataText, primary.progress, primary.accentColor, primary.type)
                }
                return@post // Halt execution here. Do NOT morph the pill.
            }

            // 3. NORMAL STATE RESOLUTION (Adaptive Flexing)
            if (primary != null && secondary != null) {
                // Scenario: Multiple activities (e.g., Music + Charging)
                island.updateActivities(primary, secondary)
                if (currentVisualState != DynamicIslandView.IslandState.TYPE_3_MAX && currentVisualState != DynamicIslandView.IslandState.TYPE_2_MID) {
                    island.setState(DynamicIslandView.IslandState.TYPE_SPLIT)
                }

            } else if (primary != null && currentController != null && primary.type != ActivityType.MEDIA && !dismissedActivities.contains("sys_media")) {
                // Scenario: Background Music + Single Notification
                island.updateActivities(LiveActivityModel("sys_media", ActivityType.MEDIA, "Media", "Playing", accentColor = android.graphics.Color.MAGENTA), primary)
                if (currentVisualState != DynamicIslandView.IslandState.TYPE_3_MAX && currentVisualState != DynamicIslandView.IslandState.TYPE_2_MID) {
                    island.setState(DynamicIslandView.IslandState.TYPE_SPLIT)
                }

            } else if (primary != null) {
                // Scenario: Single Notification Only
                if (primary.type == ActivityType.MEDIA && currentController != null) {
                    if (currentVisualState == DynamicIslandView.IslandState.HIDDEN || currentVisualState == DynamicIslandView.IslandState.TYPE_SPLIT) {
                        island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                    }
                } else {
                    island.updateLiveActivity(primary.title, primary.dataText, primary.progress, primary.accentColor, primary.type)
                    val targetState = if (primary.type == ActivityType.MESSAGE || primary.type == ActivityType.CALL) DynamicIslandView.IslandState.TYPE_2_MID else DynamicIslandView.IslandState.TYPE_1_MINI

                    // Allow the island to flex to fit the notification, but don't force it if they are busy
                    if (!isUserExpanded && currentVisualState != targetState) {
                        island.setState(targetState)
                    }
                }

            } else {
                // Scenario: Nothing is active. Clean up and hide.
                isUserExpanded = false
                if (currentController == null || dismissedActivities.contains("sys_media")) {
                    island.setState(DynamicIslandView.IslandState.HIDDEN)
                } else if (!dismissedActivities.contains("sys_media")) {
                    if (currentVisualState == DynamicIslandView.IslandState.HIDDEN || currentVisualState == DynamicIslandView.IslandState.TYPE_SPLIT) {
                        island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                    }
                }
            }
        }
    }

"""

    match = re.search(search_pattern, content, re.DOTALL)
    if match:
        content = content[:match.start()] + replacement + content[match.end():]
        with open(file_path, 'w') as f:
            f.write(content)
        print("Successfully patched resolveHighestPriority.")
    else:
        print("Could not find the resolveHighestPriority block to replace.")

patch_file('app/src/main/java/com/example/dynamicisland/IslandController.kt')
