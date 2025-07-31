package com.example.viaverde.data.datasource.remote

import com.example.viaverde.core.security.SecureNetworkManager
import com.example.viaverde.data.model.AuthToken
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Remote API service for authentication operations
 */
@Singleton
class AuthApiService @Inject constructor(
    private val networkManager: SecureNetworkManager
) {

    companion object {
        private const val LOGIN_ENDPOINT = "/mtolling/services/mtolling/login"
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<AuthToken> {
        return suspendCancellableCoroutine { continuation ->
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()

            networkManager.post(LOGIN_ENDPOINT, jsonBody) { result ->
                result.fold(
                    onSuccess = { responseBody ->
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val token = extractTokenFromResponse(jsonResponse)

                            if (token != null) {
                                continuation.resume(Result.success(AuthToken(token = token)))
                            } else {
                                continuation.resume(Result.failure(Exception("No token in response")))
                            }
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    },
                    onFailure = { exception ->
                        continuation.resume(Result.failure(exception))
                    }
                )
            }
        }
    }

    /**
     * Extract token from API response
     */
    private fun extractTokenFromResponse(jsonResponse: JSONObject): String? {
        return when {
            jsonResponse.has("authToken") -> jsonResponse.getString("authToken")
            jsonResponse.has("token") -> jsonResponse.getString("token")
            jsonResponse.has("access_token") -> jsonResponse.getString("access_token")
            else -> null
        }
    }
}
