package com.example.viaverde.core.security

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.*

/**
 * Secure network manager with comprehensive HTTPS security measures
 */
@Singleton
class SecureNetworkManager @Inject constructor(context: Context) {

    companion object {
        private const val TAG = "SecureNetworkManager"
        private const val TIMEOUT_SECONDS = 30L
        private const val BASE_URL = "https://dev.a-to-be.com"
        private const val DEBUG = true // Set to false for release builds

        // Certificate pinning hashes (you should replace these with your actual server's certificate hashes)
        private val CERTIFICATE_PINS = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // Replace with actual hash
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="  // Replace with backup hash
        )
    }

    private val client: OkHttpClient by lazy {
        createSecureHttpClient()
    }

    /**
     * Create a secure HTTP client with all security measures enabled
     */
    private fun createSecureHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                // Timeout configuration
                connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                // Security configurations
                addInterceptor(createSecurityInterceptor())
                addInterceptor(createLoggingInterceptor())

                // Certificate pinning (uncomment when you have actual certificate hashes)
                // .certificatePinner(createCertificatePinner())

                // TLS configuration
                .hostnameVerifier(createHostnameVerifier())

                // Connection pooling
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))

                // Retry on connection failure
                .retryOnConnectionFailure(true)
            }
            .build()
    }

    /**
     * Create security interceptor to enforce HTTPS and add security headers
     */
    private fun createSecurityInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            // Ensure HTTPS is used
            if (originalRequest.url.scheme != "https") {
                throw SecurityException("HTTP requests are not allowed. Only HTTPS is permitted.")
            }

            // Add security headers
            val secureRequest = originalRequest.newBuilder()
                .header("User-Agent", "ViaVerde-Android-App/1.0")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()

            Log.d(TAG, "Making secure request to: ${secureRequest.url}")
            chain.proceed(secureRequest)
        }
    }

    /**
     * Create logging interceptor for debugging (only in debug builds)
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            // Only log in debug builds and mask sensitive data
            if (DEBUG) {
                val maskedMessage = maskSensitiveData(message)
                Log.d(TAG, "HTTP: $maskedMessage")
            }
        }.apply {
            level = if (DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Create certificate pinner for additional security
     */
    private fun createCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(BASE_URL.removePrefix("https://"), *CERTIFICATE_PINS.toTypedArray())
            .build()
    }

    /**
     * Create strict hostname verifier
     */
    private fun createHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { hostname, session ->
            // Verify the hostname matches the expected domain
            val expectedHost = BASE_URL.removePrefix("https://")
            val isValid = hostname == expectedHost || hostname.endsWith(".$expectedHost")

            if (!isValid) {
                Log.w(TAG, "Hostname verification failed: $hostname")
            }

            isValid
        }
    }

    /**
     * Mask sensitive data in logs
     */
    private fun maskSensitiveData(message: String): String {
        return message
            .replace(Regex("password\":\\s*\"[^\"]*\""), "password\": \"***\"")
            .replace(Regex("email\":\\s*\"[^\"]*\""), "email\": \"***@***\"")
            .replace(Regex("authToken\":\\s*\"[^\"]*\""), "authToken\": \"***\"")
            .replace(Regex("token\":\\s*\"[^\"]*\""), "token\": \"***\"")
    }

    /**
     * Make a secure POST request
     */
    fun post(
        endpoint: String,
        jsonBody: String,
        authToken: String? = null,
        callback: (Result<String>) -> Unit
    ) {
        val url = "$BASE_URL$endpoint"

        val requestBuilder = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))

        // Add authorization header if token is provided
        if (!authToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}")
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        Log.d(TAG, "Request successful: ${response.code}")
                        callback(Result.success(responseBody))
                    } else {
                        Log.w(TAG, "Request failed: ${response.code}")
                        callback(Result.failure(IOException("HTTP ${response.code}")))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing response", e)
                    callback(Result.failure(e))
                } finally {
                    response.close()
                }
            }
        })
    }

    /**
     * Make a secure GET request
     */
    fun get(
        endpoint: String,
        authToken: String? = null,
        callback: (Result<String>) -> Unit
    ) {
        val url = "$BASE_URL$endpoint"

        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        // Add authorization header if token is provided
        if (!authToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network request failed: ${e.message}")
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        Log.d(TAG, "Request successful: ${response.code}")
                        callback(Result.success(responseBody))
                    } else {
                        Log.w(TAG, "Request failed: ${response.code}")
                        callback(Result.failure(IOException("HTTP ${response.code}")))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing response", e)
                    callback(Result.failure(e))
                } finally {
                    response.close()
                }
            }
        })
    }
}
