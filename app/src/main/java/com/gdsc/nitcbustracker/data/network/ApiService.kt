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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/api/bus/get")
    suspend fun getBusDetails(@Query("busId") busId: String): Response<Bus>

    @GET("/api/bus-statuses/get")
    suspend fun getBusStatuses(): Response<List<BusStatus>>

    @POST("/api/bus-statuses/post")
    suspend fun postBusStatuses(@Body busStatus: BusStatus): Response<GenericResponse>

    @GET("/api/location/get-buses")
    suspend fun getLocations(): Response<List<BusLocation>>

    @POST("/api/location/post")
    fun sendLocation(@Body busLocation: BusLocation): Call<ResponseBody>

    @DELETE("/api/notices/delete/{topic}")
    suspend fun deleteNotice(@Path("topic") topic: String): Response<GenericResponse>

    @GET("/api/notices/get")
    suspend fun getNotices(): Response<List<Notice>>

    @POST("/api/notices/post")
    suspend fun updateNotices(@Body notices: Notice): Response<GenericResponse>

    @POST("/api/notifications/both")
    fun sendNotificationBoth(@Body request: Notice): Call<Void>

    @POST("/api/notifications/driver")
    fun sendNotificationDriver(@Body request: Notice): Call<Void>

    @POST("/api/notifications/student")
    fun sendNotificationStudent(@Body request: Notice): Call<Void>

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