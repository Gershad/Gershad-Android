package com.gershad.gershad.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.View
import com.gershad.gershad.BaseActivity
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.R
import com.gershad.gershad.dependency.AndroidInternalsModule
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.locationrequest.LocationRequestActivity
import com.gershad.gershad.map.MapActivity
import kotlinx.android.synthetic.main.activity_onboarding.*
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

class OnboardingActivity : BaseActivity(), ViewPager.OnPageChangeListener, OnboardingFragment.OnFragmentInteractionListener, Injects<Module> {

    private val pager: ViewPager by lazy { findViewById<ViewPager>(R.id.pager) }  // TODO("Check null")
    private val adapter: OnboardingAdapter = OnboardingAdapter(supportFragmentManager)

    private val gershadSettings: GershadPreferences by required { preferences }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_onboarding, findViewById(R.id.contentFrame))

        inject(BaseApplication.module(this))

        pager.adapter = adapter
        indicator.setupWithViewPager(pager)
        pager.addOnPageChangeListener(this)
        pager.currentItem = START

        next.setOnClickListener { _ ->
            val item: Int = pager.currentItem
            when (item) {
                END -> finishOnboarding()
                else -> {
                    pager.setCurrentItem(item - 1, true)
                }
            }
        }

        skip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        gershadSettings.rawPreferences
                .edit()
                .putBoolean(AndroidInternalsModule.ONBOARDING_COMPLETE, true)
                .apply()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            startActivity(LocationRequestActivity.newIntent(this))
        } else {
            startActivity(Intent(this, MapActivity::class.java))
        }

        finish()
    }

    override fun onFragmentInteraction(uri: Uri) {
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        skip.visibility = View.VISIBLE
        next.setText(R.string.next)
        if (position == END) {
            skip.visibility = View.INVISIBLE
            next.setText(R.string.complete)
        }
    }

    companion object {
        private const val END = 0
        private const val START = 4

        fun newIntent(context: Context): Intent {
            return Intent(context, OnboardingActivity::class.java)
        }
    }
}
