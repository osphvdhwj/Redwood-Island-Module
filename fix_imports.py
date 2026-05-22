import os

files_imports = {
    "BarcodeMid.kt": ["import androidx.compose.ui.unit.sp"],
    "ConfigScreens.kt": ["import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset"],
    "IconProvider.kt": ["import androidx.compose.material.icons.filled.*", "import androidx.compose.material.icons.rounded.*"],
    "IslandCallUI.kt": ["import androidx.compose.ui.graphics.Brush", "import androidx.compose.foundation.border"],
    "IslandDashboardMax.kt": ["import com.example.dynamicisland.gesture.IslandGesture", "import androidx.compose.foundation.Image", "import androidx.compose.ui.graphics.asImageBitmap", "import androidx.compose.ui.layout.ContentScale"],
    "IslandMainUI.kt": ["import com.example.dynamicisland.settings.IconPack"],
    "IslandMusicMax.kt": ["import androidx.compose.ui.draw.blur", "import androidx.compose.ui.geometry.Offset", "import androidx.compose.animation.core.animateFloatAsState", "import androidx.compose.ui.draw.scale", "import androidx.compose.ui.input.pointer.pointerInput", "import androidx.compose.foundation.gestures.awaitEachGesture", "import androidx.compose.foundation.gestures.awaitFirstDown", "import androidx.compose.foundation.gestures.waitForUpOrCancellation"],
    "IslandMusicMid.kt": ["import com.example.dynamicisland.model.LocalIslandTheme", "import androidx.compose.ui.input.pointer.pointerInput", "import androidx.compose.foundation.gestures.awaitEachGesture", "import androidx.compose.foundation.gestures.awaitFirstDown", "import androidx.compose.foundation.gestures.waitForUpOrCancellation"],
    "OtpMid.kt": ["import androidx.compose.material.icons.filled.Add", "import androidx.compose.material.icons.Icons"]
}

base_path = "app/src/main/java/com/example/dynamicisland/ui/"

for file_name, imports in files_imports.items():
    path = os.path.join(base_path, file_name)
    if os.path.exists(path):
        with open(path, "r") as f:
            lines = f.readlines()
        
        insert_idx = 0
        for i, line in enumerate(lines):
            if line.startswith("package "):
                insert_idx = i + 1
                break
        
        imports_str = [imp + "\n" for imp in imports]
        lines = lines[:insert_idx] + ["\n"] + imports_str + lines[insert_idx:]
        
        with open(path, "w") as f:
            f.writelines(lines)
        print(f"Updated {file_name}")
    else:
        print(f"File not found: {path}")
