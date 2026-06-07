package com.example.dynamicisland.core.ipc

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.json.JSONArray

/**
 * 🛠️ ISLAND STATE SERVICE
 * 
 * Exposes live Island state and models to SystemUI over Binder.
 */
class IslandStateService : Service() {

    companion object {
        const val TAG = "IslandStateService"
        const val ACTION = "com.example.dynamicisland.STATE_SERVICE"

        const val MSG_REGISTER_CLIENT    = 1
        const val MSG_UNREGISTER_CLIENT  = 2
        const val MSG_STATE_UPDATE       = 3
        const val MSG_MODEL_UPDATE       = 4
        const val MSG_SPLIT_MODEL_UPDATE = 5
        const val MSG_FULL_SYNC          = 6
        const val MSG_CONFIG_CHANGED     = 7

        const val KEY_STATE       = "state"
        const val KEY_MODEL_JSON  = "model_json"
        const val KEY_MODEL_TYPE  = "model_type"
        const val KEY_CONFIG_KEY  = "config_key"
    }

    private val clients = ConcurrentHashMap<Int, Messenger>()
    private val scope   = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile var currentIslandState: IslandState   = IslandState.TYPE_0_RING
    @Volatile var currentModel: LiveActivityModel?  = null
    @Volatile var currentSplitModel: LiveActivityModel? = null

    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REGISTER_CLIENT -> {
                    val pid = msg.arg1
                    val clientMessenger = msg.replyTo
                    if (clientMessenger != null) {
                        clients[pid] = clientMessenger
                        sendFullSync(clientMessenger)
                    }
                }
                MSG_UNREGISTER_CLIENT -> {
                    clients.remove(msg.arg1)
                }
                MSG_FULL_SYNC -> {
                    msg.replyTo?.let { sendFullSync(it) }
                }
            }
        }
    }

    private val serverMessenger = Messenger(incomingHandler)

    override fun onBind(intent: Intent): IBinder = serverMessenger.binder

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        clients.clear()
    }

    fun publishState(state: IslandState) {
        currentIslandState = state
        val msg = Message.obtain(null, MSG_STATE_UPDATE).apply {
            data = Bundle().apply { putString(KEY_STATE, state.name) }
        }
        broadcast(msg)
    }

    fun publishModel(model: LiveActivityModel?) {
        currentModel = model
        val msg = Message.obtain(null, MSG_MODEL_UPDATE).apply {
            data = Bundle().apply {
                if (model != null) {
                    putString(KEY_MODEL_TYPE, model::class.simpleName)
                    putString(KEY_MODEL_JSON, LiveActivityModelSerializer.toJson(model).toString())
                }
            }
        }
        broadcast(msg)
    }

    fun publishSplitModel(model: LiveActivityModel?) {
        currentSplitModel = model
        val msg = Message.obtain(null, MSG_SPLIT_MODEL_UPDATE).apply {
            data = Bundle().apply {
                if (model != null) {
                    putString(KEY_MODEL_TYPE, model::class.simpleName)
                    putString(KEY_MODEL_JSON, LiveActivityModelSerializer.toJson(model).toString())
                }
            }
        }
        broadcast(msg)
    }

    fun publishConfigChange(key: String) {
        val msg = Message.obtain(null, MSG_CONFIG_CHANGED).apply {
            data = Bundle().apply { putString(KEY_CONFIG_KEY, key) }
        }
        broadcast(msg)
    }

    private fun sendFullSync(client: Messenger) {
        try {
            val msg = Message.obtain(null, MSG_FULL_SYNC).apply {
                data = Bundle().apply {
                    putString(KEY_STATE, currentIslandState.name)
                    currentModel?.let {
                        putString(KEY_MODEL_TYPE, it::class.simpleName)
                        putString(KEY_MODEL_JSON, LiveActivityModelSerializer.toJson(it).toString())
                    }
                    currentSplitModel?.let {
                        putString("split_model_type", it::class.simpleName)
                        putString("split_model_json", LiveActivityModelSerializer.toJson(it).toString())
                    }
                }
            }
            client.send(msg)
        } catch (e: RemoteException) { }
    }

    private fun broadcast(msg: Message) {
        val deadClients = mutableListOf<Int>()
        clients.forEach { (pid, messenger) ->
            try {
                val copy = Message.obtain(null, msg.what).apply { data = msg.data }
                messenger.send(copy)
            } catch (e: RemoteException) {
                deadClients.add(pid)
            }
        }
        deadClients.forEach { clients.remove(it) }
    }
}

