package com.guardsquare.devicebindingexample.api

import com.guardsquare.devicebindingexample.DeviceBinding
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class RestApiClient {

    private val BASE_URL = "http://192.168.50.10:8282"
    private var client: OkHttpClient? = null
    private var deviceBinding: DeviceBinding? = null
    private var interceptor: DeviceBindingInterceptor? = null

    var retrofit: Retrofit? = null
    var apiService: ApiService? = null

    companion object {
        @Volatile
        private var INSTANCE: RestApiClient? = null

        fun getInstance(): RestApiClient {
            return INSTANCE ?: synchronized(this) {
                val instance = RestApiClient()
                INSTANCE = instance
                instance
            }
        }
    }

    fun setup(deviceBinding: DeviceBinding): Retrofit? {
        this.deviceBinding = deviceBinding
        this.interceptor = DeviceBindingInterceptor(this.deviceBinding!!)
        client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        apiService = retrofit!!.create(ApiService::class.java)
        return retrofit
    }

    fun addSessionToken(token:String){
        interceptor?.sessionToken = token
    }

}