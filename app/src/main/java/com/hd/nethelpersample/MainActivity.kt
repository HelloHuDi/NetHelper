package com.hd.nethelpersample

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import com.hd.nethelper.*
import com.hd.nethelper.test.NetConnectionQuality
import com.hd.nethelper.test.active.NetSpeedActiveSampler
import com.hd.nethelper.test.ping.NetPingTest
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), NetworkListener {
    
    private var netAvailable=false
    
    override fun isAvailable(available: Boolean) {
        Toast.makeText(this, "网络可用 ：$available", Toast.LENGTH_SHORT).show()
        if (!available) openWifiSetting(this)
        netAvailable=available
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addObserver(this)
        netAvailable=checkNetConnect(this)
        printContent(
                "😀当前网络是否连接 ：" + netAvailable
                        + "\n😀当前网络是否使用的手机网络 ：" + checkNetConnectByType(this, ConnectivityManager.TYPE_MOBILE)
                        + "\n😀当前网络int类型 : " + getNetConnectType(this)
                        + "\n😀当前网络string类型 : " + getNetConnectTypeStr(this)
                        + "\n😀当前网络enum类型 : " + getNetConnectTypeInfo(this)
                        + "\n😀当前网络ip地址 : " + getNetConnectAddress(this)
                        + "\n😀当前设备所有wifi密码,设备需要root : " + getAllWifiPassword()
                        + "\n😀当前设备连接的wifi的密码,设备需要root : " + getConnectWifiPassword(this))
        
    }
    
    override fun onResume() {
        super.onResume()
        // active sampler
        if(netAvailable)
        setActiveSampler()
    }
    
    fun checkExternalNetwork(view: View) {
        val ip = etInputIp.text
        val ipStr = if (TextUtils.isEmpty(ip)) "www.baidu.com" else ip.toString()
        thread {
            val state = checkNetConnect(ipStr)
            printContent("\n😀ping $ipStr : $state")
        }
        
        //ping test
        NetPingTest("www.baidu.com", 6, object : NetPingTest.NetPingTestListener {
            
            override fun reportPing(finished: Boolean, instantRtt: Double, avgRtt: Double) {
                if (finished) {
                    printContent(String.format("\n😀ping结果，是否结束：%b , 实时时长 %f ms ,最终时长 %f ms", finished, instantRtt, avgRtt))
                }
            }
        }).start()
        
    }
    
    private fun setActiveSampler() {
        var downLink = 0.0
        var upLink = 0.0
        var uq=NetConnectionQuality.UNKNOWN
        var dq=NetConnectionQuality.UNKNOWN
        val sampler=NetSpeedActiveSampler(MainActivity@ this, object : NetWorkSpeedListener {
            override fun step(step: Int) {
                printContent("\n😀采样进度：$step")
            }
    
            override fun upLink(up: Double, upQuality: NetConnectionQuality) {
                upLink = up
                uq=upQuality
                setSpeedContent(up, downLink)
                setSpeedQualityContent(uq,dq)
            }
    
            override fun downLink(down: Double, downQuality: NetConnectionQuality) {
                downLink=down
                dq=downQuality
                setSpeedContent(upLink, down)
                setSpeedQualityContent(uq,dq)
            }
    
            override fun error() {
                printContent("\n😀采样失败")
            }
    
            override fun finished() {
                printContent("\n😀采样结束")
            }
        })
//        sampler.setCustomTestMode(NetSpeedSampler.UP_LINK_SAMPLING)
        sampler.startSampling()
    }
    
    @SuppressLint("SetTextI18n")
    private fun setSpeedContent(upLink: Double, downLink: Double) {
        runOnUiThread {
            tvNetSpeed.text = formatDouble(upLink) + "kbps/" + formatDouble(downLink) + "kbps"
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun setSpeedQualityContent(upQuality: NetConnectionQuality, downQuality: NetConnectionQuality) {
        runOnUiThread {
            tvNetState.text = "$upQuality/$downQuality"
        }
    }
    
    private fun formatDouble(link: Double): String {
        val df = java.text.NumberFormat.getNumberInstance()
        df.maximumFractionDigits = 2
        return df.format(link)
    }
    
    private val handler = Handler()
    
    private fun printContent(str: String) {
        runOnUiThread {
            tvContent.append(str)
            handler.post { svContent.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }
}
