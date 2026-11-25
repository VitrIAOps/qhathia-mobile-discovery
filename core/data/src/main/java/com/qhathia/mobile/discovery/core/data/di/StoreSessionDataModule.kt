package com.qhathia.mobile.discovery.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.qhathia.mobile.discovery.core.data.session.DeviceTokenMigration
import com.qhathia.mobile.discovery.core.data.session.KeystoreTokenCipher
import com.qhathia.mobile.discovery.core.data.session.StoreSessionRepositoryImpl
import com.qhathia.mobile.discovery.core.data.session.TokenCipher
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StoreSessionDataModule {

    @Binds
    @Singleton
    abstract fun bindStoreSessionRepository(
        impl: StoreSessionRepositoryImpl,
    ): StoreSessionRepository

    @Binds
    @Singleton
    abstract fun bindTokenCipher(impl: KeystoreTokenCipher): TokenCipher

    companion object {
        /**
         * The single `store_session` Preferences DataStore (same file as before), opened with the
         * migration that keeps the device token encrypted.
         */
        @Provides
        @Singleton
        fun provideStoreSessionDataStore(
            @ApplicationContext context: Context,
            cipher: TokenCipher,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            migrations = listOf(DeviceTokenMigration(cipher)),
            produceFile = { context.preferencesDataStoreFile("store_session") },
        )
    }
}
