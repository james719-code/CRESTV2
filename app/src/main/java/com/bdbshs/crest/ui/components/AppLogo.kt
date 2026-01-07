package com.bdbshs.crest.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.R
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.theme.PrimaryBlue

/**
 * App logo composable for splash screen and branding.
 * Features a subtle pulse animation for visual engagement.
 *
 * @param modifier Optional modifier for customization
 * @param showText Whether to show the app name below the logo
 * @param animateScale Whether to animate the logo with a subtle pulse
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    showText: Boolean = true,
    animateScale: Boolean = true
) {
    // Subtle pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animateScale) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon/logo
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "CREST Logo",
            modifier = Modifier
                .size(120.dp)
                .scale(if (animateScale) scale else 1f)
                .alpha(if (animateScale) alpha else 1f)
        )

        if (showText) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CREST",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Research Repository",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLogoPreview() {
    CRESTTheme {
        AppLogo()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F12)
@Composable
private fun AppLogoDarkPreview() {
    CRESTTheme(darkTheme = true) {
        AppLogo()
    }
}
