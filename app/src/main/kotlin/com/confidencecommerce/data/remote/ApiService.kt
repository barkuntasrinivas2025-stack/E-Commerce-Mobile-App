package com.confidencecommerce.data.remote

import com.confidencecommerce.data.remote.models.ApiResponse
import com.confidencecommerce.data.remote.models.PriceComparisonDto
import com.confidencecommerce.data.remote.models.ProductDto
import com.confidencecommerce.data.remote.models.ProductListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API contract.
 * All requests go through SecurityHeadersInterceptor + AuthInterceptor.
 * Base URL from BuildConfig.API_BASE_URL (never hardcoded).
 */
interface ProductApiService {

    @GET("products")
    suspend fun getProducts(
        @Query("category")  category: String? = null,
        @Query("page")      page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<ProductListResponseDto>>

    @GET("products/{id}")
    suspend fun getProductById(
        @Path("id") productId: String
    ): Response<ApiResponse<ProductDto>>

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): Response<ApiResponse<ProductListResponseDto>>
}

interface PriceComparisonApiService {

    @GET("price-comparison/{productId}")
    suspend fun getPriceComparison(
        @Path("productId") productId: String
    ): Response<ApiResponse<PriceComparisonDto>>
}
