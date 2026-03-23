package com.confidencecommerce.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Object — mirrors API contract exactly.
 * Never expose DTOs outside the data layer; map to domain models at boundary.
 */
@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id")            val id: String,
    @Json(name = "title")         val title: String,
    @Json(name = "brand")         val brand: String,
    @Json(name = "price")         val price: Double,
    @Json(name = "mrp")           val mrp: Double,
    @Json(name = "currency")      val currency: String = "INR",
    @Json(name = "images")        val images: List<String>,
    @Json(name = "description")   val description: String,
    @Json(name = "rating")        val rating: Double,
    @Json(name = "review_count")  val reviewCount: Int,
    @Json(name = "category")      val category: String,
    @Json(name = "in_stock")      val inStock: Boolean,
    @Json(name = "seller_id")     val sellerId: String,
    @Json(name = "tags")          val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProductListResponseDto(
    @Json(name = "products")      val products: List<ProductDto>,
    @Json(name = "total")         val total: Int,
    @Json(name = "page")          val page: Int,
    @Json(name = "page_size")     val pageSize: Int
)
