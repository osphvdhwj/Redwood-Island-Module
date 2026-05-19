# 1. Protect the Xposed entry point from being deleted or renamed
-keep class com.example.dynamicisland.MainHook { *; }

# 2. Protect all of our module's internal classes and models
-keep class com.example.dynamicisland.** { *; }

# 3. Allow Compose to shrink safely
-keepattributes *Annotation*

# ---- Keep the island core (reflective calls & broadcast receivers) ----
-keep class com.dynamicisland.model.** { *; }
-keep class com.dynamicisland.hook.** { *; }
-keep class com.dynamicisland.controller.** { *; }
-keep class com.dynamicisland.notification.** { *; }
-keep class com.dynamicisland.prediction.** { *; }

# Gson / serialization (if used)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# DataStore
-keep class androidx.datastore.** { *; }

# AGSL shaders
-keep class android.graphics.RuntimeShader { *; }

# ---- Strip debug logging (optional) ----
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ---- General Android rules ----
-dontwarn com.dynamicisland.**
-keepattributes Signature
-keepattributes *Annotation*

# ---- Optimize for size ----
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5