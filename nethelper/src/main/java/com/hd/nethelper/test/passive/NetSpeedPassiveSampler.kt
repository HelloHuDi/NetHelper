package com.hd.nethelper.test.passive

import android.content.Context
import com.hd.nethelper.NetWorkSpeedListener
import com.hd.nethelper.test.NetSpeedSampler

/**
 * Created by hd on 2018/8/21 .
 * 被动探测网速，不需要使用流量，适用于实时查询
 * 使用场景：联网行为中做动态调整
 * 测试流程：同步测试上下行网速,  进度(step)：WHOLE_LINK_SAMPLING
 */
class NetSpeedPassiveSampler(context: Context, listener: NetWorkSpeedListener) : NetSpeedSampler(context, listener){
    
    override fun sampling() {
    
    }
    
}
