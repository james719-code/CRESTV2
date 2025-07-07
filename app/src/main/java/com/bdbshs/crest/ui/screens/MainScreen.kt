package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.navigation.NavigationActions
import com.bdbshs.crest.ui.viewmodels.MainViewModel
import com.bdbshs.crest.ui.viewmodels.UserType
import kotlinx.coroutines.launch

data class DrawerItem(
    val route: AppDestination,
    val title: String,
    val icon: ImageVector
)

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            snackbarHostState.showSnackbar(
                message = "You are currently offline.",
                duration = SnackbarDuration.Short
            )
        }
    }

    // --- THIS IS THE FIX ---

    // Define the full list of all possible drawer items once.
    val allDrawerItems = remember {
        listOf(
            DrawerItem(AppDestination.Home, "Home", Icons.Default.Home),
            DrawerItem(AppDestination.Researches, "Researches", Icons.Default.Search),
            DrawerItem(AppDestination.Documents, "Documents", Icons.Default.Description),
            DrawerItem(AppDestination.AboutUs, "About Us", Icons.Default.Info),
            DrawerItem(AppDestination.Accounts, "Accounts", Icons.Default.Group),
            DrawerItem(AppDestination.Groups, "Groups", Icons.Default.Diversity3)
        )
    }

    // This logic now correctly constructs the list for each role.
    val itemsToShow = remember(uiState.userRole, isOnline) {
        if (isOnline) {
            when (uiState.userRole) {
                UserType.STUDENT -> allDrawerItems.filter {
                    it.route in listOf(
                        AppDestination.Home,
                        AppDestination.Researches,
                        AppDestination.Documents,
                        AppDestination.AboutUs
                    )
                }
                UserType.TEACHER -> allDrawerItems.filter {
                    it.route in listOf(
                        AppDestination.Home,
                        AppDestination.Researches,
                        AppDestination.Documents,
                        AppDestination.AboutUs,
                        AppDestination.Accounts,
                        AppDestination.Groups
                    )
                }
                else -> emptyList()
            }
        } else {
            // When offline, BOTH roles can only see Researches.
            listOfNotNull(allDrawerItems.find { it.route == AppDestination.Researches })
        }
    }

    // The title logic now correctly looks up from the FULL list of all possible items,
    // not just the visible ones. This prevents the title from disappearing.
    val title by remember(currentRoute, isOnline) {
        derivedStateOf {
            if (!isOnline && currentRoute == AppDestination.Researches.route) {
                "Researches (Offline)"
            } else {
                allDrawerItems.find { it.route.route == currentRoute }?.title ?: "CREST"
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                items = itemsToShow,
                currentRoute = currentRoute,
                onItemClick = { destination ->
                    if (destination.route != currentRoute) {
                        navigationActions.navigateTo(destination)
                    }
                    scope.launch { drawerState.close() }
                },
                onSignOutClick = if (isOnline) { { mainViewModel.onSignOut() } } else null
            )
        },
        gesturesEnabled = isOnline || drawerState.isOpen
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            content(paddingValues, uiState.userRole)
        }
    }
}


@Composable
private fun AppDrawer(
    items: List<DrawerItem>,
    currentRoute: String?,
    onItemClick: (AppDestination) -> Unit,
    onSignOutClick: (() -> Unit)?
) {
    ModalDrawerSheet {
        Column(Modifier.fillMaxWidth()) {
            DrawerHeader()
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            items.forEach { item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route.route,
                    onClick = { onItemClick(item.route) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            onSignOutClick?.let {
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, "Sign Out") },
                    label = { Text("Sign Out") },
                    selected = false,
                    onClick = it,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CREST",
            style = MaterialTheme.typography.titleLarge
        )
    }
}