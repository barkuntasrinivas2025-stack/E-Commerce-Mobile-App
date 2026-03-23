package com.confidencecommerce.domain.model

data class Cart(
    val id: String,
    val items: List<CartItem>,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalItems: Int get() = items.sumOf { it.quantity }
    val subtotal: Money get() = Money(items.sumOf { it.lineTotal.amount })
    val isEmpty: Boolean get() = items.isEmpty()
}

data class CartItem(
    val product: Product,
    val quantity: Int,
    val addedAt: Long = System.currentTimeMillis()
) {
    val lineTotal: Money get() = Money(product.price.amount * quantity)
}
