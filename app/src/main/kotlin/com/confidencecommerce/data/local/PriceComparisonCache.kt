package com.confidencecommerce.data.local

import com.confidencecommerce.domain.model.PriceComparison
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory LRU-style cache for price comparison data.
 * Price data is not PII — no encryption needed here.
 * TTL-based invalidation prevents stale price anchoring.
 *
 * Production: replace with Room DB + WorkManager for background refresh.
 */
@Singleton
class PriceComparisonCache @Inject constructor() {

    companion object {
        private const val TTL_SECONDS = 300L   // 5 minutes — price data freshness
        private const val MAX_ENTRIES = 50
    }

    private val _cache = MutableStateFlow<Map<String, CachedEntry>>(emptyMap())

    data class CachedEntry(
        val data: PriceComparison,
        val cachedAtSeconds: Long = System.currentTimeMillis() / 1000
    ) {
        val isExpired: Boolean
            get() = (System.currentTimeMillis() / 1000 - cachedAtSeconds) > TTL_SECONDS
    }

    fun put(productId: String, data: PriceComparison) {
        _cache.value = (_cache.value + (productId to CachedEntry(data)))
            .entries.sortedByDescending { it.value.cachedAtSeconds }
            .take(MAX_ENTRIES)
            .associate { it.key to it.value }
    }

    fun get(productId: String): PriceComparison? {
        val entry = _cache.value[productId]
        return if (entry != null && !entry.isExpired) entry.data else null
    }

    fun observe(productId: String): Flow<PriceComparison?> =
        _cache.map { cache ->
            val entry = cache[productId]
            if (entry != null && !entry.isExpired) entry.data else null
        }

    fun invalidate(productId: String) {
        _cache.value = _cache.value - productId
    }
}
