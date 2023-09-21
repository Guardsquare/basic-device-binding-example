package com.guardsquare.devicebindingexample.api

import com.guardsquare.devicebindingexample.DeviceBinding
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.util.Base64

class DeviceBindingInterceptor(val deviceBinding: DeviceBinding) : Interceptor {

    var sessionToken: String = ""

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalBody = originalRequest.body()
        val originalMediaType = originalBody?.contentType()

        if (originalRequest.method() == "POST") {
            val buffer = okio.Buffer()
            originalBody?.writeTo(buffer)
            val originalContent = buffer.readUtf8()
            val signedPayload = JSONObject().apply {
                put("body", originalContent)
                put("sessionToken", sessionToken)
            }.toString()

            val signature = this.deviceBinding.signSHA256withECDSA(this.deviceBinding.getDevicePrivateKey(),signedPayload.toByteArray())
            val signatureBase64 = Base64.getEncoder().encodeToString(signature)
            val modifiedContent = JSONObject().apply {
                put("signedPayload", signedPayload)
                put("signature", signatureBase64)
            }.toString()
            val modifiedBody = RequestBody.create(originalMediaType, modifiedContent)
            val modifiedRequest = originalRequest.newBuilder()
                .method(originalRequest.method(), modifiedBody)
                .headers(originalRequest.headers())
                .addHeader("Authorization", sessionToken)
                .header("Content-Type", "application/json")
                .build()

            return chain.proceed(modifiedRequest)
        }

        return chain.proceed(originalRequest)
    }
}
