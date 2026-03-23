package com.confidencecommerce.presentation.product.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.confidencecommerce.domain.model.Money
import com.confidencecommerce.presentation.theme.DiscountBadge

@Composable
fun PriceDisplay(
    price: Money,
    mrp: Money,
    discountPercent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Current price — prominent
        Text(
            text  = price.formatted(),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize   = 26.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )
        )

        // MRP struck-through
        if (discountPercent > 0) {
            Text(
                text  = mrp.formatted(),
                style = TextStyle(
                    fontSize        = 16.sp,
                    textDecoration  = TextDecoration.LineThrough,
                    color           = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Discount badge
            Surface(
                color = DiscountBadge,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text  = "$discountPercent% off",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
