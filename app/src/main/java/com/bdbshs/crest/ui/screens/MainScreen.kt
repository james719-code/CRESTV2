package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.R // Assuming you have a strings.xml
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.navigation.NavigationActions
import com.bdbshs.crest.ui.viewmodels.MainViewModel
import com.bdbshs.crest.ui.viewmodels.UserType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
    // The content lambda now provides the userRole to its caller
    content: @Composable (padding: PaddingValues, userRole: UserType?) -> Unit
) {
    // Instantiate the ViewModel here. It's scoped to the MainScreen's lifecycle.
    val mainViewModel: MainViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            snackbarHostState.showSnackbar(
                message = "You are currently offline. Functionality is limited.",
                duration = SnackbarDuration.Short
            )
        }
    }

    val allDrawerItems = remember {
        listOf(
            DrawerItem(AppDestination.Home, "Home", Icons.Default.Home),
            DrawerItem(AppDestination.Researches, "Researches", Icons.Default.Search),
            DrawerItem(AppDestination.Documents, "Documents", Icons.Default.Description),
            DrawerItem(AppDestination.Settings, "Settings", Icons.Default.Settings),
            DrawerItem(AppDestination.Groups, "Groups", Icons.Default.Diversity3),
            DrawerItem(AppDestination.Accounts, "Accounts", Icons.Default.Group)
        )
    }

    val itemsToShow = remember(uiState.userRole, isOnline) {
        if (isOnline) {
            when (uiState.userRole) {
                UserType.STUDENT -> allDrawerItems.filter {
                    it.route in listOf(AppDestination.Home, AppDestination.Researches, AppDestination.Documents, AppDestination.Settings)
                }
                UserType.TEACHER -> allDrawerItems // Show all for teacher
                else -> emptyList()
            }
        } else {
            allDrawerItems.filter { it.route == AppDestination.Researches }
        }
    }

    val title by remember(currentRoute, itemsToShow) {
        derivedStateOf {
            itemsToShow.find { it.route.route == currentRoute }?.title ?: "CREST"
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
                onSignOutClick = if (isOnline) { { FirebaseClient.auth.signOut() } } else null
            )
        },
        gesturesEnabled = drawerState.isOpen
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
            // Render the screen content and pass the userRole from our ViewModel state
            content(paddingValues, uiState.userRole)
        }
    }
}


@Composable
private fun AppDrawer(
    items: List<DrawerItem>,
    currentRoute: String?,
    onItemClick: (AppDestination) -> Unit,
    onSignOutClick: (() -> Unit)? // Nullable to hide the button when offline
) {
    ModalDrawerSheet {
        Column(Modifier.fillMaxWidth()) {
            // 1. A visually distinct header
            DrawerHeader()
            HorizontalDivider()

            // 2. Navigation Items
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

            // 3. A flexible spacer to push the sign-out button to the bottom
            Spacer(Modifier.weight(1f))

            // 4. Sign-out action, separated by a divider
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
            text = "CREST", // Or use stringResource(R.string.app_name)
            style = MaterialTheme.typography.titleLarge
        )
    }
}