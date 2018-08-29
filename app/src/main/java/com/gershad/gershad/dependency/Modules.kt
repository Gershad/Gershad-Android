package com.gershad.gershad.dependency

import android.content.Context
import android.content.SharedPreferences
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.BuildConfig
import com.gershad.gershad.Constants.Companion.URL
import com.gershad.gershad.network.ApiService
import com.gershad.gershad.network.RepositoryImpl
import com.gershad.gershad.network.SkipDeserialization
import com.gershad.gershad.network.SkipSerialization
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import space.traversal.kapsule.HasModules
import java.util.concurrent.TimeUnit

/**
 *
 * --------------------------------------------------------------------------
 *
 * Implementation of Module Contracts to provide dependencies.
 * To mock certain dependencies for testing, testModuleContracts
 * that implement the module interface are required for all modules except
 * the root Module.
 *
 * --------------------------------------------------------------------------
 *
 */

/**
 * Main Module, application scope.
 */
class Module(android: AppInternalsModule,
             presenter: PresenterModule) :
        AppInternalsModule by android,
        PresenterModule by presenter,
        HasModules {

    override val modules: Set<Any> = setOf(android, presenter)
}

/**
 * Presenter logic, combining submodules and accessing repository
 */
class MainPresenterModule(val context: Context, val net: NetModule) :
        PresenterModule,
        NetModule by net,
        HasModules {

    override val repository = RepositoryImpl(context, apiService = net.apiService)
    override val modules: Set<Any>
        get() = setOf(net, repository, apiService)
}

/**
 * Android Internals : Shared preferences, GoogleApiServices, Wifi, etc.
 */
class AndroidInternalsModule(val context: Context) : AppInternalsModule {

    override val preferences: GershadPreferences
        get() = object : GershadPreferences {
            override val rawPreferences: SharedPreferences
                get() = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE)

            override var availableVersionCode: Int
                get() = this.rawPreferences.getInt(UPDATE_AVAILABLE, 0)
                set(value) = this.rawPreferences.edit().putInt(UPDATE_AVAILABLE, value).apply()

            override var updateNotificationDate: Long
                get() = this.rawPreferences.getLong(UPDATE_NOTIFICATION_DATE, 0)
                set(value) = this.rawPreferences.edit().putLong(UPDATE_NOTIFICATION_DATE, value).apply()

            override var interval: Long
                get() = this.rawPreferences.getLong(INTERVAL, 900000)
                set(value) = this.rawPreferences.edit().putLong(INTERVAL, value).apply()

            override var reportsNearYou: Boolean
                get() = this.rawPreferences.getBoolean(REPORTS_NEAR_YOU, false)
                set(value) = this.rawPreferences.edit().putBoolean(REPORTS_NEAR_YOU, value).apply()

            override var reportsNearSaved: Boolean
                get() = this.rawPreferences.getBoolean(REPORTS_NEAR_SAVED, true)
                set(value) = this.rawPreferences.edit().putBoolean(REPORTS_NEAR_SAVED, value).apply()

            override var updateOnReports: Boolean
                get() = this.rawPreferences.getBoolean(UPDATES_ON_REPORTS, false)
                set(value) = this.rawPreferences.edit().putBoolean(UPDATES_ON_REPORTS, value).apply()

            override var proxy: Boolean
                get() = this.rawPreferences.getBoolean(PROXY, false)
                set(value) = this.rawPreferences.edit().putBoolean(PROXY, value).apply()

            override var uninstall: Boolean
                get() = this.rawPreferences.getBoolean(UNINSTALL, false)
                set(value) = this.rawPreferences.edit().putBoolean(UNINSTALL, value).apply()

            override var homeLocation: LatLng
                get() = getLocation()
                set(value) = this.rawPreferences
                        .edit()
                        .putLong(LATITUDE, value.latitude.toLong())
                        .putLong(LONGITUDE, value.longitude.toLong())
                        .apply()
            override var endpointArn: String
                get() = this.rawPreferences.getString(ENDPOINT_ARN, "")
                set(value) = this.rawPreferences.edit().putString(ENDPOINT_ARN, value).apply()

