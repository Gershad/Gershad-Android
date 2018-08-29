package com.gershad.gershad.model

import com.google.gson.annotations.SerializedName
import org.parceler.Parcel
import java.text.NumberFormat
import java.util.*

/**
 * Data class to store favourite locations.
 */
@Parcel(Parcel.Serialization.BEAN)
data class SavedLocation(val id: Int? = null, val latitude: Double = 0.0, val longitude: Double = 0.0, var address: String? = null, @SerializedName("token") var reporterToken: String = "", @SerializedName("arn") var arn: String = "") {

    override fun equals(other: Any?): Boolean {
        val format = NumberFormat.getInstance(Locale.US)
        format.minimumFractionDigits = 13
        format.maximumFractionDigits = 13
        return other is SavedLocation && format.format(this.latitude) == format.format(other.latitude) && format.format(this.longitude) == format.format(other.longitude)
    }

    override fun hashCode(): Int {
        val format = NumberFormat.getInstance(Locale.US)
        format.minimumFractionDigits = 13
        format.maximumFractionDigits = 13
        var result = format.format(this.latitude).hashCode()
        result = 31 * result + format.format(this.longitude).hashCode()
        return result
    }
}
