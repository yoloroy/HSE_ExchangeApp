package com.yoloyoj.hse_exchangeapp.web

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Link {
    @GET("latest")
    fun getExchangeValues(
        @Query("base") base: String,
        @Query("symbols") symbol: String
        ): Call<Map<Any, Any>?>?
}
