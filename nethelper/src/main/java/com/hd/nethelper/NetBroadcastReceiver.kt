package com.hd.nethelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager


/**
 * Created by hd on 2018/8/18 .
 *
 */
open class NetBroadcastReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (null == context || null == intent) return
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val available = checkNetConnect(context)
            notifyIsAvailable { listener -> listener.isAvailable(available) }
        }
    }
}