package com.confidencecommerce.presentation.product.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.confidencecommerce.domain.model.CompetitorPrice
import com.confidencecommerce.domain.model.ConfidenceTier
import com.confidencecommerce.domain.model.PriceComparison
import com.confidencecommerce.presentation.theme.*
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// PriceComparisonAnchor — THE core feature.
//
// Solves the "price justification gap" (the #1 driver of cart abandonment).
// Shows buyers that this price is objectively good relative to the market,
// giving them the confidence signal needed to complete the purchase.
//
// Placement: below price display, ABOVE "Add to Cart" CTA.
// Colour semantics: green = great deal, orange = fair, red = needs scrutiny.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Main Price Comparison Anchor composable.
 *
 * @param priceComparison  Domain model with market data (null = loading/unavailable)
 * @param isLoading        Show skeleton while fetching price data
 * @param isExpanded       Whether competitor breakdown is visible
 * @param onToggleExpanded Callback to expand/collapse the breakdown
 */
@Composable
fun PriceComparisonAnchor(
    priceComparison: PriceComparison?,
    isLoading: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> PriceAnchorSkeleton(modifier)
        priceComparison != null -> PriceAnchorContent(
            data             = priceComparison,
            isExpanded       = isExpanded,
            onToggleExpanded = onToggleExpanded,
            modifier         = modifier
        )
        // Graceful degradation — if price data unavailable, show nothing
        else -> Unit
    }
}

// ── Main Content Card ─────────────────────────────────────────────────────────

@Composable
private fun PriceAnchorContent(
    data: PriceComparison,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier   = data.confidenceTier
    val colors = tier.colors()

    // Animate the left border colour change between tiers
    val borderColor by animateColorAsState(
        targetValue = colors.border,
        animationSpec = tween(durationMillis = 400),
        label = "borderColor"
    )

    val vsPercent  = data.vsMarketPercent
    val absDiff    = abs(vsPercent)
    val isBelow    = vsPercent < 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildAnchorA11yDescription(data, vsPercent)
            },
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
        ) {

            // ── Main Row ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Left: Confidence icon badge
                ConfidenceBadge(tier = tier, colors = colors)

                // Centre: Primary message
                Column(modifier = Modifier.weight(1f)) {
                    PrimaryPriceMessage(
                        vsPercent = vsPercent,
                        absDiff   = absDiff,
                        isBelow   = isBelow,
                        colors    = colors
                    )
                    Spacer(Modifier.height(2.dp))
                    MetaLine(
                        storeCount     = data.storeCount,
                        updatedAgo     = data.updatedAgoLabel(),
                        confidenceTier = tier
                    )
                }

                // Right: Expand/collapse chevron
                ExpandChevron(isExpanded = isExpanded, tintColor = colors.text)
            }

            // ── Price Position Bar ────────────────────────────────────────────
            AnimatedVisibility(visible = !isExpanded) {
                PricePositionBar(
                    priceComparison = data,
                    colors          = colors,
                    modifier        = Modifier.padding(horizontal = 14.dp).padding(bottom = 12.dp)
                )
            }

            // ── Competitor Breakdown (collapsible) ────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                CompetitorBreakdown(
                    competitors    = data.competitorPrices,
                    currentPrice   = data.currentPrice.amount,
                    colors         = colors
                )
            }
        }
    }
}

// ── Primary Message Line ──────────────────────────────────────────────────────

@Composable
private fun PrimaryPriceMessage(
    vsPercent: Int,
    absDiff: Int,
    isBelow: Boolean,
    colors: AnchorColors
) {
    val message = when {
        vsPercent == 0 -> "At market average"
        isBelow        -> "$absDiff% below market average"
        else           -> "$absDiff% above market average"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isBelow && absDiff >= 10) {
            Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = null,
                tint  = colors.text,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text       = message,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = colors.text
        )
    }
}

// ── Meta Line: source count + freshness ──────────────────────────────────────

