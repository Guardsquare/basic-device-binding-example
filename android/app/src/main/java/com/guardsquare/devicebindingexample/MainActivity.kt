package com.guardsquare.devicebindingexample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.guardsquare.devicebindingexample.api.RestApiClient
import com.guardsquare.devicebindingexample.model.LoginRequest
import com.guardsquare.devicebindingexample.model.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Base64


class MainActivity : AppCompatActivity() {

    val apiClient = RestApiClient.getInstance()
    val deviceBinding = DeviceBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupKeystore()
        setupApiClient()
        refreshDeviceBindingTextView()

        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            login()
        }

        findViewById<Button>(R.id.buttonResetDeviceBinding).setOnClickListener{
            resetDeviceBinding()
        }
    }

    fun setupKeystore(){
        try{
            deviceBinding.initializeKeyStore(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
        }
    }
    fun setupApiClient(){
        RestApiClient.getInstance().setup(deviceBinding)
    }

    fun login(){
        if(isDeviceBindingEstablished()){
            loginWithoutHandshake()
        }
        else{
            loginWithHandshake()
        }
    }

    fun loginWithHandshake(){
        val email = findViewById<EditText>(R.id.editTextEmail).text.toString()
        val password = findViewById<EditText>(R.id.editTextPassword).text.toString()
        val loginRequest = LoginRequest(email = email, password = password, pubkey = Base64.getEncoder().encodeToString(deviceBinding.getDevicePublicKey()!!.encoded))
        val call = apiClient.apiService?.loginWithHandshake(loginRequest)
        call?.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.code() == 200) {
                    val token = response.body()!!.token!!
                    Toast.makeText(applicationContext, "Success login. Your session token is $token", Toast.LENGTH_LONG).show()
                    print("Received Session token:$token")
                    setDeviceBindingStatus(true)
                    apiClient.addSessionToken(token)
                    val intent = Intent(applicationContext, TransferActivity::class.java)
                    startActivity(intent)
                }
                else{
                    Toast.makeText(applicationContext, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Failed to connect to the server", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun loginWithoutHandshake(){
        val email = findViewById<EditText>(R.id.editTextEmail).text.toString()
        val password = findViewById<EditText>(R.id.editTextPassword).text.toString()
        val loginRequest = LoginRequest(email = email, password = password, pubkey = "")
        val call = apiClient.apiService?.loginWithoutHandshake(loginRequest)
        call?.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.code() == 200) {
                    val token = response.body()!!.token!!
                    Toast.makeText(applicationContext, "Success login. Your session token is $token", Toast.LENGTH_LONG).show()
                    print("Received Session token:$token")
                    apiClient.addSessionToken(token)
                    val intent = Intent(applicationContext, TransferActivity::class.java)
                    startActivity(intent)
                }
                else{
                    Toast.makeText(applicationContext, "Error: ${response.errorBody()?.string()?.replace("\n", " ")}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Failed to connect to the server", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun setDeviceBindingStatus(value: Boolean) {
        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("handshakeStatus", value)
        editor.apply()
        refreshDeviceBindingTextView()
    }

    fun isDeviceBindingEstablished(defaultValue: Boolean = false): Boolean {
        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        return sharedPreferences.getBoolean("handshakeStatus", defaultValue)
    }

    fun refreshDeviceBindingTextView(){
        if(isDeviceBindingEstablished()){
            findViewById<TextView>(R.id.textViewHandshake).text = "Handshake established"
            findViewById<Button>(R.id.buttonLogin).text = "Login"
            findViewById<Button>(R.id.buttonResetDeviceBinding).visibility = View.VISIBLE
        }
        else{
            findViewById<TextView>(R.id.textViewHandshake).text = "Handshake not initialized"
            findViewById<Button>(R.id.buttonLogin).text = "Login with handshake"
            findViewById<Button>(R.id.buttonResetDeviceBinding).visibility = View.INVISIBLE
        }

    }

    fun resetDeviceBinding(){
        deviceBinding.resetDeviceBinding(applicationContext)
        setupKeystore()
        setupApiClient()
        setDeviceBindingStatus(false)
        refreshDeviceBindingTextView()
    }

}