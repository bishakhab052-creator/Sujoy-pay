package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import com.example.data.repository.SujoyPayRepository
import com.example.ui.components.TransactionReceiptSheet
import com.example.ui.screens.AddMoneyScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReceiveQrScreen
import com.example.ui.screens.RechargeAndBillsScreen
import com.example.ui.screens.ScanAndPayScreen
import com.example.ui.screens.SendAndWithdrawScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SujoyPayTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.WalletViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val SCAN_PAY = "scan_pay"
    const val ADMIN = "admin"
    const val PROFILE = "profile"
    const val ADD_MONEY = "add_money"
    const val SEND_WITHDRAW = "send_withdraw/{isWithdraw}"
    const val RECEIVE_QR = "receive_qr"
    const val BILLS = "bills/{category}"

    fun sendWithdraw(isWithdraw: Boolean) = "send_withdraw/$isWithdraw"
    fun bills(category: String) = "bills/$category"
}

private data class TopNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopNavItems = listOf(
    TopNavItem(Routes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TopNavItem(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
    TopNavItem(Routes.SCAN_PAY, "Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "nav_scan"),
    TopNavItem(Routes.ADMIN, "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_admin"),
    TopNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SujoyPayAppRoot()
        }
    }
}

@Composable
fun SujoyPayAppRoot() {
    val context = LocalContext.current
    val repository = remember(context) { SujoyPayRepository(context.applicationContext) }

    val authViewModel: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY])
                AuthViewModel(repository)
            }
        }
    )

    val session by authViewModel.sessionState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isDark = session.darkModeOverride ?: isSystemInDarkTheme()

    SujoyPayTheme(darkTheme = isDark) {
        val activeUid = session.activeUserId
        if (activeUid.isNullOrBlank()) {
            AuthScreen(
                uiState = authUiState,
                onToggleSignUp = authViewModel::toggleAuthMode,
                onMobileChange = authViewModel::onMobileChange,
                onFullNameChange = authViewModel::onFullNameChange,
                onEmailChange = authViewModel::onEmailChange,
                onMpinChange = authViewModel::onMpinChange,
                onOtpChange = authViewModel::onOtpChange,
                onRequestOtp = authViewModel::requestOtp,
                onVerifyOtp = authViewModel::verifyOtpAndProceed,
                onResetOtp = authViewModel::resetOtpStep,
                onDemoLogin = authViewModel::loginSandboxDemo
            )
        } else {
            key(activeUid) {
                AuthenticatedAppShell(
                    userId = activeUid,
                    repository = repository,
                    hideBalance = session.hideBalance,
                    isDarkTheme = isDark,
                    onToggleDarkTheme = { authViewModel.setDarkMode(it) },
                    onLogout = { authViewModel.logout(activeUid) }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    userId: String,
    repository: SujoyPayRepository,
    hideBalance: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val walletViewModel: WalletViewModel = viewModel(
        key = "wallet_$userId",
        factory = viewModelFactory {
            initializer { WalletViewModel(repository, userId) }
        }
    )
    val adminViewModel: AdminViewModel = viewModel(
        key = "admin_$userId",
        factory = viewModelFactory {
            initializer { AdminViewModel(repository, userId) }
        }
    )

    val user by walletViewModel.user.collectAsStateWithLifecycle()
    val transactions by walletViewModel.transactions.collectAsStateWithLifecycle()
    val complaints by walletViewModel.complaints.collectAsStateWithLifecycle()
    val settings by walletViewModel.settings.collectAsStateWithLifecycle()
    val walletUiState by walletViewModel.uiState.collectAsStateWithLifecycle()

    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()
    val allUsers by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val allTxns by adminViewModel.allTransactions.collectAsStateWithLifecycle()
    val flaggedTxns by adminViewModel.flaggedTransactions.collectAsStateWithLifecycle()
    val allComplaints by adminViewModel.allComplaints.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(walletUiState.successMessage) {
        val msg = walletUiState.successMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            walletViewModel.clearBanners()
        }
    }

    val topLevelRoutes = remember { TopNavItems.map { it.route }.toSet() }
    val showChromeNav = currentRoute in topLevelRoutes

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (showChromeNav && !useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChromeNav && useRail) {
                    NavigationRail(
                        containerColor = Navy900,
                        contentColor = PureWhite
                    ) {
                        TopNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                user = user,
                                transactions = transactions,
                                hideBalance = hideBalance,
                                onToggleHideBalance = { walletViewModel.toggleBalanceVisibility(hideBalance) },
                                onNavigateAddMoney = { navController.navigate(Routes.ADD_MONEY) },
                                onNavigateSendMoney = {
                                    walletViewModel.clearScannedUpiTarget()
                                    navController.navigate(Routes.sendWithdraw(false))
                                },
                                onNavigateWithdraw = { navController.navigate(Routes.sendWithdraw(true)) },
                                onNavigateScanAndPay = { navigateTopLevel(Routes.SCAN_PAY) },
                                onNavigateReceiveQr = { navController.navigate(Routes.RECEIVE_QR) },
                                onNavigateBillCategory = { cat -> navController.navigate(Routes.bills(cat)) },
                                onNavigateHistory = { navigateTopLevel(Routes.HISTORY) },
                                onNavigateProfile = { navigateTopLevel(Routes.PROFILE) },
                                onSubmitDispute = walletViewModel::submitDisputeComplaint
                            )
                        }

                        composable(Routes.HISTORY) {
                            TransactionHistoryScreen(
                                transactions = transactions,
                                onSubmitDispute = walletViewModel::submitDisputeComplaint
                            )
                        }

                        composable(Routes.SCAN_PAY) {
                            ScanAndPayScreen(
                                onBack = { navigateTopLevel(Routes.DASHBOARD) },
                                onUpiPayloadScanned = { parsed ->
                                    walletViewModel.setScannedUpiTarget(
                                        handle = parsed.payeeAddress,
                                        name = parsed.payeeName,
                                        amount = parsed.amountRupees.orEmpty(),
                                        note = parsed.note.orEmpty()
                                    )
                                    navController.navigate(Routes.sendWithdraw(false))
                                }
                            )
                        }

                        composable(Routes.ADMIN) {
                            AdminPanelScreen(
                                currentUser = user,
                                adminState = adminState,
                                users = allUsers,
                                transactions = allTxns,
                                flaggedTransactions = flaggedTxns,
                                complaints = allComplaints,
                                settings = settings,
                                auditLogs = auditLogs,
                                onUnlockAdmin = { pin -> adminViewModel.unlockAdminConsole(user, pin) },
                                onLockAdmin = adminViewModel::lockAdminConsole,
                                onToggleFreeze = adminViewModel::toggleUserFreeze,
                                onUpdateKyc = adminViewModel::updateUserKyc,
                                onUpdateSettings = adminViewModel::updateSettings,
                                onResolveComplaint = adminViewModel::resolveComplaint,
                                onResolveFraud = adminViewModel::resolveFraudFlag
                            )
                        }

                        composable(Routes.PROFILE) {
                            ProfileScreen(
                                user = user,
                                complaints = complaints,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                                onUpdateBank = walletViewModel::updateBankDetails,
                                onUpdateMpin = walletViewModel::updateMpin,
                                onLogout = onLogout
                            )
                        }

                        composable(Routes.ADD_MONEY) {
                            AddMoneyScreen(
                                user = user,
                                settings = settings,
                                uiState = walletUiState,
                                onBack = { navController.popBackStack() },
                                onInitiateAddMoney = walletViewModel::initiateAddMoney,
                                onGatewayLaunchNote = walletViewModel::setGatewayLaunchNote,
                                onCompleteVerification = walletViewModel::completeAddMoneyVerification,
                                onDismissOrder = walletViewModel::dismissActiveGatewayOrder
                            )
                        }

                        composable(
                            route = Routes.SEND_WITHDRAW,
                            arguments = listOf(navArgument("isWithdraw") { type = NavType.BoolType })
                        ) { entry ->
                            val isWithdraw = entry.arguments?.getBoolean("isWithdraw") ?: false
                            SendAndWithdrawScreen(
                                initialTabIsWithdraw = isWithdraw,
                                user = user,
                                uiState = walletUiState,
                                onBack = { navController.popBackStack() },
                                onOpenScanner = { navigateTopLevel(Routes.SCAN_PAY) },
                                onSendMoney = { handle, name, amount, note, mpin ->
                                    walletViewModel.sendMoney(handle, name, amount, note, mpin)
                                },
                                onWithdrawMoney = walletViewModel::withdrawMoney
                            )
                        }

                        composable(Routes.RECEIVE_QR) {
                            ReceiveQrScreen(
                                user = user,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Routes.BILLS,
                            arguments = listOf(navArgument("category") { type = NavType.StringType })
                        ) { entry ->
                            val category = entry.arguments?.getString("category") ?: "MOBILE_RECHARGE"
                            RechargeAndBillsScreen(
                                initialCategoryCode = category,
                                user = user,
                                uiState = walletUiState,
                                onBack = { navController.popBackStack() },
                                onPayBill = walletViewModel::payBillOrRecharge
                            )
                        }
                    }
                }
            }
        }
    }

    walletUiState.latestCompletedTxn?.let { txn ->
        TransactionReceiptSheet(
            txn = txn,
            onDismiss = walletViewModel::dismissCompletedReceipt,
            onSubmitDispute = { cat, desc, refund ->
                walletViewModel.submitDisputeComplaint(txn, cat, desc, refund)
            }
        )
    }
}
