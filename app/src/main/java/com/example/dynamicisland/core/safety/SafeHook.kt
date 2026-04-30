package com.example.dynamicisland.core.safety

import de.robv.android.xposed.XC_MethodHook
import com.example.dynamicisland.core.logging.IslandLogger

object SafeHook {

    inline fun run(tag: String, crossinline block: () -> Unit) {
        runCatching {
            block()
        }.onFailure { error ->
            IslandLogger.e(tag, "Crash isolated to prevent SystemUI death", error)
        }
    }

    fun create(
        tag: String,
        before: ((XC_MethodHook.MethodHookParam) -> Unit)? = null,
        after: ((XC_MethodHook.MethodHookParam) -> Unit)? = null
    ): XC_MethodHook {
        return object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                before?.let { action ->
                    run("$tag-Before") { action(param) }
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                after?.let { action ->
                    run("$tag-After") { action(param) }
                }
            }
        }
    }
}
