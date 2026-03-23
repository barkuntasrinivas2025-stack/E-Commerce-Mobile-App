package com.confidencecommerce.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.confidencecommerce.domain.model.Product
import com.confidencecommerce.presentation.theme.DiscountBadge
import com.confidencecommerce.presentation.theme.StarYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories = listOf(null, "Electronics", "Footwear", "Clothing", "Kitchen")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ConfidenceCommerce",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Default.ShoppingCart, "Cart")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top   = padding.calculateTopPadding() + 8.dp,
                bottom = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(2) }) {
                CategoryChipRow(categories, uiState.selectedCategory, viewModel::onCategorySelected)
            }
            when {
                uiState.isLoading -> items(6) { ProductCardSkeleton() }
                uiState.errorMessage != null -> item(span = { GridItemSpan(2) }) {
                    ErrorCard(uiState.errorMessage!!)
                }
                else -> items(uiState.products) { product ->
                    ProductCard(product, onClick = { onProductClick(product.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    categories: List<String?>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(categories.size) { i ->
            val cat = categories[i]
            FilterChip(selected = selected == cat, onClick = { onSelect(cat) },
                label = { Text(cat ?: "All") })
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model              = product.primaryImage,
                    contentDescription = product.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxWidth().height(160.dp)
                )
                if (product.discountPercent > 0) {
                    Surface(
                        color    = DiscountBadge,
                        shape    = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text("${product.discountPercent}% OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.brand, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(product.title, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Star, null, Modifier.size(13.dp), tint = StarYellow)
                    Text("${product.rating}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(product.price.formatted(), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductCardSkeleton() {
    val alpha by rememberInfiniteTransition(label = "s").animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a"
    )
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(160.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(60.dp).height(12.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = alpha * 0.4f),
                        RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth().height(14.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = alpha * 0.3f),
                        RoundedCornerShape(4.dp)))
                Box(Modifier.width(80.dp).height(16.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = alpha * 0.35f),
                        RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline, null,
                tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
