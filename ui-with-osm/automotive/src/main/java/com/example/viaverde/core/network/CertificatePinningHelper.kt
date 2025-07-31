package com.example.viaverde.core.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Helper class for certificate pinning operations
 */
object CertificatePinningHelper {

    private const val TAG = "CertificatePinningHelper"

    /**
     * Extract certificate hashes from a server for development purposes
     */
    fun extractCertificateHashes(hostname: String, port: Int = 443) {
        Log.d(TAG, "extractCertificateHashes: Extracting certificates from $hostname:$port")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://$hostname:$port")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e(TAG, "extractCertificateHashes: Failed to connect to $hostname", e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                Log.d(TAG, "extractCertificateHashes: Successfully connected to $hostname")

                val certificateChain = response.handshake?.peerCertificates
                if (certificateChain != null) {
                    Log.d(TAG, "extractCertificateHashes: Found ${certificateChain.size} certificates")

                    certificateChain.forEachIndexed { index, certificate ->
                        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
                            .digest(certificate.encoded)
                            .joinToString("") { "%02x".format(it) }

                        Log.d(TAG, "Certificate $index SHA256: $sha256")
                    }
                } else {
                    Log.w(TAG, "extractCertificateHashes: No certificate chain found")
                }

                response.close()
            }
        })
    }

    /**
     * Extract certificates from development server
     */
    fun extractDevServerCertificates() {
        Log.d(TAG, "extractDevServerCertificates: Extracting certificates from dev server")
        extractCertificateHashes("dev.a-to-be.com")
    }
}
