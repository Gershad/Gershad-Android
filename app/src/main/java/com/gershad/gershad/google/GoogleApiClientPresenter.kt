package com.gershad.gershad.google

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.dependency.Module
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Handle [GoogleApiClient] connectivity and calls.
 */
class GoogleApiClientPresenter(val context: Context, private val subscriber: GoogleApiClientContract.ApiCallbackSubscriber) :
        GoogleApiClientContract.ApiClientPresenter,
        GoogleApiClientContract.ApiCallbackListener,
        Injects<Module> {

    private val apiClient by required { googleApiClient }
    private var callbacks: ApiConnectionCallbacks = ApiConnectionCallbacks(this)
    private var locationUpdates: Flowable<Location>? = null
    private var subscription: Disposable? = null

    init {
        inject(BaseApplication.module(context))
    }

    // Called internally on start
    private fun registerAndConnect() {
        apiClient.registerConnectionCallbacks(callbacks)
        apiClient.registerConnectionFailedListener(callbacks)
        apiClient.connect()
    }

    // Called internally on cancel/complete
    private fun unregisterAndDisconnect() {
        apiClient.unregisterConnectionFailedListener(callbacks)
        apiClient.unregisterConnectionCallbacks(callbacks)
        apiClient.disconnect()
    }

    private fun buildLocationRequest(): Flowable<Location> {
        return Flowable.create({ emitter: FlowableEmitter<Location> ->

            if (!emitter.isCancelled) {
                val request: LocationRequest = LocationRequest
                        .create()
                        .setInterval(20000) // 20 s
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

                try {
                    FusedLocationProviderClient(context).requestLocationUpdates(request, object : LocationCallback() {
                        override fun onLocationResult(p0: LocationResult?) {
                            Log.d("LocationCallback", "Object locationcallback sent onNext to emitter")
                            super.onLocationResult(p0)
                            emitter.onNext(p0?.lastLocation!!)
                        }

                        override fun onLocationAvailability(p0: LocationAvailability?) {
                            super.onLocationAvailability(p0)
                            (p0?.isLocationAvailable ?: Log.d("LocationCallback", "Location is available"))
                        }
                    }, null)

                } catch (se: SecurityException) {
                    // log & error on emitter.
                    Log.e("MyLocationProvider", "Location permissions error.")
                    emitter.onError(se)
                }
            }

            emitter.setCancellable {
                Log.d("MyLocationProvider", "Location updates were cancelled")
                if (apiClient.isConnected) {

                    LocationServices.FusedLocationApi
                            .removeLocationUpdates(apiClient,
                                    { emitter.onComplete() })
                }
            }
        },
                BackpressureStrategy.LATEST)

    }

    //todo @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    override fun onConnected() {

        locationUpdates = buildLocationRequest()

        subscription = (locationUpdates!!)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCancel {
                    Log.d("MapDisposable", "Canceled")
                    unregisterAndDisconnect()
                }
                .doOnComplete {
                    Log.d("MapDisposable", "Completed")
                    unregisterAndDisconnect()
                }
                .subscribe(
                        { location ->
                            Log.d("MapDisposable", "onNext")
                            subscriber.updateLocation(location)
                        },
                        { error ->
                            Log.e("MapDisposable", "error: " + error.message)
                            subscriber.onError(error)
                        })
    }

    override fun connect() {
        registerAndConnect()
    }

    override fun cancel() {
        subscription?.dispose()  // this will cancel the subscription which unregisters callbacks
    }

    override fun onError(e: Throwable) {
        subscriber.onError(e) // This is an ApiCallbacksConnectionError, not a map error
    }

    class ApiConnectionCallbacks(private val listener: GoogleApiClientContract.ApiCallbackListener) :
            GoogleApiClient.OnConnectionFailedListener,
            GoogleApiClient.ConnectionCallbacks {

        /*
         * GoogleApiClient callbacks
         */
        override fun onConnectionFailed(p0: ConnectionResult) {
            Log.e("ApiClient", "** Connection failed ** " + p0.errorMessage)
            listener.onError(Throwable(p0.errorMessage))
        }

        override fun onConnected(p0: Bundle?) {
            Log.d("ApiClient", "Connected!")
            listener.onConnected()
        }

        override fun onConnectionSuspended(p0: Int) {
            Log.i("ApiClient", "Connection Suspended")
        }
    }
}