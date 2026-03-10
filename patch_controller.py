import re

with open('./app/src/main/java/com/example/dynamicisland/IslandController.kt', 'r') as f:
    content = f.read()

# Update componentCallbacks
old_callback = """    private val componentCallbacks = object : android.content.ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
            isLandscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) _islandState.value = IslandState.HIDDEN else evaluatePriority()
        }"""

new_callback = """    private val componentCallbacks = object : android.content.ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
            isLandscape = newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            evaluatePriority()
        }"""

content = content.replace(old_callback, new_callback)

# Update evaluatePriority
old_eval = """    private fun evaluatePriority() {
        if (isLandscape) { _islandState.value = IslandState.HIDDEN; return }

        if (transientModel != null) {
            if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning) {
                // System Alerts (Text) demand the Mid Pill, temporarily overriding everything else
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_2_MID
            } else if (currentMedia?.isPlaying == true || currentMedia != null) {
                // Charging while media playing = Split Cube
                _activeModel.value = currentMedia
                _splitModel.value = transientModel
                _islandState.value = IslandState.TYPE_SPLIT
            } else {
                // Charging while idle = Tiny Cube
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_CUBE
            }
            return
        }"""

new_eval = """    private fun evaluatePriority() {
        // 🚀 EDGE CASE FIX: Allow Critical alerts to show in Landscape!
        if (isLandscape) {
            val isAlertCritical = transientModel?.isCritical == true
            if (!isAlertCritical) {
                _islandState.value = IslandState.HIDDEN
                return
            }
        }

        if (transientModel != null) {
            if (transientModel is LiveActivityModel.SystemAlert || transientModel is LiveActivityModel.AppTimerWarning) {
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_2_MID
            } else if (transientModel is LiveActivityModel.RealityPill) {
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_1_MINI
            } else if (currentMedia?.isPlaying == true || currentMedia != null) {
                _activeModel.value = currentMedia
                _splitModel.value = transientModel
                _islandState.value = IslandState.TYPE_SPLIT
            } else {
                _activeModel.value = transientModel
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_CUBE
            }
            return
        }"""

content = content.replace(old_eval, new_eval)

with open('./app/src/main/java/com/example/dynamicisland/IslandController.kt', 'w') as f:
    f.write(content)
