package com.confidencecommerce.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for the Price Comparison Anchor feature.
 * The confidence interval data that powers the decision-confidence UI.
 */
@JsonClass(generateAdapter = true)
data class PriceComparisonDto(
    @Json(name = "product_id")           val productId: String,
    @Json(name = "current_price")        val currentPrice: Double,
    @Json(name = "market_average_price") val marketAveragePrice: Double,
    @Json(name = "market_low_price")     val marketLowPrice: Double,
    @Json(name = "market_high_price")    val marketHighPrice: Double,
    @Json(name = "store_count")          val storeCount: Int,
    @Json(name = "last_updated_seconds") val lastUpdatedSeconds: Long,
    @Json(name = "confidence_score")     val confidenceScore: Float,  // 0.0 - 1.0
    @Json(name = "price_percentile")     val pricePercentile: Int,    // 0-100 (0 = cheapest)
    @Json(name = "competitor_prices")    val competitorPrices: List<CompetitorPriceDto>
)

@JsonClass(generateAdapter = true)
data class CompetitorPriceDto(
    @Json(name = "store_name")  val storeName: String,
    @Json(name = "price")       val price: Double,
    @Json(name = "url")         val url: String? = null,
    @Json(name = "in_stock")    val inStock: Boolean
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "data")    val data: T?,
    @Json(name = "error")   val error: ApiError?,
    @Json(name = "success") val success: Boolean
)

@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "code")    val code: String,
    @Json(name = "message") val message: String
)
