package com.confidencecommerce.data.repository

import com.confidencecommerce.data.local.PriceComparisonCache
import com.confidencecommerce.data.remote.MockPriceComparisonApiService
import com.confidencecommerce.data.remote.ProductApiService
import com.confidencecommerce.data.remote.PriceComparisonApiService
import com.confidencecommerce.data.remote.models.*
import com.confidencecommerce.domain.model.*
import com.confidencecommerce.domain.repository.ProductRepository
import com.confidencecommerce.security.SecureLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation — bridges data sources to the domain layer.
 * Maps DTOs → domain models. All network exceptions are caught and
 * converted to AppResult.Error so ViewModels never deal with raw exceptions.
 *
 * OWASP M3: All network calls go through the OkHttp client configured
 * with SecurityHeadersInterceptor and certificate pinning.
 */
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productApi: ProductApiService,
    private val priceComparisonApi: PriceComparisonApiService,
    private val priceComparisonCache: PriceComparisonCache
) : ProductRepository {

    companion object { private const val TAG = "ProductRepo" }

    override fun getProducts(
        category: String?,
        page: Int,
        pageSize: Int
    ): Flow<AppResult<List<Product>>> = flow {
        emit(AppResult.Loading)
        try {
            val response = productApi.getProducts(category, page, pageSize)
            if (response.isSuccessful) {
                val body = response.body()
                val products = body?.data?.products?.map { it.toDomain() }
                if (products != null) {
                    emit(AppResult.Success(products))
                } else {
                    emit(AppResult.Error(body?.error?.message ?: "Unknown error", response.code()))
                }
            } else {
                emit(AppResult.Error("Server error: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "getProducts failed", e)
            emit(AppResult.Error("Network error. Please check your connection.", cause = e))
        }
    }

    override suspend fun getProductById(productId: String): AppResult<Product> {
        return try {
            val response = productApi.getProductById(productId)
            if (response.isSuccessful) {
                val product = response.body()?.data?.toDomain()
                if (product != null) AppResult.Success(product)
                else AppResult.Error("Product not found", 404)
            } else {
                AppResult.Error("Failed to load product", response.code())
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "getProductById failed", e)
            AppResult.Error("Network error", cause = e)
        }
    }

    override fun searchProducts(query: String): Flow<AppResult<List<Product>>> = flow {
        emit(AppResult.Loading)
        try {
            val response = productApi.searchProducts(query)
            if (response.isSuccessful) {
                val products = response.body()?.data?.products?.map { it.toDomain() } ?: emptyList()
                emit(AppResult.Success(products))
            } else {
                emit(AppResult.Error("Search failed", response.code()))
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "searchProducts failed", e)
            emit(AppResult.Error("Search unavailable", cause = e))
        }
    }

    override suspend fun getPriceComparison(productId: String): AppResult<PriceComparison> {
        return try {
            val response = priceComparisonApi.getPriceComparison(productId)
            if (response.isSuccessful) {
                val dto = response.body()?.data
                if (dto != null) {
                    val domainModel = dto.toDomain()
                    priceComparisonCache.put(productId, domainModel)
                    AppResult.Success(domainModel)
                } else {
                    AppResult.Error("Price comparison unavailable")
                }
            } else {
                AppResult.Error("Price comparison service error", response.code())
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "getPriceComparison failed", e)
            // Fail gracefully — price comparison is an enhancement, not blocking
            AppResult.Error("Price data temporarily unavailable", cause = e)
        }
    }

    override fun observePriceComparison(productId: String): Flow<PriceComparison?> =
        priceComparisonCache.observe(productId)

    // ── DTO → Domain Mappers ──────────────────────────────────────────────────

    private fun ProductDto.toDomain() = Product(
        id            = id,
        title         = title,
        brand         = brand,
        price         = Money(price, currency),
        mrp           = Money(mrp, currency),
        images        = images,
        description   = description,
        rating        = rating.toFloat(),
        reviewCount   = reviewCount,
        category      = category,
        inStock       = inStock,
        sellerId      = sellerId,
        tags          = tags
    )

    private fun PriceComparisonDto.toDomain() = PriceComparison(
        productId            = productId,
        currentPrice         = Money(currentPrice),
        marketAveragePrice   = Money(marketAveragePrice),
        marketLowPrice       = Money(marketLowPrice),
        marketHighPrice      = Money(marketHighPrice),
        storeCount           = storeCount,
        lastUpdatedSeconds   = lastUpdatedSeconds,
        confidenceScore      = confidenceScore,
        pricePercentile      = pricePercentile,
        competitorPrices     = competitorPrices.map {
            CompetitorPrice(it.storeName, Money(it.price), it.inStock)
        }
    )
}
