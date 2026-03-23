package com.confidencecommerce.domain.repository

import com.confidencecommerce.domain.model.AppResult
import com.confidencecommerce.domain.model.PriceComparison
import com.confidencecommerce.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract — domain layer owns this interface.
 * Data layer provides the implementation. This inversion keeps domain pure.
 */
interface ProductRepository {

    /**
     * Returns a Flow of product list with pagination.
     * Flow emits Loading → Success or Error.
     */
    fun getProducts(
        category: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Flow<AppResult<List<Product>>>

    suspend fun getProductById(productId: String): AppResult<Product>

    fun searchProducts(query: String): Flow<AppResult<List<Product>>>

    /**
     * Fetches real-time price comparison data for the Price Anchor UI.
     * Core feature — drives the decision-confidence gap reduction.
     */
    suspend fun getPriceComparison(productId: String): AppResult<PriceComparison>

    /** Returns cached price comparison without network call. */
    fun observePriceComparison(productId: String): Flow<PriceComparison?>
}
