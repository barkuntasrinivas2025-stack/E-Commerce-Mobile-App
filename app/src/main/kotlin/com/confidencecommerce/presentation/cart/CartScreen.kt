package com.confidencecommerce.presentation.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.confidencecommerce.domain.model.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cart (${uiState.cart?.totalItems ?: 0} items)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.cart?.isEmpty == false) {
                CartCheckoutBar(subtotal = uiState.cart?.subtotal?.formatted() ?: "₹0",
                    onCheckout = onCheckout)
            }
        }
    ) { padding ->
        val cart = uiState.cart
        if (cart == null || cart.isEmpty) {
            EmptyCartView(Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp,
                    top   = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cart.items, key = { it.product.id }) { item ->
                    CartItemCard(
                        item       = item,
                        onIncrease = { viewModel.updateQuantity(item.product.id, item.quantity + 1) },
                        onDecrease = { viewModel.updateQuantity(item.product.id, item.quantity - 1) },
                        onRemove   = { viewModel.removeItem(item.product.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(item: CartItem, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = item.product.primaryImage,
                contentDescription = item.product.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.product.brand, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(item.product.title, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.product.price.formatted(), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, null, Modifier.size(14.dp))
                        }
                        Text("${item.quantity}", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                        }
                    }
                    TextButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutBar(subtotal: String, onCheckout: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Subtotal", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtotal, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            Button(onClick = onCheckout, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)) {
                Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Secure Checkout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyCartView(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Default.ShoppingCartCheckout, null, Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
        Spacer(Modifier.height(16.dp))
        Text("Your cart is empty", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("Add items to get started", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
    }
}
