package com.confidencecommerce.domain.usecase

import com.confidencecommerce.domain.model.AppResult
import com.confidencecommerce.domain.model.Cart
import com.confidencecommerce.domain.repository.CartRepository
import com.confidencecommerce.security.InputValidator
import javax.inject.Inject

class AddToCartUseCase @Inject constructor(
    private val cartRepository: CartRepository
) {
    suspend operator fun invoke(productId: String, quantity: Int): AppResult<Cart> {
        val idValid = InputValidator.validateProductId(productId)
        if (idValid is InputValidator.Result.Invalid)
            return AppResult.Error(idValid.reason)

        val qtyValid = InputValidator.validateQuantity(quantity)
        if (qtyValid is InputValidator.Result.Invalid)
            return AppResult.Error(qtyValid.reason)

        return cartRepository.addToCart(productId, quantity)
    }
}
