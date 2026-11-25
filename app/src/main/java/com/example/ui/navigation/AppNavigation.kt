package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.firebase.FirestoreProvider
import com.example.core.firebase.authStateFlow
import com.example.core.notifications.AppNotificationHelper
import com.example.core.security.AdminSecurity
import com.example.data.repository.AdminFirestoreRepository
import com.example.data.repository.RealRewardFirestoreRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.ProofStatus
import com.example.domain.model.RemovedAccountDoc
import com.example.domain.model.TransactionStatus
import com.example.domain.model.UiState
import com.example.features.admin.AdminLockScreen
import com.example.features.admin.AdminPanelScreen
import com.example.features.auth.AccountRegistrationScreen
import com.example.features.auth.AuthLandingScreen
import com.example.features.auth.GoogleAuthHelper
import com.example.features.dashboard.DashboardScreen
import com.example.features.leaderboard.LeaderboardScreen
import com.example.features.legal.LegalInfoScreen
import com.example.features.notifications.NotificationsScreen
import com.example.features.profile.ProfileScreen
import com.example.features.support.SupportCenterScreen
import com.example.features.tasks.TaskDetailsScreen
import com.example.features.tasks.TasksScreen
import com.example.features.vip.VipMembershipScreen
import com.example.features.wallet.WalletScreen
import com.example.features.wallet.WithdrawalScreen
import com.example.ui.components.AccountRemovedScreen
import com.example.ui.components.AppMaintenanceFloatingOverlay
import com.example.ui.components.BannedAccountScreen
import com.example.ui.components.DeviceAlreadyBoundScreen
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.i18n.BnStrings
import com.example.ui.i18n.EnStrings
import com.example.ui.i18n.LanguagePreferenceStore
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RealRewardTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val languageStore = remember(context) { LanguagePreferenceStore(context.applicationContext) }
    val language by languageStore.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.BANGLA)

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        AppNotificationHelper.ensureChannels(context)
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val activeStrings = if (language == AppLanguage.BANGLA) BnStrings else EnStrings

    CompositionLocalProvider(LocalStrings provides activeStrings) {
        RealRewardTheme {
            val user = currentUser
            if (user == null) {
                UnauthenticatedShell(
                    language = language,
                    onToggleLanguage = {
                        scope.launch {
                            val next = if (language == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
                            languageStore.setLanguage(next)
                        }
                    },
                    onAuthSuccess = {}
                )
            } else {
                key(user.uid) {
                    AuthenticatedShell(
                        firebaseUser = user,
                        language = language,
                        languageStore = languageStore,
                        onSignOut = {
                            GoogleAuthHelper.signOut(credentialManager, scope)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnauthenticatedShell(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    var legalSection by remember { mutableStateOf<String?>(null) }
    if (legalSection != null) {
        BackHandler { legalSection = null }
        LegalInfoScreen(
            initialSection = legalSection!!,
            language = language,
            onToggleLanguage = onToggleLanguage,
            onBack = { legalSection = null }
        )
    } else {
        AuthLandingScreen(
            language = language,
            onToggleLanguage = onToggleLanguage,
            onAuthSuccess = onAuthSuccess,
            onOpenLegal = { legalSection = it }
        )
    }
}

@Composable
private fun AuthenticatedShell(
    firebaseUser: FirebaseUser,
    language: AppLanguage,
    languageStore: LanguagePreferenceStore,
    onSignOut: () -> Unit
) {
    val uid = firebaseUser.uid
    val mainViewModel: MainViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirestoreProvider.getDatabase(app)
                MainViewModel(
                    repository = RealRewardFirestoreRepository(db),
                    languageStore = languageStore,
                    currentUserId = uid
                )
            }
        }
    )

    val userAccountState by mainViewModel.userAccountState.collectAsStateWithLifecycle()
    val tasksState by mainViewModel.tasksState.collectAsStateWithLifecycle()
    val proofsState by mainViewModel.myProofsState.collectAsStateWithLifecycle()
    val transactionsState by mainViewModel.myTransactionsState.collectAsStateWithLifecycle()
    val withdrawalsState by mainViewModel.myWithdrawalsState.collectAsStateWithLifecycle()
    val vipRequestsState by mainViewModel.myVipRequestsState.collectAsStateWithLifecycle()
    val ticketsState by mainViewModel.mySupportTicketsState.collectAsStateWithLifecycle()
    val announcementsState by mainViewModel.announcementsState.collectAsStateWithLifecycle()
    val leaderboardState by mainViewModel.leaderboardState.collectAsStateWithLifecycle()
    val appConfig by mainViewModel.paymentConfigState.collectAsStateWithLifecycle()
    val isAppConfigReady by mainViewModel.isAppConfigReady.collectAsStateWithLifecycle()
    val notificationSoundEnabled by mainViewModel.notificationSoundEnabled.collectAsStateWithLifecycle()
    val isSubmitting by mainViewModel.isSubmitting.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var registrationLegal by remember { mutableStateOf<String?>(null) }
    var isAdminUnlocked by remember(uid) { mutableStateOf(false) }

    when (val accountResult = userAccountState) {
        is UiState.Loading -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) { LoadingStateView() }
            }
        }
        is UiState.Error -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ErrorStateView(message = accountResult.message, onRetry = { mainViewModel.retryAllStreams() })
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onSignOut) { Text(LocalStrings.current.logout) }
                }
            }
        }
        is UiState.Success -> {
            val userAccount = accountResult.data
            val currentDeviceId = remember(context) { AdminSecurity.getDeviceRegistryId(context) }
            val isOwnerUser = AdminSecurity.isVerifiedOwner(firebaseUser)

            var removedDoc by remember(uid) { mutableStateOf<RemovedAccountDoc?>(null) }
            var checkedRemoval by remember(uid) { mutableStateOf(false) }
            LaunchedEffect(uid, userAccount == null) {
                if (!isOwnerUser && userAccount == null) {
                    removedDoc = mainViewModel.checkRemovedAccount(uid, firebaseUser.email.orEmpty())
                }
                checkedRemoval = true
            }

            if (!isOwnerUser && !checkedRemoval && userAccount == null) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) { LoadingStateView() }
                }
                return
            }

            if (!isOwnerUser && removedDoc != null) {
                AccountRemovedScreen(reason = removedDoc?.reason, onSignOut = onSignOut)
                return
            }

            if (!isOwnerUser && !isAppConfigReady) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) { LoadingStateView() }
                }
                return
            }

            var maintenanceNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(appConfig.appStopped, appConfig.stopEndEpochMillis) {
                maintenanceNow = System.currentTimeMillis()
                if (appConfig.appStopped && appConfig.stopEndEpochMillis > 0L) {
                    while (true) {
                        kotlinx.coroutines.delay(1000L)
                        maintenanceNow = System.currentTimeMillis()
                    }
                }
            }
            val isMaintenanceActive = !isOwnerUser && appConfig.appStopped &&
                (appConfig.stopEndEpochMillis == 0L || maintenanceNow < appConfig.stopEndEpochMillis)

            if (userAccount == null) {
                var deviceBoundOwner by remember(uid, currentDeviceId) { mutableStateOf<String?>(null) }
                var checkingDevice by remember(uid, currentDeviceId) { mutableStateOf(true) }
                LaunchedEffect(uid, currentDeviceId) {
                    checkingDevice = true
                    deviceBoundOwner = mainViewModel.checkDeviceRegistryOwner(currentDeviceId)
                    checkingDevice = false
                }

                if (checkingDevice) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) { LoadingStateView() }
                    }
                    return
                }
                if (deviceBoundOwner != null && !isOwnerUser) {
                    DeviceAlreadyBoundScreen(onSignOut = onSignOut)
                    return
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (registrationLegal != null && !isMaintenanceActive) {
                        BackHandler { registrationLegal = null }
                        LegalInfoScreen(
                            initialSection = registrationLegal!!,
                            language = language,
                            onToggleLanguage = { mainViewModel.toggleLanguage() },
                            onBack = { registrationLegal = null }
                        )
                    } else {
                        AccountRegistrationScreen(
                            firebaseUser = firebaseUser,
                            language = language,
                            isSubmitting = isSubmitting,
                            onToggleLanguage = { mainViewModel.toggleLanguage() },
                            onRegisterSubmit = { ctx, name, username, mobile, deviceId, referralCode, password, cb ->
                                mainViewModel.registerNewAccount(ctx, name, username, mobile, deviceId, referralCode, password, cb)
                            },
                            onOpenTerms = { registrationLegal = "terms" },
                            onSwitchGoogleAccount = onSignOut
                        )
                    }
                    if (isMaintenanceActive) {
                        AppMaintenanceFloatingOverlay(
                            appConfig = appConfig,
                            language = language
                        )
                    }
                }
                return
            }

            if (!isOwnerUser && userAccount.deviceId != currentDeviceId) {
                DeviceAlreadyBoundScreen(onSignOut = onSignOut)
                return
            }

            if (userAccount.banned) {
                BannedAccountScreen(banReason = userAccount.banReason, onSignOut = onSignOut)
                return
            }

            // Live IP, Country & VPN Security Tracking + Auto-Ban Enforcement
            LaunchedEffect(uid, appConfig.vpnAutoBanEnabled, appConfig.foreignIpAutoBanEnabled) {
                mainViewModel.inspectAndEnforceNetworkSecurity(
                    context = context,
                    user = userAccount,
                    config = appConfig,
                    isOwner = isOwnerUser
                )
            }

            // Runtime POST_NOTIFICATIONS request + live Firestore alert/sound dispatcher
            var promptedNotifPermission by remember(uid) { mutableStateOf(false) }
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }
            LaunchedEffect(uid) {
                if (!promptedNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !AppNotificationHelper.hasNotificationPermission(context)
                ) {
                    promptedNotifPermission = true
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            var initializedAnnouncements by remember(uid) { mutableStateOf(false) }
            val seenAnnouncementIds = remember(uid) { mutableSetOf<String>() }
            LaunchedEffect(announcementsState) {
                val list = (announcementsState as? UiState.Success)?.data ?: return@LaunchedEffect
                if (!initializedAnnouncements) {
                    list.forEach { seenAnnouncementIds.add(it.id) }
                    initializedAnnouncements = true
                } else {
                    list.filter { it.id !in seenAnnouncementIds }.forEach { notice ->
                        seenAnnouncementIds.add(notice.id)
                        AppNotificationHelper.showSystemNotification(
                            context = context,
                            notificationId = notice.id.hashCode(),
                            title = notice.title(language),
                            message = notice.message(language),
                            withSound = notificationSoundEnabled
                        )
                    }
                }
            }

            var initializedProofs by remember(uid) { mutableStateOf(false) }
            val knownProofStatuses = remember(uid) { mutableMapOf<String, String>() }
            LaunchedEffect(proofsState) {
                val list = (proofsState as? UiState.Success)?.data ?: return@LaunchedEffect
                if (!initializedProofs) {
                    list.forEach { knownProofStatuses[it.id] = it.status }
                    initializedProofs = true
                } else {
                    list.forEach { proof ->
                        val prev = knownProofStatuses.put(proof.id, proof.status)
                        if (prev != null && prev != proof.status &&
                            (proof.statusEnum() == ProofStatus.APPROVED || proof.statusEnum() == ProofStatus.REJECTED)
                        ) {
                            AppNotificationHelper.showSystemNotification(
                                context = context,
                                notificationId = proof.id.hashCode(),
                                title = proof.taskTitle,
                                message = "${proof.statusEnum().label.get(language)} • ${AdminSecurity.formatBdt(proof.rewardBdt)}",
                                withSound = notificationSoundEnabled
                            )
                        }
                    }
                }
            }

            var initializedWithdrawals by remember(uid) { mutableStateOf(false) }
            val knownWithdrawalStatuses = remember(uid) { mutableMapOf<String, String>() }
            LaunchedEffect(withdrawalsState) {
                val list = (withdrawalsState as? UiState.Success)?.data ?: return@LaunchedEffect
                if (!initializedWithdrawals) {
                    list.forEach { knownWithdrawalStatuses[it.id] = it.status }
                    initializedWithdrawals = true
                } else {
                    list.forEach { w ->
                        val prev = knownWithdrawalStatuses.put(w.id, w.status)
                        if (prev != null && prev != w.status &&
                            (w.statusEnum() == TransactionStatus.APPROVED ||
                                w.statusEnum() == TransactionStatus.COMPLETED ||
                                w.statusEnum() == TransactionStatus.FAILED)
                        ) {
                            AppNotificationHelper.showSystemNotification(
                                context = context,
                                notificationId = w.id.hashCode(),
                                title = "${w.methodEnum().shortName} ${AdminSecurity.formatBdt(w.amountBdt)}",
                                message = w.statusEnum().label.get(language),
                                withSound = notificationSoundEnabled
                            )
                        }
                    }
                }
            }

            // Keep ranking leaderboard_public in live parity with Wallet available balance & lifetime earnings
            LaunchedEffect(
                userAccount.availableBalanceBdt,
                userAccount.totalEarningsBdt,
                userAccount.completedTasksCount,
                userAccount.vipTier
            ) {
                mainViewModel.syncLeaderboardIfNeeded(userAccount)
            }

            // Live balance credit sound & notification alert
            val strings = LocalStrings.current
            var previousAvailableBalance by remember(uid) { mutableStateOf<Double?>(null) }
            LaunchedEffect(userAccount.availableBalanceBdt) {
                val prev = previousAvailableBalance
                val current = userAccount.availableBalanceBdt
                previousAvailableBalance = current
                if (prev != null && current - prev >= 0.5) {
                    val delta = current - prev
                    AppNotificationHelper.showSystemNotification(
                        context = context,
                        notificationId = ("bal_${uid}_${current}").hashCode(),
                        title = strings.balanceUpdatedAlertTitle,
                        message = "+${AdminSecurity.formatBdt(delta)} • ${strings.leaderboardWalletLabel}: ${AdminSecurity.formatBdt(current)}",
                        withSound = notificationSoundEnabled
                    )
                }
            }

            // Live support reply notification + sound alert
            var initializedTickets by remember(uid) { mutableStateOf(false) }
            val knownTicketReplies = remember(uid) { mutableMapOf<String, String>() }
            LaunchedEffect(ticketsState) {
                val list = (ticketsState as? UiState.Success)?.data ?: return@LaunchedEffect
                if (!initializedTickets) {
                    list.forEach { knownTicketReplies[it.id] = it.adminReply.orEmpty() }
                    initializedTickets = true
                } else {
                    list.forEach { ticket ->
                        val reply = ticket.adminReply.orEmpty().trim()
                        val prevReply = knownTicketReplies.put(ticket.id, reply)
                        if (reply.isNotEmpty() && reply != prevReply.orEmpty()) {
                            AppNotificationHelper.showSystemNotification(
                                context = context,
                                notificationId = ("sup_${ticket.id}_${reply.length}").hashCode(),
                                title = "${strings.supportTitle}: ${ticket.subject}",
                                message = reply,
                                withSound = notificationSoundEnabled
                            )
                        }
                    }
                }
            }

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val bottomTabs = listOf(
                BottomNavItem(Routes.DASHBOARD, strings.navHome, Icons.Default.Home),
                BottomNavItem(Routes.TASKS, strings.navTasks, Icons.Default.TaskAlt),
                BottomNavItem(Routes.WALLET, strings.navWallet, Icons.Default.AccountBalanceWallet),
                BottomNavItem(Routes.VIP, strings.navVip, Icons.Default.WorkspacePremium),
                BottomNavItem(Routes.PROFILE, strings.navProfile, Icons.Default.Person)
            )
            val showTopLevelNav = currentRoute in bottomTabs.map { it.route }

            val navigateTopTab: (String) -> Unit = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (showTopLevelNav && useRail) {
                            NavigationRail {
                                Spacer(Modifier.height(12.dp))
                                bottomTabs.forEach { tab ->
                                    NavigationRailItem(
                                        selected = currentRoute == tab.route,
                                        onClick = { navigateTopTab(tab.route) },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier.weight(1f),
                            bottomBar = {
                                if (showTopLevelNav && !useRail) {
                                    NavigationBar {
                                        bottomTabs.forEach { tab ->
                                            NavigationBarItem(
                                                selected = currentRoute == tab.route,
                                                onClick = { navigateTopTab(tab.route) },
                                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                label = { Text(tab.label) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = EmeraldPrimary,
                                                    selectedTextColor = EmeraldPrimary
                                                ),
                                                modifier = Modifier.testTag("bottom_nav_${tab.route}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Routes.DASHBOARD,
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                composable(Routes.DASHBOARD) {
                                    DashboardScreen(
                                        firebaseUser = firebaseUser,
                                        user = userAccount,
                                        language = language,
                                        tasksState = tasksState,
                                        proofsState = proofsState,
                                        transactionsState = transactionsState,
                                        announcementsState = announcementsState,
                                        appConfig = appConfig,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onRetry = { mainViewModel.retryAllStreams() },
                                        onNavigateWithdraw = { navController.navigate(Routes.WITHDRAWAL) },
                                        onNavigateLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                                        onNavigateNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                                        onNavigateSupport = { navController.navigate(Routes.SUPPORT) },
                                        onNavigateAllTasks = { navigateTopTab(Routes.TASKS) },
                                        onNavigateWallet = { navigateTopTab(Routes.WALLET) },
                                        onOpenTask = { navController.navigate(Routes.taskDetails(it)) },
                                        onOpenOwnerAdmin = {
                                            if (AdminSecurity.isVerifiedOwner(firebaseUser)) {
                                                navController.navigate(
                                                    if (isAdminUnlocked) Routes.ADMIN_PANEL else Routes.ADMIN_LOCK
                                                )
                                            }
                                        }
                                    )
                                }

                                composable(Routes.TASKS) {
                                    TasksScreen(
                                        language = language,
                                        tasksState = tasksState,
                                        proofsState = proofsState,
                                        appConfig = appConfig,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onRetry = { mainViewModel.retryAllStreams() },
                                        onOpenTask = { navController.navigate(Routes.taskDetails(it)) },
                                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                                        userVipTier = userAccount.vipTierEnum()
                                    )
                                }

                                composable(
                                    route = Routes.TASK_DETAILS,
                                    arguments = listOf(navArgument("taskId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
                                    val task = ((tasksState as? UiState.Success)?.data.orEmpty()).firstOrNull { it.id == taskId }
                                    val proof = ((proofsState as? UiState.Success)?.data.orEmpty()).firstOrNull { it.taskId == taskId }
                                    TaskDetailsScreen(
                                        task = task,
                                        user = userAccount,
                                        existingProof = proof,
                                        language = language,
                                        isSubmitting = isSubmitting,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onBack = { navController.popBackStack() },
                                        onSubmitProof = { t, u, note, attach, cb ->
                                            mainViewModel.submitTaskProof(t, u, note, attach, cb)
                                        }
                                    )
                                }

                                composable(Routes.WALLET) {
                                    WalletScreen(
                                        user = userAccount,
                                        language = language,
                                        transactionsState = transactionsState,
                                        isSubmitting = isSubmitting,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onRetry = { mainViewModel.retryAllStreams() },
                                        onNavigateWithdrawal = { navController.navigate(Routes.WITHDRAWAL) },
                                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                                        supportEnabled = appConfig.supportEnabled,
                                        onNavigateSupport = { navController.navigate(Routes.SUPPORT) },
                                        onSetupPaymentAccounts = { bkash, nagad, cb ->
                                            mainViewModel.updatePaymentAccounts(bkash, nagad, cb)
                                        }
                                    )
                                }

                                composable(Routes.WITHDRAWAL) {
                                    WithdrawalScreen(
                                        user = userAccount,
                                        language = language,
                                        withdrawalsState = withdrawalsState,
                                        isSubmitting = isSubmitting,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onBack = { navController.popBackStack() },
                                        onRetry = { mainViewModel.retryAllStreams() },
                                        onGoToProfileSetup = { navigateTopTab(Routes.PROFILE) },
                                        onSubmitWithdrawal = { u, m, acc, amt, cb ->
                                            mainViewModel.submitWithdrawalRequest(u, m, acc, amt, cb)
                                        }
                                    )
                                }

                                composable(Routes.VIP) {
                                    VipMembershipScreen(
                                        user = userAccount,
                                        appConfig = appConfig,
                                        vipRequestsState = vipRequestsState,
                                        language = language,
                                        isSubmitting = isSubmitting,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onRetry = { mainViewModel.retryAllStreams() },
                                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                                        onSubmitVipRequest = { u, tier, method, sender, trx, amt, cb ->
                                            mainViewModel.submitVipUpgradeRequest(u, tier, method, sender, trx, amt, cb)
                                        }
                                    )
                                }

                                composable(Routes.LEADERBOARD) {
                                    LeaderboardScreen(
                                        language = language,
                                        leaderboardState = leaderboardState,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onBack = { navController.popBackStack() },
                                        onRetry = { mainViewModel.retryAllStreams() }
                                    )
                                }

                                composable(Routes.PROFILE) {
                                    ProfileScreen(
                                        firebaseUser = firebaseUser,
                                        user = userAccount,
                                        appConfig = appConfig,
                                        language = language,
                                        notificationSoundEnabled = notificationSoundEnabled,
                                        isSubmitting = isSubmitting,
                                        onToggleNotificationSound = { mainViewModel.setNotificationSoundEnabled(it) },
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                                        onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                                        onOpenLegal = { navController.navigate(Routes.legal(it)) },
                                        onUpdateProfile = { name, username, cb ->
                                            mainViewModel.updateProfileInfo(name, username, cb)
                                        },
                                        onUpdatePaymentAccounts = { bkash, nagad, cb ->
                                            mainViewModel.updatePaymentAccounts(bkash, nagad, cb)
                                        },
                                        onChangePassword = { oldPw, newPw, cb ->
                                            mainViewModel.changeSecurityPassword(oldPw, newPw, cb)
                                        },
                                        onUpdateSecurityToggles = { twoFa, alerts, cb ->
                                            mainViewModel.updateSecurityToggles(twoFa, alerts, cb)
                                        },
                                        onOpenOwnerAdmin = {
                                            if (AdminSecurity.isVerifiedOwner(firebaseUser)) {
                                                navController.navigate(
                                                    if (isAdminUnlocked) Routes.ADMIN_PANEL else Routes.ADMIN_LOCK
                                                )
                                            }
                                        },
                                        onLogout = onSignOut
                                    )
                                }

                                composable(Routes.NOTIFICATIONS) {
                                    NotificationsScreen(
                                        language = language,
                                        announcementsState = announcementsState,
                                        appConfig = appConfig,
                                        notificationSoundEnabled = notificationSoundEnabled,
                                        onToggleNotificationSound = { mainViewModel.setNotificationSoundEnabled(it) },
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onBack = { navController.popBackStack() },
                                        onRetry = { mainViewModel.retryAllStreams() }
                                    )
                                }

                                composable(Routes.SUPPORT) {
                                    SupportCenterScreen(
                                        user = userAccount,
                                        language = language,
                                        ticketsState = ticketsState,
                                        isSubmitting = isSubmitting,
                                        supportEnabled = appConfig.supportEnabled,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onBack = { navController.popBackStack() },
                                        onRetry = { mainViewModel.retryAllStreams() },
                                        onSubmitTicket = { u, cat, subj, msg, attach, cb ->
                                            mainViewModel.submitSupportTicket(u, cat, subj, msg, attach, cb)
                                        }
                                    )
                                }

                                composable(
                                    route = Routes.LEGAL,
                                    arguments = listOf(navArgument("section") { type = NavType.StringType })
                                ) { backStack ->
                                    val sec = backStack.arguments?.getString("section") ?: "about"
                                    LegalInfoScreen(
                                        initialSection = sec,
                                        language = language,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(Routes.ADMIN_LOCK) {
                                    AdminLockScreen(
                                        firebaseUser = firebaseUser,
                                        language = language,
                                        onToggleLanguage = { mainViewModel.toggleLanguage() },
                                        onBack = { navController.popBackStack() },
                                        onUnlocked = {
                                            if (AdminSecurity.isVerifiedOwner(firebaseUser)) {
                                                isAdminUnlocked = true
                                                navController.navigate(Routes.ADMIN_PANEL) {
                                                    popUpTo(Routes.ADMIN_LOCK) { inclusive = true }
                                                }
                                            }
                                        }
                                    )
                                }

                                composable(Routes.ADMIN_PANEL) {
                                    if (!AdminSecurity.isVerifiedOwner(firebaseUser) || !isAdminUnlocked) {
                                        LaunchedEffect(Unit) { navController.popBackStack() }
                                    } else {
                                        val adminViewModel: AdminViewModel = viewModel(
                                            key = "owner_admin_$uid",
                                            factory = viewModelFactory {
                                                initializer {
                                                    val app = checkNotNull(this[APPLICATION_KEY])
                                                    val db = FirestoreProvider.getDatabase(app)
                                                    AdminViewModel(AdminFirestoreRepository(db))
                                                }
                                            }
                                        )
                                        AdminPanelScreen(
                                            adminViewModel = adminViewModel,
                                            language = language,
                                            onToggleLanguage = { mainViewModel.toggleLanguage() },
                                            onBack = { navController.popBackStack() },
                                            onLockAdmin = {
                                                isAdminUnlocked = false
                                                navController.popBackStack()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isMaintenanceActive) {
                    AppMaintenanceFloatingOverlay(
                        appConfig = appConfig,
                        language = language
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
