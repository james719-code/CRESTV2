package com.bdbshs.crest.ui.screens.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(800), repeatMode = RepeatMode.Reverse
            ), label = "shimmer-animation"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun ShimmerResearchCardPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ShimmerBrush())
            .padding(16.dp)
    ) {
        // Title placeholders
        Spacer(modifier = Modifier.fillMaxWidth(0.7f).height(24.dp).background(Color.LightGray.copy(alpha = 0.4f)))
        Spacer(modifier = Modifier.height(8.dp))
        Spacer(modifier = Modifier.fillMaxWidth(0.5f).height(24.dp).background(Color.LightGray.copy(alpha = 0.4f)))
        Spacer(modifier = Modifier.height(12.dp))
        // Members placeholder
        Spacer(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).background(Color.LightGray.copy(alpha = 0.4f)))
        Spacer(modifier = Modifier.height(16.dp))
        // Bottom row placeholder
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(60.dp).height(24.dp).clip(RoundedCornerShape(50)).background(Color.LightGray.copy(alpha = 0.4f)))
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(40.dp).height(16.dp).background(Color.LightGray.copy(alpha = 0.4f)))
        }
    }
}

@Composable
fun ShimmerListItemPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp) // Give it a fixed height for consistency
            .clip(RoundedCornerShape(12.dp))
            .background(ShimmerBrush())
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon placeholder
        Spacer(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.width(16.dp))
        // Text placeholders
        Column(verticalArrangement = Arrangement.Center) {
            Spacer(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.7f)
                    .background(Color.LightGray.copy(alpha = 0.6f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.4f)
                    .background(Color.LightGray.copy(alpha = 0.6f))
            )
        }
    }
}