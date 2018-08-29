package com.gershad.gershad.network

import com.gershad.gershad.model.DeleteRequest
import com.gershad.gershad.model.Report
import com.gershad.gershad.model.ReportGroup
import com.gershad.gershad.model.SavedLocation
import io.reactivex.Flowable
import retrofit2.Response
import retrofit2.http.*

/**
 * ApiService for repository access.
 * Can access repository from API or stored.
 * Endpoints are created with Retrofit.
 */
interface ApiService {

    @GET("api/external/v1/reports")
    fun getReports(@Query("latitude") latitude: Double, @Query("longitude") longitude: Double) : Flowable<List<ReportGroup>>

    @GET("api/external/v1/reports") // With bounding box (ne/sw) for details
    fun getReports(@QueryMap boundingBoxOptions: Map<String, Double>, @Query("cluster") isClustered: Boolean = false) : Flowable<List<ReportGroup>>

    @POST("api/external/v1/reports")
    fun postReport(@Body report: Report) : Flowable<Report>

    @POST("api/external/v1/reports/delete")
    fun deleteReport(@Body deleteRequest: DeleteRequest) : Flowable<Response<Void>>

    @PUT("api/external/v1/reports")
    fun updateReport(@Body report: Report) : Flowable<Report>

    @POST("api/external/v1/poi")
    fun postPoi(@Body savedLocation: SavedLocation) : Flowable<SavedLocation>

    @POST("api/external/v1/poi/delete")
    fun deletePoi(@Body deleteRequest: DeleteRequest) : Flowable<Response<Void>>

    @GET("api/external/v1/poi")
    fun getPoi(@Query("token") token: String) : Flowable<List<SavedLocation>>
}


