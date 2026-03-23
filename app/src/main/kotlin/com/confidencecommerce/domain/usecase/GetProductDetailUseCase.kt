package com.confidencecommerce.domain.usecase

import com.confidencecommerce.domain.model.AppResult
import com.confidencecommerce.domain.model.Product
import com.confidencecommerce.domain.repository.ProductRepository
import com.confidencecommerce.security.InputValidator
import javax.inject.Inject

/**
 * Use case: fetch product detail with input validation.
 * OWASP M4: Validates product ID before any network/storage call.
 */
class GetProductDetailUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String): AppResult<Product> {
        val validation = InputValidator.validateProductId(productId)
        if (validation is InputValidator.Result.Invalid) {
            return AppResult.Error(validation.reason)
        }
        return productRepository.getProductById(productId)
    }
}
