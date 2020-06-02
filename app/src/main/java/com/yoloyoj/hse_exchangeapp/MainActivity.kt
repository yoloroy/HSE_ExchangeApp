package com.yoloyoj.hse_exchangeapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.yoloyoj.hse_exchangeapp.web.getApiClient
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class MainActivity : AppCompatActivity() {
    // current currencies
    private lateinit var topCurrency: String
    private lateinit var botCurrency: String

    // to avoid recursion
    var isCalculating = true

    // divide on this to convert top to bot and multiply for do the opposite
    var convertValue: Double = 1.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        loadDefaults()
        loadConvertValue()
        loadListeners()

        super.onStart()
    }

    private fun loadDefaults() {
        topCurrency = "RUB"
        botCurrency = "USD"

        convertValue = 74.0

        top_value.hint = Currency.getInstance(topCurrency).getName()
        bot_value.hint = Currency.getInstance(botCurrency).getName()
    }

    private fun loadListeners() {
        // shows converting values in realtime

        top_value.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (isCalculating) {
                    isCalculating = false
                    bot_value.editText?.text =
                        if (s.isNotBlank())
                            (s.toDouble() / convertValue).toEditable()
                        else
                            "0".toEditable()
                } else
                    isCalculating = true
            }
        })

        bot_value.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (isCalculating) {
                    isCalculating = false
                    top_value.editText?.text =
                        if (s.isNotBlank())
                            (s.toDouble() * convertValue).toEditable()
                        else
                            "0".toEditable()
                } else
                    isCalculating = true
            }
        })
    }

    private fun loadConvertValue() {
        Log.i("load", "ConvertValue")
        getApiClient()
            .getExchangeValues(botCurrency, topCurrency)?.enqueue(object : Callback<Map<Any, Any>?> {
                override fun onFailure(call: Call<Map<Any, Any>?>, t: Throwable) {
                    Log.i("CV", "onFailure")
                }

                override fun onResponse(call: Call<Map<Any, Any>?>, response: Response<Map<Any, Any>?>) {
                    convertValue = (response.body()?.get("rates") as Map<*, *>)["RUB"] as Double
                }
            })
    }
}

// for displaying undisplayable symbols
private fun Currency.getName(): String {
    if (this.symbol == "RUB")
        return this.run { "₽ $displayName" }
    return this.run { "$symbol $displayName" }
}

// for easy displaying values
private fun Any.toEditable(): Editable? = Editable.Factory.getInstance().newEditable(toString())

// for easy reading values
private fun Editable.toDouble(): Double = this.toString().toDouble()
