package com.gershad.gershad.model

import com.gershad.gershad.network.SkipSerialization
import com.google.gson.annotations.SerializedName
import org.parceler.Parcel

/**
 *  "id" : 1001,
 *  "modified" : "2017-09-18T18:14:16.361Z",
 *  "address" : "string"
 *  "description" : "string",
 *  "type" : "VAN",
 *  "latitude" : 32.1,
 *  "longitude" : 53.0,
 *  "token" : "string",
 *  "client" : "ANDROID"
 */

/**
 * Data class for report objects
 */

@Parcel(Parcel.Serialization.BEAN)
data class Report constructor(
        var latitude: Double = 0.0,

        var longitude: Double = 0.0,

        var type: ReportType = ReportType.UNSPECIFIED,

        var client: Client = Client.ANDROID,

        @SerializedName("token")
        var reporterToken: String = "",

        @SkipSerialization
        var address: String? = null,

        @SerializedName("description")
        var comments: String? = null,

        @SkipSerialization
        var id: Int? = null,

        @SkipSerialization @SerializedName("verified")
        var isVerified: Boolean = false,

        @SkipSerialization @SerializedName("modified")
        var timestamp: String? = null
) {

    override fun equals(other: Any?): Boolean {
        return (other is Report && this.id == other.id)
    }

    override fun hashCode(): Int {
        return id ?: 0
    }

    enum class ReportType {
        GASHT, STOP, VAN, UNSPECIFIED
    }

    enum class Client {
        ANDROID
    }
}