@Composable
private fun MetaLine(
    storeCount: Int,
    updatedAgo: String,
    confidenceTier: ConfidenceTier
) {
    val staleWarning = confidenceTier == ConfidenceTier.LOW
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text  = "Based on $storeCount store${if (storeCount != 1) "s" else ""} • Updated $updatedAgo",
            style = MaterialTheme.typography.bodySmall,
            color = if (staleWarning)
                        ConfidenceLow.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (staleWarning) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Data may be outdated",
                tint     = ConfidenceLow,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ── Confidence Icon Badge ─────────────────────────────────────────────────────

@Composable
private fun ConfidenceBadge(tier: ConfidenceTier, colors: AnchorColors) {
    val icon: ImageVector = when (tier) {
        ConfidenceTier.HIGH   -> Icons.Default.Verified
        ConfidenceTier.MEDIUM -> Icons.Default.Info
        ConfidenceTier.LOW    -> Icons.Default.HelpOutline
    }
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = if (tier == ConfidenceTier.HIGH) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.border.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint     = colors.text,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Price Position Bar ────────────────────────────────────────────────────────

@Composable
private fun PricePositionBar(
    priceComparison: PriceComparison,
    colors: AnchorColors,
    modifier: Modifier = Modifier
) {
    val low     = priceComparison.marketLowPrice.amount
    val high    = priceComparison.marketHighPrice.amount
    val current = priceComparison.currentPrice.amount
    val range   = (high - low).coerceAtLeast(1.0)
    val position = ((current - low) / range).toFloat().coerceIn(0f, 1f)

    val animatedPosition by animateFloatAsState(
        targetValue  = position,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label        = "priceBarPosition"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Track + thumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            // Fill from left to current price position
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPosition)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.border.copy(alpha = 0.5f))
            )
            // Thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPosition)
                    .wrapContentWidth(Alignment.End)
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.border)
                    .align(Alignment.CenterStart)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = priceComparison.marketLowPrice.formatted(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text       = "Market range",
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = priceComparison.marketHighPrice.formatted(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Competitor Breakdown ──────────────────────────────────────────────────────

@Composable
private fun CompetitorBreakdown(
    competitors: List<CompetitorPrice>,
    currentPrice: Double,
    colors: AnchorColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(color = colors.border.copy(alpha = 0.2f))
        Spacer(Modifier.height(2.dp))

        Text(
            text  = "Price comparison across stores",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        competitors.sortedBy { it.price.amount }.forEach { competitor ->
            CompetitorRow(
                competitor   = competitor,
                currentPrice = currentPrice
            )
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text  = "Prices updated periodically. Tap to verify on retailer sites.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CompetitorRow(competitor: CompetitorPrice, currentPrice: Double) {
    val isCheaper  = competitor.price.amount < currentPrice
    val isSameStore = competitor.price.amount == currentPrice
    val diff = ((competitor.price.amount - currentPrice) / currentPrice * 100).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Store name
        Text(
            text     = competitor.storeName,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color    = if (competitor.inStock) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Out-of-stock badge
        if (!competitor.inStock) {
            Text(
                text  = "OOS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Price + diff
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = competitor.price.formatted(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isCheaper  -> ConfidenceLow        // Competitors cheaper — show prominently
                    isSameStore -> MaterialTheme.colorScheme.primary
                    else        -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (!isSameStore && diff != 0) {
                val sign = if (diff > 0) "+" else ""
                Text(
                    text  = "${sign}$diff%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCheaper) ConfidenceLow else ConfidenceHigh
                )
            }
        }
    }
}

// ── Expand Chevron ────────────────────────────────────────────────────────────

@Composable
private fun ExpandChevron(isExpanded: Boolean, tintColor: Color) {
    val rotation by animateFloatAsState(
        targetValue  = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label        = "chevronRotation"
    )
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = if (isExpanded) "Collapse" else "See all stores",
        tint     = tintColor.copy(alpha = 0.7f),
        modifier = Modifier
            .size(20.dp)
            .graphicsLayerRotation(rotation)
    )
}

// ── Loading Skeleton ──────────────────────────────────────────────────────────

@Composable
private fun PriceAnchorSkeleton(modifier: Modifier = Modifier) {
    val shimmerAlpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.9f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = shimmerAlpha * 0.4f)))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(180.dp).height(14.dp).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = shimmerAlpha * 0.3f)))
                Box(Modifier.width(130.dp).height(11.dp).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = shimmerAlpha * 0.2f)))
            }
        }
    }
}

// ── Helper: colour tokens per tier ────────────────────────────────────────────

data class AnchorColors(
    val background: Color,
    val border: Color,
    val text: Color
)

private fun ConfidenceTier.colors(): AnchorColors = when (this) {
    ConfidenceTier.HIGH   -> AnchorColors(ConfidenceHighBg,   ConfidenceHigh,   ConfidenceHigh)
    ConfidenceTier.MEDIUM -> AnchorColors(ConfidenceMediumBg, ConfidenceMedium, ConfidenceMedium)
    ConfidenceTier.LOW    -> AnchorColors(ConfidenceLowBg,    ConfidenceLow,    ConfidenceLow)
}

// ── Accessibility helper ──────────────────────────────────────────────────────
private fun buildAnchorA11yDescription(data: PriceComparison, vsPercent: Int): String {
    val direction = if (vsPercent <= 0) "below" else "above"
    val abs = abs(vsPercent)
    return "Price confidence: ${data.confidenceTier.name.lowercase()}. " +
        "This item is $abs% $direction market average. " +
        "Based on ${data.storeCount} stores. Updated ${data.updatedAgoLabel()}."
}

// ── Kotlin extension for GraphicsLayer rotation ──────────────────────────────
private fun Modifier.graphicsLayerRotation(degrees: Float): Modifier =
    this.then(Modifier.graphicsLayer { rotationZ = degrees })
