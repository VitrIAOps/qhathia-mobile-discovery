package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.session.CustomerSessionRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the in-memory customer-session signal; the implementation itself is `@Singleton`. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CustomerSessionDataModule {

    @Binds
    abstract fun bindCustomerSessionRepository(impl: CustomerSessionRepositoryImpl): CustomerSessionRepository
}
