package com.confidencecommerce.di

import com.confidencecommerce.data.repository.CartRepositoryImpl
import com.confidencecommerce.data.repository.ProductRepositoryImpl
import com.confidencecommerce.domain.repository.CartRepository
import com.confidencecommerce.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: CartRepositoryImpl): CartRepository
}
