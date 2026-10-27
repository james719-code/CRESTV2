package com.bdbshs.crest.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.ui.screens.*
import com.bdbshs.crest.ui.viewmodels.LoginResult
import com.bdbshs.crest.ui.viewmodels.MainViewModel // Import MainViewModel
import com.bdbshs.crest.ui.viewmodels.GroupDeletionViewModel
import com.bdbshs.crest.ui.viewmodels.PlanningViewModel
import com.bdbshs.crest.ui.viewmodels.UserType
import com.bdbshs.crest.ui.viewmodels.SignUpDetailsViewModel
import com.google.firebase.auth.FirebaseAuth // Make sure FirebaseAuth is imported

// --- NAVIGATION DEFINITIONS ---
sealed class AppDestination(val route: String) {
    object Login : AppDestination("login")
    object SignUpDetails : AppDestination("sign_up_details")
    object PendingApproval : AppDestination("pending_approval")
    object Home : AppDestination("home")
    object Researches : AppDestination("researches")
    object Accounts : AppDestination("accounts")
    object Documents : AppDestination("documents")
    object Favorites : AppDestination("favorites")
    object Milestones : AppDestination("milestones")
    object Inbox : AppDestination("inbox")
    object AboutUs : AppDestination("about_us")
    object GroupUpload : AppDestination("group_upload")
    object TeacherUpload : AppDestination("teacher_upload")

    object ResearchDetails : AppDestination("research_details") {
        const val routeWithArg = "research_details/{researchId}"
        const val researchIdArg = "researchId"
    }

    object Groups : AppDestination("groups")
    object GroupDetail : AppDestination("group_detail") {
        const val routeWithArg = "group_detail/{groupId}"
        const val groupIdArg = "groupId"
    }
    object StorageManagement : AppDestination("storage_management")
}

// --- NAVIGATION ACTIONS ---
class NavigationActions(private val navController: NavController) {
    fun navigateTo(dest: AppDestination) {
        val isMainTab = dest in listOf(AppDestination.Home, AppDestination.Researches, AppDestination.Documents, AppDestination.Groups)
        navController.navigate(dest.route) {
            if (isMainTab) {
                // A tab chosen from a secondary page must open that tab, rather than
                // restore an unrelated page inside the previous Home pager.
                popUpTo(navController.graph.id) { inclusive = true }
            } else {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            }
            launchSingleTop = true
            restoreState = !isMainTab
        }
    }
    fun navigateToHome() = navController.navigate(AppDestination.Home.route) {
        popUpTo(navController.graph.id) { inclusive = true }
    }
    fun navigateToSignUpDetails() = navController.navigate(AppDestination.SignUpDetails.route) {
        popUpTo(navController.graph.id) { inclusive = true }
    }
    fun navigateToPendingApproval() = navController.navigate(AppDestination.PendingApproval.route) {
        popUpTo(navController.graph.id) { inclusive = true }
    }
    fun navigateToGroupUpload() = navController.navigate(AppDestination.GroupUpload.route)
    fun navigateToTeacherUpload() = navController.navigate(AppDestination.TeacherUpload.route)
    fun navigateToResearchDetails(researchId: String) =
        navController.navigate("${AppDestination.ResearchDetails.route}/$researchId")
    fun navigateBack() = navController.popBackStack()
    fun navigateToGroupDetails(groupId: String) =
        navController.navigate("${AppDestination.GroupDetail.route}/$groupId")
    fun navigateToStorageManagement() = navController.navigate(AppDestination.StorageManagement.route)
    fun navigateToFavorites() = navController.navigate(AppDestination.Favorites.route)
}

// --- NETWORK STATE HOLDER ---
@Composable
private fun rememberNetworkState(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = context.isCurrentlyConnected()) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = true }
            override fun onLost(network: Network) { value = false }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitDispose { manager.unregisterNetworkCallback(callback) }
    }
}
private fun Context.isCurrentlyConnected(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager.activeNetwork != null
}

