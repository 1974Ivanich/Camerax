package com.example.camerax.retrofit

import retrofit2.http.Body
import retrofit2.http.POST

interface MainApi {

    @POST("/api/v1/photos/")
    suspend fun auth(@Body authRequest: AuthRequest)
}