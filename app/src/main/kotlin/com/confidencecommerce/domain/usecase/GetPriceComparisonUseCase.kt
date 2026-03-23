package com.confidencecommerce.domain.usecase

import com.confidencecommerce.domain.model.AppResult
import com.confidencecommerce.domain.model.PriceComparison
import com.confidencecommerce.domain.repository.ProductRepository
import com.confidencecommerce.security.InputValidator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case: fetch price comparison data that powers the Price Anchor UI.
 * The primary feature solving the 68-82% cart abandonment decision gap.
 */
class GetPriceComparisonUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    /** One-shot fetch for initial load */
    suspend operator fun invoke(productId: String): AppResult<PriceComparison> {
        if (InputValidator.validateProductId(productId) is InputValidator.Result.Invalid)
            return AppResult.Error("Invalid product ID")
        return productRepository.getPriceComparison(productId)
    }

    /** Live observation for auto-refresh (price data can update every few minutes) */
    fun observe(productId: String): Flow<PriceComparison?> =
        productRepository.observePriceComparison(productId)
}
