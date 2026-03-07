# 1. Protect the Xposed entry point from being deleted or renamed
-keep class com.example.dynamicisland.MainHook { *; }

# 2. Protect all of our module's internal classes and models
-keep class com.example.dynamicisland.** { *; }

# 3. Allow Compose to shrink safely
-keepattributes *Annotation*
