package com.yoloyoj.hse_exchangeapp.web

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val MAIN_BASE_URL = "https://api.exchangeratesapi.io/"

fun getApiClient(base_url: String): Link?
        = Retrofit.Builder()
    .baseUrl(base_url)
    .addConverterFactory(
        GsonConverterFactory.create()
    )
    .build().create(Link::class.java)

fun getApiClient(): Link = getApiClient(MAIN_BASE_URL)!!