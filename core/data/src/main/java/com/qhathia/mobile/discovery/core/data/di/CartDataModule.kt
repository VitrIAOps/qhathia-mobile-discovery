package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.cart.CartPricingRepositoryImpl
import com.qhathia.mobile.discovery.core.data.cart.CartRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.CartPricingRepository
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the cart repository. `@Singleton` so the in-memory basket is shared across every screen for
 * the whole session.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CartDataModule {

    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds
    @Singleton
    abstract fun bindCartPricingRepository(impl: CartPricingRepositoryImpl): CartPricingRepository
}
