package com.example.dynamicisland

import android.content.Context
import android.content.Intent
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
        val tilesCollection = XposedHelpers.callMethod(host, "getTiles") as Collection<*>
        val targetTile = tilesCollection.firstOrNull { 
            XposedHelpers.callMethod(it, "getTileSpec") == targetSpec 
        }

        if (targetTile != null) {
            XposedHelpers.callMethod(targetTile, "click", null) 
            scope.launch { delay(300); syncQSTiles(onTilesUpdated) }
        }
    }

    fun syncQSTiles(onTilesUpdated: (List<QSTileState>) -> Unit) {
        val host = SystemUIContextKeeper.qsTileHost ?: return
        try {
            val tilesCollection = XposedHelpers.callMethod(host, "getTiles") as Collection<*>
            val userSelectedSpecs = listOf("wifi", "bt", "flashlight", "custom(com.example.vpn/...)")
            val newTileStates = mutableListOf<QSTileState>()

            for (tileObj in tilesCollection) {
                if (tileObj == null) continue
                val tileSpec = XposedHelpers.callMethod(tileObj, "getTileSpec") as String
            
                if (userSelectedSpecs.contains(tileSpec)) {
                    val stateObj = XposedHelpers.callMethod(tileObj, "getState")
                    val label = XposedHelpers.getObjectField(stateObj, "label") as? CharSequence ?: ""
                    val stateInt = XposedHelpers.getIntField(stateObj, "state")
                
                    newTileStates.add(QSTileState(tileSpec, label.toString(), stateInt == 2, stateInt == 0))
                }
            }
            onTilesUpdated(newTileStates)
        } catch (e: Exception) {}
    }

    fun executeBackgroundIntent(intent: Intent, onExecuted: () -> Unit) {
        try {
            val options = android.app.ActivityOptions.makeBasic()
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                options.pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            val pendingIntent = android.app.PendingIntent.getActivity(context, System.currentTimeMillis().toInt(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            pendingIntent.send(options.toBundle())
            onExecuted()
        } catch (e: Exception) {
            try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e2: Exception) {}
        }
    }

    fun launchAppIntent(packageName: String, onExecuted: () -> Unit) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            executeBackgroundIntent(launchIntent, onExecuted)
        }
    }

    fun launchAudioOutputSwitcher(packageName: String?) {
        try {
            val intent = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
            intent.setPackage("com.android.systemui")
            packageName?.let { intent.putExtra("package_name", it) }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            try { context.startActivity(Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (ex: Exception) {}
        }
    }
}
