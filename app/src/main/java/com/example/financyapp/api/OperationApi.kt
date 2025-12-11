package com.example.financyapp.api

import com.example.financyapp.model.Operation
import retrofit2.Call
import retrofit2.http.*

interface OperationApi {
    @GET("operations")
    fun getOperations(): Call<List<Operation>>

    @POST("operations")
    fun createOperation(@Body operation: Operation): Call<Operation>

    @GET("operations/{id}")
    fun getOperation(@Path("id") id: Int): Call<Operation>

    @PUT("operations/{id}")
    fun updateOperation(@Path("id") id: Int, @Body operation: Operation): Call<Operation>

    @DELETE("operations/{id}")
    fun deleteOperation(@Path("id") id: Int): Call<Unit>
}

