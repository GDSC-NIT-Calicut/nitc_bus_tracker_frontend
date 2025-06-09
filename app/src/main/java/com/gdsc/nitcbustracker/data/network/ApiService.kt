package com.gdsc.nitcbustracker.data.network

import com.gdsc.nitcbustracker.data.model.Bus
import com.gdsc.nitcbustracker.data.model.BusLocation
import com.gdsc.nitcbustracker.data.model.BusStatus
import com.gdsc.nitcbustracker.data.model.GenericResponse
import com.gdsc.nitcbustracker.data.model.LoginRequest
import com.gdsc.nitcbustracker.data.model.LoginResponse
import com.gdsc.nitcbustracker.data.model.Notice
import com.gdsc.nitcbustracker.data.model.RegisterRequest
import com.gdsc.nitcbustracker.data.model.RouteStop
import com.gdsc.nitcbustracker.data.model.Stop
import com.gdsc.nitcbustracker.data.model.UserInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/api/bus/get")
    suspend fun getBusDetails(@Query("busId") busId: String): Response<Bus>

    @POST("/api/bus-statuses/post")
    suspend fun postBusStatuses(@Body busStatus: BusStatus): Response<GenericResponse>

    @GET("/api/location/get-buses")
    suspend fun getLocations(): Response<List<BusLocation>>

    @GET("/api/notices/get")
    suspend fun getNotices(): Response<List<Notice>>

    @GET("/api/route-stops/get/{route_id}")
    suspend fun getRouteStops(@Path("route_id") routeId: Int): Response<List<RouteStop>>

    @GET("/api/stops/get")
    suspend fun getStops(): List<Stop>

    @POST("/api/user/complete-registration")
    suspend fun completeRegistration(@Body request: RegisterRequest): Response<GenericResponse>

    @GET("/api/user/exists")
    suspend fun checkUserExists(@Query("email") email: String): Response<Map<String, Boolean>>

    @GET("/api/user/getinfo")
    suspend fun getUserInfo(@Query("email") email: String): Response<UserInfo>

    @POST("/api/user/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("/api/user/partial-registration")
    suspend fun partialRegistration(@Body request: Map<String, String>): Response<Map<String, Any>>



}