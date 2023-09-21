package com.guardsquare.devicebindingexample.model

data class LoginRequest(
    var email: String,
    var password: String,
    var pubkey: String,
)
