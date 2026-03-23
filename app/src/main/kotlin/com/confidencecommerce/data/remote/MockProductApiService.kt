package com.confidencecommerce.data.remote

import com.confidencecommerce.data.remote.models.*
import kotlinx.coroutines.delay
import retrofit2.Response

/**
 * MVP mock implementation — replace with real Retrofit service in production.
 * Implements the same interface so DI can swap it transparently.
 *
 * Mock data covers the full Price Comparison Anchor UI, demonstrating all
 * three confidence tiers: HIGH (green), MEDIUM (orange), LOW (red).
 */
class MockProductApiService : ProductApiService {

    override suspend fun getProducts(
        category: String?,
        page: Int,
        pageSize: Int
    ): Response<ApiResponse<ProductListResponseDto>> {
        delay(600) // Simulate network latency
        return Response.success(ApiResponse(
            data = ProductListResponseDto(
                products = MOCK_PRODUCTS.filter {
                    category == null || it.category == category
                }.drop((page - 1) * pageSize).take(pageSize),
                total = MOCK_PRODUCTS.size,
                page = page,
                pageSize = pageSize
            ),
            error = null,
            success = true
        ))
    }

    override suspend fun getProductById(productId: String): Response<ApiResponse<ProductDto>> {
        delay(300)
        val product = MOCK_PRODUCTS.find { it.id == productId }
        return if (product != null)
            Response.success(ApiResponse(data = product, error = null, success = true))
        else
            Response.success(ApiResponse(
                data = null,
                error = ApiError("NOT_FOUND", "Product not found"),
                success = false
            ))
    }

    override suspend fun searchProducts(
        query: String, page: Int
    ): Response<ApiResponse<ProductListResponseDto>> {
        delay(400)
        val results = MOCK_PRODUCTS.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.brand.contains(query, ignoreCase = true)
        }
        return Response.success(ApiResponse(
            data = ProductListResponseDto(results, results.size, page, 20),
            error = null,
            success = true
        ))
    }

    companion object {
        val MOCK_PRODUCTS = listOf(
            ProductDto(
                id = "prod_001",
                title = "Sony WH-1000XM5 Wireless Headphones",
                brand = "Sony",
                price = 24990.0,
                mrp = 29990.0,
                images = listOf(
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800",
                    "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800"
                ),
                description = "Industry-leading noise cancellation with Auto NC Optimizer. Up to 30-hour battery life. Multipoint connection for seamless switching between two devices.",
                rating = 4.6,
                reviewCount = 2847,
                category = "Electronics",
                inStock = true,
                sellerId = "seller_sony_official",
                tags = listOf("noise-cancelling", "wireless", "premium")
            ),
            ProductDto(
                id = "prod_002",
                title = "Nike Air Max 270 Running Shoes",
                brand = "Nike",
                price = 8995.0,
                mrp = 12995.0,
                images = listOf(
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800"
                ),
                description = "The Nike Air Max 270 delivers unrivaled, all-day comfort. The design draws inspiration from the Air Max 180 and Air Max 93.",
                rating = 4.4,
                reviewCount = 1523,
                category = "Footwear",
                inStock = true,
                sellerId = "seller_nike_india",
                tags = listOf("running", "casual", "comfortable")
            ),
            ProductDto(
                id = "prod_003",
                title = "Apple AirPods Pro (2nd Generation)",
                brand = "Apple",
                price = 19900.0,
                mrp = 24900.0,
                images = listOf(
                    "https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=800"
                ),
                description = "Active Noise Cancellation, Transparency mode, Adaptive Audio. H2 chip delivers up to 2x more Active Noise Cancellation than the previous generation.",
                rating = 4.8,
                reviewCount = 4211,
                category = "Electronics",
                inStock = true,
                sellerId = "seller_apple_auth",
                tags = listOf("earbuds", "noise-cancelling", "apple")
            ),
            ProductDto(
                id = "prod_004",
                title = "Prestige Iris 750W Mixer Grinder",
                brand = "Prestige",
                price = 2799.0,
                mrp = 4500.0,
                images = listOf(
                    "https://images.unsplash.com/photo-1585515320310-259814833e62?w=800"
                ),
                description = "3 jars, 3 speed controls with incher, motor overload protection, 750W motor for smooth grinding and mixing.",
                rating = 4.2,
                reviewCount = 892,
                category = "Kitchen",
                inStock = true,
                sellerId = "seller_prestige",
                tags = listOf("kitchen", "mixer", "grinder")
            ),
            ProductDto(
                id = "prod_005",
                title = "Levi's 511 Slim Fit Jeans",
                brand = "Levi's",
                price = 2699.0,
                mrp = 3999.0,
                images = listOf(
                    "https://images.unsplash.com/photo-1542272454315-4c01d7abdf4a?w=800"
                ),
                description = "Slim fit from hip to ankle. Classic 5-pocket styling. Sits below waist. 99% Cotton, 1% Elastane.",
                rating = 4.3,
                reviewCount = 3104,
                category = "Clothing",
                inStock = true,
                sellerId = "seller_levis_india",
                tags = listOf("jeans", "slim", "casual")
            )
        )
    }
}

