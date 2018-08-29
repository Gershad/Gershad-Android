package com.gershad.gershad.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gershad.gershad.BaseUpActivity
import com.gershad.gershad.R
import com.gershad.gershad.replaceFragmentInActivity
import com.gershad.gershad.setupActionBar

class AboutActivity : BaseUpActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))


        setupActionBar(R.id.toolbar) {
            setTitle(R.string.title_about)
        }

        supportFragmentManager.findFragmentById(R.id.contentFrame) as AboutFragment? ?:
                AboutFragment.newInstance().also{
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }

    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, AboutActivity::class.java)
        }
    }
}
