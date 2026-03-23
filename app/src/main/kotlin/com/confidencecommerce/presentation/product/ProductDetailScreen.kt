package com.confidencecommerce.presentation.product

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.confidencecommerce.presentation.product.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Consume one-shot events (AddToCartSuccess, ShowError)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductDetailEvent.AddToCartSuccess -> {
                    snackbarHostState.showSnackbar(
                        message     = "Added to cart!",
                        actionLabel = "View Cart",
                        duration    = SnackbarDuration.Short
                    ).let { result ->
                        if (result == SnackbarResult.ActionPerformed) onNavigateToCart()
                    }
                }
                is ProductDetailEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.product?.brand ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                    }
                }
            )
        },
        bottomBar = {
            ProductDetailBottomBar(
                inStock         = uiState.product?.inStock ?: false,
                isAddingToCart  = uiState.isAddingToCart,
                addToCartSuccess = uiState.addToCartSuccess,
                quantity        = uiState.cartQuantity,
                onQuantityDec   = { viewModel.onQuantityChanged(uiState.cartQuantity - 1) },
                onQuantityInc   = { viewModel.onQuantityChanged(uiState.cartQuantity + 1) },
                onAddToCart     = viewModel::onAddToCart
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        when {
            uiState.isLoadingProduct -> ProductDetailSkeleton(Modifier.padding(paddingValues))
            uiState.errorMessage != null -> ProductDetailError(
                message   = uiState.errorMessage!!,
                onRetry   = { /* viewModel.retry() */ },
                modifier  = Modifier.padding(paddingValues)
            )
            uiState.product != null -> ProductDetailContent(
                uiState  = uiState,
                viewModel = viewModel,
                modifier  = Modifier.padding(paddingValues)
            )
        }
    }
}

// ── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun ProductDetailContent(
    uiState: ProductDetailUiState,
    viewModel: ProductDetailViewModel,
    modifier: Modifier = Modifier
) {
    val product = uiState.product!!
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {

        // ── Product Image Carousel ────────────────────────────────────────────
        ProductImagePager(
            images   = product.images,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        )

        // ── Product Info Section ──────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Brand + Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = product.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text  = product.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Ratings
            RatingBar(rating = product.rating, reviewCount = product.reviewCount)

            // In-stock badge
            if (!product.inStock) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text     = "Currently out of stock",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── PRICE SECTION ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Price display (MRP + current + discount %)
                PriceDisplay(
                    price           = product.price,
                    mrp             = product.mrp,
                    discountPercent = product.discountPercent
                )

                // ╔══════════════════════════════════════════════════════════╗
                // ║  PRICE COMPARISON ANCHOR — Core Decision Confidence UI  ║
                // ║  Placed between price and Add-to-Cart CTA for maximum   ║
                // ║  impact on the cart abandonment gap.                    ║
                // ╚══════════════════════════════════════════════════════════╝
                PriceComparisonAnchor(
                    priceComparison  = uiState.priceComparison,
                    isLoading        = uiState.isLoadingPrice,
                    isExpanded       = uiState.showCompetitorBreakdown,
                    onToggleExpanded = viewModel::toggleCompetitorBreakdown
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Description ───────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text  = "About this product",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Tags ──────────────────────────────────────────────────────────
            if (product.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    product.tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label   = { Text(tag, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }

            // Extra scroll space for bottom bar
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Bottom Action Bar ─────────────────────────────────────────────────────────

@Composable
private fun ProductDetailBottomBar(
    inStock: Boolean,
    isAddingToCart: Boolean,
    addToCartSuccess: Boolean,
    quantity: Int,
    onQuantityDec: () -> Unit,
    onQuantityInc: () -> Unit,
    onAddToCart: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color           = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Quantity selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                OutlinedIconButton(
                    onClick = onQuantityDec,
                    enabled = quantity > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", Modifier.size(16.dp))
                }
                Text(
                    text  = "$quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                OutlinedIconButton(
                    onClick = onQuantityInc,
                    enabled = quantity < 99,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", Modifier.size(16.dp))
                }
            }

            // Add to Cart CTA
            Button(
                onClick  = onAddToCart,
                enabled  = inStock && !isAddingToCart,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                AnimatedContent(
                    targetState = when {
                        addToCartSuccess -> "success"
                        isAddingToCart   -> "loading"
                        !inStock         -> "oos"
                        else             -> "idle"
                    },
                    label = "cartButtonState"
                ) { state ->
                    when (state) {
                        "loading" -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Adding...")
                            }
                        }
                        "success" -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                Text("Added to Cart!")
                            }
                        }
                        "oos"  -> Text("Out of Stock")
                        else   -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ShoppingCartCheckout, null, Modifier.size(18.dp))
                                Text("Add to Cart", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Loading Skeleton ──────────────────────────────────────────────────────────

@Composable
private fun ProductDetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .shimmerBackground()
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.width(100.dp).height(16.dp).shimmerBackground(RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxWidth().height(28.dp).shimmerBackground(RoundedCornerShape(4.dp)))
            Box(Modifier.width(180.dp).height(14.dp).shimmerBackground(RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxWidth().height(80.dp).shimmerBackground(RoundedCornerShape(12.dp)))
        }
    }
}

@Composable
private fun ProductDetailError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

// ── Shimmer extension ─────────────────────────────────────────────────────────
@Composable
private fun Modifier.shimmerBackground(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp)
): Modifier {
    val alpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerAlpha"
    )
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
}
