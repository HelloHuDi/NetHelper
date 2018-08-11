package com.hd.nethelpersample

import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.hd.nethelper.checkNetConnect
import com.hd.nethelper.checkNetConnectByType
import com.hd.nethelper.getNetConnectType
import com.hd.nethelper.getNetConnectTypeStr
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        printContent(""+ checkNetConnect(this)+"=="+//
                checkNetConnectByType(this,ConnectivityManager.TYPE_MOBILE)+"=="+//
                getNetConnectType(this)+"==="+getNetConnectTypeStr(this))
    }

    fun checkExternalNetwork(view:View){
        thread{
            val state =checkNetConnect("www.baidu.com")
            runOnUiThread{ printContent("连接www.baidu.com $state")}
        }

    }

    private fun printContent(str:String){
        tvContent.text = ""
        tvContent.text = str
    }
}
