package com.hd.nethelper.test


/**
 * Created by hd on 2018/8/21 .
 *
 */

class NetQualityControl {
    
    companion object {
        val DEFAULT_SAMPLES_TO_QUALITY_CHANGE = 5.0
        val BYTES_TO_BITS = 8
        /**
         * Default values for determining quality of data connection.
         * Bandwidth numbers are in Kilobits per second (kbps).
         */
        var DEFAULT_UNKNOWN_BANDWIDTH = -1
        var DEFAULT_POOR_BANDWIDTH = 150
        var DEFAULT_MODERATE_BANDWIDTH = 550
        var DEFAULT_GOOD_BANDWIDTH = 2000
        const val DEFAULT_HYSTERESIS_PERCENT: Long = 20
        const val HYSTERESIS_TOP_MULTIPLIER = 100.0 / (100.0 - DEFAULT_HYSTERESIS_PERCENT)
        const val HYSTERESIS_BOTTOM_MULTIPLIER = (100.0 - DEFAULT_HYSTERESIS_PERCENT) / 100.0
    }
    
    fun reset() {
        setCustomQualityStandard(-1, 150, 550, 2000)
    }
    
    fun mapBandwidthQuality(average: Double): NetConnectionQuality {
        return when {
            average <= DEFAULT_UNKNOWN_BANDWIDTH -> NetConnectionQuality.UNKNOWN
            average < DEFAULT_POOR_BANDWIDTH -> NetConnectionQuality.POOR
            average < DEFAULT_MODERATE_BANDWIDTH -> NetConnectionQuality.MODERATE
            average < DEFAULT_GOOD_BANDWIDTH -> NetConnectionQuality.GOOD
            else -> NetConnectionQuality.EXCELLENT
        }
    }
    
    fun setCustomQualityStandard(unknown: Int, poor: Int, moderate: Int, good: Int) {
        DEFAULT_UNKNOWN_BANDWIDTH = unknown
        DEFAULT_POOR_BANDWIDTH = poor
        DEFAULT_MODERATE_BANDWIDTH = moderate
        DEFAULT_GOOD_BANDWIDTH = good
    }
}