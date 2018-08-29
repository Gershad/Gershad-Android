package com.gershad.gershad.google

import android.location.Location


interface GoogleApiClientContract {

    interface ApiCallbackListener {
        fun onConnected()

        fun onError(e: Throwable)
    }

    interface ApiCallbackSubscriber {
        fun updateLocation(location: Location)

        fun onError(e: Throwable)
    }

    interface ApiClientPresenter {
        fun connect()

        fun cancel()
    }

    interface View {

        fun updateCurrentLocation(loc: Location)

        fun showErrorView(e: Throwable)
    }

    interface Presenter {
        val isListeningForLocation: Boolean

        fun startLocationListener()

        fun stopLocationListener()
    }
}