package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.catalog.ProductCatalogRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.ProductCatalogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the product-catalog repository implementation to its domain port. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProductCatalogDataModule {

    @Binds
    @Singleton
    abstract fun bindProductCatalogRepository(impl: ProductCatalogRepositoryImpl): ProductCatalogRepository
}
