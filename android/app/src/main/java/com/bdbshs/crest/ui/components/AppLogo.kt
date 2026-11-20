package com.bdbshs.crest.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.em
import com.bdbshs.crest.R
import com.bdbshs.crest.branding.CrestBrandDescriptor
import com.bdbshs.crest.branding.CrestBrandName
import com.bdbshs.crest.branding.CrestWordmarkTrackingEm
import com.bdbshs.crest.ui.theme.CRESTTheme

/** The same unpadded vector mark as the website; launcher mask padding stays in its own resources. */
@Composable
fun CrestMark(size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(
        painter = painterResource(R.drawable.crest_mark),
        contentDescription = contentDescription,
        modifier = modifier.size(size)
    )
}

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
        CrestMark(112.dp, contentDescription = if (showText) null else CrestBrandName)
        if (showText) {
            Text(
                text = CrestBrandName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = CrestWordmarkTrackingEm.em,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = CrestBrandDescriptor,
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
