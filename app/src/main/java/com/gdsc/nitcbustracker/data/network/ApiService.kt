package com.gdsc.nitcbustracker.data.network

import com.gdsc.nitcbustracker.data.model.GenericResponse
import com.gdsc.nitcbustracker.data.model.LoginRequest
import com.gdsc.nitcbustracker.data.model.LoginResponse
import com.gdsc.nitcbustracker.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("/api/user/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("/api/user/partial-registration")
    suspend fun partialRegistration(@Body request: Map<String, String>): Response<Map<String, Any>>

    @POST("/api/user/complete-registration")
    suspend fun completeRegistration(@Body request: RegisterRequest): Response<GenericResponse>

    @GET("/api/user/exists")
    suspend fun checkUserExists(@Query("email") email: String): Response<Map<String, Boolean>>

}