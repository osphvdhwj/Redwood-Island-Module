package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.bluetooth.BluetoothDevice
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager

class IslandConnectivityManager(
    private val context: Context,
    private val onEventCaught: (LiveActivityModel.General) -> Unit
) {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val action = intent.action ?: return
            
            var title = ""
            var text = ""
            var color = android.graphics.Color.WHITE

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    title = "Bluetooth Connected"
                    text = device?.name ?: "Audio Device"
                    color = android.graphics.Color.parseColor("#0082FC") // Bluetooth Blue
                }
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        title = "Wi-Fi Connected"
                        text = "Network Secured"
                        color = android.graphics.Color.parseColor("#34A853") // Green
                    } else return
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    val isAirplaneMode = intent.getBooleanExtra("state", false)
                    title = "Airplane Mode"
                    text = if (isAirplaneMode) "All connections disabled" else "Connections restored"
                    color = if (isAirplaneMode) android.graphics.Color.parseColor("#FFA500") else android.graphics.Color.GREEN
                }
                "android.hardware.usb.action.USB_STATE" -> {
                    val connected = intent.getBooleanExtra("connected", false)
                    if (connected) {
                        title = "USB Connected"
                        text = "File transfer available"
                        color = android.graphics.Color.CYAN
                    } else return
                }
                "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                    val state = intent.getIntExtra("wifi_state", 0)
                    if (state == 13) { // WIFI_AP_STATE_ENABLED
                        title = "Hotspot Active"
                        text = "Sharing connection"
                        color = android.graphics.Color.parseColor("#FFD700") // Gold
                    } else return
                }
                else -> return
            }

            onEventCaught(
                LiveActivityModel.General(
                    id = "sys_conn_${System.currentTimeMillis()}",
                    type = ActivityType.HARDWARE,
                    title = title,
                    dataText = text,
                    accentColor = color
                )
            )
        }
    }

    fun startListening() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction("android.hardware.usb.action.USB_STATE")
            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
        }
        context.registerReceiver(receiver, filter)
    }

    fun stopListening() {
        try { context.unregisterReceiver(receiver) } catch (e: Throwable) {}
    }
}