class MockPriceComparisonApiService : PriceComparisonApiService {

    override suspend fun getPriceComparison(
        productId: String
    ): Response<ApiResponse<PriceComparisonDto>> {
        delay(800) // Price data takes slightly longer
        val data = MOCK_PRICE_DATA[productId] ?: defaultPriceComparison(productId)
        return Response.success(ApiResponse(data = data, error = null, success = true))
    }

    private fun defaultPriceComparison(productId: String) = PriceComparisonDto(
        productId = productId,
        currentPrice = 1299.0,
        marketAveragePrice = 1450.0,
        marketLowPrice = 1199.0,
        marketHighPrice = 1799.0,
        storeCount = 5,
        lastUpdatedSeconds = System.currentTimeMillis() / 1000 - 120,
        confidenceScore = 0.82f,
        pricePercentile = 22,
        competitorPrices = listOf(
            CompetitorPriceDto("Amazon", 1399.0, true),
            CompetitorPriceDto("Flipkart", 1450.0, true),
            CompetitorPriceDto("Croma", 1549.0, true),
            CompetitorPriceDto("Reliance Digital", 1799.0, false)
        )
    )

    companion object {
        private val now get() = System.currentTimeMillis() / 1000

        /** Mock price comparisons covering all three ConfidenceTier states */
        val MOCK_PRICE_DATA = mapOf(
            // prod_001: HIGH confidence — 17% below average (green indicator)
            "prod_001" to PriceComparisonDto(
                productId = "prod_001",
                currentPrice = 24990.0,
                marketAveragePrice = 29500.0,
                marketLowPrice = 24990.0,
                marketHighPrice = 33000.0,
                storeCount = 6,
                lastUpdatedSeconds = now - 90,
                confidenceScore = 0.91f,
                pricePercentile = 8,
                competitorPrices = listOf(
                    CompetitorPriceDto("Amazon",           26999.0, true),
                    CompetitorPriceDto("Flipkart",         27490.0, true),
                    CompetitorPriceDto("Croma",            29990.0, true),
                    CompetitorPriceDto("Reliance Digital", 30999.0, true),
                    CompetitorPriceDto("Sony Centre",      31999.0, true),
                    CompetitorPriceDto("Vijay Sales",      33000.0, false)
                )
            ),
            // prod_002: MEDIUM confidence — 4% below average (orange indicator)
            "prod_002" to PriceComparisonDto(
                productId = "prod_002",
                currentPrice = 8995.0,
                marketAveragePrice = 9350.0,
                marketLowPrice = 8499.0,
                marketHighPrice = 10999.0,
                storeCount = 3,
                lastUpdatedSeconds = now - 3600,
                confidenceScore = 0.58f,
                pricePercentile = 35,
                competitorPrices = listOf(
                    CompetitorPriceDto("Amazon",   8499.0, true),
                    CompetitorPriceDto("Flipkart", 9499.0, true),
                    CompetitorPriceDto("Nike.com", 10999.0, true)
                )
            ),
            // prod_003: HIGH confidence — 20% below average (green, star deal)
            "prod_003" to PriceComparisonDto(
                productId = "prod_003",
                currentPrice = 19900.0,
                marketAveragePrice = 24500.0,
                marketLowPrice = 19900.0,
                marketHighPrice = 26000.0,
                storeCount = 5,
                lastUpdatedSeconds = now - 300,
                confidenceScore = 0.88f,
                pricePercentile = 5,
                competitorPrices = listOf(
                    CompetitorPriceDto("Amazon",           21999.0, true),
                    CompetitorPriceDto("Flipkart",         22490.0, true),
                    CompetitorPriceDto("Croma",            24900.0, true),
                    CompetitorPriceDto("Apple Store",      24900.0, true),
                    CompetitorPriceDto("Reliance Digital", 25999.0, false)
                )
            ),
            // prod_004: LOW confidence — only 2 stores, stale data (red indicator)
            "prod_004" to PriceComparisonDto(
                productId = "prod_004",
                currentPrice = 2799.0,
                marketAveragePrice = 2750.0,
                marketLowPrice = 2499.0,
                marketHighPrice = 3200.0,
                storeCount = 2,
                lastUpdatedSeconds = now - 86400,
                confidenceScore = 0.31f,
                pricePercentile = 55,
                competitorPrices = listOf(
                    CompetitorPriceDto("Amazon",   2499.0, true),
                    CompetitorPriceDto("Flipkart", 2899.0, true)
                )
            )
        )
    }
}
