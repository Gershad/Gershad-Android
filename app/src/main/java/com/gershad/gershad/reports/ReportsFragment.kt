package com.gershad.gershad.reports

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.Constants.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOC
import com.gershad.gershad.R
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.extensions.toBounds
import com.gershad.gershad.model.ReportBounds
import com.gershad.gershad.model.ReportGroup
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_reports.*
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required


/**
 * Saved (favourited) locations view.
 */
class ReportsFragment : Fragment(), ReportsContract.View, ReportsContract.Adapter, Injects<Module> {

    private val gershadSettings: GershadPreferences by required { preferences }

    override lateinit var presenter: ReportsContract.Presenter
    private lateinit var adapter: RecyclerView.Adapter<ReportsAdapter.ReportsViewHolder>
    private lateinit var origin: LatLng
    private var reportGroup: ArrayList<ReportGroup> = ArrayList()

    override var isActive: Boolean = false
        get() = isAdded

    companion object {
        fun newInstance() = ReportsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inject(BaseApplication.module(context!!))

        swipe_refresh.isRefreshing = true
        swipe_refresh.setOnRefreshListener {
            retrieveReports()
        }

        recycler_view.layoutManager = LinearLayoutManager(activity!!.applicationContext)

        origin = gershadSettings.homeLocation
        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.lastLocation.addOnCompleteListener {
                if (it.isSuccessful && it.result != null) {
                    origin = LatLng(it.result.latitude, it.result.longitude)
                    retrieveReports()
                } else {
                    retrieveReports()
                }
            }
        } else {
            requestLocationPermissionsIfNeeded()
            retrieveReports()
        }
    }

    private fun retrieveReports() {
        reportGroup.clear()
        adapter = ReportsAdapter(this, reportGroup, origin)
        recycler_view.adapter = adapter
        presenter.loadItems(ReportBounds(origin.toBounds()))
    }

    override fun onItemsLoaded(items: List<ReportGroup>) {
        if (swipe_refresh.isRefreshing) {
            swipe_refresh.isRefreshing = false
        }

        reportGroup.addAll(items as ArrayList<ReportGroup>)
        adapter.notifyDataSetChanged()

        if (reportGroup.isEmpty()) {
            empty_view.visibility = VISIBLE
        }
    }

    override fun onItemsLoadFailed() {
        swipe_refresh.isRefreshing = false
        empty_view.visibility = VISIBLE
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
                    val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
                    if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.lastLocation.addOnCompleteListener {
                            if (it.isSuccessful && it.result != null) {
                                origin = LatLng(it.result.latitude, it.result.longitude)
                                retrieveReports()
                            }
                        }
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
