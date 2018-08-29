package com.gershad.gershad

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.support.multidex.MultiDexApplication
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ca.psiphon.PsiphonTunnel
import com.crashlytics.android.Crashlytics
import com.gershad.gershad.Constants.Companion.PRIMARY_CHANNEL
import com.gershad.gershad.dependency.*
import com.gershad.gershad.event.ProxyEvent
import com.gershad.gershad.event.RxBus
import com.gershad.gershad.service.CurrentLocationWork
import io.fabric.sdk.android.Fabric
import org.json.JSONException
import org.json.JSONObject
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required
import space.traversal.kapsule.transitive
import uk.co.chrisjenx.calligraphy.CalligraphyConfig
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Custom [Application] subclass to handle dependency injection, font, and language settings.

 * @inheritDoc
 * @see [Application]
 */
open class BaseApplication : MultiDexApplication(), PsiphonTunnel.HostService, Injects<Module> {
    private val gershadSettings: GershadPreferences by required { preferences }
    private val localHttpProxyPort = AtomicInteger(0)
    private var psiphonTunnel: PsiphonTunnel? = null
    private var amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }
    var localProxy: Proxy? = null
    lateinit var module: Module

    companion object {
        const val SERVICE = "PROXY"
        const val LOCATION_WORK = "LOCATION WORK"
        fun module(context: Context) = (context.applicationContext as BaseApplication).module // Application-wide module composed of children
    }


    override fun onCreate() {
        super.onCreate()
        module = createModule()
        inject(module(context))

        Thread {
            try {
                amazonServiceProvider.credentialsProvider.identityId
            } catch (ex: Exception) {
                Crashlytics.logException(ex)
            }
        }.start()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(getString(R.string.app_name), PRIMARY_CHANNEL, IMPORTANCE_DEFAULT)
            notificationChannel.enableVibration(false)
            notificationChannel.enableLights(false)
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val currentLocationCheck =
                PeriodicWorkRequestBuilder<CurrentLocationWork>(15, TimeUnit.MINUTES)
        val currentLocationCheckWork = currentLocationCheck.build()
        WorkManager.getInstance()?.enqueueUniquePeriodicWork(LOCATION_WORK, ExistingPeriodicWorkPolicy.KEEP, currentLocationCheckWork)

        Fabric.with(this, Crashlytics())

        CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                .setDefaultFontPath(getString(R.string.default_font))
                .setFontAttrId(R.attr.fontPath)
                .build()
        )

        if (gershadSettings.proxy) {
            getPsiphonTunnel()
        }

        setLocale()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setLocale()
    }

    private fun setLocale() {
        val locale = Locale("fa")

        val conf = baseContext.resources.configuration
        updateConfiguration(conf, locale)
        baseContext.resources.updateConfiguration(conf, resources.displayMetrics)

        val systemConf = Resources.getSystem().configuration
        updateConfiguration(systemConf, locale)
        Resources.getSystem().updateConfiguration(conf, resources.displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            baseContext.createConfigurationContext(systemConf)
        }

        Locale.setDefault(locale)
    }

    private fun updateConfiguration(conf: Configuration, locale: Locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            conf.setLocale(locale)
        } else {
            conf.locale = locale
        }
    }

    /*
     * DI - provide modules for dependency tree.
     */
    protected open fun createModule() = Module(
            android = AndroidInternalsModule(this), /* shared prefs, googleApi */
            presenter = MainPresenterModule(this, /* network data sources */
                    net = MainNetModule(this))).transitive() /* local storage */


    /**
     * Return current active psiphon tunnel.
     * @return PsiphonTunnel
     */
    @Synchronized
    fun getPsiphonTunnel(): PsiphonTunnel? {
        if (psiphonTunnel == null) {
            psiphonTunnel = PsiphonTunnel.newPsiphonTunnel(this)
            try {
                psiphonTunnel?.startTunneling("")
            } catch (exception: PsiphonTunnel.Exception) {
                Log.e(BaseApplication::class.java.name, exception.toString())
            }

        }
        return psiphonTunnel
    }


    private fun setHttpProxyPort(port: Int) {
        localHttpProxyPort.set(port)
    }

    override fun onActiveAuthorizationIDs(p0: MutableList<String>?) {
    }

    override fun onStartedWaitingForNetworkConnectivity() {
        Log.d(SERVICE, "waiting for network connectivity...")
    }

    override fun getPsiphonConfig(): String {
        try {
            val config = JSONObject(
                    readInputStreamToString(
                            context.resources.openRawResource(R.raw.psiphon_config)))

            return config.toString()

        } catch (exception: IOException) {
            Log.e(SERVICE, "error loading Psiphon config: " + exception.message)
        } catch (exception: JSONException) {
            Log.e(SERVICE, "error loading Psiphon config: " + exception.message)
        }

        return ""
    }

    override fun onBytesTransferred(p0: Long, p1: Long) {
        Log.d(SERVICE, "bytes sent: " + java.lang.Long.toString(p0))
        Log.d(SERVICE, "bytes received: " + java.lang.Long.toString(p1))
    }

    override fun onUntunneledAddress(p0: String?) {
        Log.d(SERVICE, "untunneled address: $p0")
    }

    override fun getVpnService(): Any {
        return Any()
    }

    override fun onDiagnosticMessage(p0: String?) {
        Log.i(SERVICE, p0)
    }

    override fun onListeningHttpProxyPort(p0: Int) {
        Log.d(SERVICE, "local HTTP proxy listening on port : " + Integer.toString(p0))
        setHttpProxyPort(p0)
    }

    override fun onUpstreamProxyError(p0: String?) {
        Log.d("Serivce", "upstream proxy error: $p0")
    }

    override fun getAppName(): String {
        return "Gershad"
    }

    override fun onClientUpgradeDownloaded(p0: String?) {
        Log.d(SERVICE, "client upgrade downloaded")
    }

    override fun onSocksProxyPortInUse(p0: Int) {
        Log.d(SERVICE, "local SOCKS proxy port in use: " + Integer.toString(p0))
    }

    override fun onHomepage(p0: String?) {
        Log.d(SERVICE, "Homepage: $p0")
    }

    override fun onListeningSocksProxyPort(p0: Int) {
        Log.d(SERVICE, "local SOCKS proxy listening on port: " + Integer.toString(p0))
    }

    override fun onExiting() {
        Log.d(SERVICE, "Exiting")
        psiphonTunnel = null
        localProxy = null
        module = createModule()
    }

    override fun newVpnServiceBuilder(): Any {
        return Any()
    }

    override fun onHttpProxyPortInUse(p0: Int) {
        Log.d(SERVICE, "local HTTP proxy port in use: " + Integer.toString(p0))
    }

    override fun onClientIsLatestVersion() {}

    override fun getContext(): Context {
        return applicationContext
    }

    override fun onConnecting() {
        Log.d(SERVICE, "Connecting...")
    }

    override fun onSplitTunnelRegion(p0: String?) {
        Log.d(SERVICE, "Split tunnel region: $p0")    }

    override fun onConnected() {
        Log.d(SERVICE, "Connected")
        if (localHttpProxyPort.toInt() != 0) {
            localProxy = Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress("127.0.0.1", localHttpProxyPort.toInt())
            )
            module = createModule()
            RxBus.publish(ProxyEvent())
        }
    }

    override fun onClientRegion(p0: String?) {
        Log.d(SERVICE, "Region :$p0")
    }

    override fun onAvailableEgressRegions(p0: MutableList<String>?) {
        if (p0 != null) {
            for (region in p0) {
                Log.d(SERVICE, "available egress region: $region")
            }
        }
    }

    override fun onClientVerificationRequired(p0: String?, p1: Int, p2: Boolean) {}

    @Throws(IOException::class)
    private fun readInputStreamToString(inputStream: InputStream): String {
        return String(readInputStreamToBytes(inputStream), Charset.forName("UTF-8"))
    }

    @Throws(IOException::class)
    private fun readInputStreamToBytes(inputStream: InputStream): ByteArray {
        val outputStream = ByteArrayOutputStream()
        var readCount: Int
        val buffer = ByteArray(16384)
        readCount = inputStream.read(buffer)
        while (readCount != -1) {
            outputStream.write(buffer, 0, readCount)
            readCount = inputStream.read(buffer)
        }
        outputStream.flush()
        inputStream.close()
        return outputStream.toByteArray()
    }
}

/*
 * Note on saving tokens:
 * on install, save token to server via jobschedulder (when wifi or data)
 * - > when true, save a variable to indicate that server has received token
 * - > after that, reporting is allowed bc server has seen ids.
 */
