package com.aliucord.patcher

import com.aliucord.api.PatcherAPI
import com.aliucord.api.Unpatch
import de.robv.android.xposed.XC_MethodHook
import kotlin.reflect.typeOf

private typealias HookCallback<T> = T.(XC_MethodHook.MethodHookParam) -> Unit
private typealias InsteadHookCallback<T> = T.(XC_MethodHook.MethodHookParam) -> Any?

/**
 * Replaces a constructor of a class.
 * @param paramTypes parameters of the method. Useful for patching individual overloads
 * @param callback callback for the patch
 * @return The [Unpatch] object of the patch
 * @see [XC_MethodHook.beforeHookedMethod]
 */
inline fun <reified T> PatcherAPI.instead(vararg paramTypes: Class<*>, crossinline callback: InsteadHookCallback<T>): Unpatch {
    return patch(T::class.java.getDeclaredConstructor(*paramTypes), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                param.result = callback(param.thisObject as T, param)
            } catch (th: Throwable) {
                logger.error("Exception while replacing constructor of ${param.method.declaringClass}", th)
            }
        }
    })
}

/**
 * Replaces a method of a class.
 * @param methodName name of the method to patch
 * @param paramTypes parameters of the method. Useful for patching individual overloads
 * @param callback callback for the patch
 * @return The [Unpatch] object of the patch
 * @see [XC_MethodHook.beforeHookedMethod]
 */
inline fun <reified T> PatcherAPI.instead(methodName: String, vararg paramTypes: Class<*>, crossinline callback: InsteadHookCallback<T>): Unpatch {
    return patch(T::class.java.getDeclaredMethod(methodName, *paramTypes), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                param.result = callback(param.thisObject as T, param)
            } catch (th: Throwable) {
                logger.error("Exception while replacing ${param.method.declaringClass.name}.${param.method.name}", th)
            }
        }
    })
}

private typealias P = XC_MethodHook.MethodHookParam

class PatchContext<T, R>(
    val patchMethod: (args: Array<out Class<*>>, callback: T.(P) -> R) -> Unpatch,
) {
    @PublishedApi
    internal fun patch(vararg classes: Class<*>, callback: T.(P) -> R) = patchMethod(classes, callback)

    @PublishedApi
    @OptIn(ExperimentalStdlibApi::class)
    internal inline fun <reified T> t(): Class<*> {
        val clazz = T::class as kotlin.reflect.KClass<*>
        return if (!typeOf<T>().isMarkedNullable) {
            clazz.javaPrimitiveType ?: clazz.java
        } else {
            clazz.java
        }
    }

    inline operator fun invoke(crossinline callback: T.(P) -> R)
        = patch { callback(this, it) }
    inline operator fun <reified P1> invoke(crossinline callback: T.(P, Arg1<P1>) -> R)
        = patch(t<P1>()) { callback(this, it, Arg1(it)) }
    inline operator fun <reified P1, reified P2> invoke(crossinline callback: T.(P, Arg2<P1, P2>) -> R)
        = patch(t<P1>(), t<P2>()) { callback(this, it, Arg2(it)) }
    inline operator fun <reified P1, reified P2, reified P3> invoke(crossinline callback: T.(P, Arg3<P1, P2, P3>) -> R)
        = patch(t<P1>(), t<P2>(), t<P3>()) { callback(this, it, Arg3(it)) }
    inline operator fun <reified P1, reified P2, reified P3, reified P4> invoke(crossinline callback: T.(P, Arg4<P1, P2, P3, P4>) -> R)
        = patch(t<P1>(), t<P2>(), t<P3>(), t<P4>()) { callback(this, it, Arg4(it)) }
    inline operator fun <reified P1, reified P2, reified P3, reified P4, reified P5> invoke(crossinline callback: T.(P, Arg5<P1, P2, P3, P4, P5>) -> R)
        = patch(t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>()) { callback(this, it, Arg5(it)) }
    inline operator fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6> invoke(crossinline callback: T.(P, Arg6<P1, P2, P3, P4, P5, P6>) -> R)
        = patch(t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>()) { callback(this, it, Arg6(it)) }
    inline operator fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7> invoke(crossinline callback: T.(P, Arg7<P1, P2, P3, P4, P5, P6, P7>) -> R)
        = patch(t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>()) { callback(this, it, Arg7(it)) }
    inline operator fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8> invoke(crossinline callback: T.(P, Arg8<P1, P2, P3, P4, P5, P6, P7, P8>) -> R)
        = patch(t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>()) { callback(this, it, Arg8(it)) }
    inline operator fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9> invoke(crossinline callback: T.(P, Arg9<P1, P2, P3, P4, P5, P6, P7, P8, P9>) -> R)
        = patch(t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>()) { callback(this, it, Arg9(it)) }
}

