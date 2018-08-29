package com.gershad.gershad

import android.content.Context
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.sns.AmazonSNSClient
import com.gershad.gershad.Constants.Companion.COGNITO_POOL_ID
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cognito credentials
 */
class AmazonServiceProvider(private val context: Context) {

    val credentialsProvider by lazy { createCredentialsProvider() }

    val amazonS3Provider by lazy { createAmazonS3Client() }

    val amazonSNSProvider by lazy { createAmazonSNSClient() }

    val transferUtility by lazy { createTransferUtlity() }

    val cognitoIdentity by lazy { getCognitoId() }


    private fun createCredentialsProvider() : CognitoCachingCredentialsProvider {
        return CognitoCachingCredentialsProvider(
                context.applicationContext,
                COGNITO_POOL_ID,
                Regions.US_EAST_1,
                clientConfig()
        )
    }

    private fun createAmazonS3Client() : AmazonS3Client {
        val sS3Client = AmazonS3Client(credentialsProvider, clientConfig())
        sS3Client.setRegion(Region.getRegion(Regions.US_EAST_1))
        return sS3Client
    }

    private fun createAmazonSNSClient() : AmazonSNSClient {
        val snsClient = AmazonSNSClient(credentialsProvider, clientConfig())
        snsClient.setRegion(Region.getRegion(Regions.US_EAST_1))
        return snsClient
    }

    private fun createTransferUtlity(): TransferUtility {
        return TransferUtility.builder().s3Client(amazonS3Provider).context(context.applicationContext).build()
    }

    private fun clientConfig(): ClientConfiguration {
        val clientConfiguration = ClientConfiguration()
        clientConfiguration.connectionTimeout = 300000
        clientConfiguration.socketTimeout = 300000
        return clientConfiguration
    }

    private fun getCognitoId(): String {
        return credentialsProvider.cachedIdentityId.orEmpty()
    }
}
