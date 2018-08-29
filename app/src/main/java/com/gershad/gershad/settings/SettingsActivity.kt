package com.gershad.gershad.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceActivity
import com.gershad.gershad.BaseUpActivity
import com.gershad.gershad.R
import com.gershad.gershad.map.MapActivity
import com.gershad.gershad.replaceFragmentInActivity
import com.gershad.gershad.setupActionBar

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : BaseUpActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_content, findViewById(R.id.contentFrame))

        setupActionBar(R.id.toolbar) {
            setTitle(R.string.settings)
        }

        supportFragmentManager.findFragmentById(R.id.contentFrame) as SettingsFragment? ?:
                SettingsFragment.newInstance().also{
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    override fun onBackPressed() {
        startActivity(MapActivity.newIntent(this))
    }
}
