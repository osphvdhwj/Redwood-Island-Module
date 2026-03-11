import re

with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'r') as f:
    content = f.read()

# I already imported them in a previous script (patch_view_gestures.py) but it seems it didn't use the foundation scope for awaitEachGesture. Let's fix the imports.
imports = """import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation"""

content = content.replace("import androidx.compose.ui.input.pointer.awaitEachGesture", "")
content = content.replace("import androidx.compose.ui.input.pointer.awaitFirstDown", "")
content = content.replace("import androidx.compose.ui.input.pointer.waitForUpOrCancellation", "")

content = content.replace("import androidx.compose.ui.input.pointer.PointerEventPass", f"import androidx.compose.ui.input.pointer.PointerEventPass\n{imports}")

with open('./app/src/main/java/com/example/dynamicisland/DynamicIslandView.kt', 'w') as f:
    f.write(content)
