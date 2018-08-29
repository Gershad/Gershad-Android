package com.gershad.gershad.installreceiver

import com.gershad.gershad.Constants.Companion.BASE_APK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crashlytics.android.Crashlytics
import java.io.File

class InstallListener : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        try {
            for (file in File(context.filesDir.toString() + "/").listFiles()) {
                if (file.name.startsWith(BASE_APK)) {
                    file.delete()
                }
            }
        } catch (ex: Exception) {
            Crashlytics.logException(ex)
        }
    }
}