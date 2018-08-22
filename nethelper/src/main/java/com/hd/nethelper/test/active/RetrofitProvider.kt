package com.hd.nethelper.test.active

import android.annotation.SuppressLint
import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit


class RetrofitProvider private constructor() {
    private var retrofit: Retrofit? = null
    
    lateinit var api: Api
    
    init {
        if (retrofit == null) {
            retrofit = RetrofitProvider.get()
            api = retrofit!!.create(Api::class.java)
        }
    }
    
    private object RetrofitProviderHolder {
        
        @SuppressLint("StaticFieldLeak")
        internal val instance = RetrofitProvider()
    }
    
    companion object {
        
        private val ENDPOINT = "http://www.speedtest.net"
        
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context
        
        private fun get(): Retrofit {
            val builder = OkHttpClient().newBuilder()
            val cacheHttp = File(context.cacheDir, "responses")
            val cacheSize = 100 * 1024 * 1024
            val cache = Cache(cacheHttp, cacheSize.toLong())
            builder.readTimeout(15, TimeUnit.SECONDS)
            builder.connectTimeout(15, TimeUnit.SECONDS)
            builder.cache(cache)
            return Retrofit.Builder().baseUrl(ENDPOINT)
                    .client(builder.build())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
        }
        
        fun getInstance(context: Context): RetrofitProvider {
            this.context=context
            return RetrofitProviderHolder.instance
        }
    }
}