// --- APP ENTRY POINT ---
@Composable
fun CrestApp() {
    val navController = rememberNavController()
    val actions       = remember(navController) { NavigationActions(navController) }
    val isOnline      by rememberNetworkState()
    val auth          = FirebaseClient.auth

    val mainVm  : MainViewModel = hiltViewModel()
    val groupDeletionVm: GroupDeletionViewModel = hiltViewModel()
    val uiState by mainVm.uiState.collectAsStateWithLifecycle()
    val planningVm: PlanningViewModel = hiltViewModel()
    LaunchedEffect(uiState.currentUid, uiState.userRole, uiState.isAllowedOffline, uiState.isLoading) {
        planningVm.bind(uiState.currentUid, uiState.userRole, uiState.isAllowedOffline && !uiState.isLoading)
    }

    SessionNavigationHost(
        isLoading = uiState.isLoading,
        initialDestination = {
            when {
                auth.currentUser == null -> AppDestination.Login.route
                !uiState.isAllowedOffline -> AppDestination.PendingApproval.route
                !isOnline -> AppDestination.Researches.route
                else -> AppDestination.Home.route
            }
        }
    ) { startRoute ->
        DisposableEffect(auth) {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                if (firebaseAuth.currentUser == null) {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            auth.addAuthStateListener(listener)
            onDispose { auth.removeAuthStateListener(listener) }
        }
        CrestNavHost(navController, actions, startRoute, isOnline, mainVm, groupDeletionVm, planningVm)
    }
}


// --- SINGLE NAVIGATION HOST ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrestNavHost(
    navController: NavHostController,
    navigationActions: NavigationActions,
    startDestination: String,
    isOnline: Boolean,
    mainViewModel: MainViewModel,
    groupDeletionViewModel: GroupDeletionViewModel,
    planningViewModel: PlanningViewModel
) {
    val planningState by planningViewModel.uiState.collectAsStateWithLifecycle()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        // --- AUTH FLOW ROUTES ---
        composable(AppDestination.Login.route) {
            LoginScreen(
                onNavigateToHome = navigationActions::navigateToHome,
                onNavigateToSignUpDetails = navigationActions::navigateToSignUpDetails,
                onNavigateToPendingApproval = navigationActions::navigateToPendingApproval,
                loginViewModel = hiltViewModel()
            )
        }
        composable(AppDestination.SignUpDetails.route) {
            val signUpDetailsViewModel: SignUpDetailsViewModel = hiltViewModel()

            SignUpDetails(
                onNavigate = { loginResult ->
                    when (loginResult) {
                        LoginResult.NavigateToHome -> {
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        LoginResult.NavigateToPendingApproval -> {
                            navController.navigate(AppDestination.PendingApproval.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        LoginResult.NavigateToSignUpDetails -> {
                        }
                    }
                },
                viewModel = signUpDetailsViewModel
            )
        }
        composable(AppDestination.PendingApproval.route) {
            PendingApprovalScreen(onSignOut = mainViewModel::onSignOut)
        }

        // --- SECONDARY ROUTES (No MainScreen wrapper) ---
        composable(AppDestination.GroupUpload.route) {
            GroupUploadScreen(onNavigateBack = navigationActions::navigateBack)
        }
        composable(AppDestination.TeacherUpload.route) {
            UploadResearchScreen(onNavigateBack = navigationActions::navigateBack)
        }
        composable(
            route = AppDestination.ResearchDetails.routeWithArg,
            arguments = listOf(navArgument(AppDestination.ResearchDetails.researchIdArg) { type = NavType.StringType })
        ) {
            ResearchDetailScreen(onNavigateBack = navigationActions::navigateBack)
        }
        composable(
            route = AppDestination.GroupDetail.routeWithArg,
            arguments = listOf(navArgument(AppDestination.GroupDetail.groupIdArg) { type = NavType.StringType })
        ) {
            GroupDetailScreen(
                onNavigateBack = navigationActions::navigateBack,
                onDeleteGroup = { group ->
                    groupDeletionViewModel.deleteGroup(group)
                    navigationActions.navigateTo(AppDestination.Groups)
                }
            )
        }
        composable(AppDestination.StorageManagement.route) {
            StorageManagementScreen(onNavigateBack = navigationActions::navigateBack)
        }
        listOf(AppDestination.Milestones, AppDestination.Inbox).forEach { destination ->
            composable(destination.route) {
                MainScreen(navigationActions = navigationActions, currentRoute = destination.route,
                    isOnline = isOnline, mainViewModel = mainViewModel,
                    unreadNotifications = planningState.records.notifications.count { !it.read },
                    onNavigationClick = { navigationActions.navigateBack() }) { padding, role ->
                    PlanningScreen(planningViewModel, destination == AppDestination.Inbox, Modifier.padding(padding),
                        onOpenGroup = { id -> if (role == UserType.TEACHER) navigationActions.navigateToGroupDetails(id) else navigationActions.navigateToHome() },
                        onOpenAccounts = { navigationActions.navigateTo(AppDestination.Accounts) })
                }
            }
        }

        // --- PRIMARY ROUTES ---
        // Main pager screen with swipeable tabs
        composable(AppDestination.Home.route) {
            MainPagerRouteContent(
                navigationActions = navigationActions,
                isOnline = isOnline,
                initialPage = 0,
                mainViewModel = mainViewModel,
                groupDeletionViewModel = groupDeletionViewModel,
                unreadNotifications = planningState.records.notifications.count { !it.read }
            )
        }
        composable(AppDestination.Researches.route) {
            MainPagerRouteContent(
                navigationActions = navigationActions,
                isOnline = isOnline,
                initialPage = if (isOnline) 1 else 0,
                mainViewModel = mainViewModel,
                groupDeletionViewModel = groupDeletionViewModel,
                unreadNotifications = planningState.records.notifications.count { !it.read }
            )
        }
        composable(AppDestination.Accounts.route) {
            MainScreen(
                navigationActions = navigationActions, 
                currentRoute = AppDestination.Accounts.route, 
                isOnline = isOnline,
                mainViewModel = mainViewModel,
                unreadNotifications = planningState.records.notifications.count { !it.read },
                onNavigationClick = navigationActions::navigateBack
            ) { padding, _ ->
                AccountsScreen(modifier = Modifier.padding(padding))
            }
        }
        composable(AppDestination.Groups.route) {
            MainPagerRouteContent(
                navigationActions = navigationActions,
                isOnline = isOnline,
                initialPage = 3,
                mainViewModel = mainViewModel,
                groupDeletionViewModel = groupDeletionViewModel,
                unreadNotifications = planningState.records.notifications.count { !it.read }
            )
        }
        composable(AppDestination.Documents.route) {
            MainPagerRouteContent(
                navigationActions = navigationActions,
                isOnline = isOnline,
                initialPage = 2,
                mainViewModel = mainViewModel,
                groupDeletionViewModel = groupDeletionViewModel,
                unreadNotifications = planningState.records.notifications.count { !it.read }
            )
        }
        composable(AppDestination.Favorites.route) {
            MainScreen(
                navigationActions = navigationActions,
                currentRoute = AppDestination.Favorites.route,
                isOnline = isOnline,
                mainViewModel = mainViewModel,
                unreadNotifications = planningState.records.notifications.count { !it.read },
                onNavigationClick = navigationActions::navigateBack
            ) { padding, userRole ->
                ResearchesScreen(
                    modifier = Modifier.padding(padding),
                    userRole = userRole,
                    isOnline = isOnline,
                    onNavigateToDetails = navigationActions::navigateToResearchDetails,
                    favoritesOnly = true
                )
            }
        }
        composable(AppDestination.AboutUs.route) {
            MainScreen(navigationActions = navigationActions, currentRoute = AppDestination.AboutUs.route, isOnline = isOnline, mainViewModel = mainViewModel,
                unreadNotifications = planningState.records.notifications.count { !it.read }) { padding, _ ->
                AboutUsScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun MainPagerRouteContent(
    navigationActions: NavigationActions,
    isOnline: Boolean,
    initialPage: Int,
    mainViewModel: MainViewModel,
    groupDeletionViewModel: GroupDeletionViewModel,
    unreadNotifications: Int = 0
) {
    val deletionState by groupDeletionViewModel.uiState.collectAsStateWithLifecycle()
    MainScreenWithPager(
        navigationActions = navigationActions,
        isOnline = isOnline,
        initialPage = initialPage,
        mainViewModel = mainViewModel,
        unreadNotifications = unreadNotifications,
        homeContent = { padding, userRole, onOpenGroups ->
            HomeScreen(
                modifier = Modifier.padding(padding),
                userRole = userRole,
                onNavigateToGroupUpload = navigationActions::navigateToGroupUpload,
                onNavigateToResearchDetails = navigationActions::navigateToResearchDetails,
                onNavigateToTeacherUpload = navigationActions::navigateToTeacherUpload,
                onNavigateToAccounts = { navigationActions.navigateTo(AppDestination.Accounts) },
                onNavigateToGroups = onOpenGroups
            )
        },
        researchesContent = { padding, userRole ->
            ResearchesScreen(
                modifier = Modifier.padding(padding),
                userRole = userRole,
                isOnline = isOnline,
                onNavigateToDetails = navigationActions::navigateToResearchDetails
            )
        },
        documentsContent = { padding, userRole ->
            DocumentsScreen(
                modifier = Modifier.padding(padding),
                userRole = userRole,
                viewModel = hiltViewModel(),
                onGoHome = navigationActions::navigateToHome
            )
        },
        groupsContent = { padding, _ ->
            GroupsScreen(
                modifier = Modifier.padding(padding),
                deletionState = deletionState,
                onRetryDelete = groupDeletionViewModel::retry,
                onNavigateToDetails = navigationActions::navigateToGroupDetails
            )
        }
    )
}
