package com.gershad.gershad.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Address
import com.gershad.gershad.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 * Extension functions - accessible project-wide
 *
 */

fun Double.toDistance(context: Context): String {
    return when {
        this < 1000 -> String.format("%.0f %s", this, context.getString(R.string.meter))
        else -> String.format("%.2f %s", this / 1000.0, context.getString(R.string.kilometer))
    }
}


fun LatLng.toBounds(radiusInMeters: Double = 5000.0): LatLngBounds {
    val distanceFromCenterToCorner = radiusInMeters * Math.sqrt(2.0)
    val southwestCorner = SphericalUtil.computeOffset(this, distanceFromCenterToCorner, 225.0)
    val northeastCorner = SphericalUtil.computeOffset(this, distanceFromCenterToCorner, 45.0)
    return LatLngBounds(southwestCorner, northeastCorner)
}

fun Address.formatAddress(): String {
    var address = String()
    for (i in 0..this.maxAddressLineIndex) {
        address += this.getAddressLine(i) + ", "
    }
    return address.removeSuffix(", ")
}


fun Drawable.savedLocationIcon(): BitmapDescriptor {
    val bitmap: Bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}


/** ---------------------------------------------------------------
 *
 *  Time formatting
 *
 *  ---------------------------------------------------------------
 */
fun formatTime(timestamp: String?): String? {
    if (timestamp == null) return null

    val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    sdfIn.timeZone = TimeZone.getTimeZone("UTC")

    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Tehran")
    return sdf.format(sdfIn.parse(timestamp))
}
