# 1. Protect the Xposed entry point from being deleted or renamed
-keep class com.example.dynamicisland.MainHook { *; }
-keep class com.example.dynamicisland.hook.** { *; }

# 2. Protect Hilt and Android components
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends android.app.Activity

# 3. Protect Hilt injected classes
-keep class * {
    @javax.inject.Inject <init>(...);
}

# 4. Protect LiveActivityModel and other data classes used for IPC/reflection
-keep class com.example.dynamicisland.model.** { *; }
-keep class com.example.dynamicisland.ipc.** { *; }

# 5. ML Kit rules
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# 6. Compose and general optimizations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# DataStore
-keep class androidx.datastore.** { *; }

# AGSL shaders
-keep class android.graphics.RuntimeShader { *; }

# ---- Strip debug logging ----
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ---- Optimize for size ----
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5