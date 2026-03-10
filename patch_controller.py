import re

with open('./app/src/main/java/com/example/dynamicisland/IslandController.kt', 'r') as f:
    content = f.read()

old_eval = """        if (transientModel != null) {
            if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning) {"""

new_eval = """        if (transientModel != null) {
            // 🚀 GAMING FIX: Don't show battery/charging cubes if user is gaming!
            if (currentHardware?.isGamingModeOn == true && transientModel is LiveActivityModel.Charging) {
                // Ignore the popup, leave Island hidden or in mini mode
            } else if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning) {"""

content = content.replace(old_eval, new_eval)

with open('./app/src/main/java/com/example/dynamicisland/IslandController.kt', 'w') as f:
    f.write(content)
