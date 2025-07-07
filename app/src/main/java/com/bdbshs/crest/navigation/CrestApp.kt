package com.bdbshs.crest.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.ui.screens.*
import com.bdbshs.crest.ui.screens.drawer.PlaceholderScreen
import com.bdbshs.crest.ui.viewmodels.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

// --- NAVIGATION DEFINITIONS ---

sealed class AppDestination(val route: String) {
    object Login : AppDestination("login")
    object SignUpDetails : AppDestination("sign_up_details")
    object PendingApproval : AppDestination("pending_approval")
    object Home : AppDestination("home")
    object Researches : AppDestination("researches")
    object Accounts : AppDestination("accounts")
    object Groups : AppDestination("groups")
    object Documents : AppDestination("documents")
    object Settings : AppDestination("settings")
    object GroupUpload : AppDestination("group_upload")
    object TeacherUpload : AppDestination("teacher_upload")
    object ResearchDetails : AppDestination("research_details") {
        const val routeWithArg = "research_details/{researchId}"
        const val researchIdArg = "researchId"
    }
}

class NavigationActions(private val navController: NavController) {
    fun navigateTo(dest: AppDestination) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToHome() = navigateTo(AppDestination.Home)
    fun navigateToSignUpDetails() = navController.navigate(AppDestination.SignUpDetails.route) {
        popUpTo(AppDestination.Login.route) { inclusive = true }
    }
    fun navigateToPendingApproval() = navController.navigate(AppDestination.PendingApproval.route) {
        popUpTo(AppDestination.Login.route) { inclusive = true }
    }
    fun navigateToGroupUpload() = navController.navigate(AppDestination.GroupUpload.route)
    fun navigateToTeacherUpload() = navController.navigate(AppDestination.TeacherUpload.route)
    fun navigateToResearchDetails(researchId: String) =
        navController.navigate("${AppDestination.ResearchDetails.route}/$researchId")
    fun navigateBack() = navController.popBackStack()
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
    val actions = remember(navController) { NavigationActions(navController) }
    val isOnline by rememberNetworkState()
    val auth = FirebaseClient.auth // Use the client

    // This logic is clever and correct. It determines the starting point of the whole app.
    val startDestination = remember(isOnline, auth.currentUser) {
        when {
            auth.currentUser == null -> AppDestination.Login.route
            !isOnline -> AppDestination.Researches.route // If logged in but offline, go to Researches
            else -> AppDestination.Home.route // If logged in and online, go to Home
        }
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                // This correctly handles sign-out from anywhere in the app.
                navController.navigate(AppDestination.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    CrestNavHost(navController, actions, startDestination, isOnline)
}


// --- SINGLE NAVIGATION HOST (Corrected) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrestNavHost(
    navController: NavHostController,
    navigationActions: NavigationActions,
    startDestination: String,
    isOnline: Boolean
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // --- AUTH FLOW ROUTES (No MainScreen wrapper) ---
        composable(AppDestination.Login.route) {
            LoginScreen(
                onNavigateToHome = navigationActions::navigateToHome,
                onNavigateToSignUpDetails = navigationActions::navigateToSignUpDetails,
                onNavigateToPendingApproval = navigationActions::navigateToPendingApproval
            )
        }
        composable(AppDestination.SignUpDetails.route) {
            SignUpDetails(onNavigateToHome = navigationActions::navigateToHome)
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

        // --- PRIMARY ROUTES (These are wrapped in the MainScreen layout) ---
        // This is a much cleaner way to apply the same layout to multiple screens.
        mainScreenGraph(navController, navigationActions, isOnline)
    }
}

// --- NEW: Helper function to define the graph for screens within the MainScreen layout ---
private fun NavGraphBuilder.mainScreenGraph(
    navController: NavController,
    actions: NavigationActions,
    isOnline: Boolean
) {
    // Define all routes that should have the drawer, top bar, etc.
    val mainScreenRoutes = listOf(
        AppDestination.Home, AppDestination.Researches, AppDestination.Accounts,
        AppDestination.Groups, AppDestination.Documents, AppDestination.Settings
    )

    mainScreenRoutes.forEach { destination ->
        composable(destination.route) { backStackEntry ->
            // Use the MainScreen as the layout wrapper
            MainScreen(
                navigationActions = actions,
                currentRoute = backStackEntry.destination.route,
                isOnline = isOnline
            ) { paddingValues, userRole ->
                // This is the content area. We decide which actual screen to show
                // based on the destination route.
                when (destination) {
                    AppDestination.Home -> HomeScreen(
                        modifier = Modifier.padding(paddingValues),
                        onNavigateToGroupUpload = actions::navigateToGroupUpload,
                        onNavigateToResearchDetails = actions::navigateToResearchDetails,
                        onNavigateToTeacherUpload = actions::navigateToTeacherUpload,
                        userRole = userRole
                    )
                    AppDestination.Researches -> ResearchesScreen(
                        modifier = Modifier.padding(paddingValues),
                        isOnline = isOnline,
                        onNavigateToDetails = actions::navigateToResearchDetails,
                        viewModel = viewModel(),
                        // Pass the userRole down
                        userRole = userRole,
                    )
                    AppDestination.Accounts -> AccountsScreen(modifier = Modifier.padding(paddingValues))
                    AppDestination.Groups -> GroupsScreen(modifier = Modifier.padding(paddingValues))
                    AppDestination.Documents -> PlaceholderScreen("Documents", Modifier.padding(paddingValues))
                    AppDestination.Settings -> PlaceholderScreen("Settings", Modifier.padding(paddingValues))
                    else -> {} // Should not happen
                }
            }
        }
    }
}