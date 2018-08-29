package com.gershad.gershad

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


open class BaseUpActivity(private val layoutId: Int = R.layout.activity_base_up) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(layoutId)

        setupActionBar(R.id.toolbar) {
            title = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                setHomeAsUpIndicator(R.drawable.navigation_arrow)
            } else {
                setHomeAsUpIndicator(R.drawable.navigation_arrow_ltr)
            }

            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(GershadContextWrapper.wrap(newBase)))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                android.R.id.home -> onBackPressed()
                else -> {
                }
            }
        }
        return super.onOptionsItemSelected(item)    }
}