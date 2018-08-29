package com.gershad.gershad.locationrequest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.gershad.gershad.R
import com.gershad.gershad.map.MapActivity

class LocationRequestFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_location_request, container, false)

        view.findViewById<Button>(R.id.button_request_location).setOnClickListener { _ -> requestLocationPermissions() }
        view.findViewById<Button>(R.id.button_skip_location).setOnClickListener { _ -> startMapActivity() }

        return view
    }

    private fun startMapActivity() {
        startActivity(Intent(activity, MapActivity::class.java))
        activity!!.finish()
    }

    /*
     * Prompt for location permissions
     */
    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else {
            startMapActivity()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            val granted : Boolean = (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)

            if (granted) {
                startMapActivity()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val LOCATION_REQUEST_CODE : Int = 637

        fun newInstance() = LocationRequestFragment()
    }
}