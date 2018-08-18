package com.hd.nethelper


/**
 * Created by hd on 2018/8/18 .
 *
 */
interface NetworkListener {
    
    /** 网络是否可用*/
    fun sate(available: Boolean)
}

interface NetWorkSpeedListener : NetworkListener {
    
    /** 上下行网速*/
    fun speed(up: Long, down: Long)
}