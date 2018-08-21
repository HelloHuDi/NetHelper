package com.hd.nethelper.test.ping

import java.io.BufferedReader
import java.io.InputStreamReader


/**
 * Created by hd on 2018/8/21 .
 * ping 时长
 */
class NetPingTest(private val serverIpAddress: String, private val pingTryCount: Int,
                  private val listener: NetPingTestListener) : Thread() {
    
    interface NetPingTestListener {
        /**
         * @param finished 是否结束
         * @param instantRtt 实时时长, 单位 ms
         * @param avgRtt 最终时长 ，单位 ms
         * */
        fun reportPing(finished: Boolean, instantRtt: Double, avgRtt: Double)
    }
    
    /**实时 ms*/
    private var instantRtt = 0.0
    /**最终 ms*/
    private var avgRtt = 0.0
    private var finished = false
    
    private fun getAvgRtt(): Double {
        return avgRtt
    }
    
    private fun getInstantRtt(): Double {
        return instantRtt
    }
    
    private fun isFinished(): Boolean {
        return finished
    }
    
    override fun run() {
        super.run()
        try {
            finished = false
            val ps = ProcessBuilder("ping", "-c $pingTryCount", serverIpAddress)
            ps.redirectErrorStream(true)
            val pr = ps.start()
            val inStream = BufferedReader(InputStreamReader(pr.inputStream))
            var line: String
            while (true){
                line = inStream.readLine()
                if(line==null)break
                val res = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (line.contains("icmp_seq")) {
                    instantRtt = java.lang.Double.parseDouble(res[res.size - 2].replace("time=", ""))
                }
                val res2 = line.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (line.startsWith("rtt ")) {
                    avgRtt = java.lang.Double.parseDouble(res2[4])
                    break
                }
                report()
            }
            pr.waitFor()
            inStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finished = true
        report()
    }
    
    private fun report() {
        listener.reportPing(isFinished(), getInstantRtt(), getAvgRtt())
    }
}
