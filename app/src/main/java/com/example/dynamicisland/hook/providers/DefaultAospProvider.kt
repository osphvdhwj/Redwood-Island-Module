package com.example.dynamicisland.hook.providers

import com.example.dynamicisland.hook.SystemEventListener
import com.example.dynamicisland.hook.SystemEventProvider

class DefaultAospProvider : SystemEventProvider {
    private var listener: SystemEventListener? = null

    override fun initHooks(classLoader: ClassLoader) {
        // Stub: Implement default AOSP hooks here using XposedHelpers
    }

    override fun setSystemEventListener(listener: SystemEventListener) {
        this.listener = listener
    }
}
