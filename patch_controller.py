import re

with open("app/src/main/java/com/example/dynamicisland/IslandController.kt", "r") as f:
    text = f.read()

old_code = """                "NONE" -> {
                    if (gesture == IslandGesture.SWIPE_UP) _islandState.value = IslandState.TYPE_1_MINI
                    if (gesture == IslandGesture.SWIPE_DOWN) {
                        if (_islandState.value != IslandState.TYPE_3_MAX && _islandState.value != IslandState.TYPE_SPLIT) {
                            _islandState.value = IslandState.TYPE_3_MAX
                        } else {
                            // Expand System Notification Shade natively
                            try {
                                @android.annotation.SuppressLint("WrongConstant")
                                val sbs = context.getSystemService("statusbar")
                                val expandMethod = sbs?.javaClass?.getMethod("expandNotificationsPanel")
                                expandMethod?.invoke(sbs)
                            } catch (e: Exception) {}
                        }
                    }
                }
            }"""

new_code = """                "NONE" -> {
                    if (gesture == IslandGesture.SWIPE_UP) _islandState.value = IslandState.TYPE_1_MINI
                    if (gesture == IslandGesture.SWIPE_DOWN) {
                        if (_islandState.value != IslandState.TYPE_3_MAX && _islandState.value != IslandState.TYPE_SPLIT) {
                            _islandState.value = IslandState.TYPE_3_MAX
                        } else {
                            // Expand System Notification Shade natively
                            try {
                                @android.annotation.SuppressLint("WrongConstant")
                                val sbs = context.getSystemService("statusbar")
                                val expandMethod = sbs?.javaClass?.getMethod("expandNotificationsPanel")
                                expandMethod?.invoke(sbs)
                            } catch (e: Exception) {}
                        }
                    }
                }
            }
        }"""

content = text.replace(old_code, new_code)
with open("app/src/main/java/com/example/dynamicisland/IslandController.kt", "w") as f:
    f.write(content)
