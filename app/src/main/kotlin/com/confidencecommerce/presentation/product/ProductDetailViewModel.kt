package com.confidencecommerce.presentation.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.confidencecommerce.domain.model.*
import com.confidencecommerce.domain.usecase.*
import com.confidencecommerce.security.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val priceComparison: PriceComparison? = null,
    val isLoadingProduct: Boolean = false,
    val isLoadingPrice: Boolean = false,
    val cartQuantity: Int = 1,
    val isAddingToCart: Boolean = false,
    val addToCartSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showCompetitorBreakdown: Boolean = false
)

sealed class ProductDetailEvent {
    object AddToCartSuccess : ProductDetailEvent()
    data class ShowError(val message: String) : ProductDetailEvent()
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductDetail: GetProductDetailUseCase,
    private val getPriceComparison: GetPriceComparisonUseCase,
    private val addToCart: AddToCartUseCase
) : ViewModel() {

    companion object { private const val TAG = "ProductDetailVM" }

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProductDetailEvent>()
    val events: SharedFlow<ProductDetailEvent> = _events.asSharedFlow()

    init {
        loadProduct()
        loadPriceComparison()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProduct = true, errorMessage = null) }
            when (val result = getProductDetail(productId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(product = result.data, isLoadingProduct = false) }
                }
                is AppResult.Error -> {
                    SecureLogger.e(TAG, "Product load failed: ${result.code}")
                    _uiState.update {
                        it.copy(isLoadingProduct = false, errorMessage = result.message)
                    }
                }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadPriceComparison() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPrice = true) }

            // Observe cache immediately (instant feedback)
            getPriceComparison.observe(productId)
                .onEach { cached ->
                    if (cached != null) {
                        _uiState.update { it.copy(priceComparison = cached, isLoadingPrice = false) }
                    }
                }
                .launchIn(viewModelScope)

            // Fetch fresh data from network
            when (val result = getPriceComparison(productId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(priceComparison = result.data, isLoadingPrice = false) }
                }
                is AppResult.Error -> {
                    SecureLogger.w(TAG, "Price comparison unavailable — not blocking UX")
                    _uiState.update { it.copy(isLoadingPrice = false) }
                    // Graceful degradation: price anchor just doesn't show
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun onQuantityChanged(quantity: Int) {
        if (quantity in 1..99) {
            _uiState.update { it.copy(cartQuantity = quantity) }
        }
    }

    fun onAddToCart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingToCart = true) }
            when (val result = addToCart(productId, _uiState.value.cartQuantity)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isAddingToCart = false, addToCartSuccess = true) }
                    _events.emit(ProductDetailEvent.AddToCartSuccess)
                    // Reset success state after brief delay
                    kotlinx.coroutines.delay(2000)
                    _uiState.update { it.copy(addToCartSuccess = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isAddingToCart = false) }
                    _events.emit(ProductDetailEvent.ShowError(result.message))
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun toggleCompetitorBreakdown() {
        _uiState.update { it.copy(showCompetitorBreakdown = !it.showCompetitorBreakdown) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
