package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.auth.attemptAutoSignIn
import com.example.core.i18n.AppStrings
import com.example.data.catalog.ToolCatalog
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.ToolCategory
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.BharatToolsRepository
import com.example.ui.components.BadgeTone
import com.example.ui.components.BharatBadge
import com.example.ui.components.BharatCard
import com.example.ui.components.categoryIcon
import com.example.ui.components.rememberReducedMotion
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ProfileAndPurchaseScreen
import com.example.ui.screens.SavedAndHistoryScreen
import com.example.ui.screens.tools.ImageToolkitScreen
import com.example.ui.screens.tools.PdfToolkitScreen
import com.example.ui.screens.tools.QrToolkitScreen
import com.example.ui.screens.tools.TextToolkitScreen
import com.example.ui.screens.tools.UtilityToolsScreen
import com.example.ui.theme.BharatTheme
import com.example.ui.theme.BharatToolsTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            BharatToolsAppRoot()
        }
    }
}

@Composable
fun BharatToolsAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var startupChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (!startupChecked) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    startupChecked = true
                },
                onUnauthenticated = {
                    startupChecked = true
                },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (!startupChecked) {
        BharatToolsTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    } else if (user == null) {
        BharatToolsTheme {
            AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
        }
    } else {
        val uid = user.uid
        key(uid) {
            BharatToolsAuthenticatedShell(
                currentUserId = uid,
                onSignedOut = { currentUser = Firebase.auth.currentUser }
            )
        }
    }
}

