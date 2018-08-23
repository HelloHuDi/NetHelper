package com.hd.nethelper.test.active

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.hd.nethelper.NetWorkSpeedListener
import com.hd.nethelper.test.NetSpeedSampler
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import java.io.DataOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


/**
 * Created by hd on 2018/8/21 .
 * 主动探测网速，需要使用流量，不适用于实时查询
 * 使用场景：联网行为前做预判
 * 测试流程：不会同步测试上下行网速，优先测试下载速度，
 * 进度(step)：DOWN_LINK_SAMPLING --> UP_LINK_SAMPLING  or WHOLE_LINK_SAMPLING
 * 注意，借用第三方测速平台，有可能会出现不稳定或者访问失败等情况
 */
open class NetSpeedActiveSampler(context: Context, listener: NetWorkSpeedListener) : NetSpeedSampler(context, listener) {
    
    override fun sampling() {
        var selfLat = 0.0
        var selfLon = 0.0
        val api = RetrofitProvider.getInstance(context.applicationContext).api
        api.getLatitudeLongitude().map(Function<ResponseBody, Boolean> { responseBody ->
            val triple = getLocation(responseBody, selfLat, selfLon)
            val starTime = triple.first
            selfLat = triple.second
            selfLon = triple.third
            Log.d(TAG, "print location ：$selfLat===$selfLon" + "==duration=" + (System.currentTimeMillis() - starTime) + "ms")
            return@Function true
        }).flatMap(Function<Boolean, Observable<ResponseBody>> {
            return@Function api.getClosestServer()
        }).map(Function<ResponseBody, String> { responseBody ->
            return@Function getClosestServer(responseBody, selfLat, selfLon)
        }).map(Function<String, String> { address ->
            if (mode == DOWN_LINK_SAMPLING || mode == WHOLE_LINK_SAMPLING)
                downLinkSampler(address)
            return@Function address
        }).map(Function<String, Boolean> { address ->
            if (mode == UP_LINK_SAMPLING || mode == WHOLE_LINK_SAMPLING)
                upLinkSampler(address)
            return@Function true
        }).subscribeOn(Schedulers.computation()).subscribe({
            reportCompleted()
            stopSampling()
        }, { throwable ->
            run {
                reportFailed()
                stopSampling()
                throwable.printStackTrace()
            }
        })
    }
    
