package com.bdbshs.crest.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.bdbshs.crest.data.ThemeMode
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.navigation.NavigationActions
import com.bdbshs.crest.ui.components.*
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.MainUiState
import com.bdbshs.crest.ui.viewmodels.MainViewModel
import com.bdbshs.crest.ui.viewmodels.UserType
import kotlinx.coroutines.launch

/**
 * MainScreen with HorizontalPager for swipe navigation between tabs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navigationActions: NavigationActions,
    currentRoute: String?,
    isOnline: Boolean,
    onNavigationClick: (() -> Unit)? = null,
    content: @Composable (padding: PaddingValues, userRole: UserType?) -> Unit
) {
    val mainViewModel: MainViewModel = hiltViewModel()
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
                    selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                    unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks
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
            AppDestination.Favorites.route -> "Favorites"
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
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                userRole = uiState.userRole,
                userPhotoUrl = uiState.userPhotoUrl,
                isOnline = isOnline,
                currentTheme = uiState.themeMode,
                onAccountsClick = if (uiState.userRole == UserType.TEACHER && isOnline) {
                    { navigationActions.navigateTo(AppDestination.Accounts) }
                } else null,
                onFavoritesClick = if (isOnline) {
                    { navigationActions.navigateTo(AppDestination.Favorites) }
                } else null,
                onAboutClick = { navigationActions.navigateTo(AppDestination.AboutUs) },
                onStorageClick = { navigationActions.navigateToStorageManagement() },
                onThemeChange = mainViewModel::onThemeChanged,
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
                userName = uiState.userName,
                userPhotoUrl = uiState.userPhotoUrl,
                isOnline = isOnline,
                onNavigationClick = onNavigationClick,
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
        // No padding here because it's passed to content
        content(paddingValues, uiState.userRole)
    }
}

/**
 * MainScreen with integrated HorizontalPager for swipeable tab navigation
 * This composable hosts all main tabs and enables left/right swiping
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithPager(
    navigationActions: NavigationActions,
    isOnline: Boolean,
    initialPage: Int = 0,
    homeContent: @Composable (PaddingValues, UserType?) -> Unit,
    researchesContent: @Composable (PaddingValues, UserType?) -> Unit,
    documentsContent: @Composable (PaddingValues, UserType?) -> Unit,
    groupsContent: (@Composable (PaddingValues, UserType?) -> Unit)? = null
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.uiState.collectAsState()

    MainScreenWithPagerContent(
        navigationActions = navigationActions,
        isOnline = isOnline,
        initialPage = initialPage,
        homeContent = homeContent,
        researchesContent = researchesContent,
        documentsContent = documentsContent,
        groupsContent = groupsContent,
        uiState = uiState,
        onThemeChanged = mainViewModel::onThemeChanged,
        onSignOutClick = {
            mainViewModel.onSignOut()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithPagerContent(
    navigationActions: NavigationActions,
    isOnline: Boolean,
    initialPage: Int,
    homeContent: @Composable (padding: PaddingValues, userRole: UserType?) -> Unit,
    researchesContent: @Composable (padding: PaddingValues, userRole: UserType?) -> Unit,
    documentsContent: @Composable (padding: PaddingValues, userRole: UserType?) -> Unit,
    groupsContent: (@Composable (padding: PaddingValues, userRole: UserType?) -> Unit)?,
    uiState: MainUiState,
    onThemeChanged: (ThemeMode) -> Unit,
    onSignOutClick: () -> Unit
) {
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
            listOf(
                BottomNavItem(
                    route = AppDestination.Researches,
                    title = "Researches",
                    selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                    unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks
                )
            )
        }
    }

    // Pager state - number of pages based on user role
    val pageCount = if (isOnline) {
        if (uiState.userRole == UserType.TEACHER && groupsContent != null) 4 else 3
    } else {
        1 // Only Researches in offline mode
    }

    val pagerState = rememberPagerState(
        initialPage = if (isOnline) initialPage.coerceIn(0, pageCount - 1) else 0,
        pageCount = { pageCount }
    )

    // Sync pager with navigation
    val currentRoute = remember(pagerState.currentPage, isOnline) {
        if (!isOnline) {
            AppDestination.Researches.route
        } else {
            when (pagerState.currentPage) {
                0 -> AppDestination.Home.route
                1 -> AppDestination.Researches.route
                2 -> AppDestination.Documents.route
                3 -> AppDestination.Groups.route
                else -> AppDestination.Home.route
            }
        }
    }

    // Title based on current page
    val title = remember(pagerState.currentPage, isOnline) {
        if (!isOnline) {
            "Researches (Cached)"
        } else {
            when (pagerState.currentPage) {
                0 -> "Home"
                1 -> "Researches"
                2 -> "Documents"
                3 -> "Groups"
                else -> "CREST"
            }
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
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                userRole = uiState.userRole,
                userPhotoUrl = uiState.userPhotoUrl,
                isOnline = isOnline,
                currentTheme = uiState.themeMode,
                onAccountsClick = if (uiState.userRole == UserType.TEACHER && isOnline) {
                    { navigationActions.navigateTo(AppDestination.Accounts) }
                } else null,
                onFavoritesClick = if (isOnline) {
                    { navigationActions.navigateTo(AppDestination.Favorites) }
                } else null,
                onAboutClick = { navigationActions.navigateTo(AppDestination.AboutUs) },
                onStorageClick = { navigationActions.navigateToStorageManagement() },
                onThemeChange = onThemeChanged,
                onSignOutClick = {
                    onSignOutClick()
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
                userName = uiState.userName,
                userPhotoUrl = uiState.userPhotoUrl,
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
                    CrestBottomNavBarWithPager(
                        items = bottomNavItems,
                        pagerState = pagerState,
                        onItemClick = { index ->
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                } else {
                    OfflineBottomNavBar(
                        currentRoute = AppDestination.Researches.route,
                        onResearchesClick = { }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isOnline) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1 // Preload adjacent pages
            ) { page ->
                when (page) {
                    0 -> homeContent(paddingValues, uiState.userRole)
                    1 -> researchesContent(paddingValues, uiState.userRole)
                    2 -> documentsContent(paddingValues, uiState.userRole)
                    3 -> groupsContent?.invoke(paddingValues, uiState.userRole)
                }
            }
        } else {
            // Offline mode - only show researches
            researchesContent(paddingValues, uiState.userRole)
        }
    }
}

@Preview(name = "Main Screen With Pager")
@Composable
private fun MainScreenWithPagerPreview() {
    CRESTTheme {
        MainScreenWithPagerContent(
            navigationActions = NavigationActions(rememberNavController()),
            isOnline = true,
            initialPage = 0,
            homeContent = { padding, _ ->
                Box(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()) {
                    Text(text = "Home Screen")
                }
            },
            researchesContent = { padding, _ ->
                Box(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()) {
                    Text(text = "Researches Screen")
                }
            },
            documentsContent = { padding, _ ->
                Box(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()) {
                    Text(text = "Documents Screen")
                }
            },
            groupsContent = { padding, _ ->
                Box(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()) {
                    Text(text = "Groups Screen")
                }
            },
            uiState = MainUiState(
                userRole = UserType.TEACHER,
                userName = "Algen Cadiogan",
                userEmail = "algen.cadiogan@email.com",
            ),
            onThemeChanged = {},
            onSignOutClick = {}
        )
    }
}