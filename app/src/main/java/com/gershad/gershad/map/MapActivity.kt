package com.gershad.gershad.map

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.gershad.gershad.BaseNavigationActivity
import com.gershad.gershad.R
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.map.MapFragment.Companion.LATITUDE
import com.gershad.gershad.map.MapFragment.Companion.LONGITUDE
import com.gershad.gershad.replaceFragmentInActivity
import com.gershad.gershad.service.UpdateConfigIntentService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_navigation.*
import space.traversal.kapsule.Injects

class MapActivity : BaseNavigationActivity(R.layout.activity_navigation_floating), Injects<Module> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))

        startService(Intent(this, UpdateConfigIntentService::class.java))

        var bundle: Bundle? = intent.extras
        val i = intent
        val action = i.action
        val data = i.dataString
        if (Intent.ACTION_VIEW == action && data != null) {
            try {
                val location = data.substring(data.lastIndexOf("/") + 1)
                val latitude = location.substring(0, location.lastIndexOf(",")).toDouble()
                val longitude = location.substring(location.lastIndexOf(",") + 1).toDouble()
                bundle = Bundle()
                bundle.putDouble(LATITUDE, latitude)
                bundle.putDouble(LONGITUDE, longitude)
            } catch (ex: Exception) {
                Toast.makeText(this, "Incorrect Location Format", LENGTH_SHORT).show()
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.contentFrame) as MapFragment? ?:
                MapFragment.newInstance().also{
                    it.arguments = bundle
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }

        MapPresenter(applicationContext, mapFragment)

        // Check Google APIs
        val playApiChecker: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        playApiChecker.isGooglePlayServicesAvailable(this).also {
            when (it) {
                ConnectionResult.SUCCESS -> {
                    Log.d("GooglePlay", "Great- Play services are up to date.")
                }
                else -> {
                    if (playApiChecker.isUserResolvableError(it)) {
                        playApiChecker.getErrorDialog(this, it, PLAY_SERVICE_UPDATE,
                                { dialogInterface -> dialogInterface.dismiss() })
                    } else {

                        // There's something wrong with Play Services and it can't be easily fixed
                        //TODO("Analytics -> dispatch play services error")
                        Log.e("MainActivity", "Map encountered unfixable error code $it")
                        Snackbar.make(nav_view, getString(R.string.error_google_play_services), Snackbar.LENGTH_INDEFINITE)
                                .setAction(getString(R.string.dismiss), { _ -> run {} }) // Dismiss this message.
                                .show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nav_view.menu.getItem(NAVIGATION_MAP).isChecked = true
    }

    companion object {
        private const val PLAY_SERVICE_UPDATE = 9

        fun newIntent(context: Context): Intent {
            return Intent(context, MapActivity::class.java)
        }
    }
}