object StatePublisher {
    private var service: IslandStateService? = null
    fun attach(svc: IslandStateService) { service = svc }
    fun detach() { service = null }
    fun publishState(state: IslandState) = service?.publishState(state)
    fun publishModel(model: LiveActivityModel?) = service?.publishModel(model)
    fun publishSplitModel(model: LiveActivityModel?) = service?.publishSplitModel(model)
    fun publishConfigChange(key: String) = service?.publishConfigChange(key)
}

object LiveActivityModelSerializer {

    fun toJson(model: LiveActivityModel): JSONObject {
        val obj = JSONObject()
        obj.put("id",          model.id)
        obj.put("type",        model.type.name)
        obj.put("isTransient", model.isTransient)
        obj.put("isCritical",  model.isCritical)
        obj.put("isSensitive", model.isSensitive)
        obj.put("cls",         model::class.simpleName)

        when (model) {
            is LiveActivityModel.General -> {
                obj.put("title",       model.title)
                obj.put("dataText",    model.dataText)
                obj.put("accentColor", model.accentColor)
            }
            is LiveActivityModel.Charging -> {
                obj.put("level",      model.level)
                obj.put("isPluggedIn", model.isPluggedIn)
            }
            is LiveActivityModel.Call -> {
                obj.put("callerName", model.callerName)
                obj.put("state",      model.state)
                obj.put("startTime",  model.startTime)
            }
            is LiveActivityModel.Music -> {
                obj.put("title",        model.title)
                obj.put("artist",       model.artist)
                obj.put("isPlaying",    model.isPlaying)
                obj.put("durationMs",   model.durationMs)
                obj.put("positionMs",   model.positionMs)
                obj.put("pkg",          model.appPackageName)
                obj.put("dominantColor", model.dominantColor ?: 0)
            }
            is LiveActivityModel.OngoingTask -> {
                obj.put("pkgName",     model.pkgName)
                obj.put("title",       model.title)
                obj.put("text",        model.text)
                obj.put("progress",    model.progress)
                obj.put("progressMax", model.progressMax)
            }
            is LiveActivityModel.HardwareMonitor -> {
                obj.put("cpuTempCelsius", model.cpuTempCelsius)
                obj.put("cpuFreqMhz",    model.cpuFreqMhz)
                obj.put("isGamingModeOn", model.isGamingModeOn)
            }
            is LiveActivityModel.RealityPill -> {
                obj.put("contextLabel",   model.contextLabel)
                obj.put("actionLabel",    model.actionLabel)
            }
        }
        return obj
    }

    fun fromJson(obj: JSONObject): LiveActivityModel? {
        return try {
            val type = ActivityType.valueOf(obj.getString("type"))
            when (obj.optString("cls")) {
                "General" -> LiveActivityModel.General(
                    id          = obj.getString("id"),
                    type        = type,
                    title       = obj.getString("title"),
                    dataText    = obj.getString("dataText"),
                    accentColor = obj.getInt("accentColor")
                )
                "Charging" -> LiveActivityModel.Charging(
                    id         = obj.getString("id"),
                    level      = obj.getInt("level"),
                    isPluggedIn = obj.getBoolean("isPluggedIn")
                )
                "Call" -> LiveActivityModel.Call(
                    callerName = obj.getString("callerName"),
                    state      = obj.getString("state"),
                    startTime  = obj.getLong("startTime")
                )
                "Music" -> LiveActivityModel.Music(
                    id             = obj.getString("id"),
                    type           = type,
                    title          = obj.getString("title"),
                    artist         = obj.getString("artist"),
                    isPlaying      = obj.getBoolean("isPlaying"),
                    durationMs     = obj.getLong("durationMs"),
                    positionMs     = obj.getLong("positionMs"),
                    appPackageName = obj.getString("pkg"),
                    dominantColor  = obj.optInt("dominantColor").takeIf { it != 0 }
                )
                "OngoingTask" -> LiveActivityModel.OngoingTask(
                    id          = obj.getString("id"),
                    pkgName     = obj.getString("pkgName"),
                    title       = obj.getString("title"),
                    text        = obj.getString("text"),
                    progress    = obj.getInt("progress"),
                    progressMax = obj.getInt("progressMax")
                )
                "HardwareMonitor" -> LiveActivityModel.HardwareMonitor(
                    id              = obj.getString("id"),
                    cpuTempCelsius  = obj.optDouble("cpuTempCelsius", 0.0).toFloat(),
                    cpuFreqMhz      = obj.optInt("cpuFreqMhz", 0),
                    isGamingModeOn  = obj.optBoolean("isGamingModeOn", false)
                )
                "RealityPill" -> LiveActivityModel.RealityPill(
                    id           = obj.getString("id"),
                    contextLabel = obj.getString("contextLabel"),
                    actionLabel  = obj.getString("actionLabel")
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