    private fun getLocation(responseBody: ResponseBody, selfLat: Double, selfLon: Double): Triple<Long, Double, Double> {
        var selfLat1 = selfLat
        var selfLon1 = selfLon
        val starTime = System.currentTimeMillis()
        val buffer = StringBuffer()
        responseBody.charStream().forEachLine { line ->
            buffer.append(line)
            if (buffer.toString().contains("isp=")) {
                selfLat1 = java.lang.Double.parseDouble(buffer.toString().split("lat=\"".toRegex()).dropLastWhile
                { it.isEmpty() }.toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].replace("\"", ""))
                selfLon1 = java.lang.Double.parseDouble(buffer.toString().split("lon=\"".toRegex()).dropLastWhile
                { it.isEmpty() }.toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].replace("\"", ""))
            }
        }
        return Triple(starTime, selfLat1, selfLon1)
    }
    
    @SuppressLint("UseSparseArrays")
    private fun getClosestServer(responseBody: ResponseBody, selfLat: Double, selfLon: Double): String? {
        val mapKey = HashMap<Int, String>()
        val mapValue = HashMap<Int, List<String>>()
        var uploadAddress: String
        var name: String
        var country: String
        var cc: String
        var sponsor: String
        var lat: String
        var lon: String
        var host: String
        //Best server
        var count = 0
        responseBody.charStream().forEachLine { line ->
            if (line.contains("<server url")) {
                uploadAddress = line.split("server url=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                lat = line.split("lat=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                lon = line.split("lon=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                name = line.split("name=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                country = line.split("country=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                cc = line.split("cc=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                sponsor = line.split("sponsor=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                host = line.split("host=\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                val ls = Arrays.asList<String>(lat, lon, name, country, cc, sponsor, host)
                mapKey[count] = uploadAddress
                mapValue[count] = ls
                count++
            }
        }
        var tmp = 19349458.0
        var dist = 0.0
        var findServerIndex = 0
        for (index in mapKey.keys) {
            val source = Location("Source")
            source.latitude = selfLat
            source.longitude = selfLon
            val ls = mapValue[index]
            val dest = Location("Dest")
            dest.latitude = java.lang.Double.parseDouble(ls!![0])
            dest.longitude = java.lang.Double.parseDouble(ls[1])
            val distance = source.distanceTo(dest).toDouble()
            if (tmp > distance) {
                tmp = distance
                dist = distance
                findServerIndex = index
            }
        }
        val info = mapValue[findServerIndex]
        val distance = dist
        Log.d(TAG, String.format("Hosted by %s (%s) [%s km]", info!![5], info[3], DecimalFormat("#.##").format(distance / 1000)))
        val address = mapKey[findServerIndex]
        Log.d(TAG, "address : $address")
        return address
    }
    
    private fun downLinkSampler(address: String) {
        //download
        reportStep(DOWN_LINK_SAMPLING)
        val ss = address.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val fileURL = address.replace(ss[ss.size - 1], "")
        var downloadElapsedTime: Double
        val downloadedByte = AtomicInteger()
        var instantDownloadRate: Double
        val fileUrls = ArrayList<String>()
        fileUrls.add(fileURL + "random4000x4000.jpg")
        fileUrls.add(fileURL + "random3000x3000.jpg")
        val startTime = System.currentTimeMillis()
        var endTime: Long
        var httpConn: HttpURLConnection? = null
        var responseCode: Int = -1
        var url: URL
        outer@ for (link in fileUrls) {
            try {
                url = URL(link)
                httpConn = url.openConnection() as HttpURLConnection
                responseCode = httpConn.responseCode
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            try {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val buffer = ByteArray(10240)
                    val inputStream = httpConn?.inputStream
                    var len: Int
                    while (inputStream != null) {
                        len = inputStream.read(buffer)
                        if (len == -1) break
                        downloadedByte.addAndGet(len)
                        endTime = System.currentTimeMillis()
                        downloadElapsedTime = (endTime - startTime) / 1000.0
                        //实时
                        instantDownloadRate = if (downloadedByte.get() >= 0) {
                            round((downloadedByte.get() * 8 / (1000 * 1000) / downloadElapsedTime), 2)
                        } else {
                            0.0
                        }
                        down = getKbpsSpeed(instantDownloadRate)
                        reportDownLink()
                        if (downloadElapsedTime >= 15) {//超时
                            break@outer
                        }
                    }
                    inputStream?.close()
                    httpConn?.disconnect()
                } else {
                    Log.e(TAG, "Link not found...")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        endTime = System.currentTimeMillis()
        downloadElapsedTime = (endTime - startTime) / 1000.0
        val finalDownloadRate = downloadedByte.get() * 8 / (1000 * 1000.0) / downloadElapsedTime
        down = getKbpsSpeed(finalDownloadRate)
        Log.d(TAG, "the last downLink speed：" + formatSpeed(finalDownloadRate) + "Mbps" + " = " + formatSpeed(down) + "kbps")
        reportDownLink()
    }
    
    private fun upLinkSampler(address: String) {
        //upload
        reportStep(UP_LINK_SAMPLING)
        val url = URL(address)
        var uploadElapsedTime: Double
        val uploadedKByte = AtomicInteger()
        val startTimes = System.currentTimeMillis()
        var instantUploadRate: Double
        var count = 4
        while (count > 0) {
            count--
            executor.submit {
                val buffer = ByteArray(150 * 1024)
                while (true) {
                    var conn: HttpURLConnection? = null
                    var dos: DataOutputStream? = null
                    try {
                        conn = url.openConnection() as HttpURLConnection
                        conn.doOutput = true
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Connection", "Keep-Alive")
                        dos = DataOutputStream(conn.outputStream)
                        dos.write(buffer, 0, buffer.size)
                        dos.flush()
                        uploadedKByte.addAndGet(buffer.size / 1024)
                        //实时
                        uploadElapsedTime = (System.currentTimeMillis() - startTimes) / 1000.0
                        instantUploadRate = if (uploadedKByte.get() >= 0) {
                            round((uploadedKByte.get() / 1000.0 * 8 / uploadElapsedTime), 2)
                        } else {
                            0.0
                        }
                        up = getKbpsSpeed(instantUploadRate)
                        reportUpLink()
                        if (uploadElapsedTime >= 15) {
                            break
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    } finally {
                        dos?.close()
                        conn?.disconnect()
                    }
                }
            }
        }
        executor.shutdown()
        while (!executor.isTerminated) {
            SystemClock.sleep(100)
        }
        uploadElapsedTime = (System.currentTimeMillis() - startTimes) / 1000.0
        val finalUploadRate = (uploadedKByte.get() / 1000.0 * 8 / uploadElapsedTime)
        up = getKbpsSpeed(finalUploadRate)
        Log.d(TAG, "the last upLink speed：" + formatSpeed(finalUploadRate) + "Mbps" + " = " + formatSpeed(up) + "kbps")
        reportUpLink()
    }
    
    private val executor = Executors.newFixedThreadPool(4)
    
    private fun formatSpeed(speed: Double): String {
        val df = java.text.NumberFormat.getNumberInstance()
        df.maximumFractionDigits = 2
        return df.format(speed)
    }
    
    private fun getKbpsSpeed(mbps: Double): Double {
        return mbps * 1024 / 8
    }
    
    private fun round(value: Double, places: Int): Double {
        if (places < 0) return value
        var bd: BigDecimal
        try {
            bd = BigDecimal(value)
        } catch (ex: Exception) {
            return 0.0
        }
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }
}


