package com.gershad.gershad.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * ReportBounds to specify [ReportGroup] detailed response or clustered reports.
 * The bounds are based on the [LatLngBounds] (southwest and northeast coordinates),
 * with longitude (X) representing west and east, and latitude (Y) representing north and south.
 */
data class ReportBounds(private val west: Double,
                        private val south: Double,
                        private val east: Double,
                        private val north: Double) {

    /*
     * Alternate constructor built from LatLngBounds.
     */
    constructor(bounds: LatLngBounds) :
            this(bounds.southwest.longitude,
            bounds.southwest.latitude,
            bounds.northeast.longitude,
            bounds.northeast.latitude)

    fun toQueryMap() : Map<String, Double> {
        return HashMap<String, Double>().also {
            it["west"] = this.west
            it["north"] = this.north
            it["south"] = this.south
            it["east"] = this.east
        }
    }

    fun toLatLngBounds() : LatLngBounds = LatLngBounds(LatLng(south, west), LatLng(north, east))
}
