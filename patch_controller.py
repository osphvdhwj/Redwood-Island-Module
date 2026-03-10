import re

with open('./app/src/main/java/com/example/dynamicisland/IslandController.kt', 'r') as f:
    content = f.read()

old_create = """        val view = DynamicIslandView(context, moduleContext)
        this.islandView = view // 🚀 FIX: Store the reference
        view.windowManager = wm"""

new_create = """        val view = DynamicIslandView(context, moduleContext)
        this.islandView = view // 🚀 FIX: Store the reference
        view.onSplitPillClick = {
            // If the split model is the Battery Manager's Reality Pill or Charging, do nothing.
            // If it's a second app, launch it here!
            val sModel = _splitModel.value
            if (sModel is LiveActivityModel.Charging) {
                 _islandState.value = IslandState.TYPE_CUBE
            }
        }
        view.windowManager = wm"""

content = content.replace(old_create, new_create)

with open('./app/src/main/java/com/example/dynamicisland/IslandController.kt', 'w') as f:
    f.write(content)
