package com.bdbshs.crest.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.util.Log
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import com.bdbshs.crest.ui.components.CrestIllustration
import com.bdbshs.crest.ui.components.IconBadge
import com.bdbshs.crest.ui.components.heroContainer
import com.bdbshs.crest.ui.components.heroContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.R
import com.bdbshs.crest.ui.components.CrestMark
import com.bdbshs.crest.ui.components.FeedbackPanel
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.LoginResult
import com.bdbshs.crest.ui.viewmodels.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignUpDetails: () -> Unit,
    onNavigateToPendingApproval: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val credentialManager = remember { CredentialManager.create(context) }
    var isRequestingCredential by remember { mutableStateOf(false) }
    var credentialIssue by remember { mutableStateOf<String?>(null) }

    val signIn: () -> Unit = {
        if (!isRequestingCredential && !uiState.isLoading) {
            isRequestingCredential = true
            credentialIssue = null
            scope.launch {
                try {
                    val option = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(context.getString(R.string.default_web_client_id))
                        .build()
                    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                    loginViewModel.signInWithGoogleCredential(credentialManager.getCredential(context, request))
                } catch (e: NoCredentialException) {
                    credentialIssue = "No Google account is available on this device. Add your account in Settings, then return to sign in."
                } catch (e: GetCredentialException) {
                    Log.e("LoginScreen", "Google sign-in failed", e)
                    snackbarHostState.showSnackbar("Sign-in did not finish. Try again when you're ready.")
                } finally {
                    isRequestingCredential = false
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        loginViewModel.loginResult.collect { result ->
            when (result) {
                is LoginResult.NavigateToHome -> onNavigateToHome()
                is LoginResult.NavigateToSignUpDetails -> onNavigateToSignUpDetails()
                is LoginResult.NavigateToPendingApproval -> onNavigateToPendingApproval()
            }
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            loginViewModel.clearError()
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LoginScreenContent(
                isLoading = uiState.isLoading || isRequestingCredential,
                onGoogleLoginClick = signIn,
                credentialIssue = credentialIssue,
                onAddAccount = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ADD_ACCOUNT).putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google")))
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            )
        }
    }
}

@Composable
internal fun LoginScreenContent(
    isLoading: Boolean,
    onGoogleLoginClick: () -> Unit,
    credentialIssue: String? = null,
    onAddAccount: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginBrandHeader()
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "CREST helps students and teachers find, submit, review, and preserve school research.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            LoginActions(isLoading, onGoogleLoginClick, credentialIssue, onAddAccount)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                LoginFeature(Icons.Outlined.Search, "Find research", "Browse papers from your school by title, author, or strand.")
                LoginFeature(Icons.Outlined.Groups, "Work with your group", "Create a group and submit your research for review.")
                LoginFeature(Icons.Outlined.RateReview, "Get feedback", "Teachers review, comment, and approve submissions.")
            }
            Text(
                "Bonifacio D. Borebor Sr. High School",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoginBrandHeader() {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(heroContainer())
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrestMark(72.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "CREST",
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 1.68.sp),
                color = heroContent()
            )
            Text(
                "School research, shared.",
                style = MaterialTheme.typography.titleMedium,
                color = heroContent().copy(alpha = .85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
        }
    }
}

@Composable
private fun LoginFeature(icon: ImageVector, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoginActions(
    isLoading: Boolean,
    onGoogleLoginClick: () -> Unit,
    credentialIssue: String?,
    onAddAccount: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onGoogleLoginClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
            } else {
                Surface(color = Color.White, shape = CircleShape) {
                    Image(painterResource(R.drawable.ic_google_logo), null, Modifier.padding(4.dp).size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(if (isLoading) "Signing in…" else "Continue with Google", style = MaterialTheme.typography.labelLarge)
        }
        if (credentialIssue != null && onAddAccount != null) {
            FeedbackPanel("Add a Google account", credentialIssue, "Open account settings", onAddAccount)
        }
        Text(
            "New to CREST? Use your Google account to register. New student registrations require teacher approval.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Sign in · Light", showBackground = true, backgroundColor = 0xFFF5F7FB)
@Composable
private fun LoginPreview() { CRESTTheme { LoginScreenContent(false, {}) } }

@Preview(name = "Sign in · Dark", showBackground = true, backgroundColor = 0xFF0E1726)
@Composable
private fun LoginDarkPreview() { CRESTTheme(darkTheme = true) { LoginScreenContent(false, {}) } }

@Preview(name = "Sign in · Large text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun LoginLargeTextPreview() { CRESTTheme { LoginScreenContent(false, {}) } }

@Preview(name = "Sign in · Loading", showBackground = true)
@Composable
private fun LoginLoadingPreview() { CRESTTheme { LoginScreenContent(true, {}) } }

@Preview(name = "Sign in · Tablet", showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
private fun LoginTabletPreview() { CRESTTheme { LoginScreenContent(false, {}) } }
