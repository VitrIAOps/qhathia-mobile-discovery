package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.order.OrderRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the order repository implementation to its domain port. Stateless — no scope needed. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OrderDataModule {

    @Binds
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository
}
