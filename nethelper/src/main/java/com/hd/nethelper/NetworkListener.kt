package com.hd.nethelper

import com.hd.nethelper.test.NetConnectionQuality
import com.hd.nethelper.test.NetSpeedSampler.Companion.DOWN_LINK_SAMPLING
import com.hd.nethelper.test.NetSpeedSampler.Companion.UP_LINK_SAMPLING
import com.hd.nethelper.test.NetSpeedSampler.Companion.WHOLE_LINK_SAMPLING


/**
 * Created by hd on 2018/8/18 .
 *
 */
interface NetworkListener {
    
    /** 网络是否可用*/
    fun isAvailable(available: Boolean)
}

interface NetWorkSpeedListener {
    
    /**
     * @param step 报告当前进行到哪一步，（正在进行上行网速采样 或 正在进行下行网速采样 或 同时进行中）
     * [DOWN_LINK_SAMPLING]
     * [UP_LINK_SAMPLING]
     * [WHOLE_LINK_SAMPLING]
     * */
    fun step(step: Int)
    
    /**上行网*/
    fun upLink(up: Double, upQuality: NetConnectionQuality)
    
    /**下行网*/
    fun downLink(down: Double, downQuality: NetConnectionQuality)
    
    /**采样失败*/
    fun error()
    
    /**采样结束*/
    fun finished()
}