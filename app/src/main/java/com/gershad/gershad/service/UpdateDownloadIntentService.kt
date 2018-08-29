package com.gershad.gershad.service

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.FileProvider
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

/**
 * Service for retrieving version configuration file from
 * Amazon S3.
 */
/**
 * Paskoocheh Config Service.
 */
class UpdateDownloadIntentService : IntentService("UpdateIntentService"), Injects<Module> {

    private val amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }

    private val gershadSettings: GershadPreferences by required { preferences }

    private val version: String by lazy { gershadSettings.availableVersionCode.toString() }

    override fun onCreate() {
        super.onCreate()
        inject(BaseApplication.module(this))
    }

    override fun onHandleIntent(intent: Intent?) {
        try {
            val internalFile = File(applicationContext.filesDir.toString() + "/" + String.format("%s_%s.apk", Constants.BASE_APK, version))

            if (internalFile.exists()) {
                installApk()
                return
            }

            val internalTempFile = File(applicationContext.filesDir.toString() + "/" + String.format("%s_%s.apk.temp", Constants.BASE_APK, version))

            clearFiles()

            val observer = amazonServiceProvider.transferUtility.download(Constants.BUCKET, String.format("%s.apk", Constants.BASE_APK), internalTempFile)

            observer.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState) {
                    if (state == TransferState.COMPLETED) {
                        internalTempFile.renameTo(internalFile)
                        installApk()
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    Log.e(bytesCurrent.toString(), bytesTotal.toString())
                }

                override fun onError(id: Int, ex: Exception) {
                    clearFiles()

                    Crashlytics.logException(ex)
                    Log.e("UpdateIntentService", ex.toString())
                }
            })
        } catch (ex: Exception) {
            clearFiles()
            Crashlytics.logException(ex)
        }
    }

    private fun clearFiles() {
        for (file in File(applicationContext.filesDir.toString() + "/").listFiles()) {
            if (file.getName().startsWith(Constants.BASE_APK)) {
                file.delete()
            }
        }
    }

    private fun installApk() {
        val internalFile = File(applicationContext.filesDir.toString() + "/" + String.format("%s_%s.apk", Constants.BASE_APK, version))
        internalFile.setReadable(true, false)

        var internalUri = Uri.fromFile(internalFile)
        if (Build.VERSION.SDK_INT >= 24) {
            internalUri = FileProvider.getUriForFile(applicationContext, Constants.AUTHORITY, internalFile)
        }

        val installIntent = Intent(Intent.ACTION_VIEW)
        installIntent.setDataAndType(internalUri, "application/vnd.android.package-archive")
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(installIntent)
    }
}
