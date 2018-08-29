package com.gershad.gershad.map

import android.content.Context
import android.location.Location
import android.util.Log
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.google.GoogleApiClientContract
import com.gershad.gershad.google.GoogleApiClientPresenter
import com.gershad.gershad.model.*
import com.google.android.gms.maps.model.LatLngBounds
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import java.io.IOException

/**
 * Presenter that loads [ReportGroup] items. This is the main presenter in charge of the reports map.
 */
class MapPresenter(private val context: Context, private val mapFragment: MapContract.View) : MapContract.Presenter,
        GoogleApiClientContract.ApiCallbackSubscriber,
        GoogleApiClientContract.Presenter,
        Injects<Module> {

    private val repository by required { repository }
    private val gershadSettings by required { preferences }
    private val apiClient by lazy { createMapClient() }
    private var amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }

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

    /*
     * @return Report if user contributed to report group, or null otherwise.
     */
    override fun findMyReportIn(reportGroup: ReportGroup) : Report? {
        return reportGroup.members.find { it.reporterToken == getToken() }
    }

    override fun getToken() : String {
        return amazonServiceProvider.cognitoIdentity
    }

    override fun getEndpointArn(): String {
        return gershadSettings.endpointArn
    }

    /**
     * Retrieve reports in given [ReportBounds] asynchronously, and pass to [MapFragment]
     * implementer for display.
     */
    override fun loadItems(bounds: ReportBounds) {
        repository.getReports(bounds)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    if (mapFragment.isActive) {
                        mapFragment.onItemsLoaded(result)
                    }
                }, { error ->
                    if (mapFragment.isActive) {
                        mapFragment.showErrorView(error)
                    }
                })
    }

    /**
     * Post new item.
     */
    override fun addItem(newItem: Report) {
            repository.postReport(newItem)
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

    override fun updateItem(newItem: Report) {
            repository.updateReport(newItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        if (mapFragment.isActive) {
                            mapFragment.onItemUpdated(response)
                        }
                    }, { error ->
                        if (mapFragment.isActive) {
                            mapFragment.showErrorView(error)
                        }
                    })
    }

    override fun deleteItem(item: Report, group: ReportGroup) {
            repository.deleteReport(item)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        if (response.isSuccessful) {
                            if (mapFragment.isActive) {
                                mapFragment.onItemRemoved(item, group)
                            }
                        } else {
                            if (mapFragment.isActive) {
                                mapFragment.showErrorView(IOException("Could not delete report"))
                            }
                        }
                    }, { error ->
                        if (mapFragment.isActive) {
                            mapFragment.showErrorView(error)
                        }
                    })
    }

    override fun loadPoi() {
        repository.getPoi(getToken())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    if (mapFragment.isActive) {
                        mapFragment.onPoiLoaded(result)
                    }
                }, { error ->
                    if (mapFragment.isActive) {
                        mapFragment.showErrorView(error)
                    }
                })
    }

    override fun deletePoi(item: SavedLocation) {
        item.reporterToken = getToken()
        repository.deletePoi(DeleteRequest(item))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (mapFragment.isActive) {
                        mapFragment.onPoiDeleted(item)
                    }
                }, { error ->
                    if (mapFragment.isActive) {
                        mapFragment.showErrorView(error)
                    }
                })
    }

    override fun poiAdded(item: SavedLocation) {
        item.reporterToken = getToken()
        item.arn = getEndpointArn()
        repository.postPoi(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    if (mapFragment.isActive) {
                        mapFragment.onPoiAdded(result)
                    }
                }, { error ->
                    if (mapFragment.isActive) {
                        mapFragment.showErrorView(error)
                    }
                })
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

    @Deprecated("Zoom-to-view details no longer in use")
    fun loadDetailedView(bounds: LatLngBounds) {
        repository.getReports(ReportBounds(bounds))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { response ->
                    Log.d("ReportPresenter", "Detail response of size" + response.size)
                }, { _ ->

                } )
    }
}