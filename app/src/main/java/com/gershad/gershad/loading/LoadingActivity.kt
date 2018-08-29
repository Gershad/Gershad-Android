package com.gershad.gershad.loading

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gershad.gershad.BaseActivity
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.R
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.event.ProxyEvent
import com.gershad.gershad.event.RxBus
import com.gershad.gershad.map.MapActivity
import com.gershad.gershad.replaceFragmentInActivity
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

class LoadingActivity : BaseActivity(), Injects<Module> {

    private val gershadSettings: GershadPreferences by required { preferences }

    private var started: Boolean = false

    private val regularStartDelay: Long = 1000
    private val proxyStartDelay: Long = 10000


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))
        inject(BaseApplication.module(this))

        supportFragmentManager.findFragmentById(R.id.contentFrame) as LoadingFragment? ?:
                LoadingFragment.newInstance().also {
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }

        RxBus.listen(ProxyEvent::class.java).subscribe({
            startActivity()
        })

        if (gershadSettings.proxy) {
            delayStart(proxyStartDelay)
        } else {
            delayStart(regularStartDelay)
        }
    }

    private fun delayStart(time: Long) {
        Thread {
            Thread.sleep(time)
            startActivity()
        }.start()
    }

    private fun startActivity() {
        if (started.not()) {
            started = true
            val intent = Intent(this@LoadingActivity, MapActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LoadingActivity::class.java)
        }
    }
}