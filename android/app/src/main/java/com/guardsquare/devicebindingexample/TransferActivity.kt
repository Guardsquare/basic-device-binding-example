package com.guardsquare.devicebindingexample

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.guardsquare.devicebindingexample.api.RestApiClient
import com.guardsquare.devicebindingexample.model.BalanceResponse
import com.guardsquare.devicebindingexample.model.TransferRequest
import com.guardsquare.devicebindingexample.model.TransferResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class TransferActivity : AppCompatActivity() {

    val apiClient = RestApiClient.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        getBalance()

        findViewById<Button>(R.id.buttonGetBalance).setOnClickListener{
            getBalance()
        }
        findViewById<Button>(R.id.buttonTransfer).setOnClickListener{
            transferMoney()
        }
    }

    fun getBalance(){
        val call = apiClient.apiService?.getBalance()
        call?.enqueue(object : Callback<BalanceResponse> {
            override fun onResponse(call: Call<BalanceResponse>, response: Response<BalanceResponse>) {
                if (response.code() == 200) {
                    val balance = response.body()!!.balance
                    runOnUiThread {
                        findViewById<TextView>(R.id.textViewBalance).text = "\$$balance"
                        Toast.makeText(applicationContext, "Balance updated", Toast.LENGTH_LONG).show()
                    }
                }
                else{
                    Toast.makeText(applicationContext, "HTTP Error ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BalanceResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Failed to connect to the server", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun transferMoney(){
        var amount = findViewById<EditText>(R.id.editTextAmount).text.toString()
        if (amount == ""){
            amount = "100" // By default we send $100
        }

        val recipient = findViewById<EditText>(R.id.editTextRecipient).text.toString()

        val call = apiClient.apiService?.transfer(TransferRequest(amount.toInt(), recipient))
        call?.enqueue(object : Callback<TransferResponse> {

            override fun onResponse(call: Call<TransferResponse>, response: Response<TransferResponse>) {
                if (response.code() == 200) {
                    val balance = response.body()!!.balance
                    runOnUiThread {
                        findViewById<TextView>(R.id.textViewBalance).text = "\$$balance"
                        Toast.makeText(applicationContext, "Transfer has been sent", Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(applicationContext, "HTTP Error ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<TransferResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Failed to connect to the server", Toast.LENGTH_LONG).show()
            }
        })
    }
}