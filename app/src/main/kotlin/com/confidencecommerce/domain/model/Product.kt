package com.confidencecommerce.domain.model

/**
 * Pure domain model — no framework dependencies, no serialization annotations.
 * Clean Architecture: domain layer knows nothing about Retrofit, Room, or Compose.
 */
data class Product(
    val id: String,
    val title: String,
    val brand: String,
    val price: Money,
    val mrp: Money,
    val images: List<String>,
    val description: String,
    val rating: Float,
    val reviewCount: Int,
    val category: String,
    val inStock: Boolean,
    val sellerId: String,
    val tags: List<String>
) {
    val discountPercent: Int
        get() = if (mrp.amount > 0)
            ((mrp.amount - price.amount) / mrp.amount * 100).toInt()
        else 0

    val primaryImage: String get() = images.firstOrNull() ?: ""
}

data class Money(
    val amount: Double,
    val currency: String = "INR"
) {
    fun formatted(): String = "₹${"%,.0f".format(amount)}"
}

data class PriceComparison(
    val productId: String,
    val currentPrice: Money,
    val marketAveragePrice: Money,
    val marketLowPrice: Money,
    val marketHighPrice: Money,
    val storeCount: Int,
    val lastUpdatedSeconds: Long,
    val confidenceScore: Float,
    val pricePercentile: Int,
    val competitorPrices: List<CompetitorPrice>
) {
    /** % below or above market average. Negative = cheaper than average. */
    val vsMarketPercent: Int
        get() {
            val avg = marketAveragePrice.amount
            return if (avg > 0)
                ((currentPrice.amount - avg) / avg * 100).toInt()
            else 0
        }

    val isBelowAverage: Boolean get() = vsMarketPercent < 0

    /** Maps confidence score + percentile to a 3-tier label for UI. */
    val confidenceTier: ConfidenceTier
        get() = when {
            confidenceScore >= 0.75f && storeCount >= 4 -> ConfidenceTier.HIGH
            confidenceScore >= 0.45f && storeCount >= 2 -> ConfidenceTier.MEDIUM
            else                                         -> ConfidenceTier.LOW
        }

    /** Human-readable relative time (e.g. "Updated 2m ago") */
    fun updatedAgoLabel(): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - lastUpdatedSeconds
        return when {
            diff < 60    -> "just now"
            diff < 3600  -> "${diff / 60}m ago"
            diff < 86400 -> "${diff / 3600}h ago"
            else         -> "${diff / 86400}d ago"
        }
    }
}

enum class ConfidenceTier { HIGH, MEDIUM, LOW }

data class CompetitorPrice(
    val storeName: String,
    val price: Money,
    val inStock: Boolean
)
