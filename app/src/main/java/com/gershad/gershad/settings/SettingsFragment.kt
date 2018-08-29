package com.gershad.gershad.settings

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.R
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import kotlinx.android.synthetic.main.fragment_settings.*
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required




class SettingsFragment : Fragment(), Injects<Module> {

    private val settings : GershadPreferences by required { preferences }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        inject(BaseApplication.module(context!!))

        reports_near_you.isChecked = (settings.reportsNearYou)
        reports_near_you.setOnCheckedChangeListener(
                { _ : CompoundButton, enabled: Boolean ->
                    settings.reportsNearYou = enabled
                }
        )

        reports_near_saved.isChecked = (settings.reportsNearSaved)
        reports_near_saved.setOnCheckedChangeListener(
                { _: CompoundButton, enabled: Boolean ->
                    settings.reportsNearSaved = enabled
                }
        )

        uninstall.isChecked = (settings.uninstall)
        uninstall.setOnCheckedChangeListener(
                { _: CompoundButton, enabled: Boolean ->
                    settings.uninstall = enabled
                }
        )

        proxy.isChecked = (settings.proxy)
        proxy.setOnCheckedChangeListener(
                { _: CompoundButton, enabled: Boolean ->
                    settings.proxy = enabled
                    val psiphonTunnel = (context!!.applicationContext as BaseApplication).getPsiphonTunnel()
                    if (!enabled) {
                        psiphonTunnel?.stop()
                    }
                }
        )
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}