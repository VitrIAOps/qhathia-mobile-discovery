package com.qhathia.mobile.discovery.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.qhathia.mobile.discovery.core.data.session.StoreSessionRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Single Preferences DataStore instance for the store session. */
private val Context.storeSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "store_session",
)

@Module
@InstallIn(SingletonComponent::class)
abstract class StoreSessionDataModule {

    @Binds
    @Singleton
    abstract fun bindStoreSessionRepository(
        impl: StoreSessionRepositoryImpl,
    ): StoreSessionRepository

    companion object {
        @Provides
        @Singleton
        fun provideStoreSessionDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.storeSessionDataStore
    }
}
