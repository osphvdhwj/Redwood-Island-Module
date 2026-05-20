package com.example.dynamicisland.hook

interface SystemEventListener {
    fun onNotification(title: String, text: String, pkg: String)
    fun onMediaPlay()
    // Add other relevant system events here
}

interface SystemEventProvider {
    fun initHooks(classLoader: ClassLoader)
    fun setSystemEventListener(listener: SystemEventListener)
}
