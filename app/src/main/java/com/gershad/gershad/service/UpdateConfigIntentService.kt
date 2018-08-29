package com.gershad.gershad.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.crashlytics.android.Crashlytics
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.Constants
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import java.io.File
import java.io.InputStream

/**
 * Service for retrieving version configuration file from
 * Amazon S3.
 */
class UpdateConfigIntentService : IntentService("UpdateConfigIntentService"), Injects<Module> {

    private val amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }

    private val gershadSettings: GershadPreferences by required { preferences }

    override fun onCreate() {
        super.onCreate()
        inject(BaseApplication.module(this))
    }

    override fun onHandleIntent(intent: Intent?) {
        try {
            val internalFile = File(applicationContext.filesDir.toString() + "/" + Constants.VERSION_FILE)
            amazonServiceProvider.transferUtility.download(Constants.BUCKET, Constants.VERSION_FILE, internalFile, object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState) {
                    if (state == TransferState.COMPLETED) {
                        try {
                            val inputStream: InputStream = internalFile.inputStream()
                            val inputString = inputStream.bufferedReader().use { it.readText() }
                            gershadSettings.availableVersionCode = inputString.toInt()
                        } catch (ex: Exception) {
                            handleException(ex)
                        }
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                }

                override fun onError(id: Int, ex: Exception) {
                    handleException(ex)
                }
            })
        } catch (ex: Exception) {
            handleException(ex)
        }

    }

    private fun handleException(ex: Exception) {
        Log.e("UpdateConfigService", ex.toString())
        Crashlytics.logException(ex)
    }
}
