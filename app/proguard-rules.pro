# 1. Protect the Xposed entry point from being deleted or renamed
-keep class com.example.dynamicisland.MainHook { *; }

# 2. Protect all of our module's internal classes and models
-keep class com.example.dynamicisland.** { *; }

# 3. Allow Compose to shrink safely
-keepattributes *Annotation*

# 🚀 PROGUARD FIX: Protect the data models from R8 obfuscation
-keep class com.example.dynamicisland.LiveActivityModel** { *; }
-keep class com.example.dynamicisland.IslandAction { *; }
-keep class com.example.dynamicisland.IslandGesture { *; }
