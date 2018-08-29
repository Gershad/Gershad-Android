package com.gershad.gershad.locationrequest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gershad.gershad.BaseActivity
import com.gershad.gershad.R
import com.gershad.gershad.replaceFragmentInActivity

class LocationRequestActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))

        supportFragmentManager.findFragmentById(R.id.contentFrame) as LocationRequestFragment? ?:
                LocationRequestFragment.newInstance().also{
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LocationRequestActivity::class.java)
        }
    }
}