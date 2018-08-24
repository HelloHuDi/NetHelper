/*
 *  Copyright (c) 2015, Facebook, Inc.
 *  All rights reserved.
 *
 *  This source code is licensed under the BSD-style license found in the
 *  LICENSE file in the root directory of this source tree. An additional grant
 *  of patent rights can be found in the PATENTS file in the same directory.
 *
 */

package com.hd.nethelper.test.passive

/**
 * Moving average calculation for ConnectionClass.
 */
internal class ExponentialGeometricAverage(private val mDecayConstant: Double) {
    
    private val mCutover: Int = if (mDecayConstant == 0.0) Integer.MAX_VALUE else Math.ceil(1 / mDecayConstant).toInt()
    
    var average = -1.0
        private set
    private var mCount: Int = 0
    
    /**
     * Adds a new measurement to the moving average.
     *
     * @param measurement - Bandwidth measurement in bits/ms to add to the moving average.
     */
    fun addMeasurement(measurement: Double) {
        val keepConstant = 1 - mDecayConstant
        average = when {
            mCount > mCutover -> Math.exp(keepConstant * Math.log(average) + mDecayConstant * Math.log(measurement))
            mCount > 0 -> {
                val retained = keepConstant * mCount / (mCount + 1.0)
                val newcomer = 1.0 - retained
                Math.exp(retained * Math.log(average) + newcomer * Math.log(measurement))
            }
            else -> measurement
        }
        mCount++
    }

    /**
     * Reset the moving average.
     */
    fun reset() {
        average = -1.0
        mCount = 0
    }
}
