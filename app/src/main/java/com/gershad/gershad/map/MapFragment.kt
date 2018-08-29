package com.gershad.gershad.map

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import com.crashlytics.android.Crashlytics
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.BuildConfig
import com.gershad.gershad.Constants.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOC
import com.gershad.gershad.R
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.extensions.formatAddress
import com.gershad.gershad.extensions.formatTime
import com.gershad.gershad.extensions.savedLocationIcon
import com.gershad.gershad.extensions.toDistance
import com.gershad.gershad.model.Report
import com.gershad.gershad.model.Report.ReportType
import com.gershad.gershad.model.ReportBounds
import com.gershad.gershad.model.ReportGroup
import com.gershad.gershad.model.SavedLocation
import com.gershad.gershad.reportcomment.ReportCommentActivity
import com.gershad.gershad.reportcomment.ReportCommentFragment.Companion.REPORT
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import com.google.maps.android.clustering.ClusterManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_navigation_floating.*
import kotlinx.android.synthetic.main.bottomsheet_report_summary.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.item_report_detail.*
import kotlinx.android.synthetic.main.popup_comment.view.*
import org.parceler.Parcels
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapFragment : Fragment(), OnMapReadyCallback, MapContract.View, MapContract.Adapter, Injects<Module> {
    override lateinit var presenter: MapContract.Presenter

    private var mGoToLatLng: LatLng? = null
    private var mCurrentLocationLatLng: LatLng? = null
    private var mReportRadius: Circle? = null
    private var isFirstLocationUpdate: Boolean = true

    private lateinit var mMap: GoogleMap
    private lateinit var manager: ClusterManager<ReportGroupClusterItem>
    private lateinit var reportSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var reportAdapter: ReportsAdapter
    private lateinit var autocompleteFragment: SupportPlaceAutocompleteFragment
    private val toolbar: Toolbar by lazy { activity!!.findViewById<View>(R.id.toolbar) as Toolbar }
    private val cameraPosition: TextView by lazy { activity!!.findViewById<View>(R.id.camera_position) as TextView }

    private var targetAddress: String? = null
    private var savedLocationMarker: HashMap<SavedLocation, Marker> = HashMap()

    private var currentBounds: LatLngBounds? = null

    private var reportGroup = ReportGroup()

    private val gershadPreferences by required { preferences }

    /**
     * Lifecycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        inject(BaseApplication.module(context!!))

        reportAdapter = ReportsAdapter(this, ArrayList())

        val bundle = arguments

        if (bundle != null) {
            if (bundle.containsKey(LATITUDE) && bundle.containsKey(LONGITUDE)) {
                mGoToLatLng = LatLng(bundle.getDouble(LATITUDE), bundle.getDouble(LONGITUDE))
            }

            if (bundle.containsKey(REPORT)) {
                reportGroup = Parcels.unwrap(bundle.getParcelable(REPORT))
                mGoToLatLng = LatLng(reportGroup.members[0].latitude, reportGroup.members[0].longitude)
            }
        }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    /*
     * Set up view properties for elements accessed via kotlin extension/synthetic properties
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        autocompleteFragment = place_autocomplete_searchbar as SupportPlaceAutocompleteFragment
        autocompleteFragment.activity!!.findViewById<View>(R.id.place_autocomplete_search_button).visibility = GONE
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

        reportSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottom_sheet_reports))

        report_detail_toolbar.setOnClickListener {
            reportSheetBehavior.state =
                    if (reportSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                        BottomSheetBehavior.STATE_EXPANDED
                    else BottomSheetBehavior.STATE_COLLAPSED
        }

        additonal_details.setOnClickListener {
            val intent = ReportCommentActivity.newIntent(context!!)
            intent.putExtra("REPORT", Parcels.wrap(reportGroup))
            startActivity(intent)
        }

        reportSheetBehavior.isHideable = true
        reportSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        my_location.setOnClickListener {
            if (isMapReady()) {
                findMyLocation()
            }
        }

        report_saved.setOnClickListener { report_saved.toggleVisibility(GONE) }

        bottom_sheet_reports.visibility = View.VISIBLE

        button_action_report.setOnClickListener {
            reportGroup = ReportGroup(address = targetAddress, latitude = mMap.cameraPosition.target.latitude, longitude = mMap.cameraPosition.target.longitude)
            setupReportingLayout()
        }

        reporting_layout.setOnClickListener {
            setUpMapLayout()
        }

        cancel_report_button.setOnClickListener { _ ->
            setUpMapLayout()
        }

        button_report_checkpoint.setOnClickListener {
            report(ReportType.STOP)
        }
        button_report_police.setOnClickListener {
            report(ReportType.GASHT)
        }
        button_report_van.setOnClickListener {
            report(ReportType.VAN)
        }

        options.setOnClickListener {
            setUpPopupWindow(it)
        }
    }

    private fun setUpMapLayout() {
        map_pin.visibility = VISIBLE
        progress_bar.visibility = GONE
        reporting_layout.visibility = GONE
        map_layout.visibility = VISIBLE
        toolbar.visibility = VISIBLE
        reporting_title.text = getString(R.string.select_gasht_and_report)
    }


    private fun setupReportingLayout() {
        reportSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        reporting_layout.visibility = VISIBLE
        toolbar.visibility = GONE
        map_layout.visibility = GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        autocompleteFragment.onActivityResult(requestCode, resultCode, data)
    }

    private fun getIconFor(reportType: ReportType): Int {

        return when (reportType) {
            ReportType.VAN -> {
                R.drawable.report_van
            }
            ReportType.GASHT -> {
                R.drawable.report_police
            }
            ReportType.STOP -> {
                R.drawable.report_stop
            }
            else /* or UNSPECIFIED */ -> {
                R.drawable.logo
            }
        }
    }

    private fun setUpPopupWindow(view: View) {
        val layoutInflater: LayoutInflater = view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = layoutInflater.inflate(R.layout.popup_comment, null)



        val popUpWindow = PopupWindow(popUpView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        popUpView.edit.setOnClickListener {
            val intent = ReportCommentActivity.newIntent(context!!)
            intent.putExtra("REPORT", Parcels.wrap(reportGroup))
            startActivity(intent)
            popUpWindow.dismiss()
        }

        popUpView.delete.setOnClickListener {
            val report = presenter.findMyReportIn(reportGroup)
            report?.comments = null
            presenter.updateItem(report!!)
            popUpWindow.dismiss()
        }

        popUpWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popUpWindow.isOutsideTouchable = true
        popUpWindow.showAsDropDown(view, 0, 0)
    }

    /*
     * BottomSheet Report Dialog as shown when an existing pinpoint ([ReportGroup] location) is clicked.
     * Shows highlights of report history, as well as the option to re-report (verify), share, or delete.
     */
    private fun updateReportBottomSheet(context: Context, reportGroup: ReportGroup) {
        this.reportGroup = reportGroup
        report_details_list.layoutManager = LinearLayoutManager(context)
        report_details_list.isNestedScrollingEnabled = true
        report_details_list.adapter = reportAdapter

        reports.text = String.format(getString(R.string.report_count), reportGroup.members.size)
        val distanceInMeters: Double? = distanceInMetres(reportGroup.position)
        val distanceString = distanceInMeters?.toDistance(context)
        distance.text = distanceString
        timestamp.text = if (reportGroup.lastUpdate != null) formatTime(reportGroup.lastUpdate) else getString(R.string.recent_submission_no_timestamp)
        address.text = reportGroup.members[0].address

        report_type_icon.setImageDrawable(ContextCompat.getDrawable(context, getIconFor(reportGroup.displayType)))
        reportAdapter.updateMembers(reportGroup)

        button_share.setOnClickListener {
            reportSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            val format = NumberFormat.getInstance(Locale.US)
            format.minimumFractionDigits = MAX_FRACTION_DIGITS
            format.maximumFractionDigits = MAX_FRACTION_DIGITS
            val url: String = MOBILE_URL_PREFIX + format.format(reportGroup.position.latitude) + "," + format.format(reportGroup.position.longitude)
            val shareIntentText = getString(R.string.share_report_text).format(url)

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareIntentText)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_this_report)))
        }


        val savedLocation = SavedLocation(latitude = reportGroup.latitude, longitude = reportGroup.longitude, address = reportGroup.members[0].address)
        button_save.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.save, 0, 0)
        if (savedLocationMarker.contains(savedLocation)) {
            button_save.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.saved, 0, 0)
        }

        button_save.setOnClickListener {
            if (savedLocationMarker.contains(savedLocation)) {
                savedLocationMarker.keys.forEach {
                    if (it.equals(savedLocation)) {
                        presenter.deletePoi(it)
                    }
                }
            } else {
                presenter.poiAdded(savedLocation)
            }
        }

        val myExistingReport: Report? = presenter.findMyReportIn(reportGroup)

        additonal_details.visibility = View.GONE
        comments.visibility = View.GONE
        when (myExistingReport) {
            null -> {
                button_verify.isEnabled = if (distanceInMetres(reportGroup.position) == null) false else (distanceInMetres(reportGroup.position)!! <= REPORT_RADIUS_METERS)
                button_verify.visibility = View.VISIBLE
                button_delete.visibility = View.GONE

                button_verify.setOnClickListener {
                    setupReportingLayout()
                }

                reportSheetBehavior.peekHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 277f, context.resources.displayMetrics).toInt()
                bottom_sheet_reports.requestLayout()
                reportSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            }

            else -> {
                button_verify.visibility = View.GONE
                button_delete.visibility = View.VISIBLE
                if (myExistingReport.comments.isNullOrEmpty()) {
                    additonal_details.visibility = View.VISIBLE
                    reportSheetBehavior.peekHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 335f, context.resources.displayMetrics).toInt()
                    bottom_sheet_reports.requestLayout()
                    reportSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    comments.visibility = View.VISIBLE
                    description.text = myExistingReport.comments
                    reportSheetBehavior.peekHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 277f, context.resources.displayMetrics).toInt()
                    bottom_sheet_reports.requestLayout()
                    reportSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }

                button_delete.setOnClickListener {
                    reportSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    presenter.deleteItem(myExistingReport, reportGroup)
                }
            }
        }
    }

    /*
     * Calculate distance from current location to target position.
     * @return  distance in metres, or null if current position unavailable.
     */
    private fun distanceInMetres(target: LatLng): Double? {
        return if (mCurrentLocationLatLng != null)
            SphericalUtil.computeDistanceBetween(mCurrentLocationLatLng, target)
        else null
    }

    private fun View.toggleVisibility(visibility: Int) {
        if (visibility == View.GONE) {
            this.animate()
                    .alpha(0.0f)
                    .setDuration(200)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            this@toggleVisibility.visibility = View.GONE
                        }
                    })
        } else if (visibility == View.VISIBLE) {
            this.animate()
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            this@toggleVisibility.visibility = View.VISIBLE
                        }
                    })
                    .setDuration(200)
                    .alpha(1.0f)
        }
    }

    private fun report(type: ReportType) {
        map_pin.visibility = INVISIBLE
        progress_bar.visibility = VISIBLE
        reporting_title.text = getString(R.string.submitting_report)

        val newReport = Report(latitude = reportGroup.latitude,
                longitude = reportGroup.longitude,
                address = reportGroup.address,
                type = type, reporterToken = presenter.getToken())

        presenter.addItem(newReport)
    }

    /**
     * Map functions
     */
    private fun isMapReady(): Boolean {
        val ready = ::mMap.isInitialized
        if (!ready) Toast.makeText(context, getString(R.string.error_location_undetermined), Toast.LENGTH_SHORT).show()
        return ready
    }

    /**
     * Manipulates the map once available.
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

        mMap.setOnCameraMoveStartedListener {
            reportSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            button_action_report.isClickable = false
            report_saved.toggleVisibility(GONE)
        }

        mMap.setOnMapClickListener {
            reportSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        mMap.setOnCameraIdleListener {
            val cameraBounds = mMap.projection.visibleRegion.latLngBounds
            if (currentBounds == null || !currentBounds!!.contains(cameraBounds.northeast) || !currentBounds!!.contains(cameraBounds.southwest)) {
                reportSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                presenter.loadItems(ReportBounds(cameraBounds))
                currentBounds = cameraBounds
            }

            try {
                val geo = Geocoder(context, Locale("fa"))
                Observable.just(geo.getFromLocation(mMap.cameraPosition.target.latitude, mMap.cameraPosition.target.longitude, 1))
                        .subscribeOn(Schedulers.io())
                        .take(1)
                        .flatMap { it -> Observable.just(it[0]) }
                        .observeOn(AndroidSchedulers.mainThread())
                        .onExceptionResumeNext {  }
                        .subscribe { singleAddress: Address ->
                            targetAddress = singleAddress.formatAddress()
                            cameraPosition.text = if (targetAddress.isNullOrEmpty()) "" else targetAddress
                        }
            } catch (ex: Exception) {
                Crashlytics.logException(ex)
            }

            manager.cluster()

            if (mCurrentLocationLatLng != null) {
                val isInRange = SphericalUtil.computeDistanceBetween(mCurrentLocationLatLng, mMap.cameraPosition.target)<= REPORT_RADIUS_METERS
                button_action_report.isEnabled = isInRange
                button_action_report.isClickable = isInRange
                button_action_report.text = if (isInRange) getString(R.string.report) else getString(R.string.out_of_range)
                map_pin.isEnabled = isInRange
                button_action_report.text = getString(R.string.report)
            } else {
                button_action_report.text = getString(R.string.location_not_available)
            }
        }


        manager = ClusterManager(context, mMap)
        manager.renderer = ReportClusterRenderer(context!!, mMap, manager)
        manager.setOnClusterClickListener {
            reportSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            true
        }

        manager.setOnClusterItemClickListener { item ->
            onClusterItemClick(item)
        }

        mMap.setOnMarkerClickListener(manager)


        // Check for location permissions before showing 'my location'
        if (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            presenter.startLocationListener()
        } else {
            requestLocationPermissionsIfNeeded()
        }

        presenter.loadPoi()

        val home = mGoToLatLng ?: mCurrentLocationLatLng ?: gershadPreferences.homeLocation
        if (isFirstLocationUpdate) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(home, LOCATION_ZOOM_FLOAT)))
        }
        mGoToLatLng?.let { isFirstLocationUpdate = false }
        manager.cluster()
    }

    override fun showErrorView(e: Throwable) {
        if (BuildConfig.DEBUG) Log.e("MapView", "Error -> " + e.message)
    }

    /*
     * If status at location changes (user has or has not reported here)
     */
    private fun onReportSubmissionStatusChange(hasReported: Boolean) {
        if (hasReported) {
            button_verify.visibility = View.GONE
            button_delete.visibility = View.VISIBLE
        } else {
            button_verify.visibility = View.VISIBLE
            button_delete.visibility = View.GONE
        }
    }

    private fun onClusterItemClick(item: ReportGroupClusterItem): Boolean {
        bottom_sheet_reports.visibility = View.VISIBLE
        address.text = item.members[0].address

        updateReportBottomSheet(context!!, item)
        return true
    }

    override fun updateCurrentLocation(loc: Location) {
        val where = LatLng(loc.latitude, loc.longitude)
        mCurrentLocationLatLng = where
        reportGroup.let {
            val distanceInMeters: Double? = distanceInMetres(it.position)
            val distanceString = distanceInMeters?.toDistance(context!!)
            distance.text = distanceString
        }
        
        if (isMapReady()) {

            mReportRadius?.remove()

            mReportRadius = mMap.addCircle(CircleOptions()
                    .center(where)
                    .radius(REPORT_RADIUS_METERS)
                    .fillColor(ContextCompat.getColor(context!!, R.color.colorMapRadiusOverlay))
                    .strokeColor(ContextCompat.getColor(context!!, android.R.color.transparent))
                    .zIndex(1.0f))

            if (isFirstLocationUpdate) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocationLatLng!!, LOCATION_ZOOM_FLOAT))
                isFirstLocationUpdate = false
            }
        }
    }

    /*
     * Intercept 'find my location.'
     * @return false in order to allow default behaviour (camera move) to occur.
     */
    private fun findMyLocation(): Boolean {
        requestLocationPermissionsIfNeeded()

        (mCurrentLocationLatLng?.let { mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, LOCATION_ZOOM_FLOAT)) } ?: Toast.makeText(context, getString(R.string.error_location_undetermined), Toast.LENGTH_SHORT).show())
        return false
    }

    private fun requestLocationPermissionsIfNeeded() {
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity!!,
                        Array(1) { Manifest.permission.ACCESS_FINE_LOCATION },
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOC)

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOC -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isMapReady()) {
                        presenter.startLocationListener()
                        findMyLocation()
                    }
                } else {
                    // No fine maps access; no ability to report.
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onItemsLoaded(items: List<ReportGroup>) {
        if (isMapReady()) {
            manager.clearItems()
            items.forEach {item ->
                manager.addItem(item)
                mGoToLatLng?.let {
                    val format = NumberFormat.getInstance(Locale.US)
                    format.minimumFractionDigits = MAX_FRACTION_DIGITS
                    format.maximumFractionDigits = MAX_FRACTION_DIGITS
                    if (format.format(it.latitude).toDouble() == format.format(item.latitude).toDouble() && format.format(it.longitude).toDouble() == format.format(item.longitude).toDouble()) {
                        onClusterItemClick(item)
                        mGoToLatLng = null
                        isFirstLocationUpdate = false
                    }
                }
            }
            manager.cluster()
        }
    }

    override fun onItemAdded(item: Report) {
        var reportGroupItem = ReportGroup(item)
        if (manager.algorithm.items.contains(reportGroupItem)) {
            manager.algorithm.items.forEach {
                if (it.equals(reportGroupItem)) {
                    manager.removeItem(it)
                    reportGroupItem = it
                    it.members.add(item)
                    it.reportCount = it.members.size
                    manager.addItem(it)

                    reportAdapter.updateMembers(reportGroup)

                    onReportSubmissionStatusChange(hasReported = true)
                }
            }
        } else {
            manager.addItem(reportGroupItem)
        }

        manager.cluster()

        setUpMapLayout()
        onClusterItemClick(reportGroupItem)
    }

    override fun onItemUpdated(item: Report) {
        var reportGroupItem = ReportGroup(item)
        if (manager.algorithm.items.contains(reportGroupItem)) {
            manager.algorithm.items.forEach { reportGroup ->
                if (reportGroup.equals(reportGroupItem)) {
                    reportGroupItem = reportGroup
                    manager.removeItem(reportGroup)
                    reportGroup.members.forEach { report ->
                        if (report.equals(item)) {
                            reportGroup.remove(report)
                            reportGroup.add(item)
                        }
                    }
                    manager.addItem(reportGroup)

                    reportAdapter.updateMembers(reportGroup)
                }
            }
        }

        manager.cluster()

        setUpMapLayout()
        onClusterItemClick(reportGroupItem)
    }

    override fun onItemRemoved(item: Report, group: ReportGroup) {
        if (manager.algorithm.items.contains(group)) {
            manager.algorithm.items.forEach {
                if (it.equals(group)) {
                    if (it.members.size > 1) {
                        manager.removeItem(it)
                        it.members.remove(item)
                        it.reportCount = it.members.size
                        manager.cluster()
                        manager.addItem(it)
                        manager.cluster()

                        onClusterItemClick(group)
                        onReportSubmissionStatusChange(hasReported = false)
                    } else {
                        reportSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        manager.removeItem(group)
                    }
                }
            }
        }
        reportAdapter.updateMembers(reportGroup)

        manager.cluster()
    }

    override fun onPoiLoaded(items: List<SavedLocation>) {
        items.forEach {
            savedLocationMarker[it] = mMap.addMarker(MarkerOptions()
                    .position(LatLng(it.latitude, it.longitude))
                    .icon(ContextCompat.getDrawable(context!!, R.drawable.saved_location)!!.savedLocationIcon()))
        }
    }

    override fun onPoiAdded(item: SavedLocation) {
        button_save.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.saved, 0, 0)
        savedLocationMarker[item] = mMap.addMarker(MarkerOptions()
                .position(LatLng(item.latitude, item.longitude))
                .icon(ContextCompat.getDrawable(context!!, R.drawable.saved_location)!!.savedLocationIcon()))
    }

    override fun onPoiDeleted(item: SavedLocation) {
        button_save.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.save, 0, 0)
        savedLocationMarker[item]?.let {
            it.remove()
            savedLocationMarker.remove(item)
        }
    }

    override var isActive: Boolean = false
        get() = isAdded

    companion object {
        const val LATITUDE = "ExtraLatitude"
        const val LONGITUDE = "ExtraLongitude"

        const val REPORT_RADIUS_METERS: Double = 2000.0
        const val LOCATION_ZOOM_FLOAT: Float = 12.0f
        const val MAX_FRACTION_DIGITS: Int = 4

        const val MOBILE_URL_PREFIX: String = "https://gershad.com/"

        fun newInstance() = MapFragment()
    }
}