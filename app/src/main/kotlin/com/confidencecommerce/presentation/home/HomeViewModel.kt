package com.confidencecommerce.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.confidencecommerce.domain.model.AppResult
import com.confidencecommerce.domain.model.Product
import com.confidencecommerce.domain.usecase.GetProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadProducts() }

    private fun loadProducts() {
        viewModelScope.launch {
            getProductsUseCase(category = _uiState.value.selectedCategory)
                .collect { result ->
                    when (result) {
                        AppResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is AppResult.Success -> _uiState.update {
                            it.copy(products = result.data, isLoading = false, errorMessage = null)
                        }
                        is AppResult.Error -> _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
        }
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadProducts()
    }
}
