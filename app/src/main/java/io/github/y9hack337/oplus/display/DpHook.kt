package io.github.y9hack337.oplus.display

import android.app.Dialog
import android.util.Log
import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedHashSet

class DpHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam == null) {
            return
        }
        if (lpparam.packageName != SYSTEMUI_PKG || lpparam.processName != SYSTEMUI_PKG) {
            return
        }

        val classLoader = lpparam.classLoader
        val implClass = findClassSafely(
            "com.oplus.systemui.oplusstatusbar.display.OplusConnectingDisplayExImpl",
            classLoader
        )
        if (implClass == null) {
            XposedBridge.log("$TAG: target class not found, skip in ${lpparam.processName}")
            return
        }

        var cachedShowDialog: Any? = null

        XposedBridge.hookAllMethods(
            implClass,
            "showDialog",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args.size >= 2 && param.args[1] is Boolean) {
                        val original = param.args[1] as Boolean
                        if (!original) {
                            param.args[1] = true
                            XposedBridge.log("$TAG: showDialog arg[1] forced false->true")
                        }
                    }
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    if (tryClickPositiveButton(param.thisObject)) {
                        cachedShowDialog = null
                        return
                    }

                    val cached = cachedShowDialog ?: findCallbackInFields(param.thisObject)
                    if (cached == null) {
                        val invokedByScan = tryInvokeOnSelectedFromObjectGraph(param.thisObject, param.args)
                        if (!invokedByScan) {
                            XposedBridge.log("$TAG: showDialog called but callback not found")
                        }
                        return
                    }
                    val invoked = tryInvokeOnSelected(cached)
                    if (!invoked) {
                        val invokedByScan = tryInvokeOnSelectedFromObjectGraph(param.thisObject, param.args)
                        if (!invokedByScan) {
                            XposedBridge.log("$TAG: onSelected(int, boolean) not found on ${cached.javaClass.name}")
                        }
                    }
                    cachedShowDialog = null
                }
            }
        )

        val callbackClasses = LinkedHashSet<Class<*>>()
        for (index in 1..8) {
            findClassSafely(
                "com.oplus.systemui.oplusstatusbar.display.OplusConnectingDisplayExImpl\$showDialog\$$index",
                classLoader
            )?.let { clazz ->
                if (findOnSelectedMethod(clazz) != null) {
                    callbackClasses.add(clazz)
                }
            }
        }
        implClass.declaredClasses.forEach { clazz ->
            if (findOnSelectedMethod(clazz) != null) {
                callbackClasses.add(clazz)
            }
        }

        if (callbackClasses.isEmpty()) {
            XposedBridge.log("$TAG: showDialog callback class not found, auto-confirm may not work")
            return
        }

        callbackClasses.forEach { callbackClass ->
            XposedBridge.hookAllConstructors(
                callbackClass,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        cachedShowDialog = param.thisObject
                        val callbackName = param.thisObject.javaClass.name
                        XposedBridge.log("$TAG: captured callback $callbackName")
                        Log.d(TAG, "showDialog callback created: $callbackName")
                    }
                }
            )
        }
        XposedBridge.log("$TAG: hooks installed in ${lpparam.processName}, callbackClasses=${callbackClasses.size}")
    }

    private fun findClassSafely(name: String, classLoader: ClassLoader?): Class<*>? {
        return try {
            XposedHelpers.findClass(name, classLoader)
        } catch (_: Throwable) {
            null
        }
    }

    private fun findOnSelectedMethod(clazz: Class<*>): java.lang.reflect.Method? {
        val methods = clazz.declaredMethods + clazz.methods
        return methods.firstOrNull { method ->
            if (method.name != "onSelected") {
                return@firstOrNull false
            }
            val params = method.parameterTypes
            params.size == 2 &&
                (params[0] == Int::class.javaPrimitiveType || params[0] == Int::class.javaObjectType) &&
                (params[1] == Boolean::class.javaPrimitiveType || params[1] == Boolean::class.javaObjectType)
        }
    }

    private fun findCallbackInFields(target: Any): Any? {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            clazz.declaredFields.forEach { field ->
                try {
                    field.isAccessible = true
                    val value = field.get(target) ?: return@forEach
                    if (findOnSelectedMethod(value.javaClass) != null) {
                        XposedBridge.log("$TAG: callback found in field ${clazz.name}.${field.name}")
                        return value
                    }
                } catch (_: Throwable) {
                    // Ignore inaccessible or vendor-private fields.
                }
            }
            clazz = clazz.superclass
        }
        return null
    }

    private fun tryInvokeOnSelected(target: Any): Boolean {
        return try {
            val method = findOnSelectedMethod(target.javaClass) ?: return false
            method.isAccessible = true
            method.invoke(target, -1, true)
            XposedBridge.log("$TAG: invoked onSelected(-1, true) on ${target.javaClass.name}")
            true
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: invoke onSelected failed on ${target.javaClass.name}: ${t.message}")
            false
        }
    }

    private fun tryInvokeOnSelectedFromObjectGraph(root: Any, args: Array<Any?>): Boolean {
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val candidates = ArrayDeque<Any>()

        fun enqueue(value: Any?) {
            if (value == null) return
            if (seen.add(value)) {
                candidates.addLast(value)
            }
        }

        enqueue(root)
        args.forEach { enqueue(it) }

        var visited = 0
        while (candidates.isNotEmpty() && visited < 80) {
            visited++
            val current = candidates.removeFirst()
            if (tryInvokeOnSelected(current)) {
                XposedBridge.log("$TAG: fallback object-graph invoke success (${current.javaClass.name})")
                return true
            }
            if (tryInvokeIntBoolMethodFallback(current)) {
                XposedBridge.log("$TAG: fallback int-bool invoke success (${current.javaClass.name})")
                return true
            }

            var clazz: Class<*>? = current.javaClass
            var scannedFieldCount = 0
            while (clazz != null && scannedFieldCount < 80) {
                clazz.declaredFields.forEach { field ->
                    if (scannedFieldCount >= 80) return@forEach
                    scannedFieldCount++
                    try {
                        field.isAccessible = true
                        enqueue(field.get(current))
                    } catch (_: Throwable) {
                        // Ignore inaccessible vendor fields.
                    }
                }
                clazz = clazz.superclass
            }
        }

        return false
    }

    private fun tryInvokeIntBoolMethodFallback(target: Any): Boolean {
        val className = target.javaClass.name
        if (
            !className.contains("display", ignoreCase = true) &&
            !className.contains("dialog", ignoreCase = true) &&
            !className.contains("showDialog")
        ) {
            return false
        }

        val methods = (target.javaClass.declaredMethods + target.javaClass.methods)
            .filter { method ->
                if (method.name == "onSelected") {
                    return@filter false
                }
                val params = method.parameterTypes
                params.size == 2 &&
                    (params[0] == Int::class.javaPrimitiveType || params[0] == Int::class.javaObjectType) &&
                    (params[1] == Boolean::class.javaPrimitiveType || params[1] == Boolean::class.javaObjectType)
            }
            .distinctBy { method ->
                "${method.declaringClass.name}#${method.name}:${method.parameterTypes.joinToString(",") { it.name }}"
            }

        methods.take(4).forEach { method ->
            try {
                method.isAccessible = true
                method.invoke(target, -1, true)
                XposedBridge.log("$TAG: invoked fallback ${method.declaringClass.name}.${method.name}(-1, true)")
                return true
            } catch (_: Throwable) {
                // Try next candidate.
            }
        }
        return false
    }

    private fun tryClickPositiveButton(target: Any): Boolean {
        val dialog = try {
            XposedHelpers.getObjectField(target, "dialog") as? Dialog
        } catch (_: Throwable) {
            null
        } ?: return false

        if (!dialog.isShowing) {
            return false
        }

        val positiveButton = try {
            val getButton = dialog.javaClass.getMethod("getButton", Int::class.javaPrimitiveType)
            getButton.isAccessible = true
            getButton.invoke(dialog, -1) as? View
        } catch (_: Throwable) {
            null
        } ?: dialog.findViewById(android.R.id.button1)

        if (positiveButton != null && positiveButton.isEnabled) {
            positiveButton.performClick()
            XposedBridge.log("$TAG: clicked positive button via dialog field")
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "DpHook"
        private const val SYSTEMUI_PKG = "com.android.systemui"
    }
}
