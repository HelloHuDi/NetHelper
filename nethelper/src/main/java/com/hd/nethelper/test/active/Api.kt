package com.hd.nethelper.test.active

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET


/**
 * Created by hd on 2018/8/22 .
 *
 */
interface Api{
    
    @GET("speedtest-config.php")
    fun getLatitudeLongitude(): Observable<ResponseBody>
    
    @GET("speedtest-servers-static.php")
    fun getClosestServer(): Observable<ResponseBody>
}