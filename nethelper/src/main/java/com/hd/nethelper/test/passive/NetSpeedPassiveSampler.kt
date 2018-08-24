package com.hd.nethelper.test.passive

import android.content.Context
import android.net.TrafficStats
import android.os.SystemClock
import android.util.Log
import com.hd.nethelper.NetWorkSpeedListener
import com.hd.nethelper.test.NetSpeedSampler
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers

/**
 * Created by hd on 2018/8/21 .
 * 被动探测网速，不需要使用流量，适用于实时查询
 * 使用场景：联网行为中做动态调整
 * 测试流程：同步测试上下行网速,  进度(step)：WHOLE_LINK_SAMPLING
 */
class NetSpeedPassiveSampler(context: Context, listener: NetWorkSpeedListener) : NetSpeedSampler(context, listener) {
    
    private var downPreviousBytes: Long = -1
    private var upPreviousBytes: Long = -1
    private var downLastTimeReading = SystemClock.elapsedRealtime()
    private var upLastTimeReading = SystemClock.elapsedRealtime()
    private val downManager = NetConnectionClassManager()
    private val upManager = NetConnectionClassManager()
    
    override fun sampling() {
        Observable.just(true).map(Function<Boolean, Boolean> {
            while (isSampling()) {
                //downLink
                if (mode == DOWN_LINK_SAMPLING || mode == WHOLE_LINK_SAMPLING) {
                    val downNewBytes = TrafficStats.getTotalRxBytes()
                    val downByteDiff = downNewBytes - downPreviousBytes
                    if (downPreviousBytes >= 0) {
                        synchronized(this) {
                            val curTimeReading = SystemClock.elapsedRealtime()
                            downManager.addBandwidth(downByteDiff, curTimeReading - downLastTimeReading) { bandwidth ->
                                down = bandwidth
                                reportDownLink()
                            }
                            downLastTimeReading = curTimeReading
                        }
                    }
                    downPreviousBytes = downNewBytes
                }
                //upLink
                if (mode == UP_LINK_SAMPLING || mode == WHOLE_LINK_SAMPLING) {
                    val upNewBytes = TrafficStats.getTotalTxBytes()
                    val upByteDiff = upNewBytes - upPreviousBytes
                    if (upPreviousBytes >= 0) {
                        synchronized(this) {
                            val curTimeReading = SystemClock.elapsedRealtime()
                            upManager.addBandwidth(upByteDiff, curTimeReading - upLastTimeReading) { bandwidth ->
                                up = bandwidth
                                reportUpLink()
                            }
                            upLastTimeReading = curTimeReading
                        }
                    }
                    upPreviousBytes = upNewBytes
                }
                //wait 1 seconds
                SystemClock.sleep(1000)
            }
            return@Function true
        }).subscribeOn(Schedulers.computation()).subscribe({
            Log.d(TAG, "sampler completed")
        }, { throwable ->
            run {
                reportFailed()
                stopSampling()
                throwable.printStackTrace()
            }
        })
    }
    
}
