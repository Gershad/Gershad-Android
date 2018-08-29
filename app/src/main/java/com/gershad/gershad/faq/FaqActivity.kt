package com.gershad.gershad.faq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gershad.gershad.BaseUpActivity
import com.gershad.gershad.R
import com.gershad.gershad.replaceFragmentInActivity
import com.gershad.gershad.setupActionBar

class FaqActivity : BaseUpActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))

        setupActionBar(R.id.toolbar) {
            setTitle(R.string.help)
        }

        supportFragmentManager.findFragmentById(R.id.contentFrame) as FaqFragment? ?:
                FaqFragment.newInstance().also{
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, FaqActivity::class.java)
        }
    }
}