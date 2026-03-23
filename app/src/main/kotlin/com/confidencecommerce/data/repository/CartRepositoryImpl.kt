package com.confidencecommerce.data.repository

import com.confidencecommerce.domain.model.*
import com.confidencecommerce.domain.repository.CartRepository
import com.confidencecommerce.domain.repository.ProductRepository
import com.confidencecommerce.security.SecureLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cart for MVP. Production implementation would persist
 * to encrypted Room database and sync with backend.
 *
 * OWASP M9: In production, cart data (without card details) stored
 * in encrypted Room DB, never in cleartext SharedPreferences.
 */
@Singleton
class CartRepositoryImpl @Inject constructor(
    private val productRepository: ProductRepository
) : CartRepository {

    companion object { private const val TAG = "CartRepo" }

    private val _cart = MutableStateFlow(
        Cart(id = java.util.UUID.randomUUID().toString(), items = emptyList())
    )

    override fun observeCart(): Flow<Cart> = _cart.asStateFlow()

    override suspend fun addToCart(productId: String, quantity: Int): AppResult<Cart> {
        return try {
            val productResult = productRepository.getProductById(productId)
            when (productResult) {
                is AppResult.Success -> {
                    _cart.update { currentCart ->
                        val existingItem = currentCart.items.find { it.product.id == productId }
                        val newItems = if (existingItem != null) {
                            currentCart.items.map {
                                if (it.product.id == productId)
                                    it.copy(quantity = (it.quantity + quantity).coerceAtMost(99))
                                else it
                            }
                        } else {
                            currentCart.items + CartItem(product = productResult.data, quantity = quantity)
                        }
                        currentCart.copy(items = newItems)
                    }
                    AppResult.Success(_cart.value)
                }
                is AppResult.Error -> productResult
                AppResult.Loading  -> AppResult.Error("Loading")
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "addToCart failed", e)
            AppResult.Error("Failed to add item to cart", cause = e)
        }
    }

    override suspend fun updateQuantity(productId: String, quantity: Int): AppResult<Cart> {
        _cart.update { cart ->
            cart.copy(items = cart.items.map {
                if (it.product.id == productId) it.copy(quantity = quantity) else it
            }.filter { it.quantity > 0 })
        }
        return AppResult.Success(_cart.value)
    }

    override suspend fun removeFromCart(productId: String): AppResult<Cart> {
        _cart.update { cart ->
            cart.copy(items = cart.items.filter { it.product.id != productId })
        }
        return AppResult.Success(_cart.value)
    }

    override suspend fun clearCart(): AppResult<Unit> {
        _cart.update { it.copy(items = emptyList()) }
        return AppResult.Success(Unit)
    }
}
