package com.hd.nethelper

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v4.content.ContextCompat
import android.util.Log

/**
 * Created by hd on 2018/8/10 .
 * network helper
 * </p>
 * need permission :
 * [android.Manifest.permission.INTERNET]
 * [android.Manifest.permission.ACCESS_NETWORK_STATE]
 */

const val NET_TAG = "NetworkStatusExample"

fun getNetworkManager(context: Context): ConnectivityManager? {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.INTERNET) ==
            PackageManager.PERMISSION_GRANTED) {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    }
    Log.e(NET_TAG, "android.permission.INTERNET is denied")
    return null
}

/** return null if no net */
fun getNetworkInfo(context: Context): NetworkInfo? {
    return getNetworkManager(context)?.activeNetworkInfo
}

fun getNetworkInfo(context: Context, type: Int): NetworkInfo? {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_NETWORK_STATE) ==
            PackageManager.PERMISSION_GRANTED) {
        return getNetworkManager(context)?.getNetworkInfo(type)
    }
    Log.e(NET_TAG, "android.permission.ACCESS_NETWORK_STATE is denied")
    return null
}

/** 检查指定网络是否连接
 * @param [getNetConnectType()]
 * */
fun checkNetConnectByType(context: Context, type: Int): Boolean {
    return getNetworkInfo(context, type)?.isConnected ?: false
}

/** 检查网络是否连接*/
fun checkNetConnect(context: Context): Boolean {
    return getNetworkInfo(context)?.isConnected ?: false
}

/** 根据指定网络地址检查网络是否连接，可用于检查内外网，例如：www.baidu.com
 *  注意需要在线程下调用
 * */
fun checkNetConnect(uri: String): Boolean {
    return try {
        val exec = Runtime.getRuntime().exec("ping -c 2 -W 2 $uri")
        exec.waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

/** 检查网络连接类型
 * [ConnectivityManager.TYPE_MOBILE]
 * [ConnectivityManager.TYPE_WIFI]
 * [ConnectivityManager.TYPE_WIMAX]
 * [ConnectivityManager.TYPE_ETHERNET]
 * [ConnectivityManager.TYPE_BLUETOOTH]
 * */
fun getNetConnectType(context: Context): Int {
    return getNetworkInfo(context)?.type ?: -1
}

/**
 * 检查网络连接类型名称
 * return "WIFI" or "MOBILE".
 * */
fun getNetConnectTypeStr(context: Context): String {
    return getNetworkInfo(context)?.typeName ?: context.resources.getString(R.string.net_none)
}

/** 获取连接网络ip地址*/
fun getNetConnectAddress(context: Context): String {
    return "00:00:00:00"
}
