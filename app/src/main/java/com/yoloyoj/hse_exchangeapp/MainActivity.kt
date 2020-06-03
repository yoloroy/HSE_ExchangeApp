package com.yoloyoj.hse_exchangeapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.yoloyoj.hse_exchangeapp.web.getApiClient
import io.github.rokarpov.backdrop.BackdropController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.bot_value
import kotlinx.android.synthetic.main.activity_main.top_value
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var backdropController: BackdropController

    // current currencies
    private lateinit var topCurrency: String
    private lateinit var botCurrency: String

    // to avoid recursion
    var isCalculating = true

    // top_value / convertValue -> bot_value
    // bot_value * convertValue -> top_value
    var convertValue: Double = 1.0

    // Available currencies
    var names = emptyList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadBackdrop()
    }

    override fun onStart() {
        loadListeners()
        loadChoosers()
        // loadDefaults calls after get response in loadChoosers
        // loadConvertValue calls in loadDefaults

        super.onStart()
    }

    override fun onBackPressed() {
        if (!backdropController.conceal())
            super.onBackPressed()
    }

    private fun loadBackdrop() {
        backdropController = BackdropController.build(backLayout, this) {
            supportToolbar = this@MainActivity.toolbar
            navigationIconSettings(currency_choose)

            concealedTitleId = R.string.app_name
            concealedNavigationIconId = R.drawable.ic_menu_black
            revealedNavigationIconId = R.drawable.ic_close_black
        }

        toolbar.setTitle(R.string.app_name)
    }

    private fun loadDefaults() {
        topCurrency = "RUB"
        botCurrency = "USD"

        top_currency_choose.setSelection(names.indexOf(topCurrency))
        bot_currency_choose.setSelection(names.indexOf(botCurrency))

        loadConvertValue()
    }

    private fun loadConvertValue() {
        updateConvertValue()
    }

    private fun loadListeners() {
        // shows converting values in realtime

        top_value.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (isCalculating) {
                    isCalculating = false
                    recalculate(bot_value)
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
                    recalculate(top_value)
                } else
                    isCalculating = true
            }
        })
    }

    private fun loadChoosers() {
        getApiClient()
            .getAvailableNames()?.enqueue(object : Callback<Map<Any, Any>?> {
                override fun onFailure(call: Call<Map<Any, Any>?>, t: Throwable) {
                    // TODO: Add snackBar
                }

                override fun onResponse(call: Call<Map<Any, Any>?>, response: Response<Map<Any, Any>?>) {
                    @Suppress("UNCHECKED_CAST")  // all checked))
                    (response.body()?.get("rates") as Map<String, *>).keys.plus("EUR").toList().sorted().also { names ->
                        this@MainActivity.names = names
                        with(
                            object : ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, names) {
                                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                    return super.getView(position, convertView, parent)
                                        .convertView(position).apply {
                                            @Suppress("DEPRECATION")
                                            setTextColor(resources.getColor(R.color.onPrimary))
                                        }
                                }

                                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                                    return super.getDropDownView(position, convertView, parent)
                                        .convertView(position)
                                }

                                private fun View.convertView(position: Int) = (this as TextView).apply {
                                    text = Currency.getInstance(getItem(position)).getName()
                                }
                            }
                        ) {
                            top_currency_choose.adapter = this
                            bot_currency_choose.adapter = this
                        }
                        top_currency_choose.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: AdapterView<*>?) {}

                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                topCurrency = parent!!.adapter.getItem(position).toString()
                                top_value.hint = Currency.getInstance(topCurrency).getName()
                                updateConvertValue()
                            }
                        }
                        bot_currency_choose.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: AdapterView<*>?) {}

                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                botCurrency = parent!!.adapter.getItem(position).toString()
                                bot_value.hint = Currency.getInstance(botCurrency).getName()
                                updateConvertValue()
                            }
                        }
                        loadDefaults()
                    }
                }
            })
    }

    @SuppressLint("SetTextI18n")  // this is only a number
    private fun recalculate(me: View) {
        (me as TextInputLayout).editText!!.text =
            "%.2f".format(if (me == top_value) bot_value.editText!!.text.toDouble() * convertValue else top_value.editText!!.text.toDouble() / convertValue).toEditable()
    }

    private fun updateConvertValue() {
        getApiClient()
            .getExchangeValues(botCurrency, topCurrency)?.enqueue(object : Callback<Map<Any, Any>?> {
                override fun onFailure(call: Call<Map<Any, Any>?>, t: Throwable) {
                    // TODO: Add snackBar
                }

                override fun onResponse(call: Call<Map<Any, Any>?>, response: Response<Map<Any, Any>?>) {
                    try {
                        convertValue = (response.body()?.get("rates") as Map<*, *>)[topCurrency] as Double
                    } catch (e: TypeCastException) {
                        // it's probably first chooser's call
                    }
                    top_value.editText!!.text = top_value.editText!!.text
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
private fun Editable.toDouble(): Double {
    this.toString().run {
        try {
            return this.toDouble()
        } catch (e: Exception) {
            return 0.0
        }
    }
}
