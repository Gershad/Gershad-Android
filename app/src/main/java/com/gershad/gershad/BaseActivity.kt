package com.gershad.gershad

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.activity_base)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(GershadContextWrapper.wrap(newBase)))
    }
}