package com.bdbshs.crest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.UserType


/**
 * Data class representing a bottom navigation item
 */
data class BottomNavItem(
    val route: AppDestination,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badge: Int? = null
)

/**
 * Get navigation items based on user role
 */
fun getBottomNavItems(userRole: UserType?): List<BottomNavItem> {
    val commonItems = listOf(
        BottomNavItem(
            route = AppDestination.Home,
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = AppDestination.Researches,
            title = "Research",
            selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
            unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks
        ),
        BottomNavItem(
            route = AppDestination.Documents,
            title = "Documents",
            selectedIcon = Icons.Filled.Description,
            unselectedIcon = Icons.Outlined.Description
        )
    )
    
    return when (userRole) {
        UserType.TEACHER -> commonItems + listOf(
            BottomNavItem(
                route = AppDestination.Groups,
                title = "Groups",
                selectedIcon = Icons.Filled.Groups,
                unselectedIcon = Icons.Outlined.Groups
            )
        )
        else -> commonItems
    }
}

/**
 * Stable, labeled Material navigation with equal space for each destination.
 */
@Composable
fun CrestBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    BrandNavigationBar(items, items.indexOfFirst { it.route.route == currentRoute },
        { onItemClick(items[it].route) }, modifier)
}

@Composable
private fun BrandNavigationBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(index) },
                alwaysShowLabel = true,
                icon = { NavigationIcon(item, selected) },
                label = { NavLabel(item.title) },
                colors = crestNavigationItemColors()
            )
        }
    }
}

/** Side navigation for medium and expanded windows, using the same destinations as the bottom bar. */
@Composable
fun CrestNavigationRail(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets.navigationBars,
        header = { Spacer(Modifier.height(8.dp)) }
    ) {
        Spacer(Modifier.weight(1f))
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            NavigationRailItem(
                selected = selected,
                onClick = { onItemClick(index) },
                alwaysShowLabel = true,
                icon = { NavigationIcon(item, selected) },
                label = { NavLabel(item.title) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Tab labels stay on one line; text beyond 130% would break words in a four-tab bar. */
@Composable
private fun NavLabel(text: String) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NavigationIcon(item: BottomNavItem, selected: Boolean) {
    BadgedBox(badge = {
        item.badge?.takeIf { it > 0 }?.let { count ->
            Badge { Text(if (count > 99) "99+" else count.toString()) }
        }
    }) {
        Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = null)
    }
}

@Composable
private fun crestNavigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun OfflineBottomNavBar(
    currentRoute: String?,
    onResearchesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .clickable(onClick = onResearchesClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null)
            Text("You're offline. Saved papers are available.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CrestBottomNavBarWithPager(
    items: List<BottomNavItem>,
    pagerState: PagerState,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BrandNavigationBar(items, pagerState.currentPage, onItemClick, modifier)
}
