package com.hd.nethelpersample

import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.hd.nethelper.*
import com.hd.nethelper.test.ping.NetPingTest
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), NetworkListener {
    
    override fun isAvailable(available: Boolean) {
        Toast.makeText(this, "网络可用 ：$available",Toast.LENGTH_SHORT).show()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addObserver(this)
        printContent(
                "😀当前网络是否连接 ：" + checkNetConnect(this)
                        + "\n😀当前网络是否使用的手机网络 ：" + checkNetConnectByType(this, ConnectivityManager.TYPE_MOBILE)
                        + "\n😀当前网络int类型 : " + getNetConnectType(this)
                        + "\n😀当前网络string类型 : " + getNetConnectTypeStr(this)
                        + "\n😀当前网络enum类型 : " + getNetConnectTypeInfo(this)
                        + "\n😀当前网络ip地址 : " + getNetConnectAddress(this)
                        + "\n😀当前设备所有wifi密码,设备需要root : " + getAllWifiPassword()
                        + "\n😀当前设备连接的wifi的密码,设备需要root : " + getConnectWifiPassword(this))
    }
    
    fun checkExternalNetwork(view: View) {
        val ip = etInputIp.text
        val ipStr = if (TextUtils.isEmpty(ip)) "www.baidu.com" else ip.toString()
        thread {
            val state = checkNetConnect(ipStr)
            runOnUiThread { printContent("\n😀ping $ipStr : $state") }
        }
        
        //ping test
        NetPingTest("www.baidu.com", 6, object : NetPingTest.NetPingTestListener {
            
            override fun reportPing(finished: Boolean, instantRtt: Double, avgRtt: Double) {
                Log.d("hd", String.format("ping结果，是否结束：%b , 实时时长 %f ms ,最终时长 %f ms", finished, instantRtt, avgRtt))
            }
        }).start()
    }
    
    private fun printContent(str: String) {
        tvContent.append(str)
    }
}
