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
import androidx.compose.ui.text.font.FontWeight
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
            selectedIcon = Icons.Filled.LibraryBooks,
            unselectedIcon = Icons.Outlined.LibraryBooks
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
 * Modern bottom navigation bar with pill indicator and smooth animations
 */
@Composable
fun CrestBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            // Subtle top border to separate from content
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Move padding here to extend background
                    .height(80.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route.route
                    val weight by animateFloatAsState(
                        targetValue = if (isSelected) 1.5f else 1f,
                        label = "weight"
                    )
                    BottomNavItemView(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemClick(item.route) },
                        modifier = Modifier.weight(weight)
                    )
                }
            }
        }
    }
}

// Same logic for Item View but added maxLines and softWrap
@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else 
            Color.Transparent,
        label = "containerColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary
        else 
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        label = "contentColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .clip(CircleShape)
                .background(containerColor)
                .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BadgedBox(
                    badge = {
                        item.badge?.let { count ->
                            if (count > 0) {
                                Badge {
                                    Text(text = if (count > 99) "99+" else count.toString())
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Offline mode bottom navigation - simplified with only Researches
 */
@Composable
fun OfflineBottomNavBar(
    currentRoute: String?,
    onResearchesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offlineItems = listOf(
        BottomNavItem(
            route = AppDestination.Researches,
            title = "Researches (Offline)",
            selectedIcon = Icons.Filled.LibraryBooks,
            unselectedIcon = Icons.Outlined.LibraryBooks
        )
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Offline Mode - Cached researches available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Bottom navigation bar synced with HorizontalPager state
 * Automatically updates selection when user swipes between pages
 */
@Composable
fun CrestBottomNavBarWithPager(
    items: List<BottomNavItem>,
    pagerState: PagerState,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Move padding here
                    .height(80.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = pagerState.currentPage == index
                    val weight by animateFloatAsState(
                        targetValue = if (isSelected) 1.5f else 1f,
                        label = "weight"
                    )
                    BottomNavItemView(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemClick(index) },
                        modifier = Modifier.weight(weight)
                    )
                }
            }
        }
    }
}

