package com.bdbshs.crest.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.bdbshs.crest.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import coil.compose.AsyncImage
import com.bdbshs.crest.ui.theme.*
import com.bdbshs.crest.ui.viewmodels.UserType
import com.bdbshs.crest.data.ThemeMode

private val MainTabTitles = listOf("Home", "Research", "Documents", "Groups")

/**
 * Standard Material top app bar. Main tabs carry the CREST mark; secondary screens use a back arrow and their own title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrestTopAppBar(
    title: String,
    userRole: UserType?,
    userName: String? = null,
    userPhotoUrl: String? = null,
    isOnline: Boolean = true,
    onProfileClick: () -> Unit = {},
    onNavigationClick: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier,
    unreadNotifications: Int = 0
) {
    val isMainTab = title in MainTabTitles
    TopAppBar(
        navigationIcon = {
            if (onNavigationClick != null) {
                IconButton(onClick = onNavigationClick, Modifier.padding(start = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else if (isMainTab) {
                CrestMark(36.dp, Modifier.padding(start = 20.dp, end = 4.dp))
            }
        },
        title = {
            Column {
                Text(
                    text = if (title == "Home") "CREST" else title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isOnline) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.CloudOff, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Offline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        actions = {
            onNotificationClick?.let { onClick ->
                IconButton(onClick = onClick) {
                    BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text(if (unreadNotifications > 99) "99+" else unreadNotifications.toString()) } }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications, $unreadNotifications unread")
                    }
                }
            }
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (userPhotoUrl != null) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "Account menu",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Account menu",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

/**
 * Profile/Settings Bottom Sheet content
 */
@Composable
fun ProfileBottomSheet(
    userName: String?,
    userEmail: String?,
    userRole: UserType?,
    userPhotoUrl: String?,
    isOnline: Boolean,
    currentTheme: ThemeMode = ThemeMode.SYSTEM,
    onAccountsClick: (() -> Unit)? = null,
    onFavoritesClick: (() -> Unit)? = null,
    onMilestonesClick: (() -> Unit)? = null,
    onInboxClick: (() -> Unit)? = null,
    onAboutClick: () -> Unit,
    onStorageClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onThemeChange: (ThemeMode) -> Unit = {},
    onSignOutClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    CrestDialogSystemBars()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User info header
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (userPhotoUrl != null) {
                AsyncImage(
                    model = userPhotoUrl,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = userName ?: "User",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        userEmail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Role badge
        userRole?.let { role ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = role.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Menu items
        if (isOnline) {
            onMilestonesClick?.let { onClick -> ProfileMenuItem(
                icon = Icons.Outlined.CalendarMonth, title = "Deadlines & milestones", subtitle = "Follow research deadlines",
                onClick = { onClick(); onDismiss() }) }
            onInboxClick?.let { onClick -> ProfileMenuItem(
                icon = Icons.Outlined.Notifications, title = "Notification inbox", subtitle = "Submissions, approvals, and feedback",
                onClick = { onClick(); onDismiss() }) }
            // Accounts (Teacher only)
            onAccountsClick?.let { onClick ->
                ProfileMenuItem(
                    icon = Icons.Outlined.ManageAccounts,
                    title = "Manage Accounts",
                    subtitle = "Approve or manage users",
                    onClick = {
                        onClick()
                        onDismiss()
                    }
                )
            }

            onFavoritesClick?.let { onClick ->
                ProfileMenuItem(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "Favorites",
                    subtitle = "View your saved papers",
                    onClick = {
                        onClick()
                        onDismiss()
                    }
                )
            }
            
            ProfileMenuItem(
                icon = Icons.Outlined.Info,
                title = "About CREST",
                subtitle = "Learn about this app",
                onClick = {
                    onAboutClick()
                    onDismiss()
                }
            )
        }
        
        // Storage Management - available both online and offline
        ProfileMenuItem(
            icon = Icons.Outlined.Storage,
            title = "Storage",
            subtitle = "Manage cached files",
            onClick = {
                onStorageClick()
                onDismiss()
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Theme Selection
        ThemeSelector(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange
        )
            
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isOnline) {
            // Sign out
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                subtitle = "Log out of your account",
                onClick = onSignOutClick,
                tint = MaterialTheme.colorScheme.error
            )
        } else {
            // Offline message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Limited options while offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (tint == MaterialTheme.colorScheme.error)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = tint
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
@Composable
private fun ThemeSelector(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOption(
                mode = ThemeMode.SYSTEM,
                selected = currentTheme == ThemeMode.SYSTEM,
                onClick = { onThemeChange(ThemeMode.SYSTEM) },
                icon = Icons.Outlined.SettingsSuggest,
                label = "System",
                modifier = Modifier.weight(1f)
            )
            ThemeOption(
                mode = ThemeMode.LIGHT,
                selected = currentTheme == ThemeMode.LIGHT,
                onClick = { onThemeChange(ThemeMode.LIGHT) },
                icon = Icons.Outlined.LightMode,
                label = "Light",
                modifier = Modifier.weight(1f)
            )
            ThemeOption(
                mode = ThemeMode.DARK,
                selected = currentTheme == ThemeMode.DARK,
                onClick = { onThemeChange(ThemeMode.DARK) },
                icon = Icons.Outlined.DarkMode,
                label = "Dark",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics {
            this.selected = selected
            role = Role.RadioButton
        },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
