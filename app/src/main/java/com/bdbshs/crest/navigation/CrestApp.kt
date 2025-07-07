package com.bdbshs.crest.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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
}

// --- NAVIGATION ACTIONS ---
class NavigationActions(private val navController: NavController) {
    fun navigateTo(dest: AppDestination) {
        navController.navigate(dest.route) {
            // Pop up to the start destination of the graph to avoid a deep back stack
            // and ensure single instance of the destination
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun navigateToHome() = navigateTo(AppDestination.Home)
    fun navigateToSignUpDetails() = navController.navigate(AppDestination.SignUpDetails.route) {
        // When going to signup details from login, replace login screen
        popUpTo(AppDestination.Login.route) { inclusive = true }
    }
    fun navigateToPendingApproval() = navController.navigate(AppDestination.PendingApproval.route) {
        // When going to pending approval from login or signup, replace the previous screens
        popUpTo(AppDestination.Login.route) { inclusive = true } // Pop up to login route (clearing it)
    }
    fun navigateToGroupUpload() = navController.navigate(AppDestination.GroupUpload.route)
    fun navigateToTeacherUpload() = navController.navigate(AppDestination.TeacherUpload.route)
    fun navigateToResearchDetails(researchId: String) =
        navController.navigate("${AppDestination.ResearchDetails.route}/$researchId")
    fun navigateBack() = navController.popBackStack()
    fun navigateToGroupDetails(groupId: String) =
        navController.navigate("${AppDestination.GroupDetail.route}/$groupId")
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

    // Correct way to get AndroidViewModel instance
    val mainVm  : MainViewModel = viewModel()
    val uiState by mainVm.uiState.collectAsState()

    // Show a loading spinner until MainViewModel has determined the initial user state
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return // Don't proceed with NavHost until loading is complete
    }

    // Determine the start route based on authentication, approval status, and network state
    val startRoute = remember(auth.currentUser, uiState.isAllowedOffline, isOnline) {
        when {
            // 1. No user authenticated: always go to Login
            auth.currentUser == null -> AppDestination.Login.route
            // 2. User authenticated, but not allowed (i.e., not accepted/approved): always go to PendingApproval
            !uiState.isAllowedOffline -> AppDestination.PendingApproval.route
            // 3. User authenticated AND allowed:
            //    a. If offline, go to Researches (to access cached content)
            !isOnline -> AppDestination.Researches.route
            //    b. Otherwise (online and allowed), go to Home
            else -> AppDestination.Home.route
        }
    }

    // This DisposableEffect is for handling logout *during* app runtime
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                // Navigate to login and clear back stack when user logs out
                navController.navigate(AppDestination.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    // Do not save state here if logging out, start fresh.
                    launchSingleTop = true
                }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Once the startRoute is determined and loading is complete, build the NavHost
    CrestNavHost(navController, actions, startRoute, isOnline)
}


// --- SINGLE NAVIGATION HOST ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrestNavHost(
    navController: NavHostController,
    navigationActions: NavigationActions,
    startDestination: String, // This now comes directly from CrestApp's initial determination
    isOnline: Boolean
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // --- AUTH FLOW ROUTES ---
        composable(AppDestination.Login.route) {
            // LoginScreen's ViewModel handles its own initial checks for user status (if online).
            // It will then emit the correct LoginResult for navigation.
            LoginScreen(
                onNavigateToHome = navigationActions::navigateToHome,
                onNavigateToSignUpDetails = navigationActions::navigateToSignUpDetails,
                onNavigateToPendingApproval = navigationActions::navigateToPendingApproval,
                loginViewModel = viewModel()
            )
        }
        composable(AppDestination.SignUpDetails.route) {
            val signUpDetailsViewModel: SignUpDetailsViewModel = viewModel()

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
                            // This scenario should not occur from SignUpDetails itself
                            // It would mean staying on the current screen or an unexpected loop.
                        }
                    }
                },
                viewModel = signUpDetailsViewModel
            )
        }
        composable(AppDestination.PendingApproval.route) {
            PendingApprovalScreen()
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
            GroupDetailScreen(onNavigateBack = navigationActions::navigateBack)
        }

        // --- PRIMARY ROUTES (Individually wrapped in the MainScreen layout) ---
        composable(AppDestination.Home.route) {
            MainScreen(navigationActions = navigationActions, currentRoute = AppDestination.Home.route, isOnline = isOnline) { padding, userRole ->
                HomeScreen(
                    modifier = Modifier.padding(padding),
                    userRole = userRole,
                    onNavigateToGroupUpload = navigationActions::navigateToGroupUpload,
                    onNavigateToResearchDetails = navigationActions::navigateToResearchDetails,
                    onNavigateToTeacherUpload = navigationActions::navigateToTeacherUpload
                )
            }
        }
        composable(AppDestination.Researches.route) {
            MainScreen(navigationActions = navigationActions, currentRoute = AppDestination.Researches.route, isOnline = isOnline) { padding, userRole ->
                ResearchesScreen(
                    modifier = Modifier.padding(padding),
                    userRole = userRole,
                    isOnline = isOnline,
                    onNavigateToDetails = navigationActions::navigateToResearchDetails
                )
            }
        }
        composable(AppDestination.Accounts.route) {
            MainScreen(navigationActions = navigationActions, currentRoute = AppDestination.Accounts.route, isOnline = isOnline) { padding, _ ->
                AccountsScreen(modifier = Modifier.padding(padding))
            }
        }
        composable(AppDestination.Groups.route) {
            MainScreen(navigationActions = navigationActions, currentRoute = AppDestination.Groups.route, isOnline = isOnline) { padding, _ ->
                GroupsScreen(
                    modifier = Modifier.padding(padding),
                    onNavigateToDetails = navigationActions::navigateToGroupDetails
                )
            }
        }
        composable(AppDestination.Documents.route) {
            MainScreen(navigationActions = navigationActions, currentRoute = AppDestination.Documents.route, isOnline = isOnline) { padding, userRole ->
                // Ensure userRole is passed to DocumentsScreen
                DocumentsScreen(
                    modifier = Modifier.padding(padding),
                    userRole = userRole,
                    viewModel = viewModel()
                )
            }
        }
        composable(AppDestination.AboutUs.route) {
            MainScreen(navigationActions = navigationActions, currentRoute = AppDestination.AboutUs.route, isOnline = isOnline) { padding, _ ->
                AboutUsScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}