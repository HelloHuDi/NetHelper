package com.hd.nethelper.test

import android.content.Context
import android.util.Log
import com.hd.nethelper.NetWorkSpeedListener
import com.hd.nethelper.checkNetConnect
import com.hd.nethelper.notifyIsAvailable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by hd on 2018/8/21 .
 */
abstract class NetSpeedSampler {
    
    companion object {
        
        const val DOWN_LINK_SAMPLING = 0
        
        const val UP_LINK_SAMPLING = 1
        
        const val WHOLE_LINK_SAMPLING = 2
    }
    
    protected abstract fun sampling()
    
    protected val executor = Executors.newFixedThreadPool(2)
    
    protected val TAG = this.javaClass.simpleName
    
    protected lateinit var context:Context
    
    /**unit : kbps*/
    protected var up: Double = 0.0
    
    /**unit : kbps*/
    protected var down: Double = 0.0
    
    /** 采样模式*/
    protected var mode = WHOLE_LINK_SAMPLING
    
    private val mSamplingCounter = AtomicInteger()
    
    private lateinit var listener: NetWorkSpeedListener
    
    private val qualityControl = NetQualityControl()
    
    private fun startSampling(listener: NetWorkSpeedListener) {
        if (mSamplingCounter.getAndIncrement() == 0) {
            this@NetSpeedSampler.listener = listener
            sampling()
        }
    }
    
    fun startSampling(context: Context, listener: NetWorkSpeedListener) {
        if (checkNetConnect(context)) {
            this.context=context.applicationContext
            startSampling(listener)
        } else {
            notifyIsAvailable { it -> it.isAvailable(false) }
            Log.e(TAG, "No network")
        }
    }
    
    fun stopSampling() {
        if (mSamplingCounter.decrementAndGet() == 0) {
            reportCompleted()
        }
    }
    
    fun isSampling() = mSamplingCounter.get() != 0
    
    /**
     * 设置采样模式，根据需要可以只采样一种网速状况，默认全部采样
     *
     * [DOWN_LINK_SAMPLING]
     * [UP_LINK_SAMPLING]
     * [WHOLE_LINK_SAMPLING]
     * */
    fun setCustomTestMode(mode: Int) {
        if (mode != DOWN_LINK_SAMPLING && mode != UP_LINK_SAMPLING && mode != WHOLE_LINK_SAMPLING)
            throw IllegalArgumentException("please set correct mode")
        this@NetSpeedSampler.mode = mode
    }
    
    protected fun reportStep(step:Int) {
        listener.step(step)
    }
    
    protected fun reportUpLink() {
        if (mode == DOWN_LINK_SAMPLING) return
        listener.upLink(up, qualityControl.mapBandwidthQuality(up))
    }
    
    protected fun reportDownLink() {
        if (mode == UP_LINK_SAMPLING) return
        listener.downLink(down, qualityControl.mapBandwidthQuality(down))
    }
    
    protected fun reportFailed() {
        listener.error()
        mSamplingCounter.set(0)
    }
    
    protected fun reportCompleted() {
        listener.finished()
        mSamplingCounter.set(0)
    }
}
