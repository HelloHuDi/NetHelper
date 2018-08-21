package com.hd.nethelper

import com.hd.nethelper.test.NetConnectionQuality


/**
 * Created by hd on 2018/8/18 .
 *
 */
interface NetworkListener {
    
    /** 网络是否可用*/
    fun isAvailable(available: Boolean)
}

interface NetWorkSpeedListener : NetworkListener {
    
    /** 上下行网速及网络质量*/
    fun netSpeed(up: Double, upQuality: NetConnectionQuality, down: Double, downQuality: NetConnectionQuality)
}