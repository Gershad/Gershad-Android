package com.gershad.gershad.reports

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gershad.gershad.BaseNavigationActivity
import com.gershad.gershad.R
import com.gershad.gershad.replaceFragmentInActivity
import kotlinx.android.synthetic.main.activity_navigation.*

class ReportsActivity : BaseNavigationActivity(R.layout.activity_navigation) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))

        toolbar.title = getString(R.string.title_reports)

        val reportsFragment = supportFragmentManager.findFragmentById(R.id.contentFrame) as ReportsFragment? ?:
                ReportsFragment.newInstance().also{
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }

        ReportsPresenter(this, reportsFragment)
    }

    override fun onResume() {
        super.onResume()
        nav_view.menu.getItem(NAVIGATION_REPORTS).isChecked = true
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ReportsActivity::class.java)
        }
    }
}