open class Arg1<P1>(val param: P)
open class Arg2<P1, P2>(param: P) : Arg1<P1>(param)
open class Arg3<P1, P2, P3>(param: P) : Arg2<P1, P2>(param)
open class Arg4<P1, P2, P3, P4>(param: P) : Arg3<P1, P2, P3>(param)
open class Arg5<P1, P2, P3, P4, P5>(param: P) : Arg4<P1, P2, P3, P4>(param)
open class Arg6<P1, P2, P3, P4, P5, P6>(param: P) : Arg5<P1, P2, P3, P4, P5>(param)
open class Arg7<P1, P2, P3, P4, P5, P6, P7>(param: P) : Arg6<P1, P2, P3, P4, P5, P6>(param)
open class Arg8<P1, P2, P3, P4, P5, P6, P7, P8>(param: P) : Arg7<P1, P2, P3, P4, P5, P6, P7>(param)
open class Arg9<P1, P2, P3, P4, P5, P6, P7, P8, P9>(param: P) : Arg8<P1, P2, P3, P4, P5, P6, P7, P8>(param)

inline operator fun <reified T> Arg1<T>.component1() = param.args[0] as T
inline operator fun <reified T> Arg2<*, T>.component2() = param.args[1] as T
inline operator fun <reified T> Arg3<*, *, T>.component3() = param.args[2] as T
inline operator fun <reified T> Arg4<*, *, *, T>.component4() = param.args[3] as T
inline operator fun <reified T> Arg5<*, *, *, *, T>.component5() = param.args[4] as T
inline operator fun <reified T> Arg6<*, *, *, *, *, T>.component6() = param.args[5] as T
inline operator fun <reified T> Arg7<*, *, *, *, *, *, T>.component7() = param.args[6] as T
inline operator fun <reified T> Arg8<*, *, *, *, *, *, *, T>.component8() = param.args[7] as T
inline operator fun <reified T> Arg9<*, *, *, *, *, *, *, *, T>.component9() = param.args[8] as T

inline fun <reified T : Any> PatcherAPI.instead(name: String) = PatchContext { args, cb ->
    instead<T>(name, *args) { cb(it) }
}

inline fun <reified T : Any> PatcherAPI.after(name: String) = PatchContext { args, cb ->
    after<T>(name, *args) { cb(it) }
}

inline fun <reified T : Any> PatcherAPI.before(name: String) = PatchContext { args, cb ->
    before<T>(name, *args) { cb(it) }
}

/**
 * Adds a [PreHook] to a constructor of a class.
 * @param paramTypes parameters of the constructor. Useful for patching individual overloads
 * @param callback callback for the patch
 * @return The [Unpatch] object of the patch
 * @see [XC_MethodHook.beforeHookedMethod]
 */
inline fun <reified T> PatcherAPI.before(vararg paramTypes: Class<*>, crossinline callback: HookCallback<T>): Unpatch {
    return patch(T::class.java.getDeclaredConstructor(*paramTypes), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                callback(param.thisObject as T, param)
            } catch (th: Throwable) {
                logger.error("Exception while pre-hooking constructor of ${param.method.declaringClass}", th)
            }
        }
    })
}

/**
 * Adds a [PreHook] to a method of a class.
 * @param methodName name of the method to patch
 * @param paramTypes parameters of the method. Useful for patching individual overloads
 * @param callback callback for the patch
 * @return The [Unpatch] object of the patch
 * @see [XC_MethodHook.beforeHookedMethod]
 */
inline fun <reified T> PatcherAPI.before(methodName: String, vararg paramTypes: Class<*>, crossinline callback: HookCallback<T>): Unpatch {
    return patch(T::class.java.getDeclaredMethod(methodName, *paramTypes), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                callback(param.thisObject as T, param)
            } catch (th: Throwable) {
                logger.error("Exception while pre-hooking ${param.method.declaringClass.name}.${param.method.name}", th)
            }
        }
    })
}

/**
 * Adds a [Hook] to a constructor of a class.
 * @param paramTypes parameters of the constructor. Useful for patching individual overloads
 * @param callback callback for the patch
 * @return the [Unpatch] object of the patch
 * @see [XC_MethodHook.afterHookedMethod]
 */
inline fun <reified T> PatcherAPI.after(vararg paramTypes: Class<*>, crossinline callback: HookCallback<T>): Unpatch {
    return patch(T::class.java.getDeclaredConstructor(*paramTypes), object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                callback(param.thisObject as T, param)
            } catch (th: Throwable) {
                logger.error("Exception while hooking constructor of ${param.method.declaringClass}", th)
            }
        }
    })
}

/**
 * Adds a [Hook] to a method of a class.
 * @param methodName name of the method to patch
 * @param paramTypes parameters of the method. Useful for patching individual overloads
 * @param callback callback for the patch
 * @return the [Unpatch] object of the patch
 * @see [XC_MethodHook.afterHookedMethod]
 */
inline fun <reified T> PatcherAPI.after(methodName: String, vararg paramTypes: Class<*>, crossinline callback: HookCallback<T>): Unpatch {
    return patch(T::class.java.getDeclaredMethod(methodName, *paramTypes), object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                callback(param.thisObject as T, param)
            } catch (th: Throwable) {
                logger.error("Exception while hooking ${param.method.declaringClass.name}.${param.method.name}", th)
            }
        }
    })
}
