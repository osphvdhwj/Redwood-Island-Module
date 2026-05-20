package com.example.dynamicisland.hook

interface SystemEventListener {
    fun onNotification(title: String, text: String, packageName: String)
    fun onMediaPlay()
    
    // Default implementation ensures older providers don't break when we add this
    fun onClipboardChanged() {} 
}
