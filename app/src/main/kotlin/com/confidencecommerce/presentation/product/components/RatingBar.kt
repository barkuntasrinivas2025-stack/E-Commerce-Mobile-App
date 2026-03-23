package com.confidencecommerce.presentation.product.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confidencecommerce.presentation.theme.StarYellow

@Composable
fun RatingBar(
    rating: Float,
    reviewCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row {
            for (i in 1..5) {
                val icon = when {
                    i <= rating.toInt()                -> Icons.Filled.Star
                    i == rating.toInt() + 1 && rating % 1 >= 0.5f -> Icons.Filled.StarHalf
                    else                               -> Icons.Outlined.StarOutline
                }
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = StarYellow,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text  = "$rating (${"%.1f".format(rating)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = "· $reviewCount reviews",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
