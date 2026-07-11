package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.share.CartShareRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the cart-share repository implementation to its domain port. Stateless — no scope needed. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CartShareDataModule {

    @Binds
    abstract fun bindCartShareRepository(impl: CartShareRepositoryImpl): CartShareRepository
}
