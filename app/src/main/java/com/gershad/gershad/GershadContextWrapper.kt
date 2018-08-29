package com.gershad.gershad

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import com.gershad.gershad.dependency.AndroidInternalsModule
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import java.util.*

class GershadContextWrapper(base: Context) : ContextWrapper(base), Injects<Module> {

    val gershadSettings: GershadPreferences by required { preferences }

    init {
        inject(BaseApplication.module(base))
    }

    companion object {

        fun wrap(context: Context): ContextWrapper {
            var context = context
            val config = context.resources.configuration
            val locale = Locale("fa")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                context = context.createConfigurationContext(config)
            } else {
                config.locale = locale
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    config.setLayoutDirection(locale)
                }

                context.resources.updateConfiguration(config, context.resources.displayMetrics)
            }

            Locale.setDefault(locale)

            return GershadContextWrapper(context)
        }
    }
}
