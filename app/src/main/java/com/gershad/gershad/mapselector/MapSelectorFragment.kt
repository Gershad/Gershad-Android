package com.gershad.gershad.mapselector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.Constants.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOC
import com.gershad.gershad.R
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.extensions.formatAddress
import com.gershad.gershad.extensions.savedLocationIcon
import com.gershad.gershad.map.MapFragment.Companion.LOCATION_ZOOM_FLOAT
import com.gershad.gershad.model.SavedLocation
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_navigation_floating.*
import kotlinx.android.synthetic.main.fragment_map_selector.*
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import java.util.*

class MapSelectorFragment : Fragment(), OnMapReadyCallback, MapSelectorContract.View, Injects<Module> {
    override lateinit var presenter: MapSelectorContract.Presenter
    private var currentLocation: LatLng? = null

    private lateinit var mMap: GoogleMap
    private val preferences by required { preferences }
    private var address: String? = null
    private var isFirstLocationUpdate = true
    private lateinit var autocompleteFragment: SupportPlaceAutocompleteFragment
    private val cameraPosition: TextView by lazy { activity!!.findViewById<View>(R.id.camera_position) as TextView }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map_selector, container, false)
        inject(BaseApplication.module(context!!))

        currentLocation = currentLocation ?: preferences.homeLocation

        val mapFragment = childFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        location_saved.alpha = 0.0f
        my_location.setOnClickListener({
            if (isMapReady()) {
                (currentLocation?.let {  mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, LOCATION_ZOOM_FLOAT)) } ?: Toast.makeText(context, getString(R.string.error_location_undetermined), Toast.LENGTH_SHORT).show())
            }
        })

        autocompleteFragment = place_autocomplete_searchbar as SupportPlaceAutocompleteFragment
        autocompleteFragment.activity!!.findViewById<View>(R.id.place_autocomplete_search_button).visibility = View.GONE
        autocompleteFragment.setHint(getString(R.string.search_location_hint))
        autocompleteFragment.view?.findViewById<EditText>(R.id.place_autocomplete_search_input)?.textSize = 15.0f
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                if (isMapReady()) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng, LOCATION_ZOOM_FLOAT))
                }
            }

            override fun onError(status: Status) {
                Log.e(this.javaClass.simpleName, status.toString())
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        autocompleteFragment.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        if (!presenter.isListeningForLocation &&
                (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED)) presenter.startLocationListener()
    }

    override fun onStop() {
        if (presenter.isListeningForLocation) presenter.stopLocationListener() // if is started
        super.onStop()
    }

    override var isActive: Boolean = false
        get() = isAdded

    override fun updateCurrentLocation(loc: Location) {
        currentLocation = LatLng(loc.latitude, loc.longitude)

        if (isMapReady()) {
            if (isFirstLocationUpdate) {
                isFirstLocationUpdate = false

                (currentLocation?.let {  mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, LOCATION_ZOOM_FLOAT)) } ?: Toast.makeText(context, getString(R.string.error_location_undetermined), Toast.LENGTH_SHORT).show())
            }
        }
    }

    override fun showErrorView(e: Throwable) {
        Crashlytics.logException(e)
    }

    private fun isMapReady(): Boolean {
        val ready = ::mMap.isInitialized
        if (!ready) Toast.makeText(context, getString(R.string.error_location_undetermined), Toast.LENGTH_SHORT).show()
        return ready
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isRotateGesturesEnabled = true
        mMap.uiSettings.isTiltGesturesEnabled = false

        mMap.setOnCameraIdleListener {
            try {
                val geo = Geocoder(context, Locale("fa"))
                Observable.just(geo.getFromLocation(mMap.cameraPosition.target.latitude, mMap.cameraPosition.target.longitude, 1))
                        .subscribeOn(Schedulers.io())
                        .take(1)
                        .flatMap { it -> Observable.just(it[0]) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .onExceptionResumeNext {  }
                        .subscribe (
                                { singleAddress: Address ->
                                    address = singleAddress.formatAddress()
                                    cameraPosition.text = if (address.isNullOrEmpty()) "" else address
                                }
                        )
                } catch (ex: Exception) {
                    Crashlytics.logException(ex)
                }
        }

        mMap.setOnCameraMoveStartedListener {
            map_pin.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.pin_active))
            location_saved.animate().alpha(0.0f).setDuration(500).start()
        }

        mMap.setOnMarkerClickListener { true }

        if (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            presenter.startLocationListener() // LocationServices updates.
        } else {
            ActivityCompat.requestPermissions(activity!!,
                    Array(1) { Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSIONS_REQUEST_ACCESS_FINE_LOC)
        }

        presenter.loadItems()

        currentLocation?.let { mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 13.0f)) }

        button_save.setOnClickListener({
            presenter.addItem(SavedLocation(latitude = mMap.cameraPosition.target.latitude, longitude = mMap.cameraPosition.target.longitude, address = address))
        })
    }

    override fun onItemAdded(response: SavedLocation?) {
        mMap.addMarker(MarkerOptions()
                .position(LatLng(mMap.cameraPosition.target.latitude, mMap.cameraPosition.target.longitude))
                .icon(ContextCompat.getDrawable(context!!, R.drawable.saved_location)?.savedLocationIcon()))
        location_saved.animate().alpha(1.0f).setDuration(500).start()
        map_pin.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.saved_location))
    }

    override fun onItemsLoaded(result: List<SavedLocation>?) {
        result?.forEach {
            mMap.addMarker(MarkerOptions()
                    .position(LatLng(it.latitude, it.longitude))
                    .icon(ContextCompat.getDrawable(context!!, R.drawable.saved_location)?.savedLocationIcon()))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOC -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isMapReady()) {
                        currentLocation?.let {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, LOCATION_ZOOM_FLOAT))
                        }
                    } else {
                        // No fine maps access; no ability to report. TODO("Display info to user that they can view but not report").
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        fun newInstance() = MapSelectorFragment()
    }
}