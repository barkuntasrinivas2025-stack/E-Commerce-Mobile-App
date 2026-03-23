package com.confidencecommerce.domain.usecase

import com.confidencecommerce.domain.model.AppResult
import com.confidencecommerce.domain.model.Product
import com.confidencecommerce.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(
        category: String? = null,
        page: Int = 1
    ): Flow<AppResult<List<Product>>> =
        productRepository.getProducts(category = category, page = page)
}
