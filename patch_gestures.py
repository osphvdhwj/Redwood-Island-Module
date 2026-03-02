import re

def patch_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Define the old block to match exactly (using regex for flexibility with whitespace)
    search_pattern = r"        view\.onSingleTap = \{.*?(?=        view\.onSwipeLeft = \{)"

    replacement = """        // CENTRALIZED GESTURE MACHINE WITH INTERACTION LOCKS
        view.onSingleTap = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.TYPE_1_MINI -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    }
                    DynamicIslandView.IslandState.TYPE_2_MID -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_3_MAX)
                    }
                    DynamicIslandView.IslandState.TYPE_SPLIT -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    }
                    DynamicIslandView.IslandState.TYPE_3_MAX -> {
                        isUserExpanded = false // User manually closed it, release the lock
                        island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                        resolveHighestPriorityDebounced()
                    }
                    else -> {}
                }
            }
        }

        view.onSwipeDown = {
            val island = islandViewRef?.get()
            if (island != null) {
                isUserExpanded = true // Swiping down implies manual expansion
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.TYPE_1_MINI -> island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    DynamicIslandView.IslandState.TYPE_2_MID -> island.setState(DynamicIslandView.IslandState.TYPE_3_MAX)
                    DynamicIslandView.IslandState.TYPE_SPLIT -> island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    else -> {}
                }
            }
        }

        view.onSwipeUp = {
            val island = islandViewRef?.get()
            if (island != null) {
                when (island.islandState.value) {
                    DynamicIslandView.IslandState.TYPE_3_MAX -> {
                        isUserExpanded = true
                        island.setState(DynamicIslandView.IslandState.TYPE_2_MID)
                    }
                    DynamicIslandView.IslandState.TYPE_2_MID -> {
                        isUserExpanded = false // Releasing the lock
                        island.setState(DynamicIslandView.IslandState.TYPE_1_MINI)
                        resolveHighestPriorityDebounced()
                    }
                    else -> {
                        isUserExpanded = false
                        forceHide()
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
        print("Successfully patched gesture listeners.")
    else:
        print("Could not find the gesture listeners block to replace.")

patch_file('app/src/main/java/com/example/dynamicisland/IslandController.kt')
