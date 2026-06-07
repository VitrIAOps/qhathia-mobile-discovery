package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.detail.ProductDetailRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the product-detail repository implementation to its domain port. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProductDetailDataModule {

    @Binds
    @Singleton
    abstract fun bindProductDetailRepository(impl: ProductDetailRepositoryImpl): ProductDetailRepository
}
