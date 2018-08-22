package com.hd.nethelper.test.active

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import com.hd.nethelper.test.NetSpeedSampler
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.*


/**
 * Created by hd on 2018/8/21 .
 * 主动探测网速，需要使用流量，不适用于实时查询
 * 使用场景：联网行为前做预判
 * 测试流程：不会同步测试上下行网速，优先测试下载速度，
 * 进度(step)：DOWN_LINK_SAMPLING --> UP_LINK_SAMPLING  or WHOLE_LINK_SAMPLING
 */
class NetSpeedActiveSampler : NetSpeedSampler() {
    
    @SuppressLint("UseSparseArrays")
    override fun sampling() {
        var selfLat = 0.0
        var selfLon = 0.0
        val mapKey = HashMap<Int, String>()
        val mapValue = HashMap<Int, List<String>>()
        val api = RetrofitProvider.getInstance(context).api
        api.getLatitudeLongitude().map(Function<ResponseBody, Boolean> { responseBody ->
            val starTime = System.currentTimeMillis()
            val buffer = StringBuffer()
            responseBody.charStream().forEachLine { line ->
                buffer.append(line)
                if (buffer.toString().contains("isp=")) {
                    selfLat = java.lang.Double.parseDouble(buffer.toString().split("lat=\"".toRegex()).dropLastWhile
                    { it.isEmpty() }.toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].replace("\"", ""))
                    selfLon = java.lang.Double.parseDouble(buffer.toString().split("lon=\"".toRegex()).dropLastWhile
                    { it.isEmpty() }.toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].replace("\"", ""))
                }
            }
            Log.d(TAG, "打印经纬度：$selfLat===$selfLon" + "==耗时=" + (System.currentTimeMillis() - starTime) + "ms")
            return@Function true
        }).flatMap(Function<Boolean, Observable<ResponseBody>> {
            return@Function api.getClosestServer()
        }).map(Function<ResponseBody, String> { responseBody ->
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
            return@Function address
        }).map(Function<String, String> { address ->
            //download
            reportStep(DOWN_LINK_SAMPLING)
            val ss = address.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val fileURL = address.replace(ss[ss.size - 1], "")
            var downloadElapsedTime: Double
            var downloadedByte = 0
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
                            downloadedByte += len
                            endTime = System.currentTimeMillis()
                            downloadElapsedTime = (endTime - startTime) / 1000.0
                            instantDownloadRate = if (downloadedByte >= 0) { //实时
                                round((downloadedByte * 8 / (1000 * 1000) / downloadElapsedTime).toDouble(), 2)
                            } else {
                                0.0
                            }
                            down = getKbpsSpeed(instantDownloadRate)
                            Log.d(TAG, "实时速度：" + formatSpeed(instantDownloadRate) + " Mbps" + " = " + formatSpeed(down) + "kbps")
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
            val finalDownloadRate = downloadedByte * 8 / (1000 * 1000.0) / downloadElapsedTime
            down = getKbpsSpeed(finalDownloadRate)
            Log.d(TAG, "最终速度：" + formatSpeed(finalDownloadRate) + " Mbps" + " = " + formatSpeed(down) + "kbps")
            reportDownLink()
            return@Function address
        }).map(Function<String, Boolean> { address ->
            //upload
            reportStep(UP_LINK_SAMPLING)
    
           /* val url = URL(address)
            var uploadedKByte = 0
            var startTime = System.currentTimeMillis()
            */
            
            
            reportUpLink()
            return@Function true
        }).subscribeOn(Schedulers.io()).subscribe({
            reportCompleted()
        }, { throwable ->
            run {
                reportFailed()
                throwable.printStackTrace()
            }
        })
    }
    
    private fun formatSpeed(speed: Double): String {
        val df = java.text.NumberFormat.getNumberInstance()
        df.maximumFractionDigits = 2
        return df.format(speed)
    }
    
    private fun getKbpsSpeed(mbps: Double): Double {
        return mbps * 1024 / 8
    }
    
    private fun round(value: Double, places: Int): Double {
        if (places < 0) throw IllegalArgumentException()
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


