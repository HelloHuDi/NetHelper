@file:JvmName("NetObserver")
package com.hd.nethelper

import java.util.*


/**
 * Created by hd on 2018/8/18 .
 *
 */

private const val TAG_LISTENER = "net_observer_listener"

private val netWeakMap = WeakHashMap<String, NetworkListener>()

fun getNetWeakMap(): WeakHashMap<String, NetworkListener> {
    return netWeakMap
}

fun addObserver(listener: NetworkListener) {
    addObserver(TAG_LISTENER, listener)
}

fun addObserver(tag: String, listener: NetworkListener) {
    netWeakMap[tag] = listener
}

fun clearObserver(tag: String) {
    netWeakMap.remove(tag)
}

fun clearAllObserver() {
    netWeakMap.clear()
}

inline fun notifyIsAvailable(notify: (listener: NetworkListener) -> Unit) {
    val wakMap = getNetWeakMap()
    for (listener in wakMap.values) {
        if (null == listener) return
        notify(listener)
    }
}





