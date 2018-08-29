package com.gershad.gershad.model

import com.gershad.gershad.model.Report.ReportType
import com.gershad.gershad.network.SkipDeserialization
import com.gershad.gershad.network.SkipSerialization
import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.google.maps.android.clustering.ClusterItem
import org.parceler.Parcel


/**
 * Report family. Contains many reports of different types.
 */
@Parcel(Parcel.Serialization.BEAN)
data class ReportGroup constructor(
        @SerializedName("centroidLatitude")
        val latitude: Double = 0.0,

        @SerializedName("centroidLongitude")
        val longitude: Double = 0.0,

        @SkipSerialization @SkipDeserialization
        var address: String? = null,

        @SkipSerialization
        var reportCount: Int = 0,

        @SkipSerialization
        var score: Double = 1.0,

        @SerializedName("reports")
        var members: ArrayList<Report> = ArrayList(),

        @SkipSerialization @SerializedName("faded")
        var fadeValue: Float = 1.0f,

        @SkipSerialization @SerializedName("verified")
        var isVerified: Boolean = false,

        @SkipSerialization
        var lastUpdate: String? = null,

        @SerializedName("type")
        var displayType: ReportType = ReportType.UNSPECIFIED,

        @SkipSerialization
        var distance: Double = 0.0
): ClusterItem {

    /*
     * Alternate constructor based on a single report instance.
     */
    constructor(report: Report) : this(latitude = report.latitude,
            longitude = report.longitude, address = report.address, reportCount = 1, displayType = report.type, members = ArrayList<Report>().also { it.add(report) }, distance = 0.0)

    override fun getSnippet(): String {
        return address?: "(%.4f, %.4f)".format(latitude, longitude) // TODO
    }

    override fun getTitle(): String {
        return address?: "(%.4f, %.4f)".format(latitude, longitude)
    }

    override fun getPosition(): LatLng {
        return LatLng(latitude, longitude)
    }

    /**
     * Two [ReportGroup] instances are equal if they are at the same location.
     * This is to ensure that ReportGroups are updated as new instances of [Report] are added,
     * rather than considered as separate points.
     * The [com.google.maps.android.clustering.ClusterManager<T>] uses the report equals method to manage its marker collection.
     */
    override fun equals(other: Any?): Boolean {
        return (other is ReportGroup && this.latitude == other.latitude && this.longitude == other.longitude)
    }
}
