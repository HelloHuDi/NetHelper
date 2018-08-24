package com.hd.nethelper.test.passive

import com.hd.nethelper.test.NetConnectionQuality
import com.hd.nethelper.test.NetQualityControl
import java.util.concurrent.atomic.AtomicReference


/**
 * Created by hd on 2018/8/23 .
 *
 */
class NetConnectionClassManager {
    
    internal val DEFAULT_SAMPLES_TO_QUALITY_CHANGE = 5.0
    
    private val BYTES_TO_BITS = 8
    /**
     * The factor used to calculate the current bandwidth
     * depending upon the previous calculated value for bandwidth.
     *
     *
     * The smaller this value is, the less responsive to new samples the moving average becomes.
     */
    private val DEFAULT_DECAY_CONSTANT = 0.05
    
    internal val DEFAULT_HYSTERESIS_PERCENT: Long = 20
    private val HYSTERESIS_TOP_MULTIPLIER = 100.0 / (100.0 - DEFAULT_HYSTERESIS_PERCENT)
    private val HYSTERESIS_BOTTOM_MULTIPLIER = (100.0 - DEFAULT_HYSTERESIS_PERCENT) / 100.0
    
    /** Current bandwidth of the user's connection depending upon the response.  */
    private val bandwidth = ExponentialGeometricAverage(DEFAULT_DECAY_CONSTANT)
    
    @Volatile
    private var mInitiateStateChange = false
    private val mCurrentBandwidthConnectionQuality = AtomicReference(NetConnectionQuality.UNKNOWN)
    private var mNextBandwidthConnectionQuality: AtomicReference<NetConnectionQuality>? = null
    private var mSampleCounter: Int = 0
    private val netQualityControl = NetQualityControl()
    
    /**
     * The lower bound for measured bandwidth in bits/ms. Readings
     * lower than this are treated as effectively zero (therefore ignored).
     */
    internal val BANDWIDTH_LOWER_BOUND: Long = 10
    
    /**
     * Adds bandwidth to the current filtered latency counter. Sends a broadcast to all
     * if the counter moves from one bucket
     * to another (i.e. poor bandwidth -> moderate bandwidth).
     */
    @Synchronized fun addBandwidth(bytes: Long, timeInMs: Long,notify: (bandwidth:Double) -> Unit) {
        //Ignore garbage values.
        val bandwidth = bytes * 1.0 / timeInMs * BYTES_TO_BITS
        if (timeInMs == 0L || bandwidth < BANDWIDTH_LOWER_BOUND) {
            return
        }
        //bit/s>10
        this.bandwidth.addMeasurement(bandwidth)
        if (mInitiateStateChange) {
            mSampleCounter += 1
            if (getCurrentBandwidthQuality() !== mNextBandwidthConnectionQuality!!.get()) {
                mInitiateStateChange = false
                mSampleCounter = 1
            }
            if (mSampleCounter >= DEFAULT_SAMPLES_TO_QUALITY_CHANGE && significantlyOutsideCurrentBand()) {
                mInitiateStateChange = false
                mSampleCounter = 1
                mCurrentBandwidthConnectionQuality.set(mNextBandwidthConnectionQuality!!.get())
                notify(getKBitsPerSecond())
            }
            return
        }
        if (mCurrentBandwidthConnectionQuality.get() !== getCurrentBandwidthQuality()) {
            mInitiateStateChange = true
            mNextBandwidthConnectionQuality = AtomicReference(getCurrentBandwidthQuality())
        }
    }
    
    private fun significantlyOutsideCurrentBand(): Boolean {
        val currentQuality = mCurrentBandwidthConnectionQuality.get()
        val bottomOfBand: Double
        val topOfBand: Double
        when (currentQuality) {
            NetConnectionQuality.POOR -> {
                bottomOfBand = 0.0
                topOfBand = NetQualityControl.DEFAULT_POOR_BANDWIDTH.toDouble()
            }
            NetConnectionQuality.MODERATE -> {
                bottomOfBand = NetQualityControl.DEFAULT_POOR_BANDWIDTH.toDouble()
                topOfBand = NetQualityControl.DEFAULT_MODERATE_BANDWIDTH.toDouble()
            }
            NetConnectionQuality.GOOD -> {
                bottomOfBand = NetQualityControl.DEFAULT_MODERATE_BANDWIDTH.toDouble()
                topOfBand = NetQualityControl.DEFAULT_GOOD_BANDWIDTH.toDouble()
            }
            NetConnectionQuality.EXCELLENT -> {
                bottomOfBand = NetQualityControl.DEFAULT_GOOD_BANDWIDTH.toDouble()
                topOfBand = java.lang.Float.MAX_VALUE.toDouble()
            }
            else // If current quality is UNKNOWN, then changing is always valid.
            -> return true
        }
        val average = bandwidth.average
        if (average > topOfBand) {
            if (average > topOfBand * HYSTERESIS_TOP_MULTIPLIER) {
                return true
            }
        } else if (average < bottomOfBand * HYSTERESIS_BOTTOM_MULTIPLIER) {
            return true
        }
        return false
    }
    
    /**
     * Resets the bandwidth average for this instance of the bandwidth manager.
     */
    fun reset() {
        bandwidth.reset()
        mCurrentBandwidthConnectionQuality.set(NetConnectionQuality.UNKNOWN)
    }
    
    /**
     * Get the ConnectionQuality that the moving bandwidth average currently represents.
     *
     * @return A ConnectionQuality representing the device's bandwidth at this exact moment.
     */
    @Synchronized
    fun getCurrentBandwidthQuality(): NetConnectionQuality {
        return netQualityControl.mapBandwidthQuality(bandwidth.average)
    }
    
    /**
     * Accessor method for the current bandwidth average.
     *
     * @return The current bandwidth average, or -1 if no average has been recorded.
     */
    @Synchronized
    fun getKBitsPerSecond(): Double {
        return bandwidth.average
    }
}