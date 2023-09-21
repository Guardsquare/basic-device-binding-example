package com.guardsquare.devicebindingexample.api

import com.guardsquare.devicebindingexample.model.BalanceResponse
import com.guardsquare.devicebindingexample.model.LoginRequest
import com.guardsquare.devicebindingexample.model.LoginResponse
import com.guardsquare.devicebindingexample.model.TransferRequest
import com.guardsquare.devicebindingexample.model.TransferResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiService {
    // This should be a 2 step login process
    @POST("loginWithHandshake")
    fun loginWithHandshake(@Body request: LoginRequest): Call<LoginResponse>

    // This should be used to log in after the handshake
    @POST("loginWithoutHandshake")
    fun loginWithoutHandshake(@Body request: LoginRequest): Call<LoginResponse>

    @POST("getBalance")
    fun getBalance(): Call<BalanceResponse>

    @POST("transfer")
    fun transfer(@Body request: TransferRequest): Call<TransferResponse>
}