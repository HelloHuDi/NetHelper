@file:JvmName("NetHelper")

package com.hd.nethelper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import android.util.Log
import com.stealthcopter.networktools.IPTools
import com.stealthcopter.networktools.Ping
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.regex.Pattern


/**
 * Created by hd on 2018/8/10 .
 * network helper
 */
const val NET_TAG = "NetworkStatusExample"

fun openWifiSetting(context: Context) {
    context.applicationContext.startActivity(
            Intent(Settings.ACTION_WIFI_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun openNetSetting(context: Context) {
    context.applicationContext.startActivity(
            Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun setWifiEnabled(context: Context, enable: Boolean) {
    getWifiManager(context)?.isWifiEnabled = enable
}

fun getWifiManager(context: Context): WifiManager? {
    return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
}

fun getNetworkManager(context: Context?): ConnectivityManager? {
    if (null == context) return null
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.INTERNET) ==
            PackageManager.PERMISSION_GRANTED) {
        return context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
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
        val manager = getNetworkManager(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val info = manager?.getNetworkInfo(manager.activeNetwork)
            return if (info?.type == type) info else null
        } else {
            manager?.getNetworkInfo(type)
        }
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
        Ping.onAddress(uri).setTimeOutMillis(2000).setTimes(5).doPing().isReachable
    } catch (e: Exception) {
        e.printStackTrace()
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

/** 检查网络连接类型详情
 * [NetworkType]
 * */
fun getNetConnectTypeInfo(context: Context): NetworkType {
    var netType = NetworkType.NETWORK_NO
    val info = getNetworkInfo(context)
    if (null != info && info.isConnected && info.isAvailable) {
        when (info.type) {
            ConnectivityManager.TYPE_ETHERNET ->
                netType = NetworkType.NETWORK_ETHERNET
            ConnectivityManager.TYPE_WIFI ->
                netType = NetworkType.NETWORK_WIFI
            ConnectivityManager.TYPE_MOBILE ->
                when (info.subtype) {
                    TelephonyManager.NETWORK_TYPE_GSM,
                    TelephonyManager.NETWORK_TYPE_GPRS,
                    TelephonyManager.NETWORK_TYPE_CDMA,
                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_1xRTT,
                    TelephonyManager.NETWORK_TYPE_IDEN
                    -> netType = NetworkType.NETWORK_2G
                    TelephonyManager.NETWORK_TYPE_TD_SCDMA,
                    TelephonyManager.NETWORK_TYPE_EVDO_A,
                    TelephonyManager.NETWORK_TYPE_UMTS,
                    TelephonyManager.NETWORK_TYPE_EVDO_0,
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_EVDO_B,
                    TelephonyManager.NETWORK_TYPE_EHRPD,
                    TelephonyManager.NETWORK_TYPE_HSPAP
                    -> netType = NetworkType.NETWORK_3G
                    TelephonyManager.NETWORK_TYPE_IWLAN,
                    TelephonyManager.NETWORK_TYPE_LTE
                    -> netType = NetworkType.NETWORK_4G
                    else -> {
                        val subtypeName = info.subtypeName
                        netType = if (subtypeName.equals("TD-SCDMA", ignoreCase = true)
                                || subtypeName.equals("WCDMA", ignoreCase = true)
                                || subtypeName.equals("CDMA2000", ignoreCase = true)) {
                            NetworkType.NETWORK_3G
                        } else {
                            NetworkType.NETWORK_UNKNOWN
                        }
                    }
                }
            else -> netType = NetworkType.NETWORK_UNKNOWN
        }
    }
    return netType
}

/** 获取连接网络ipv4地址*/
fun getNetConnectAddress(context: Context): String {
    val info = getNetworkInfo(context)
    return if (null != info && info.isConnected) {
        IPTools.getLocalIPv4Address().hostAddress.toString()
    } else {
        ""
    }
}

/**获取当前正在使用的Wifi的密码
 * 需要root权限
 * */
fun getConnectWifiPassword(context: Context): String {
    val password = ""
    val passwordMap = getAllWifiPassword()
    if (passwordMap.isEmpty()) return password
    val wifiManager = getWifiManager(context)
    val wifiInfo = wifiManager?.connectionInfo
    if (null != wifiInfo) {
        for (pas in passwordMap) {
            var name = wifiInfo.ssid
            name = if (name.startsWith("\"") and name.endsWith("\""))
                name.substring(1, name.length - 1) else name
            if (pas.key == name)
                return pas.value
        }
    }
    return password
}

/**获取所有有连接并保存记录的Wifi密码
 * 需要root权限
 * */
fun getAllWifiPassword(): Map<String, String> {
    val wifiParMap = hashMapOf<String, String>()
    var process: Process? = null
    var dataOutputStream: DataOutputStream? = null
    var dataInputStream: DataInputStream? = null
    val wifiConf = StringBuffer()
    try {
        process = Runtime.getRuntime().exec("su")
        if (null == process) {
            return wifiParMap
        }
        dataOutputStream = DataOutputStream(process.outputStream)
        dataInputStream = DataInputStream(process.inputStream)
        dataOutputStream.writeBytes("cat /data/misc/wifi/*.conf\n")
        dataOutputStream.writeBytes("exit\n")
        dataOutputStream.flush()
        val inputStreamReader = InputStreamReader(dataInputStream, "UTF-8")
        val bufferedReader = BufferedReader(inputStreamReader)
        var line: String?
        do {
            line = bufferedReader.readLine()
            if (line != null)
                wifiConf.append(line)
        } while (line != null)
        bufferedReader.close()
        inputStreamReader.close()
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            dataOutputStream?.close()
            dataInputStream?.close()
            process?.destroy()
        } catch (e: Exception) {
            throw e
        }
    }
    val network = Pattern.compile("network=\\{([^\\}]+)\\}", Pattern.DOTALL)
    val networkMatcher = network.matcher(wifiConf.toString())
    while (networkMatcher.find()) {
        val networkBlock = networkMatcher.group()
        val ssid = Pattern.compile("ssid=\"([^\"]+)\"")
        val ssidMatcher = ssid.matcher(networkBlock)
        var password: String
        var sid: String
        if (ssidMatcher.find()) {
            sid = ssidMatcher.group(1)
            val psk = Pattern.compile("psk=\"([^\"]+)\"")
            val pskMatcher = psk.matcher(networkBlock)
            password = if (pskMatcher.find()) {
                pskMatcher.group(1)
            } else {
                ""
            }
            wifiParMap[sid] = password
        }
    }
    return wifiParMap
}




