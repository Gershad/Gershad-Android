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

import android.util.Log
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest
import com.crashlytics.android.Crashlytics
import com.gershad.gershad.AmazonServiceProvider
import com.gershad.gershad.BaseApplication
import com.gershad.gershad.Constants
import com.gershad.gershad.dependency.GershadPreferences
import com.gershad.gershad.dependency.Module
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required




class MyFirebaseInstanceIdService : FirebaseInstanceIdService(), Injects<Module> {

    private var amazonServiceProvider: AmazonServiceProvider by required { amazonServiceProvider }

    private var gershadSettings: GershadPreferences by required { preferences }

    private var createNeeded = false

    private var updateNeeded = false

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onTokenRefresh() {
        inject(BaseApplication.module(this))
        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Log.d(TAG, "Refreshed token: " + refreshedToken!!)
        sendRegistrationToServer(refreshedToken)
    }

    /**
     * Persist token to third-party servers.
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        try {
            createNeeded = gershadSettings.endpointArn.isEmpty()
            if (createNeeded) createNeeded = createEndpoint(token)

            try {
                val getEndpointAttributeRequest = GetEndpointAttributesRequest().withEndpointArn(gershadSettings.endpointArn)
                val getEndpointAttributeResponse = amazonServiceProvider.amazonSNSProvider.getEndpointAttributes(getEndpointAttributeRequest)
                updateNeeded = getEndpointAttributeResponse.attributes["Token"] != token || !getEndpointAttributeResponse.attributes["Enabled"].equals("true", ignoreCase = true)
            } catch (ex: Exception) {
                createNeeded = true
                Log.e(this.javaClass.name, ex.message)
                Crashlytics.logException(ex)
            }

            if (createNeeded) createEndpoint(token)

            if (updateNeeded) {
                val attributes = hashMapOf("Token" to token!!, "Enabled" to "true", "CustomUserData" to amazonServiceProvider.cognitoIdentity)
                val setEndpointAttributesRequest = SetEndpointAttributesRequest().withEndpointArn(gershadSettings.endpointArn).withAttributes(attributes)
                amazonServiceProvider.amazonSNSProvider.setEndpointAttributes(setEndpointAttributesRequest)
            }

            try {
                amazonServiceProvider.amazonSNSProvider.subscribe(Constants.TOPIC_ARN, "application", gershadSettings.endpointArn)
            } catch (ex: Exception) {
                Log.e(this.javaClass.name, ex.message)
                Crashlytics.logException(ex)
            }
        } catch (ex: Exception) {
            Log.e(this.javaClass.name, ex.message)
            Crashlytics.logException(ex)
        }
        return
    }

    private fun createEndpoint(token: String?): Boolean {
        try {
            val createPlatformEndpointRequest = CreatePlatformEndpointRequest()
                    .withPlatformApplicationArn(Constants.PLATFORM_APPLICATION_ARN)
                    .withToken(token)
                    .withCustomUserData(amazonServiceProvider.cognitoIdentity)
            val createPlatformEndpointResult = amazonServiceProvider.amazonSNSProvider.createPlatformEndpoint(createPlatformEndpointRequest)
            gershadSettings.endpointArn = createPlatformEndpointResult.endpointArn
        } catch (ex: Exception) {
            Log.e(this.javaClass.name, ex.message)
            Crashlytics.logException(ex)
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "MyFirebaseIIDService"
    }
}
