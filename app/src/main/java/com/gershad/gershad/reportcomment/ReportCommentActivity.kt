package com.gershad.gershad.reportcomment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gershad.gershad.BaseUpActivity
import com.gershad.gershad.R
import com.gershad.gershad.replaceFragmentInActivity
import kotlinx.android.synthetic.main.activity_navigation.*

class ReportCommentActivity : BaseUpActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))

        toolbar.title = getString(R.string.title_report_details)

        val reportsFragment = supportFragmentManager.findFragmentById(R.id.contentFrame) as ReportCommentFragment? ?:
                ReportCommentFragment.newInstance().also{
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }
        reportsFragment.arguments = intent.extras
        ReportCommentPresenter(this, reportsFragment)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ReportCommentActivity::class.java)
        }
    }
}
