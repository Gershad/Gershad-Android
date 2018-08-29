package com.gershad.gershad.dependency

import android.content.SharedPreferences
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.network.ApiService
import com.gershad.gershad.network.Repository
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson

/**
 * --------------------------------------------------------------------------
 *
 * Module contracts
 *
 * --------------------------------------------------------------------------
 *
 */

/**
 * Android internals
 */
interface AppInternalsModule {
    val preferences: GershadPreferences
    val googleApiClient : GoogleApiClient
    val amazonServiceProvider: AmazonServiceProvider
}

/**
 * Network data.
 * [Repository] has dependencies [ApiService]
 * which are provided through injection.
 */
interface PresenterModule {
    val repository: Repository
}

/**
 * Endpoints (created with Retrofit)
 */
interface NetModule {
    val apiService : ApiService
    val gson: Gson
}

/*
 * SharedPreferences and app preferences.
 */
interface GershadPreferences {
    val rawPreferences: SharedPreferences
    var availableVersionCode: Int
    var updateNotificationDate: Long
    var interval: Long
    var reportsNearYou: Boolean
    var reportsNearSaved: Boolean
    var updateOnReports: Boolean
    var proxy: Boolean
    var uninstall: Boolean
    var homeLocation: LatLng
    var endpointArn: String
    var topicArn: String
}