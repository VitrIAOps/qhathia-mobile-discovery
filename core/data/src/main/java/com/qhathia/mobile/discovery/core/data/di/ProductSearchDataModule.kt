package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.search.ProductSearchRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.ProductSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the product-search repository implementation to its domain port. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProductSearchDataModule {

    @Binds
    @Singleton
    abstract fun bindProductSearchRepository(impl: ProductSearchRepositoryImpl): ProductSearchRepository
}
