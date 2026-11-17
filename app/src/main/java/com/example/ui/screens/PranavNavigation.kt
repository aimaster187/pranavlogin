package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.PranavLocalDatabase
import com.example.data.local.PranavPreferences
import com.example.data.remote.ApkInspectorEngine
import com.example.data.remote.PranavFirestoreRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.HolographicBackdrop
import com.example.ui.theme.PranavAdminTheme
import com.example.ui.viewmodel.PranavAdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_APPS = "apps_update"
const val ROUTE_INTEGRATION = "dex_hook_integration"
const val ROUTE_SECURITY = "security_licenses"
const val ROUTE_BOTS = "bots_ops"

private data class NavDest(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val pranavDestinations = listOf(
    NavDest(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavDest(ROUTE_APPS, R.string.nav_apps, Icons.Filled.Apps, Icons.Outlined.Apps),
    NavDest(ROUTE_INTEGRATION, R.string.nav_integration, Icons.Filled.Build, Icons.Outlined.Build),
    NavDest(ROUTE_SECURITY, R.string.nav_security, Icons.Filled.Key, Icons.Outlined.Key),
    NavDest(ROUTE_BOTS, R.string.nav_bots, Icons.Filled.SmartToy, Icons.Outlined.SmartToy)
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

const val ROOT_OWNER_EMAIL = "ai.master.bot.official@gmail.com"

@Composable
fun PranavRootShell() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val preferences = remember(context) { PranavPreferences(context.applicationContext) }
    val prefsState by preferences.settingsFlow.collectAsStateWithLifecycle(initialValue = com.example.data.local.AdminSettingsState())

    PranavAdminTheme(darkTheme = prefsState.darkThemeEnabled) {
        val user = currentUser
        when {
            user == null -> {
                AuthScreen(onAuthSuccess = { /* AuthStateListener updates automatically */ })
            }
            user.email?.equals(ROOT_OWNER_EMAIL, ignoreCase = true) != true || !user.isEmailVerified -> {
                UnauthorizedOwnerScreen(
                    signedInEmail = user.email ?: "unknown",
                    onSignOut = {
                        GoogleSignInHelper.signOut(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
            else -> {
                key(user.uid) {
                    AuthenticatedAdminConsole(
                        uid = user.uid,
                        email = user.email ?: ROOT_OWNER_EMAIL
                    )
                }
            }
        }
    }
}

@Composable
private fun UnauthorizedOwnerScreen(
    signedInEmail: String,
    onSignOut: () -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
        HolographicBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                com.example.ui.theme.GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Access Restricted", style = MaterialTheme.typography.headlineMedium, color = com.example.ui.theme.CrimsonError)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        text = "PRANAV permits access strictly for the Single Root Owner ($ROOT_OWNER_EMAIL). Current account ($signedInEmail) is not authorized.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 14.dp))
                    androidx.compose.material3.Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().testTag("unauthorized_sign_out_button")
                    ) {
                        Text(stringResource(id = R.string.sign_out))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedAdminConsole(
    uid: String,
    email: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline = rememberIsOffline(context)

    val viewModel: PranavAdminViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = PranavFirestoreRepository(db)
                val prefs = PranavPreferences(app)
                val localDb = PranavLocalDatabase.getInstance(app)
                val inspector = ApkInspectorEngine(app)
                PranavAdminViewModel(repo, prefs, localDb.cacheDao(), inspector, uid, email)
            }
        }
    )

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()
    val appsState by viewModel.appsState.collectAsStateWithLifecycle()
    val dialogsState by viewModel.dialogsState.collectAsStateWithLifecycle()
    val devicesState by viewModel.devicesState.collectAsStateWithLifecycle()
    val licensesState by viewModel.licensesState.collectAsStateWithLifecycle()
    val botsState by viewModel.botsState.collectAsStateWithLifecycle()
    val dexState by viewModel.dexReleasesState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val backendHealth by viewModel.backendHealth.collectAsStateWithLifecycle()
    val backendError by viewModel.backendHealthError.collectAsStateWithLifecycle()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsStateWithLifecycle()
    val apkReport by viewModel.apkReport.collectAsStateWithLifecycle()
    val isInspectingApk by viewModel.isInspectingApk.collectAsStateWithLifecycle()
    val apkInspectError by viewModel.apkInspectError.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        pranavDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(id = dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            HolographicBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            pranavDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                val label = stringResource(id = dest.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (isOffline) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = AmberWarning.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.offline_banner),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberWarning,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_DASHBOARD
                            ) {
                                composable(ROUTE_DASHBOARD) {
                                    DashboardScreen(
                                        ownerEmail = email,
                                        workspaceState = workspaceState,
                                        appsState = appsState,
                                        devicesState = devicesState,
                                        licensesState = licensesState,
                                        botsState = botsState,
                                        dexState = dexState,
                                        eventsState = eventsState,
                                        settings = settings,
                                        backendHealth = backendHealth,
                                        backendError = backendError,
                                        isCheckingHealth = isCheckingHealth,
                                        isOffline = isOffline,
                                        onCheckBackendHealth = viewModel::checkBackendHealth,
                                        onToggleMaintenance = viewModel::toggleMasterMaintenance,
                                        onToggleLockdown = viewModel::toggleEmergencyLockdown,
                                        onBootstrapWorkspace = viewModel::bootstrapDefaultWorkspaceIfNeeded,
                                        onNavigateToRoute = { route ->
                                            navController.navigate(route) { launchSingleTop = true }
                                        }
                                    )
                                }
                                composable(ROUTE_APPS) {
                                    AppsAndUpdateScreen(
                                        ownerUid = uid,
                                        appsState = appsState,
                                        dialogsState = dialogsState,
                                        onSaveApp = viewModel::saveApp,
                                        onDeleteApp = viewModel::deleteApp,
                                        onSaveDialog = viewModel::saveDialog
                                    )
                                }
                                composable(ROUTE_INTEGRATION) {
                                    IntegrationScreen(
                                        ownerUid = uid,
                                        dexState = dexState,
                                        apkReport = apkReport,
                                        isInspectingApk = isInspectingApk,
                                        apkInspectError = apkInspectError,
                                        onInspectApkUri = viewModel::inspectAuthorizedApk,
                                        onSaveDexRelease = viewModel::saveDexRelease
                                    )
                                }
                                composable(ROUTE_SECURITY) {
                                    SecurityAndLicensesScreen(
                                        appsState = appsState,
                                        devicesState = devicesState,
                                        licensesState = licensesState,
                                        onRegisterDevice = viewModel::registerDeviceRecord,
                                        onUpdateDeviceApproval = viewModel::updateDeviceApproval,
                                        onGenerateLicense = viewModel::generateLicense,
                                        onUpdateLicenseStatus = viewModel::updateLicenseStatus
                                    )
                                }
                                composable(ROUTE_BOTS) {
                                    BotsAndOpsScreen(
                                        botsState = botsState,
                                        eventsState = eventsState,
                                        workspaceState = workspaceState,
                                        settings = settings,
                                        onAddBot = viewModel::addAndValidateBot,
                                        onBotAction = viewModel::performBotAction,
                                        onDeleteBot = viewModel::deleteBot,
                                        onSendCommsEvent = viewModel::sendAnnouncementOrTicket,
                                        onResolveEvent = viewModel::resolveEvent,
                                        onToggleModule = viewModel::toggleAdminModule,
                                        onSaveBackendConfig = viewModel::saveBackendConfig,
                                        onSetDarkTheme = viewModel::setDarkTheme,
                                        onSetLowEndFallback = viewModel::setLowEndFallback,
                                        onSignOut = {
                                            GoogleSignInHelper.signOut(
                                                credentialManager = credentialManager,
                                                scope = scope
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberIsOffline(context: Context): Boolean {
    var offline by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        fun update() {
            val active = cm?.activeNetwork
            val caps = if (active != null) cm.getNetworkCapabilities(active) else null
            offline = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        update()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { offline = false }
            override fun onLost(network: Network) { update() }
        }
        try { cm?.registerDefaultNetworkCallback(cb) } catch (_: Exception) {}
        onDispose {
            try { cm?.unregisterNetworkCallback(cb) } catch (_: Exception) {}
        }
    }
    return offline
}
