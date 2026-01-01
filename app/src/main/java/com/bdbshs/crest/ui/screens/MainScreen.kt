package com.bdbshs.crest.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.navigation.NavigationActions
import com.bdbshs.crest.ui.components.*
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.MainViewModel
import com.bdbshs.crest.ui.viewmodels.UserType
import kotlinx.coroutines.launch

data class DrawerItem(
    val route: AppDestination,
    val title: String,
    val icon: ImageVector
)

/**
 * Modernized MainScreen with bottom navigation architecture
 * Replaces the drawer-based navigation for better UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navigationActions: NavigationActions,
    currentRoute: String?,
    isOnline: Boolean,
    content: @Composable (padding: PaddingValues, userRole: UserType?) -> Unit
) {
    val mainViewModel: MainViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Bottom sheet state for profile menu
    val sheetState = rememberModalBottomSheetState()
    var showProfileSheet by remember { mutableStateOf(false) }
    
    // Scroll behavior for collapsing top bar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    // Show offline snackbar
    LaunchedEffect(isOnline) {
        if (!isOnline) {
            snackbarHostState.showSnackbar(
                message = "You are currently offline. Some features are limited.",
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
        }
    }
    
    // Get navigation items based on user role and online status
    val bottomNavItems = remember(uiState.userRole, isOnline) {
        if (isOnline) {
            getBottomNavItems(uiState.userRole)
        } else {
            // Offline: only show Researches
            listOf(
                BottomNavItem(
                    route = AppDestination.Researches,
                    title = "Researches",
                    selectedIcon = Icons.Filled.LibraryBooks,
                    unselectedIcon = Icons.Outlined.LibraryBooks
                )
            )
        }
    }
    
    // Determine title based on current route
    val title = remember(currentRoute, isOnline) {
        val baseTitle = when (currentRoute) {
            AppDestination.Home.route -> "Home"
            AppDestination.Researches.route -> "Researches"
            AppDestination.Documents.route -> "Documents"
            AppDestination.Groups.route -> "Groups"
            AppDestination.Accounts.route -> "Accounts"
            AppDestination.AboutUs.route -> "About"
            else -> "CREST"
        }
        if (!isOnline && currentRoute == AppDestination.Researches.route) {
            "$baseTitle (Cached)"
        } else {
            baseTitle
        }
    }
    
    // Profile bottom sheet
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = CrestShapeTokens.BottomSheet
        ) {
            ProfileBottomSheet(
                userName = null, // TODO: Get from user state
                userEmail = null, // TODO: Get from user state
                userRole = uiState.userRole,
                userPhotoUrl = null, // TODO: Get from user state
                isOnline = isOnline,
                onAccountsClick = if (uiState.userRole == UserType.TEACHER && isOnline) {
                    { navigationActions.navigateTo(AppDestination.Accounts) }
                } else null,
                onAboutClick = { navigationActions.navigateTo(AppDestination.AboutUs) },
                onSignOutClick = {
                    mainViewModel.onSignOut()
                    showProfileSheet = false
                },
                onDismiss = { showProfileSheet = false }
            )
        }
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (!isOnline) 
                        MaterialTheme.colorScheme.secondaryContainer 
                    else 
                        MaterialTheme.colorScheme.inverseSurface,
                    contentColor = if (!isOnline)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.inverseOnSurface,
                    shape = CrestShapeTokens.CardSmall
                )
            }
        },
        topBar = {
            CrestTopAppBar(
                title = title,
                userRole = uiState.userRole,
                isOnline = isOnline,
                onProfileClick = { showProfileSheet = true },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = bottomNavItems.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                if (isOnline) {
                    CrestBottomNavBar(
                        items = bottomNavItems,
                        currentRoute = currentRoute,
                        onItemClick = { destination ->
                            if (destination.route != currentRoute) {
                                navigationActions.navigateTo(destination)
                            }
                        }
                    )
                } else {
                    // Simplified offline bar
                    OfflineBottomNavBar(
                        currentRoute = currentRoute,
                        onResearchesClick = {
                            navigationActions.navigateTo(AppDestination.Researches)
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        content(paddingValues, uiState.userRole)
    }
}