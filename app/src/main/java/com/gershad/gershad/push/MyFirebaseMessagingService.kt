/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gershad.gershad.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.Constants.Companion.PRIMARY_CHANNEL
import com.gershad.gershad.R
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import com.gershad.gershad.map.MapActivity
import com.gershad.gershad.map.MapFragment
import com.gershad.gershad.model.SavedLocation
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

class MyFirebaseMessagingService : FirebaseMessagingService(), Injects<Module> {

    private val gershadSettings: GershadPreferences by required { preferences }

    override fun onCreate() {
        super.onCreate()
        inject(BaseApplication.module(this))
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        try {
            remoteMessage?.let { message ->
                Log.d(TAG, "From: " + message.from)

                if (message.data.isNotEmpty()) {
                    message.data["message"]?.let {
                        sendNotification(it)
                    }

                    if (gershadSettings.reportsNearSaved) {
                        message.data["default"]?.let {
                            val savedLocation = Gson().fromJson(it, SavedLocation::class.java)
                            sendNotification(message.data.hashCode(), getString(R.string.saved_locations), savedLocation.address!!, savedLocation.latitude, savedLocation.longitude)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("FirebaseMessageService", ex.message.orEmpty())
            Crashlytics.logException(ex)
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */

    private fun sendNotification(id: Int, title: String, messageBody: String, latitude: Double, longitude: Double) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra(MapFragment.LATITUDE, latitude)
        intent.putExtra(MapFragment.LONGITUDE, longitude)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(applicationContext, id, intent, PendingIntent.FLAG_ONE_SHOT)

        val notificationBuilder = NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(id, notificationBuilder.build())
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MapActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, messageBody.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT)

        val notificationBuilder = NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(messageBody.hashCode(), notificationBuilder.build())
    }

    companion object {

        private const val TAG = "MyFirebaseMsgService"
    }
}
