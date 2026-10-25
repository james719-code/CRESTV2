package com.bdbshs.crest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bdbshs.crest.R
import com.bdbshs.crest.ui.theme.CRESTTheme

/**
 * The existing launcher artwork on a round white base. The foreground layer carries launcher safe-zone padding,
 * so it is scaled up to fill the circle. Replace when approved logo masters are available.
 */
@Composable
fun CrestMark(size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(
        modifier.size(size).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1.45f, scaleY = 1.45f)
        )
    }
}

/** Uses the existing artwork until approved replacement masters are available. */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CrestMark(112.dp, contentDescription = if (showText) null else "CREST")
        if (showText) {
            Text(
                text = "CREST",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.68.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "School research library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLogoPreview() {
    CRESTTheme { AppLogo() }
}