private const val ROUTE_HOME = "home"
private const val ROUTE_CATEGORIES = "categories"
private const val ROUTE_SAVED = "saved"
private const val ROUTE_ACCOUNT = "account"
private const val ROUTE_ADMIN = "admin"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatToolsAuthenticatedShell(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val mainViewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MainViewModel(BharatToolsRepository(db), currentUserId, LocalPreferencesStore(app))
            }
        }
    )

    val profileState by mainViewModel.profileState.collectAsStateWithLifecycle()
    val historyState by mainViewModel.historyState.collectAsStateWithLifecycle()
    val purchasesState by mainViewModel.purchasesState.collectAsStateWithLifecycle()
    val ticketsState by mainViewModel.ticketsState.collectAsStateWithLifecycle()
    val announcementsState by mainViewModel.announcementsState.collectAsStateWithLifecycle()
    val appConfig by mainViewModel.configState.collectAsStateWithLifecycle()
    val isAdmin by mainViewModel.isAdmin.collectAsStateWithLifecycle()
    val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by mainViewModel.selectedCategory.collectAsStateWithLifecycle()
    val recentSearches by mainViewModel.recentSearches.collectAsStateWithLifecycle()
    val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data ?: UserProfile(userId = currentUserId, displayName = "Bharat User")
    val history = (historyState as? UiState.Success)?.data ?: emptyList()
    val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()
    val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()
    val announcements = (announcementsState as? UiState.Success)?.data ?: emptyList()
    val isHindi = profile.preferredLanguage == "hi"
    val strings = AppStrings.forLang(profile.preferredLanguage)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val topLevelRoutes = listOf(ROUTE_HOME, ROUTE_CATEGORIES, ROUTE_SAVED, ROUTE_ACCOUNT)
    val showMainNav = currentRoute in topLevelRoutes
    val reducedMotion = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showQuickLaunchDialog by remember { mutableStateOf(false) }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(ROUTE_HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showQuickLaunchDialog) {
        val quickTools = remember {
            listOfNotNull(
                ToolCatalog.byId("pdf_merge"),
                ToolCatalog.byId("img_compress"),
                ToolCatalog.byId("qr_upi"),
                ToolCatalog.byId("util_gst"),
                ToolCatalog.byId("txt_word_count")
            )
        }
        AlertDialog(
            onDismissRequest = { showQuickLaunchDialog = false },
            title = { Text("Quick Tool Launcher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickTools.forEach { tool ->
                        BharatCard(
                            onClick = {
                                showQuickLaunchDialog = false
                                navController.navigate("tool/${tool.category.id}/${tool.initialSubTab}")
                            }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(categoryIcon(tool.category), contentDescription = null, tint = tool.category.accentColor)
                                Spacer(Modifier.width(10.dp))
                                Text(if (isHindi) tool.titleHi else tool.titleEn, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickLaunchDialog = false }) { Text("Close") }
            }
        )
    }

    BharatToolsTheme(themeMode = profile.themeMode) {
        val tokens = BharatTheme.colors

        @Composable
        fun SidebarMenuContent(onCloseDrawer: () -> Unit) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF7C3AED)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(strings.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        BharatBadge(
                            text = if (profile.hasProAccess) "ACTIVE" else "₹399/MO",
                            tone = if (profile.hasProAccess) BadgeTone.SUCCESS else BadgeTone.PRIMARY
                        )
                    }
                }
                HorizontalDivider(color = tokens.border, modifier = Modifier.padding(vertical = 6.dp))

                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    selected = currentRoute == ROUTE_HOME,
                    onClick = { navigateTop(ROUTE_HOME); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("All Tools") },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    selected = currentRoute == ROUTE_CATEGORIES,
                    onClick = { navigateTop(ROUTE_CATEGORIES); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("PDF Tools") },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    selected = false,
                    onClick = { navController.navigate("tool/pdf/0"); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("Image Tools") },
                    icon = { Icon(Icons.Default.Image, contentDescription = null) },
                    selected = false,
                    onClick = { navController.navigate("tool/image/0"); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("Text Tools") },
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                    selected = false,
                    onClick = { navController.navigate("tool/text/0"); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("QR Tools") },
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                    selected = false,
                    onClick = { navController.navigate("tool/qr/0"); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("Calculators") },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    selected = false,
                    onClick = { navController.navigate("tool/utility/0"); onCloseDrawer() }
                )
                HorizontalDivider(color = tokens.border, modifier = Modifier.padding(vertical = 6.dp))
                NavigationDrawerItem(
                    label = { Text("Favorites") },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    selected = currentRoute == ROUTE_SAVED,
                    onClick = { navigateTop(ROUTE_SAVED); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("History") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    selected = false,
                    onClick = { navigateTop(ROUTE_SAVED); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("Membership") },
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null) },
                    selected = currentRoute == ROUTE_ACCOUNT,
                    onClick = { navigateTop(ROUTE_ACCOUNT); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { navigateTop(ROUTE_ACCOUNT); onCloseDrawer() }
                )
                NavigationDrawerItem(
                    label = { Text("Support") },
                    icon = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                    selected = false,
                    onClick = { navigateTop(ROUTE_ACCOUNT); onCloseDrawer() }
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isDesktop = maxWidth >= 840.dp
            val isTabletRail = maxWidth in 600.dp..839.dp

            @Composable
            fun MainScaffoldBody(showMenuButton: Boolean, showBottomBar: Boolean) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = tokens.background,
                    contentWindowInsets = if (showMainNav) WindowInsets.statusBars else WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (showMainNav && !isDesktop) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.appName, style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.width(8.dp))
                                        BharatBadge(
                                            text = if (profile.hasProAccess) "ACTIVE" else "FREE",
                                            tone = if (profile.hasProAccess) BadgeTone.SUCCESS else BadgeTone.PRIMARY
                                        )
                                    }
                                },
                                navigationIcon = {
                                    if (showMenuButton) {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            modifier = Modifier.testTag("mobile_drawer_btn")
                                        ) {
                                            Icon(Icons.Default.Menu, contentDescription = "Open sidebar menu")
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = tokens.surface)
                            )
                        }
                    },
                    floatingActionButton = {
                        if (showMainNav && !isDesktop) {
                            FloatingActionButton(
                                onClick = { showQuickLaunchDialog = true },
                                containerColor = tokens.secondary,
                                contentColor = Color.White,
                                modifier = Modifier.testTag("quick_action_fab")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = "Quick Tool Launcher")
                            }
                        }
                    },
                    bottomBar = {
                        if (showMainNav && showBottomBar) {
                            NavigationBar(
                                windowInsets = WindowInsets.navigationBars,
                                containerColor = tokens.surface
                            ) {
                                NavItemMeta.all(strings).forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTop(item.route) },
                                        icon = { Icon(if (selected) item.activeIcon else item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        modifier = Modifier.testTag("nav_${item.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (showMainNav && isTabletRail) {
                            NavigationRail(
                                containerColor = tokens.surface,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                NavItemMeta.all(strings).forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTop(item.route) },
                                        icon = { Icon(if (selected) item.activeIcon else item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            enterTransition = {
                                if (reducedMotion) EnterTransition.None else fadeIn(tween(180))
                            },
                            exitTransition = {
                                if (reducedMotion) ExitTransition.None else fadeOut(tween(140))
                            },
                            modifier = Modifier.weight(1f).fillMaxSize()
                        ) {
                            composable(ROUTE_HOME) {
                                HomeDashboardScreen(
                                    profile = profile,
                                    announcements = announcements,
                                    appConfig = appConfig,
                                    strings = strings,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    recentSearches = recentSearches,
                                    totalHistoryCount = history.size,
                                    onSearchChange = mainViewModel::updateSearch,
                                    onSubmitSearch = mainViewModel::recordRecentSearch,
                                    onClearRecentSearches = mainViewModel::clearRecentSearches,
                                    onSelectCategory = mainViewModel::selectCategory,
                                    onOpenTool = { tool ->
                                        navController.navigate("tool/${tool.category.id}/${tool.initialSubTab}")
                                    },
                                    onToggleFavorite = mainViewModel::toggleFavorite,
                                    onUpgradeClick = { navigateTop(ROUTE_ACCOUNT) }
                                )
                            }
                            composable(ROUTE_CATEGORIES) {
                                CategoriesScreen(
                                    isHindi = isHindi,
                                    strings = strings,
                                    onSelectCategoryToBrowse = { cat ->
                                        navController.navigate("tool/${cat.id}/0")
                                    }
                                )
                            }
                            composable(ROUTE_SAVED) {
                                SavedAndHistoryScreen(
                                    profile = profile,
                                    history = history,
                                    strings = strings,
                                    onOpenTool = { tool ->
                                        navController.navigate("tool/${tool.category.id}/${tool.initialSubTab}")
                                    },
                                    onToggleFavorite = mainViewModel::toggleFavorite,
                                    onClearHistory = mainViewModel::clearToolHistory
                                )
                            }
                            composable(ROUTE_ACCOUNT) {
                                ProfileAndPurchaseScreen(
                                    profile = profile,
                                    purchases = purchases,
                                    tickets = tickets,
                                    appConfig = appConfig,
                                    isAdmin = isAdmin,
                                    strings = strings,
                                    onSetLanguage = mainViewModel::setLanguage,
                                    onSetTheme = mainViewModel::setThemeMode,
                                    onSubmitSubscriptionVerification = mainViewModel::submitSubscriptionVerification,
                                    onCancelSubscription = mainViewModel::cancelActiveSubscription,
                                    onSetAutoRenewConsent = mainViewModel::setAutoRenewConsent,
                                    onRequestRefund = mainViewModel::requestRefund,
                                    onSubmitSupportTicket = mainViewModel::submitSupportTicket,
                                    onApplyReferral = mainViewModel::applyReferralCode,
                                    onOpenAdmin = { navController.navigate(ROUTE_ADMIN) },
                                    onSignedOut = onSignedOut
                                )
                            }
                            composable(ROUTE_ADMIN) {
                                val adminVm: AdminViewModel = viewModel(
                                    key = "admin_$currentUserId",
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                            AdminViewModel(BharatToolsRepository(db), currentUserId)
                                        }
                                    }
                                )
                                AdminDashboardScreen(
                                    adminViewModel = adminVm,
                                    currentConfig = appConfig,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "tool/{cat}/{subTab}",
                                arguments = listOf(
                                    navArgument("cat") { type = NavType.StringType },
                                    navArgument("subTab") { type = NavType.IntType }
                                )
                            ) { entry ->
                                val catId = entry.arguments?.getString("cat") ?: "pdf"
                                val subTab = entry.arguments?.getInt("subTab") ?: 0
                                val cat = ToolCategory.entries.firstOrNull { it.id == catId } ?: ToolCategory.PDF
                                when (cat) {
                                    ToolCategory.PDF -> PdfToolkitScreen(
                                        initialSubTab = subTab,
                                        isHindi = isHindi,
                                        hasProAccess = profile.hasProAccess,
                                        onNavigateToProPass = { navigateTop(ROUTE_ACCOUNT) },
                                        onToolCompleted = mainViewModel::recordToolRun,
                                        onBack = { navController.popBackStack() }
                                    )
                                    ToolCategory.IMAGE -> ImageToolkitScreen(
                                        initialSubTab = subTab,
                                        isHindi = isHindi,
                                        hasProAccess = profile.hasProAccess,
                                        onNavigateToProPass = { navigateTop(ROUTE_ACCOUNT) },
                                        onToolCompleted = mainViewModel::recordToolRun,
                                        onBack = { navController.popBackStack() }
                                    )
                                    ToolCategory.TEXT -> TextToolkitScreen(
                                        initialSubTab = subTab,
                                        isHindi = isHindi,
                                        onToolCompleted = mainViewModel::recordToolRun,
                                        onBack = { navController.popBackStack() }
                                    )
                                    ToolCategory.QR -> QrToolkitScreen(
                                        initialSubTab = subTab,
                                        isHindi = isHindi,
                                        hasProAccess = profile.hasProAccess,
                                        onNavigateToProPass = { navigateTop(ROUTE_ACCOUNT) },
                                        onToolCompleted = mainViewModel::recordToolRun,
                                        onBack = { navController.popBackStack() }
                                    )
                                    ToolCategory.UTILITY -> UtilityToolsScreen(
                                        initialSubTab = subTab,
                                        isHindi = isHindi,
                                        onToolCompleted = mainViewModel::recordToolRun,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isDesktop && showMainNav) {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(
                            modifier = Modifier.width(260.dp),
                            drawerContainerColor = tokens.surface
                        ) {
                            SidebarMenuContent(onCloseDrawer = {})
                        }
                    }
                ) {
                    MainScaffoldBody(showMenuButton = false, showBottomBar = false)
                }
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = showMainNav,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(280.dp),
                            drawerContainerColor = tokens.surface
                        ) {
                            SidebarMenuContent(onCloseDrawer = { scope.launch { drawerState.close() } })
                        }
                    }
                ) {
                    MainScaffoldBody(showMenuButton = true, showBottomBar = !isTabletRail)
                }
            }
        }
    }
}

private data class NavItemMeta(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
) {
    companion object {
        fun all(s: com.example.core.i18n.UiStrings) = listOf(
            NavItemMeta(ROUTE_HOME, s.navHome, Icons.Outlined.Home, Icons.Filled.Home),
            NavItemMeta(ROUTE_CATEGORIES, s.navCategories, Icons.Outlined.Category, Icons.Filled.Category),
            NavItemMeta(ROUTE_SAVED, s.navSaved, Icons.Outlined.StarOutline, Icons.Filled.Star),
            NavItemMeta(ROUTE_ACCOUNT, s.navAccount, Icons.Outlined.Person, Icons.Filled.Person)
        )
    }
}