            override var topicArn: String
                get() = this.rawPreferences.getString(TOPIC_ARN, "")
                set(value) = this.rawPreferences.edit().putString(TOPIC_ARN, value).apply()
        }

    companion object {
        const val LANGUAGE: String = "LanguageKey"
        const val UPDATE_AVAILABLE: String = "UpdateAvailable"
        const val UPDATE_NOTIFICATION_DATE: String = "UpdateNotification"
        const val INTERVAL: String = "IntervalKey"
        const val REPORTS_NEAR_YOU: String = "ReportsNearYou"
        const val REPORTS_NEAR_SAVED: String = "ReportsNearSaved"
        const val UPDATES_ON_REPORTS: String = "UpdatesOnReports"
        const val PROXY: String = "Proxy"
        const val UNINSTALL: String = "Uninstall"
        const val ENDPOINT_ARN: String = "Endpoint_ARN"
        const val TOPIC_ARN: String = "Topic_ARN"

        const val ONBOARDING_COMPLETE: String = "OnboardingCompleteKey"
        val TEHRAN = LatLng(35.715, 51.404)
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CONST_DEFAULT_LATLNG_NOT_FOUND: Long = -1000L
        const val FILENAME = "com.gershad.gershad.app_preferences"
    }

    override val googleApiClient: GoogleApiClient get() = GoogleApiClient.Builder(context).addApi(LocationServices.API).build()

    override val amazonServiceProvider: AmazonServiceProvider get() = AmazonServiceProvider(context)

    /*
     * Return the default location at which to centre the map.
     */
    private fun getLocation() : LatLng {
        val lat: Double = this.preferences.rawPreferences.getLong(LATITUDE, CONST_DEFAULT_LATLNG_NOT_FOUND).toDouble()
        val lng: Double = this.preferences.rawPreferences.getLong(LONGITUDE, CONST_DEFAULT_LATLNG_NOT_FOUND).toDouble()

        return when {
            lat != CONST_DEFAULT_LATLNG_NOT_FOUND.toDouble() && lng != CONST_DEFAULT_LATLNG_NOT_FOUND.toDouble() -> LatLng(lat, lng)
            else -> TEHRAN
        }
    }
}

/**
 * Endpoints
 */
class MainNetModule(val context: Context) : NetModule {

    override val gson: Gson = createGson()

    override val apiService: ApiService
        get() = createRetrofit()

    private fun createRetrofit(): ApiService {

        val interceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().addHeader("Version", BuildConfig.VERSION_NAME).build()
            chain.proceed(request)
        }

        val client: OkHttpClient.Builder = OkHttpClient
                .Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
                })
                .addNetworkInterceptor(interceptor)
                .connectTimeout(15000, TimeUnit.MILLISECONDS)
                .readTimeout(15000, TimeUnit.MILLISECONDS)
                .writeTimeout(15000, TimeUnit.MILLISECONDS)

        if (context.getSharedPreferences("com.gershad.gershad.app_preferences", Context.MODE_PRIVATE).getBoolean(AndroidInternalsModule.PROXY, false)) {
            client.proxy((context.applicationContext as BaseApplication).localProxy)
        }

        val okHttpClient = client.build()

        return Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(URL)
                .client(okHttpClient)
                .build()
                .create(ApiService::class.java)
    }

    /**
     * Gson with custom serializing/deserializing based on annotations
     */
    private fun createGson(): Gson {
        return GsonBuilder()
                .addSerializationExclusionStrategy(object: ExclusionStrategy {
                    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                        return false
                    }

                    override fun shouldSkipField(f: FieldAttributes?): Boolean {
                        return f?.getAnnotation(SkipSerialization::class.java) != null
                    }
                })
                .addDeserializationExclusionStrategy(object: ExclusionStrategy{
                    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                        return false
                    }

                    override fun shouldSkipField(f: FieldAttributes?): Boolean {
                        return f?.getAnnotation(SkipDeserialization::class.java) != null
                    }
                })
                .setPrettyPrinting()
                .create()

    }

}
