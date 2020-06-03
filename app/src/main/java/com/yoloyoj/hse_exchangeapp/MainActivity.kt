package com.yoloyoj.hse_exchangeapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.yoloyoj.hse_exchangeapp.web.getApiClient
import io.github.rokarpov.backdrop.BackdropController
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

        loadDatePicker()

        super.onStart()
    }

    @SuppressLint("SetTextI18n")
    private fun loadDatePicker() {
        date_picker.editText?.text = "latest".toEditable()

        // TODO (https://github.com/Ibotta/Supported-Picker-Dialogs): Add support
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            date_picker.visibility = View.GONE
            return
        }

        date_picker.editText!!.setOnClickListener {
            DatePickerDialog(this).apply {
                setOnDateSetListener { _, year, month, day ->
                    (it as TextInputEditText).text = "$year-$month-$day".toEditable()
                    updateConvertValue()
                }
            }.show()
        }

        date_picker.editText!!.setOnFocusChangeListener { view, b ->
            if (b) view.callOnClick()
        }
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
            "%.2f".format(
                if (me == top_value) bot_value.editText!!.text.toDouble() * convertValue
                else top_value.editText!!.text.toDouble() / convertValue
            ).toEditable()
    }

    private fun updateConvertValue() {
        getApiClient()
            .getExchangeValues(date_picker.editText?.text.toString(), botCurrency, topCurrency)?.enqueue(object : Callback<Map<Any, Any>?> {
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

                    // why I don't defined normal func? - I dunno
                    // view current currency rate
                    convert_value.text =
                        listOf(botCurrency, topCurrency)  // create list
                            .map { checkSymbol(Currency.getInstance(it).symbol) }  // get symbols
                            .run { if (convertValue < 1) reversed() + (1/convertValue) else this + convertValue }  // sort by cheap
                            .run { "1 ${this[0]} = ${"%.2f".format(this[2])} ${this[1]}" }  // view currency rate
                }
            })
    }
}

// return unicode symbols if we can
private fun checkSymbol(s: String) = when (s) {
    Currency.getInstance("RUB").symbol -> "₽"
    else -> s
}.run {
        if ((this.length == 1) and this.toCharArray()[0].isDefined()) this
        else s
    }

// for displaying undisplayable symbols
private fun Currency.getName(): String = "${checkSymbol(symbol)} $displayName"

// for easy displaying values
private fun Any.toEditable(): Editable? = Editable.Factory.getInstance().newEditable(toString())

// for easy reading values
private fun Editable.toDouble(): Double = toString().run {
    try {
        this.toDouble()
    } catch (e: Exception) {
        0.0
    }
}

