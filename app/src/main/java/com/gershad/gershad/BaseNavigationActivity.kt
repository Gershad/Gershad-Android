package com.gershad.gershad

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import com.gershad.gershad.about.AboutActivity
import com.gershad.gershad.dependency.AndroidInternalsModule
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.faq.FaqActivity
import com.gershad.gershad.map.MapActivity
import com.gershad.gershad.onboarding.OnboardingActivity
import com.gershad.gershad.reports.ReportsActivity
import com.gershad.gershad.savedlocations.SavedLocationsActivity
import com.gershad.gershad.savedlocationsdeletedialog.UpdateDialogFragment
import com.gershad.gershad.settings.SettingsActivity
import kotlinx.android.synthetic.main.activity_navigation.*
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.util.concurrent.TimeUnit

open class BaseNavigationActivity(private val layoutId: Int) : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, Injects<Module> {

    private val gershadSettings: GershadPreferences by required { preferences }

    /*
     * Lifecycle
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(BaseApplication.module(this))


        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(layoutId)

        if (!gershadSettings.rawPreferences.getBoolean(AndroidInternalsModule.ONBOARDING_COMPLETE, false)) {
            startActivity(Intent(OnboardingActivity.newIntent(this)))
            finish()
            return
        }

        if (gershadSettings.availableVersionCode > BuildConfig.VERSION_CODE && (gershadSettings.updateNotificationDate + TimeUnit.DAYS.toMillis(1) < System.currentTimeMillis())) {
            UpdateDialogFragment.newInstance().show(supportFragmentManager, UpdateDialogFragment::javaClass.name)
        }

        setupActionBar(R.id.toolbar) {
            title = ""
        }

        initializeNavigationDrawer()
    }

    private fun initializeNavigationDrawer() {
        if (nav_view != null) {
            nav_view.setNavigationItemSelectedListener(this)

            nav_view.menu.findItem(R.id.nav_uninstall).isVisible = gershadSettings.uninstall

            nav_view.menu.findItem(R.id.nav_version).title = getString(R.string.version) + " " + BuildConfig.VERSION_NAME

            val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_close, R.string.navigation_drawer_open)
            drawer_layout.addDrawerListener(toggle)
            toggle.syncState()
            supportActionBar!!.show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(GershadContextWrapper.wrap(newBase)))
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (!item.isChecked) {
            when (item.itemId) {
                R.id.nav_main_map -> {
                    startActivity(MapActivity.newIntent(this))
                }
                R.id.nav_reports -> {
                    startActivity(ReportsActivity.newIntent(this))
                }
                R.id.nav_saved_locations -> {
                    startActivity(SavedLocationsActivity.newIntent(this))
                }
                R.id.nav_feedback -> {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:")
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@gershad.com"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.gershad_feedback))
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.gershad_version) + BuildConfig.VERSION_NAME)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, R.string.no_email, Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_about -> {
                    startActivity(AboutActivity.newIntent(this))
                }
                R.id.nav_settings -> {
                    startActivity(SettingsActivity.newIntent(this))
                }
                R.id.nav_help -> {
                    startActivity(FaqActivity.newIntent(this))
                }
                R.id.nav_uninstall -> {
                    val intent = Intent(Intent.ACTION_DELETE)
                    intent.data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                    startActivity(intent)
                }
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val NAVIGATION_MAP = 0
        const val NAVIGATION_REPORTS = 1
        const val NAVIGATION_SAVED = 2
    }
}
