package com.example.dynamicisland.core.manager

import android.content.Context
import android.content.Intent
import android.view.View
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.hook.SystemUIContextKeeper
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.QSTileState
import com.example.dynamicisland.shared.settings.*
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IslandActionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    fun handleQSTileClick(gestureName: String, onTilesUpdated: (List<QSTileState>) -> Unit) {
        val targetSpec = gestureName.removePrefix("QS_CLICK_")
        val host = SystemUIContextKeeper.qsTileHost ?: return

        try {
            val tilesCollection = XposedHelpers.callMethod(host, "getTiles") as Collection<*>
            val targetTile = tilesCollection.firstOrNull {
                XposedHelpers.callMethod(it, "getTileSpec") == targetSpec
            }

            if (targetTile != null) {
                // ✅ FIXED: Call click(View) with null argument
                XposedHelpers.callMethod(targetTile, "click", null)

                // Give the system time to toggle the hardware before syncing the UI
                scope.launch {
                    delay(300)
                    syncQSTiles(listOf(targetSpec), onTilesUpdated)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun syncQSTiles(activeTileSpecs: List<String>, onTilesUpdated: (List<QSTileState>) -> Unit) {
        val host = SystemUIContextKeeper.qsTileHost ?: return
        try {
            val tilesCollection = XposedHelpers.callMethod(host, "getTiles") as Collection<*>
            val newTileStates = mutableListOf<QSTileState>()

            for (tileObj in tilesCollection) {
                if (tileObj == null) continue
                val tileSpec = XposedHelpers.callMethod(tileObj, "getTileSpec") as String

                if (activeTileSpecs.contains(tileSpec)) {
                    val stateObj = XposedHelpers.callMethod(tileObj, "getState")
                    val label = XposedHelpers.getObjectField(stateObj, "label") as? CharSequence ?: ""
                    val stateInt = XposedHelpers.getIntField(stateObj, "state")

                    // AOSP States: 0 = Unavailable, 1 = Inactive, 2 = Active
                    newTileStates.add(
                        QSTileState(
                            tileName = tileSpec,               // tileSpec is the identifier
                            isActive = stateInt == 2,
                            iconRes = 0,                       // placeholder, you can load actual icon later
                            isUnavailable = stateInt == 0
                        )
                    )
                }
            }
            onTilesUpdated(newTileStates)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun executeBackgroundIntent(intent: Intent, inFreeform: Boolean = false, onExecuted: () -> Unit) {
        try {
            val options = android.app.ActivityOptions.makeBasic()
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                options.pendingIntentBackgroundActivityStartMode =
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            
            if (inFreeform) {
                // Set windowing mode to FREEFORM (5)
                try {
                    val method = options.javaClass.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                    method.invoke(options, 5)
                    
                    // Set bounds (Centered, 70% of screen)
                    val dm = context.resources.displayMetrics
                    val width = (dm.widthPixels * 0.8f).toInt()
                    val height = (dm.heightPixels * 0.7f).toInt()
                    val left = (dm.widthPixels - width) / 2
                    val top = (dm.heightPixels - height) / 2
                    options.setLaunchBounds(android.graphics.Rect(left, top, left + width, top + height))
                } catch (e: Exception) {}
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent.send(options.toBundle())
            onExecuted()
        } catch (e: Exception) {
            try {
                val finalIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(finalIntent)
            } catch (e2: Exception) {
                // ignore
            }
        }
    }

    fun launchAppIntent(packageName: String, inFreeform: Boolean = false, onExecuted: () -> Unit) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            executeBackgroundIntent(launchIntent, inFreeform, onExecuted)
        }
    }

    fun launchAudioOutputSwitcher(packageName: String?) {
        try {
            val intent = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
            intent.setPackage("com.android.systemui")
            packageName?.let { intent.putExtra("package_name", it) }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    fun triggerOneHandMode() {
        try {
            // Attempt to trigger AOSP One-Handed mode via reflection
            val shellClazz = Class.forName("com.android.wm.shell.onehanded.OneHandedController")
            // This usually requires a reference to the singleton, which we'd get from a hook
            // Fallback: Trigger via system shortcut intent or broadcast
            context.sendBroadcast(Intent("com.android.systemui.action.TOGGLE_ONE_HANDED_MODE").setPackage("com.android.systemui"))
            
            // Second Fallback: Shell command (as we are in SU context potentially or SystemUI)
            Runtime.getRuntime().exec("cmd onehanded start")
        } catch (e: Exception) {
             // Third Fallback: MIUI specific if detected
             try {
                 context.sendBroadcast(Intent("com.miui.action.ONE_HANDED_MODE").setPackage("com.android.systemui"))
             } catch(e2: Exception) {}
        }
    }

    fun exitOneHandMode() {
        try {
            Runtime.getRuntime().exec("cmd onehanded stop")
            context.sendBroadcast(Intent("com.android.systemui.action.STOP_ONE_HANDED_MODE").setPackage("com.android.systemui"))
        } catch (e: Exception) {}
    }
}
