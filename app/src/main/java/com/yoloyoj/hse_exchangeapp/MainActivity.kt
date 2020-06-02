package com.yoloyoj.hse_exchangeapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    // current currencies
    lateinit var topCurrency: String
    lateinit var botCurrency: String

    // to avoid recursion
    var isCalculating = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        loadDefaults()
        loadListeners()

        super.onStart()
    }

    private fun loadDefaults() {
        topCurrency = "RUB"
        botCurrency = "USD"

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
                            (s.toDouble() / 74).toEditable()
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
                            (s.toDouble() * 74).toEditable()
                        else
                            "0".toEditable()
                } else
                    isCalculating = true
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
