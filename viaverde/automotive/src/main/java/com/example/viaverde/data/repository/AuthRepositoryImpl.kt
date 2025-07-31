package com.example.viaverde.data.repository

import com.example.viaverde.data.datasource.local.SecurePreferencesDataSource
import com.example.viaverde.data.datasource.remote.AuthApiService
import com.example.viaverde.data.model.AuthToken
import com.example.viaverde.data.model.User
import com.example.viaverde.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository that coordinates between local and remote data sources
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val localDataSource: SecurePreferencesDataSource,
    private val remoteDataSource: AuthApiService
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthToken> {
        return try {
            // Attempt remote login
            val loginResult = remoteDataSource.login(email, password)

            loginResult.fold(
                onSuccess = { authToken ->
                    // Store token locally
                    localDataSource.storeAuthToken(authToken.token)
                    localDataSource.storeUserEmail(email)
                    Result.success(authToken)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            // Clear local authentication data
            localDataSource.clearAuthData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuthToken(): AuthToken? {
        val tokenString = localDataSource.getAuthToken()
        return if (tokenString != null) {
            AuthToken(token = tokenString)
        } else {
            null
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return localDataSource.isLoggedIn()
    }

    override suspend fun getCurrentUser(): User? {
        val email = localDataSource.getUserEmail()
        return if (email != null) {
            User(
                id = email, // Using email as ID for now
                email = email,
                isLoggedIn = true
            )
        } else {
            null
        }
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            localDataSource.storeUserEmail(user.email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAuthData(): Result<Unit> {
        return try {
            localDataSource.clearAuthData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAuthState(): Flow<Boolean> {
        return localDataSource.observeAuthState()
    }
}
