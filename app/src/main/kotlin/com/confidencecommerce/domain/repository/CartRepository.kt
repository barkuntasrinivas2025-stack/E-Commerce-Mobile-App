package com.confidencecommerce.domain.repository

import com.confidencecommerce.domain.model.AppResult
import com.confidencecommerce.domain.model.Cart
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<Cart>
    suspend fun addToCart(productId: String, quantity: Int): AppResult<Cart>
    suspend fun updateQuantity(productId: String, quantity: Int): AppResult<Cart>
    suspend fun removeFromCart(productId: String): AppResult<Cart>
    suspend fun clearCart(): AppResult<Unit>
}
