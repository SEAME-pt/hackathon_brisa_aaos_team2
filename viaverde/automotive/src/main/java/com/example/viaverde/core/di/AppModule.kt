package com.example.viaverde.core.di

import android.content.Context
import com.example.viaverde.core.security.SecureStorageManager
import com.example.viaverde.core.security.SecureNetworkManager
import com.example.viaverde.data.datasource.local.SecurePreferencesDataSource
import com.example.viaverde.data.datasource.remote.AuthApiService
import com.example.viaverde.data.datasource.remote.LocationApiService
import com.example.viaverde.data.datasource.remote.TollApiService
import com.example.viaverde.data.repository.AuthRepositoryImpl
import com.example.viaverde.data.repository.LocationRepositoryImpl
import com.example.viaverde.data.repository.TollRepositoryImpl
import com.example.viaverde.domain.repository.AuthRepository
import com.example.viaverde.domain.repository.LocationRepository
import com.example.viaverde.domain.repository.TollRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Main application module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provide SecureStorageManager
     */
    @Provides
    @Singleton
    fun provideSecureStorageManager(
        @ApplicationContext context: Context
    ): SecureStorageManager {
        return SecureStorageManager(context)
    }



    /**
     * Provide SecurePreferencesDataSource
     */
    @Provides
    @Singleton
    fun provideSecurePreferencesDataSource(
        secureStorage: SecureStorageManager
    ): SecurePreferencesDataSource {
        return SecurePreferencesDataSource(secureStorage)
    }

    /**
     * Provide AuthApiService
     */
    @Provides
    @Singleton
    fun provideAuthApiService(
        networkManager: SecureNetworkManager
    ): AuthApiService {
        return AuthApiService(networkManager)
    }

    /**
     * Provide LocationApiService
     */
    @Provides
    @Singleton
    fun provideLocationApiService(
        networkManager: SecureNetworkManager
    ): LocationApiService {
        return LocationApiService(networkManager)
    }

    /**
     * Provide AuthRepository implementation
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        localDataSource: SecurePreferencesDataSource,
        remoteDataSource: AuthApiService
    ): AuthRepository {
        return AuthRepositoryImpl(localDataSource, remoteDataSource)
    }

    /**
     * Provide LocationRepository implementation
     */
    @Provides
    @Singleton
    fun provideLocationRepository(
        localDataSource: SecurePreferencesDataSource,
        remoteDataSource: LocationApiService
    ): LocationRepository {
        return LocationRepositoryImpl(localDataSource, remoteDataSource)
    }

    /**
     * Provide TollApiService
     */
    @Provides
    @Singleton
    fun provideTollApiService(
        networkManager: SecureNetworkManager
    ): TollApiService {
        return TollApiService(networkManager)
    }

    /**
     * Provide TollRepository implementation
     */
    @Provides
    @Singleton
    fun provideTollRepository(
        remoteDataSource: TollApiService
    ): TollRepository {
        return TollRepositoryImpl(remoteDataSource)
    }
}
