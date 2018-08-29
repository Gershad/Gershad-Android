package com.gershad.gershad.mapselector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gershad.gershad.BaseUpActivity
import com.gershad.gershad.R
import com.gershad.gershad.replaceFragmentInActivity
import com.gershad.gershad.setupActionBar

class MapSelectorActivity : BaseUpActivity(R.layout.activity_base_up_search) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))


        setupActionBar(R.id.toolbar) {
            setTitle(R.string.title_save_location)
        }

        val mapSelectorFragment = supportFragmentManager.findFragmentById(R.id.contentFrame) as MapSelectorFragment? ?:
        MapSelectorFragment.newInstance().also{
            replaceFragmentInActivity(it, R.id.contentFrame)
        }

        MapSelectorPresenter(applicationContext, mapSelectorFragment)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MapSelectorActivity::class.java)
        }
    }
}
