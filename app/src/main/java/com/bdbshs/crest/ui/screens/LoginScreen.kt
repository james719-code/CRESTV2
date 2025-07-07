package com.bdbshs.crest.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.R
import com.bdbshs.crest.ui.viewmodels.LoginResult
import com.bdbshs.crest.ui.viewmodels.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignUpDetails: () -> Unit,
    onNavigateToPendingApproval: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by loginViewModel.uiState.collectAsState()

    // --- Modern Credential Manager Logic ---
    val credentialManager = remember { CredentialManager.create(context) }

    val signIn: () -> Unit = {
        coroutineScope.launch {
            // 1. Create the request for a Google ID token
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all Google accounts on the device
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 2. Launch the One-Tap UI
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                // 3. On success, send the result to the ViewModel
                loginViewModel.signInWithGoogleCredential(result)

            } catch (e: GetCredentialException) {
                // Handle errors, such as user canceling the flow
                Log.e("LoginScreen", "GetCredentialException", e)
                snackbarHostState.showSnackbar("Sign-in was canceled or failed.")
            }
        }
    }
    // --- End Credential Manager Logic ---

    // Listen for navigation events from the ViewModel (This logic is unchanged)
    LaunchedEffect(key1 = Unit) {
        loginViewModel.loginResult.collect { result ->
            when (result) {
                is LoginResult.NavigateToHome -> onNavigateToHome()
                is LoginResult.NavigateToSignUpDetails -> onNavigateToSignUpDetails()
                is LoginResult.NavigateToPendingApproval -> onNavigateToPendingApproval()
            }
        }
    }

    // Show error snackbar (This logic is unchanged)
    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(errorMessage)
            loginViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                // This will now show for the initial session check, hiding the button
                CircularProgressIndicator()
            } else {
                LoginScreenContent(
                    isLoading = uiState.isLoading, // This will be false here
                    onGoogleLoginClick = signIn
                )
            }
        }
    }
}

// The actual UI content composable remains completely unchanged.
@Composable
private fun LoginScreenContent(
    isLoading: Boolean,
    onGoogleLoginClick: () -> Unit
) {
    // ... This function is identical to the one you provided ...
    // ... No changes are needed here at all. ...
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Header
        Text(
            text = "Welcome to Crest",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sub-header
        Text(
            text = "Sign in to continue and access all features.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Google Login Button
        Button(
            onClick = onGoogleLoginClick,
            enabled = !isLoading, // Disable button when loading
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Optional: Terms and Conditions text
        Text(
            text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}