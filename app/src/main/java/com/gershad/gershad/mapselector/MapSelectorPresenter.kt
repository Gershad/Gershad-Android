package com.gershad.gershad.mapselector

import android.content.Context
import android.location.Location
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.google.GoogleApiClientContract
import com.gershad.gershad.google.GoogleApiClientPresenter
import com.gershad.gershad.model.ReportGroup
import com.gershad.gershad.model.SavedLocation
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

/**
 * Presenter that loads [ReportGroup] items. This is the main presenter in charge of the reports map.
 */
class MapSelectorPresenter(private val context: Context, private val mapFragment: MapSelectorContract.View) : MapSelectorContract.Presenter,
        GoogleApiClientContract.ApiCallbackSubscriber,
        GoogleApiClientContract.Presenter,
        Injects<Module> {

    private var amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }
    private val repository by required { repository }
    private val gershadSettings by required { preferences }
    private val apiClient by lazy { createMapClient() }

    // Check whether actively listening for location updates s
    private var mIsListening: Boolean = false
    override var isListeningForLocation: Boolean
        get() = mIsListening
        private set(isNewListeningStatus) {
            mIsListening = isNewListeningStatus
        }

    init {
        mapFragment.presenter = this
        inject(BaseApplication.module(context))
    }

    override fun startLocationListener() {
        apiClient.connect()
        isListeningForLocation = true
    }

    override fun stopLocationListener() {
        apiClient.cancel()
        isListeningForLocation = false
    }

    override fun updateLocation(location: Location) {
        if (mapFragment.isActive) {
            mapFragment.updateCurrentLocation(location)
        }
    }

    override fun onError(e: Throwable) {
        if (mapFragment.isActive) {
            mapFragment.showErrorView(e)
        }
    }

    private fun createMapClient(): GoogleApiClientContract.ApiClientPresenter = GoogleApiClientPresenter(context, this)

    override fun loadItems() {
        repository.getPoi(getToken())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    result ->
                    if (mapFragment.isActive) {
                        mapFragment.onItemsLoaded(result)
                    }
                }, { _ ->
                    if (mapFragment.isActive) {
                        mapFragment.onItemsLoaded(ArrayList())
                    }
                })
    }

    override fun addItem(newItem: SavedLocation) {
        newItem.reporterToken = getToken()
        newItem.arn = getEndpointArn()
        repository.postPoi(newItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    if (mapFragment.isActive) {
                        mapFragment.onItemAdded(response)
                    }
                }, { error ->
                    if (mapFragment.isActive) {
                        mapFragment.showErrorView(error)
                    }
                })
    }

    override fun getEndpointArn(): String {
        return gershadSettings.endpointArn
    }

    override fun getToken(): String {
        return amazonServiceProvider.cognitoIdentity
    }
}